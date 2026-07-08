// ==UserScript==
// @name         Redirect Blocker Hardened Full
// @namespace    via-browser
// @version      1.4
// @description  Hardened redirect blocker: document.write/open sanitization, overlay neutralizer, strict validation, history/anchor/form interception, meta refresh removal, per-site whitelist UI.
// @author       You
// @match        *://*/*
// @grant        none
// @run-at       document-start
// ==/UserScript==

(function () {
  'use strict';

  // ---------- Config ----------
  const WHITELIST_KEY = 'rb_whitelist_v2';
  const LOG_KEY = WHITELIST_KEY + '_log';
  const GESTURE_WINDOW_MS = 1000;
  const OVERLAY_ATTR = 'data-rb-overlay';
  const MIN_COVER_RATIO = 0.6;
  const SCAN_INTERVAL = 300;

  // ---------- Utilities ----------
  function now() { return Date.now(); }
  function safeParseJSON(s, fallback) { try { return JSON.parse(s); } catch { return fallback; } }
  function canonicalHost(h) { return h ? String(h).toLowerCase().replace(/\.$/, '') : ''; }
  function sanitizeHostCandidate(h) {
    try { const u = new URL('http://' + h); return canonicalHost(u.hostname); } catch { return ''; }
  }

  // ---------- Whitelist ----------
  function loadWhitelist() {
    const raw = localStorage.getItem(WHITELIST_KEY);
    const arr = safeParseJSON(raw, []);
    if (!Array.isArray(arr)) return [];
    return arr.map(sanitizeHostCandidate).filter(Boolean);
  }
  function saveWhitelist(list) {
    const unique = Array.from(new Set(list.map(sanitizeHostCandidate).filter(Boolean)));
    try { localStorage.setItem(WHITELIST_KEY, JSON.stringify(unique)); } catch {}
  }
  function isWhitelistedHost(host) { return !!host && loadWhitelist().includes(canonicalHost(host)); }
  function addHostToWhitelist(host) { const h = sanitizeHostCandidate(host); if (!h) return; const list = loadWhitelist(); if (!list.includes(h)) { list.push(h); saveWhitelist(list); } }

  // ---------- Logging ----------
  function pushLog(entry) {
    try {
      const arr = safeParseJSON(localStorage.getItem(LOG_KEY), []);
      arr.push({ t: now(), ...entry });
      while (arr.length > 300) arr.shift();
      localStorage.setItem(LOG_KEY, JSON.stringify(arr));
    } catch {}
  }

  // ---------- URL validation ----------
  function resolveUrlObj(url) {
    try { return new URL(url, location.href); } catch { return null; }
  }
  function isSchemeAllowed(protocol) {
    const allowed = ['https:', 'http:', 'intent:', 'mailto:', 'tel:', 'blob:', 'data:', 'javascript:'];
    return allowed.includes(protocol);
  }
  function isAllowedNavigation(urlObj, opts = { allowSameHost: true }) {
    if (!urlObj) return true;
    if (urlObj.protocol === 'intent:') return true;
    if (!isSchemeAllowed(urlObj.protocol)) return false;
    if (opts.allowSameHost && canonicalHost(urlObj.hostname) === canonicalHost(location.hostname)) return true;
    if (isWhitelistedHost(urlObj.hostname)) return true;
    return false;
  }

  // ---------- Trusted gesture tracking ----------
  let lastTrustedGesture = 0;
  function markTrustedGesture(e) { if (e && e.isTrusted) lastTrustedGesture = now(); }
  function hadRecentTrustedGesture() { return (now() - lastTrustedGesture) <= GESTURE_WINDOW_MS; }
  window.addEventListener('click', markTrustedGesture, true);
  window.addEventListener('pointerup', markTrustedGesture, true);
  window.addEventListener('keydown', (e) => { if (e.isTrusted && (e.key === 'Enter' || e.key === ' ')) lastTrustedGesture = now(); }, true);

  // ---------- Block handling ----------
  let blockedAttempt = null;
  function handleBlocked(url, reason) {
    blockedAttempt = { url: url || '', reason: reason || '', time: now() };
    pushLog({ type: 'blocked', url: blockedAttempt.url, reason: blockedAttempt.reason });
    showUI(blockedAttempt.url);
  }

  // ---------- UI in closed Shadow DOM ----------
  const hostEl = document.createElement('div');
  const shadow = hostEl.attachShadow ? hostEl.attachShadow({ mode: 'closed' }) : hostEl;
  const style = document.createElement('style');
  style.textContent = `
    :host { all: initial; position: fixed; z-index: 2147483647; pointer-events: none; }
    #rb { pointer-events: all; position: fixed; right: 16px; bottom: 120px; background: rgba(18,18,18,0.96); color: #fff; font-family: system-ui, sans-serif; padding: 10px 12px; border-radius: 12px; box-shadow: 0 6px 24px rgba(0,0,0,0.6); display:flex; gap:8px; align-items:center; max-width:340px; transform: translateY(8px); opacity:0; transition:opacity .18s, transform .18s; }
    #rb.visible { opacity:1; transform:none; }
    .title { font-weight:700; font-size:13px; color:#ffdede; }
    .domain { font-size:12px; color:#ffdddd; max-width:160px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
    .actions { display:flex; gap:6px; margin-left:auto; }
    button { background:transparent; border:1px solid rgba(255,255,255,0.12); color:#fff; padding:6px 8px; border-radius:8px; cursor:pointer; font-weight:600; font-size:12px; }
    button.allow { background: rgba(0,120,0,0.9); border-color: rgba(0,160,0,0.9); }
    button.once { background: rgba(0,90,160,0.9); border-color: rgba(0,140,220,0.9); }
    button.dismiss { background: rgba(255,255,255,0.06); }
  `;
  shadow.appendChild(style);
  const container = document.createElement('div');
  container.id = 'rb';
  container.innerHTML = `
    <div style="display:flex;gap:8px;align-items:center;">
      <div style="font-size:16px;">🛡️</div>
      <div style="display:flex;flex-direction:column;min-width:0;">
        <div class="title">Redirect blocked</div>
        <div class="domain" id="rb-domain"></div>
      </div>
    </div>
    <div class="actions">
      <button class="once" id="rb-allow-once">Allow once</button>
      <button class="allow" id="rb-whitelist">Always allow</button>
      <button class="dismiss" id="rb-dismiss">Dismiss</button>
    </div>
  `;
  shadow.appendChild(container);
  (document.documentElement || document).appendChild(hostEl);

  function showUI(url) {
    try {
      const domainEl = shadow.querySelector ? shadow.querySelector('#rb-domain') : null;
      if (domainEl) {
        try { domainEl.textContent = (new URL(url)).hostname; } catch { domainEl.textContent = url; }
      }
      const rb = shadow.querySelector ? shadow.querySelector('#rb') : null;
      if (rb) rb.classList.add('visible');
    } catch {}
  }
  function hideUI() {
    try { const rb = shadow.querySelector ? shadow.querySelector('#rb') : null; if (rb) rb.classList.remove('visible'); } catch {}
    blockedAttempt = null;
  }

  (function wireButtons() {
    try {
      const allowOnce = shadow.querySelector('#rb-allow-once');
      const whitelistBtn = shadow.querySelector('#rb-whitelist');
      const dismiss = shadow.querySelector('#rb-dismiss');

      if (allowOnce) allowOnce.addEventListener('click', () => {
        if (!blockedAttempt || !blockedAttempt.url) { hideUI(); return; }
        const u = resolveUrlObj(blockedAttempt.url);
        if (u) { try { location.assign(u.href); } catch { location.href = u.href; } }
        hideUI();
      });

      if (whitelistBtn) whitelistBtn.addEventListener('click', () => {
        if (!blockedAttempt || !blockedAttempt.url) { hideUI(); return; }
        const u = resolveUrlObj(blockedAttempt.url);
        if (u) { addHostToWhitelist(u.hostname); try { location.assign(u.href); } catch { location.href = u.href; } }
        hideUI();
      });

      if (dismiss) dismiss.addEventListener('click', () => { hideUI(); });
    } catch {}
  })();

  // ---------- Early document.write/open sanitization ----------
  (function protectDocumentWrite() {
    try {
      const origWrite = Document.prototype.write;
      const origOpen = Document.prototype.open;

      function sanitizeHtml(html) {
        try {
          if (!html || typeof html !== 'string') return html;
          // Neutralize meta refresh by setting a very long delay and remove inline location assignments
          html = html.replace(/(<meta[^>]*http-equiv\s*=\s*["']?refresh[^>]*content\s*=\s*["']?)([^"'>]*)(["']?[^>]*>)/ig,
            (m, p1, content, p3) => {
              // keep url but set long delay
              const urlMatch = content.match(/url=(.*)$/i);
              const urlPart = urlMatch ? urlMatch[1] : '';
              return p1 + '99999; url=' + urlPart + p3;
            });
          // Remove or neutralize inline location assignments
          html = html.replace(/(top|window|location)\s*\.\s*(href|replace|assign)\s*=\s*([^;<>]+)/ig, '/*blocked-location*/');
          html = html.replace(/window\.open\s*\(/ig, '/*blocked-window-open*/(');
        } catch (e) {}
        return html;
      }

      Document.prototype.write = function () {
        try {
          const args = Array.from(arguments).map(sanitizeHtml);
          return origWrite.apply(this, args);
        } catch (e) { return origWrite.apply(this, arguments); }
      };

      Document.prototype.open = function () {
        try {
          const doc = origOpen.apply(this, arguments);
          if (doc && doc.write && doc.write !== Document.prototype.write) {
            const localWrite = doc.write.bind(doc);
            doc.write = function () {
              const args = Array.from(arguments).map(sanitizeHtml);
              return localWrite.apply(this, args);
            };
          }
          return doc;
        } catch (e) { return origOpen.apply(this, arguments); }
      };
    } catch (e) { pushLog({ type: 'protectDocumentWrite_error', error: String(e) }); }
  })();

  // ---------- Overlay detector and neutralizer ----------
  (function overlayProtector() {
    try {
      function isLargeOverlay(el) {
        if (!el || el.nodeType !== 1) return false;
        const style = window.getComputedStyle(el);
        if (!style) return false;
        if (!/(fixed|absolute|sticky)/i.test(style.position)) return false;
        if (style.pointerEvents === 'none') return false;
        const rect = el.getBoundingClientRect();
        if (rect.width <= 0 || rect.height <= 0) return false;
        const vw = window.innerWidth || document.documentElement.clientWidth;
        const vh = window.innerHeight || document.documentElement.clientHeight;
        const areaRatio = (rect.width * rect.height) / (vw * vh);
        const coversCenter = rect.left <= vw * 0.1 && rect.top <= vh * 0.1 && rect.right >= vw * 0.9 && rect.bottom >= vh * 0.9;
        return areaRatio >= MIN_COVER_RATIO || coversCenter;
      }

      function markIfOverlay(el) {
        try {
          if (!el || !el.setAttribute) return false;
          if (el.hasAttribute(OVERLAY_ATTR)) return false;
          if (isLargeOverlay(el)) {
            el.setAttribute(OVERLAY_ATTR, '1');
            el.style.pointerEvents = 'none';
            el.style.userSelect = 'none';
            return true;
          }
        } catch (e) {}
        return false;
      }

      function scanExisting() {
        try {
          const all = document.querySelectorAll ? document.querySelectorAll('body *') : [];
          for (let i = 0; i < all.length; i++) markIfOverlay(all[i]);
        } catch (e) {}
      }

      const mo = new MutationObserver((records) => {
        for (const r of records) {
          if (r.type === 'childList') {
            for (const n of r.addedNodes) {
              if (n && n.nodeType === 1) {
                if (markIfOverlay(n)) continue;
                const desc = n.querySelectorAll ? n.querySelectorAll('*') : [];
                for (let j = 0; j < desc.length; j++) markIfOverlay(desc[j]);
              }
            }
          } else if (r.type === 'attributes' && r.target) {
            markIfOverlay(r.target);
          }
        }
      });

      try { mo.observe(document.documentElement || document, { childList: true, subtree: true, attributes: true, attributeFilter: ['style', 'class'] }); } catch (e) {}

      if (document.readyState !== 'loading') scanExisting();
      else document.addEventListener('DOMContentLoaded', scanExisting, { once: true });

      document.addEventListener('pointerdown', function (e) {
        try {
          if (!e.isTrusted) return;
          let el = e.target;
          while (el && el !== document.documentElement) {
            if (el.getAttribute && el.getAttribute(OVERLAY_ATTR)) {
              el.style.pointerEvents = '';
              el.removeAttribute(OVERLAY_ATTR);
              lastTrustedGesture = now();
              break;
            }
            el = el.parentElement;
          }
        } catch (err) {}
      }, true);

      document.addEventListener('click', function (e) {
        try {
          if (e.defaultPrevented) return;
          let el = e.target;
          while (el && el !== document.documentElement) {
            if (el.getAttribute && el.getAttribute(OVERLAY_ATTR)) {
              const recent = hadRecentTrustedGesture();
              if (!recent) {
                e.preventDefault();
                e.stopImmediatePropagation();
                const href = (function findHref(n) {
                  let cur = n;
                  while (cur && cur !== document.documentElement) {
                    if (cur.tagName === 'A' && cur.href) return cur.href;
                    cur = cur.parentElement;
                  }
                  return null;
                })(el);
                handleBlocked(href || location.href, 'overlay.click');
              }
              return;
            }
            el = el.parentElement;
          }
        } catch (err) {}
      }, true);

      setInterval(() => {
        try {
          const candidates = document.querySelectorAll ? document.querySelectorAll('body *') : [];
          for (let i = 0; i < candidates.length; i++) {
            const el = candidates[i];
            if (!el.getAttribute) continue;
            if (el.hasAttribute(OVERLAY_ATTR)) continue;
            markIfOverlay(el);
          }
        } catch (e) {}
      }, SCAN_INTERVAL);
    } catch (e) { pushLog({ type: 'overlayProtector_error', error: String(e) }); }
  })();

  // ---------- Meta refresh neutralization ----------
  (function observeMetaRefresh() {
    try {
      function neutralizeMeta(m) {
        try {
          const content = m.getAttribute('content') || '';
          const mMatch = content.match(/^\s*\d+\s*;\s*url=(.*)$/i);
          if (mMatch && mMatch[1]) {
            const url = mMatch[1].trim().replace(/^['"]|['"]$/g, '');
            const u = resolveUrlObj(url);
            if (!isAllowedNavigation(u)) {
              m.setAttribute('content', '99999; url=' + (u ? u.href : ''));
              handleBlocked(u ? u.href : url, 'meta.refresh');
            }
          }
        } catch {}
      }

      const metas = document.getElementsByTagName ? document.getElementsByTagName('meta') : [];
      for (let i = 0; i < metas.length; i++) {
        const m = metas[i];
        if ((m.getAttribute('http-equiv') || '').toLowerCase() === 'refresh') neutralizeMeta(m);
      }

      const mo = new MutationObserver((records) => {
        for (const r of records) {
          for (const n of r.addedNodes) {
            if (n && n.nodeType === 1) {
              if (n.tagName === 'META' && (n.getAttribute('http-equiv') || '').toLowerCase() === 'refresh') neutralizeMeta(n);
              else {
                const found = n.querySelectorAll ? n.querySelectorAll('meta[http-equiv]') : [];
                for (let j = 0; j < found.length; j++) {
                  if ((found[j].getAttribute('http-equiv') || '').toLowerCase() === 'refresh') neutralizeMeta(found[j]);
                }
              }
            }
          }
        }
      });
      mo.observe(document.documentElement || document, { childList: true, subtree: true });
    } catch (e) { pushLog({ type: 'metaObserver_error', error: String(e) }); }
  })();

  // ---------- Patch navigation APIs ----------
  (function patchLocationAndOpen() {
    function patchWindow(win) {
      if (!win || !win.Location) return;
      try {
        if (win.__rb_patched) return;
        win.__rb_patched = true;
      } catch (e) { return; }

      // Direct assignment handler (e.g., location.href = 'url')
      try {
        const desc = Object.getOwnPropertyDescriptor(win.Location.prototype, 'href');
        if (desc && desc.set) {
          const origSetHref = desc.set;
          Object.defineProperty(win.Location.prototype, 'href', {
            set: function (url) {
              if (location.hostname.includes('bstsrs')) return origSetHref.call(this, url);
              const u = resolveUrlObj(url);
              if (!isAllowedNavigation(u)) {
                handleBlocked(u ? u.href : String(url), 'location.href');
                return;
              }
              return origSetHref.call(this, url);
            },
            configurable: true
          });
        }
      } catch (e) { pushLog({ type: 'patch_href_error', error: String(e) }); }

      try {
        const origAssign = win.Location.prototype.assign;
        if (origAssign) {
          win.Location.prototype.assign = function (url) {
            if (location.hostname.includes('bstsrs')) return origAssign.call(this, url);
            const u = resolveUrlObj(url);
            if (!isAllowedNavigation(u)) {
              handleBlocked(u ? u.href : String(url), 'location.assign');
              return;
            }
            return origAssign.call(this, url);
          };
        }
      } catch (e) { pushLog({ type: 'patch_assign_error', error: String(e) }); }

      try {
        const origReplace = win.Location.prototype.replace;
        if (origReplace) {
          win.Location.prototype.replace = function (url) {
            if (location.hostname.includes('bstsrs')) return origReplace.call(this, url);
            const u = resolveUrlObj(url);
            if (!isAllowedNavigation(u)) {
              handleBlocked(u ? u.href : String(url), 'location.replace');
              return;
            }
            return origReplace.call(this, url);
          };
        }
      } catch (e) { pushLog({ type: 'patch_replace_error', error: String(e) }); }

      try {
        const origOpen = win.open;
        if (origOpen) {
          win.open = function (url, target, features) {
            if (location.hostname.includes('bstsrs')) return origOpen.call(this, url, target, features);
            const u = resolveUrlObj(url || '');
            if (url && !isAllowedNavigation(u)) {
              handleBlocked(u ? u.href : String(url), 'window.open');
              return null;
            }
            return origOpen.call(this, url, target, features);
          };
        }
      } catch (e) { pushLog({ type: 'patch_open_error', error: String(e) }); }
    }

    patchWindow(window);

    try {
      const origAppendChild = Node.prototype.appendChild;
      Node.prototype.appendChild = function(node) {
        const res = origAppendChild.apply(this, arguments);
        if (node && node.nodeName === 'IFRAME') {
          try { patchWindow(node.contentWindow); } catch (e) {}
        }
        return res;
      };
      const origInsertBefore = Node.prototype.insertBefore;
      Node.prototype.insertBefore = function(node, child) {
        const res = origInsertBefore.apply(this, arguments);
        if (node && node.nodeName === 'IFRAME') {
          try { patchWindow(node.contentWindow); } catch (e) {}
        }
        return res;
      };
    } catch (e) {}
  })();

  // ---------- Patch history APIs ----------
  (function patchHistory() {
    try {
      const origPush = history.pushState;
      const origReplace = history.replaceState;
      history.pushState = function (state, title, url) {
        if (location.hostname.includes('bstsrs')) return origPush.apply(history, arguments);
        if (typeof url === 'string' && url.length) {
          const u = resolveUrlObj(url);
          if (!isAllowedNavigation(u)) {
            handleBlocked(u ? u.href : String(url), 'history.pushState');
            return;
          }
        }
        return origPush.apply(history, arguments);
      };
      history.replaceState = function (state, title, url) {
        if (location.hostname.includes('bstsrs')) return origReplace.apply(history, arguments);
        if (typeof url === 'string' && url.length) {
          const u = resolveUrlObj(url);
          if (!isAllowedNavigation(u)) {
            handleBlocked(u ? u.href : String(url), 'history.replaceState');
            return;
          }
        }
        return origReplace.apply(history, arguments);
      };
    } catch (e) { pushLog({ type: 'patch_history_error', error: String(e) }); }
  })();

  // ---------- JS Form API interception ----------
  (function patchFormAPI() {
    try {
      const origSubmit = HTMLFormElement.prototype.submit;
      HTMLFormElement.prototype.submit = function () {
        if (location.hostname.includes('bstsrs')) return origSubmit.apply(this, arguments);
        const action = this.getAttribute('action') || location.href;
        const u = resolveUrlObj(action);
        if (!isAllowedNavigation(u)) {
          handleBlocked(u ? u.href : action, 'form.submit_api');
          return;
        }
        return origSubmit.apply(this, arguments);
      };
    } catch (e) {}
  })();

  // ---------- Anchor click and form submit interception ----------
  function onAnchorClick(e) {
    try {
      if (e.defaultPrevented) return;
      if (e.button !== 0) return;
      if (e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
      let a = e.target;
      while (a && a.nodeName !== 'A') a = a.parentElement;
      if (!a || !a.href) return;
      const u = resolveUrlObj(a.getAttribute('href'));
      if (!isAllowedNavigation(u)) {
        if (e.isTrusted && hadRecentTrustedGesture()) return;
        e.preventDefault();
        e.stopImmediatePropagation();
        handleBlocked(u ? u.href : a.href, 'anchor.click');
      }
    } catch (err) {}
  }
  function onFormSubmit(e) {
    try {
      if (e.defaultPrevented) return;
      const form = e.target;
      if (!form || form.nodeName !== 'FORM') return;
      const action = form.getAttribute('action') || location.href;
      const u = resolveUrlObj(action);
      if (!isAllowedNavigation(u)) {
        if (e.isTrusted && hadRecentTrustedGesture()) return;
        e.preventDefault();
        e.stopImmediatePropagation();
        handleBlocked(u ? u.href : action, 'form.submit');
      }
    } catch {}
  }
  window.addEventListener('click', onAnchorClick, true);
  window.addEventListener('submit', onFormSubmit, true);

  // ---------- Defensive notes ----------
  pushLog({ type: 'init', host: location.hostname });

})();
