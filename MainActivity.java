package com.zeus.mousetest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Standalone Fire TV mouse performance test: web pages, Google search and local video. */
public class MainActivity extends Activity implements Choreographer.FrameCallback {
    private static final int REQUEST_SAVE_LOG = 4107;
    private static final int REQUEST_VIDEO = 4108;
    private static final long LONG_PRESS_MS = 550L;
    private static final float SPEED_DP_PER_SECOND = 600f;
    /** Longest stalled gap (s) the cursor catches up on, so a busy UI thread (page load) cannot slow it down. */
    private static final float MAX_CATCH_UP_SECONDS = 0.25f;
    /** A very short tap always moves at least this far (about one 60 fps frame at full speed). */
    private static final float MIN_TAP_DP = 10f;
    private static final int MAX_LOG_CHARS = 900_000;
    private static final String LOTTERY_URL = "https://www.national-lottery.co.uk/";
    private static final String GOOGLE_URL = "https://www.google.com/";

    private final StringBuilder log = new StringBuilder(48_000);
    private final Object logLock = new Object();
    private HandlerThread logThread;
    private volatile Handler logHandler;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.UK);
    private final SimpleDateFormat fileFormat = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.UK);

    private FrameLayout root;
    private View contentView;
    private WebView webView;
    private VideoView videoView;
    private CursorView cursorView;
    private AlertDialog optionsDialog;
    private int activeDirection = KeyEvent.KEYCODE_UNKNOWN;
    private boolean frameCallbackPosted;
    private long lastFrameNanos;   // previous vsync time, used only for frame statistics
    private long lastMoveNanos;    // time up to which cursor movement has been applied
    private long pressStartNanos;
    private float movedThisPress;
    private int lastAreaWidth, lastAreaHeight;
    private long statsWindowStartNanos;
    private int statsFrames, slowFrames, verySlowFrames, cappedFrames;
    private float statsFrameTotalMs, statsFrameMaxMs;
    private long lastPositionLogNanos;
    private boolean edgeLogged;
    private int edgeScrollDirection = KeyEvent.KEYCODE_UNKNOWN;
    private long lastScrollLogNanos;
    private boolean centreKeyDown;
    private boolean longPressTriggered;
    private boolean contentTouchDown;
    private long contentTouchDownTime;
    private long lastPageStartMs;

    private final Runnable longPressRunnable = () -> {
        if (!centreKeyDown) return;
        longPressTriggered = true;
        appendLog("RIGHT_CLICK long-press at " + positionDescription());
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logThread = new HandlerThread("ZeusMouseLog");
        logThread.start();
        logHandler = new Handler(logThread.getLooper());
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        applyImmersiveMode();

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        cursorView = new CursorView();
        cursorView.setFocusable(true);
        cursorView.setFocusableInTouchMode(true);
        cursorView.setClickable(false);
        // Small sprite moved with translation: no full-screen layer to redraw while the page loads.
        root.addView(cursorView, new FrameLayout.LayoutParams(cursorView.sizePx, cursorView.sizePx));
        root.addOnLayoutChangeListener((v, l, t, r, b, oldL, oldT, oldR, oldB) -> {
            int w = r - l, h = b - t;
            if (w <= 0 || h <= 0) return;
            float rad = cursorView.cursorRadius;
            if (!cursorView.positioned) { cursorView.cursorX = w / 2f; cursorView.cursorY = h / 2f; cursorView.positioned = true; }
            else {
                cursorView.cursorX = clamp(cursorView.cursorX, rad, Math.max(rad, w - rad));
                cursorView.cursorY = clamp(cursorView.cursorY, rad, Math.max(rad, h - rad));
            }
            cursorView.applyPosition();
            if (w != lastAreaWidth || h != lastAreaHeight) {
                lastAreaWidth = w; lastAreaHeight = h;
                appendLog("VIEW_SIZE " + w + "x" + h + " " + positionDescription());
            }
        });
        setContentView(root);
        cursorView.requestFocus();

        appendLog("==================================================");
        appendLog("ZEUS MOUSE TEST 1.3 - EDGE SCROLL + POINTER LOCK + VIDEO CURSOR SESSION START");
        appendLog("Movement: constant speed, no acceleration; " + SPEED_DP_PER_SECOND + " dp/second");
        logDisplayMetrics();
        appendLog("TEST OPTIONS: National Lottery website, Google search, local video picker");
    }

    private void applyImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersiveMode();
    }

    private void logDisplayMetrics() {
        android.util.DisplayMetrics m = new android.util.DisplayMetrics();
        Display d = getWindowManager().getDefaultDisplay();
        d.getRealMetrics(m);
        appendLog(String.format(Locale.UK, "DISPLAY real=%dx%d px density=%.3f dpi=%d scaledDensity=%.3f rotation=%d",
                m.widthPixels, m.heightPixels, m.density, m.densityDpi, m.scaledDensity, d.getRotation()));
        appendLog("ANDROID SDK=" + android.os.Build.VERSION.SDK_INT + " MODEL=" + android.os.Build.MODEL
                + " DEVICE=" + android.os.Build.DEVICE + " MANUFACTURER=" + android.os.Build.MANUFACTURER);
    }

    /**
     * Capture remote pointer keys before a focused WebView or video control can
     * consume them. This keeps the app in virtual-mouse mode after webpage clicks.
     */
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int action = event.getAction();
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
            if (action == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                appendLog("MENU_KEY " + KeyEvent.keyCodeToString(keyCode));
                showOptionsMenu();
            }
            return true;
        }
        if (isDirectionKey(keyCode)) {
            if (action == KeyEvent.ACTION_DOWN) {
                if (event.getRepeatCount() == 0 || activeDirection != keyCode) {
                    flushMovement();
                    activeDirection = keyCode;
                    edgeLogged = false;
                    edgeScrollDirection = KeyEvent.KEYCODE_UNKNOWN;
                    appendLog("POINTER_INPUT direction=" + directionName(keyCode) + " focus=" + focusDescription());
                    appendLog("MOVE_START direction=" + directionName(keyCode));
                    startMovement();
                }
            } else if (action == KeyEvent.ACTION_UP) {
                if (activeDirection == keyCode) {
                    finishMovement();
                    String summary = pressSummary();
                    stopMovement();
                    appendLog("MOVE_STOP direction=" + directionName(keyCode) + " " + positionDescription() + summary);
                }
            }
            return true;
        }
        if (isCentreKey(keyCode)) {
            if (action == KeyEvent.ACTION_DOWN) {
                if (!centreKeyDown) {
                    centreKeyDown = true;
                    longPressTriggered = false;
                    handler.removeCallbacks(longPressRunnable);
                    handler.postDelayed(longPressRunnable, LONG_PRESS_MS);
                    appendLog("SELECT_DOWN " + KeyEvent.keyCodeToString(keyCode));
                    dispatchContentTouch(MotionEvent.ACTION_DOWN);
                }
            } else if (action == KeyEvent.ACTION_UP) {
                if (centreKeyDown) {
                    centreKeyDown = false;
                    handler.removeCallbacks(longPressRunnable);
                    if (!longPressTriggered) appendLog("LEFT_CLICK " + positionDescription());
                    dispatchContentTouch(MotionEvent.ACTION_UP);
                    keepPointerMode();
                }
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private String focusDescription() {
        View focused = getCurrentFocus();
        return focused == null ? "none" : focused.getClass().getSimpleName();
    }

    private void keepPointerMode() {
        if (cursorView == null) return;
        boolean hadFocus = cursorView.hasFocus();
        cursorView.setVisibility(View.VISIBLE);
        cursorView.setAlpha(1f);
        if (root.indexOfChild(cursorView) != root.getChildCount() - 1) cursorView.bringToFront();
        cursorView.requestFocus();
        if (!hadFocus) appendLog("POINTER_MODE_RESTORED focus=" + focusDescription());
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (optionsDialog != null && optionsDialog.isShowing()) { optionsDialog.dismiss(); return true; }
            if (webView != null && webView.canGoBack()) {
                appendLog("BACK web history"); webView.goBack(); keepPointerMode(); return true;
            }
            if (contentView != null) { showBlackScreen(); return true; }
            appendLog("BACK - activity closing"); finish(); return true;
        }
        if (event.getRepeatCount() == 0) appendLog("KEY_DOWN code=" + KeyEvent.keyCodeToString(keyCode)
                + " numeric=" + keyCode + " device=" + event.getDeviceId());
        return super.onKeyDown(keyCode, event);
    }

    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

    private boolean isDirectionKey(int k) { return k == KeyEvent.KEYCODE_DPAD_UP || k == KeyEvent.KEYCODE_DPAD_DOWN
            || k == KeyEvent.KEYCODE_DPAD_LEFT || k == KeyEvent.KEYCODE_DPAD_RIGHT; }
    private boolean isCentreKey(int k) { return k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER
            || k == KeyEvent.KEYCODE_NUMPAD_ENTER || k == KeyEvent.KEYCODE_BUTTON_SELECT; }
    private String directionName(int k) {
        if (k == KeyEvent.KEYCODE_DPAD_UP) return "UP";
        if (k == KeyEvent.KEYCODE_DPAD_DOWN) return "DOWN";
        if (k == KeyEvent.KEYCODE_DPAD_LEFT) return "LEFT";
        return "RIGHT";
    }

    private void dispatchContentTouch(int action) {
        if (contentView == null || cursorView == null) return;
        long now = SystemClock.uptimeMillis();
        if (action == MotionEvent.ACTION_DOWN) { contentTouchDownTime = now; contentTouchDown = true; }
        long downTime = contentTouchDown ? contentTouchDownTime : now;
        float x = cursorView.cursorX, y = cursorView.cursorY;
        MotionEvent e = MotionEvent.obtain(downTime, now, action, x, y, 0);
        try {
            contentView.dispatchTouchEvent(e);
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                contentTouchDown = false;
            }
        } catch (Exception ex) {
            appendLog("CURSOR_CLICK dispatch failed: " + ex);
        } finally { e.recycle(); }
    }

    private void startMovement() {
        pressStartNanos = System.nanoTime();
        movedThisPress = 0f;
        if (frameCallbackPosted) return; // direction change while held: already running, lastMoveNanos is current
        lastMoveNanos = pressStartNanos;
        lastFrameNanos = 0L; statsWindowStartNanos = 0L;
        statsFrames = slowFrames = verySlowFrames = cappedFrames = 0;
        statsFrameTotalMs = statsFrameMaxMs = 0f; lastPositionLogNanos = 0L;
        frameCallbackPosted = true;
        Choreographer.getInstance().postFrameCallback(this);
    }

    private void stopMovement() {
        activeDirection = KeyEvent.KEYCODE_UNKNOWN;
        if (frameCallbackPosted) Choreographer.getInstance().removeFrameCallback(this);
        frameCallbackPosted = false; lastFrameNanos = 0L; lastMoveNanos = 0L;
    }

    /** Applies the movement owed since the last frame, so a release or direction change is exact. */
    private void flushMovement() {
        if (cursorView == null || activeDirection == KeyEvent.KEYCODE_UNKNOWN || lastMoveNanos == 0L) return;
        long now = System.nanoTime();
        long delta = now - lastMoveNanos;
        if (delta <= 0L) return;
        lastMoveNanos = now;
        float seconds = Math.min(delta / 1_000_000_000f, MAX_CATCH_UP_SECONDS);
        movedThisPress += moveCursor(activeDirection, SPEED_DP_PER_SECOND * density() * seconds);
    }

    /** On key release: apply owed movement, and make sure even a sub-frame tap moves the cursor a little. */
    private void finishMovement() {
        flushMovement();
        float minPx = MIN_TAP_DP * density();
        if (activeDirection != KeyEvent.KEYCODE_UNKNOWN && movedThisPress < minPx) {
            movedThisPress += moveCursor(activeDirection, minPx - movedThisPress);
        }
    }

    /** Held time, plus effective speed for longer presses that did not hit a screen edge. */
    private String pressSummary() {
        long heldMs = (System.nanoTime() - pressStartNanos) / 1_000_000L;
        if (heldMs < 200L || edgeLogged) return " held=" + heldMs + "ms";
        float avgDp = movedThisPress / density() / (heldMs / 1000f);
        return String.format(Locale.UK, " held=%dms avg=%.0fdp/s", heldMs, avgDp);
    }

    /** Moves the cursor sprite and returns the distance actually travelled (after edge clamping). */
    private float moveCursor(int direction, float distance) {
        if (cursorView == null || distance <= 0f) return 0f;
        float oldX = cursorView.cursorX, oldY = cursorView.cursorY;
        switch (direction) {
            case KeyEvent.KEYCODE_DPAD_UP: cursorView.cursorY -= distance; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: cursorView.cursorY += distance; break;
            case KeyEvent.KEYCODE_DPAD_LEFT: cursorView.cursorX -= distance; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: cursorView.cursorX += distance; break;
            default: return 0f;
        }
        float r = cursorView.cursorRadius;
        cursorView.cursorX = clamp(cursorView.cursorX, r, Math.max(r, areaWidth() - r));
        cursorView.cursorY = clamp(cursorView.cursorY, r, Math.max(r, areaHeight() - r));
        cursorView.applyPosition();
        return Math.abs(cursorView.cursorX - oldX) + Math.abs(cursorView.cursorY - oldY);
    }

    private void handleEdge(float distance, long frameTimeNanos) {
        float r = cursorView.cursorRadius;
        boolean edge = (cursorView.cursorX <= r && activeDirection == KeyEvent.KEYCODE_DPAD_LEFT)
                || (cursorView.cursorX >= areaWidth() - r && activeDirection == KeyEvent.KEYCODE_DPAD_RIGHT)
                || (cursorView.cursorY <= r && activeDirection == KeyEvent.KEYCODE_DPAD_UP)
                || (cursorView.cursorY >= areaHeight() - r && activeDirection == KeyEvent.KEYCODE_DPAD_DOWN);
        if (edge) {
            if (!edgeLogged) { appendLog("SCREEN_EDGE_REACHED " + directionName(activeDirection)); edgeLogged = true; }
            scrollContentAtEdge(activeDirection, distance, frameTimeNanos);
        } else {
            if (edgeScrollDirection != KeyEvent.KEYCODE_UNKNOWN) appendLog("EDGE_SCROLL_STOP direction=" + directionName(edgeScrollDirection));
            edgeLogged = false;
            edgeScrollDirection = KeyEvent.KEYCODE_UNKNOWN;
        }
    }

    private int areaWidth() { return root == null ? 0 : root.getWidth(); }
    private int areaHeight() { return root == null ? 0 : root.getHeight(); }
    private float density() { return getResources().getDisplayMetrics().density; }

    /**
     * Movement is integrated from real elapsed time, not from the number of frames, so a slow frame
     * (WebView loading, layout, JS) makes the cursor jump to where it should be instead of crawling.
     */
    @Override public void doFrame(long frameTimeNanos) {
        if (!frameCallbackPosted || activeDirection == KeyEvent.KEYCODE_UNKNOWN || cursorView == null) {
            frameCallbackPosted = false; return;
        }
        boolean measured = lastFrameNanos != 0L;
        float frameMs = measured ? (frameTimeNanos - lastFrameNanos) / 1_000_000f : 0f;
        lastFrameNanos = frameTimeNanos;
        if (statsWindowStartNanos == 0L) statsWindowStartNanos = frameTimeNanos;

        long deltaNanos = frameTimeNanos - lastMoveNanos;
        if (deltaNanos > 0L) {
            lastMoveNanos = frameTimeNanos;
            float seconds = deltaNanos / 1_000_000_000f;
            if (seconds > MAX_CATCH_UP_SECONDS) { seconds = MAX_CATCH_UP_SECONDS; cappedFrames++; }
            float distance = SPEED_DP_PER_SECOND * density() * seconds;
            movedThisPress += moveCursor(activeDirection, distance);
            handleEdge(distance, frameTimeNanos);
        }
        if (measured) {
            statsFrames++; statsFrameTotalMs += frameMs; statsFrameMaxMs = Math.max(statsFrameMaxMs, frameMs);
            if (frameMs > 20f) slowFrames++;
            if (frameMs > 33.3f) verySlowFrames++;
        }
        if (frameTimeNanos - lastPositionLogNanos >= 1_000_000_000L) {
            appendLog("POSITION " + positionDescription()); lastPositionLogNanos = frameTimeNanos;
        }
        if (frameTimeNanos - statsWindowStartNanos >= 1_000_000_000L) {
            float elapsed = (frameTimeNanos - statsWindowStartNanos) / 1_000_000_000f;
            appendLog(String.format(Locale.UK, "FRAME_STATS fps=%.1f avgFrame=%.2fms maxFrame=%.2fms over20ms=%d over33ms=%d capped=%d",
                    elapsed > 0 ? statsFrames / elapsed : 0, statsFrames > 0 ? statsFrameTotalMs / statsFrames : 0,
                    statsFrameMaxMs, slowFrames, verySlowFrames, cappedFrames));
            statsWindowStartNanos = frameTimeNanos; statsFrames = slowFrames = verySlowFrames = cappedFrames = 0;
            statsFrameTotalMs = statsFrameMaxMs = 0f;
        }
        Choreographer.getInstance().postFrameCallback(this);
    }

    /** Scroll the WebView viewport while the cursor remains pinned at the screen edge. */
    private void scrollContentAtEdge(int direction, float distancePx, long frameTimeNanos) {
        if (webView == null || distancePx <= 0f) return;
        int amount = Math.max(1, Math.round(distancePx));
        int dx = 0, dy = 0;
        if (direction == KeyEvent.KEYCODE_DPAD_UP) dy = -amount;
        else if (direction == KeyEvent.KEYCODE_DPAD_DOWN) dy = amount;
        else if (direction == KeyEvent.KEYCODE_DPAD_LEFT) dx = -amount;
        else if (direction == KeyEvent.KEYCODE_DPAD_RIGHT) dx = amount;
        if (edgeScrollDirection != direction) {
            edgeScrollDirection = direction;
            appendLog("EDGE_SCROLL_START direction=" + directionName(direction) + " before=" + webView.getScrollX() + "," + webView.getScrollY());
        }
        int beforeX = webView.getScrollX(), beforeY = webView.getScrollY();
        webView.scrollBy(dx, dy);
        if (frameTimeNanos - lastScrollLogNanos >= 1_000_000_000L) {
            appendLog("EDGE_SCROLL position=" + webView.getScrollX() + "," + webView.getScrollY()
                    + " moved=" + (webView.getScrollX() - beforeX) + "," + (webView.getScrollY() - beforeY));
            lastScrollLogNanos = frameTimeNanos;
        }
    }

    private float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
    private String positionDescription() {
        if (cursorView == null) return "cursor=not-ready";
        float d = getResources().getDisplayMetrics().density;
        return String.format(Locale.UK, "cursor=(%.1f,%.1f)px (%.1f,%.1fdp) view=%dx%dpx density=%.3f",
                cursorView.cursorX, cursorView.cursorY, cursorView.cursorX / d, cursorView.cursorY / d,
                areaWidth(), areaHeight(), d);
    }

    private void showOptionsMenu() {
        if (optionsDialog != null && optionsDialog.isShowing()) return;
        String[] options = {"Open National Lottery website", "Google Search", "Play Local Video", "Return to Black Screen",
                "Save Log", "Share Log", "Clear Log"};
        optionsDialog = new AlertDialog.Builder(this).setTitle("Zeus Mouse Test")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: openWebsite(LOTTERY_URL, "NATIONAL_LOTTERY"); break;
                        case 1: promptGoogleSearch(); break;
                        case 2: chooseLocalVideo(); break;
                        case 3: showBlackScreen(); break;
                        case 4: saveLog(); break;
                        case 5: shareLog(); break;
                        case 6: clearLog(); break;
                    }
                }).setNegativeButton("Back", (dialog, which) -> dialog.dismiss()).create();
        optionsDialog.setOnShowListener(v -> { if (optionsDialog.getListView() != null) {
            optionsDialog.getListView().setSelection(0); optionsDialog.getListView().requestFocus(); }});
        optionsDialog.setOnDismissListener(v -> { optionsDialog = null; if (cursorView != null) cursorView.requestFocus(); appendLog("OPTIONS_MENU closed"); });
        optionsDialog.show(); appendLog("OPTIONS_MENU opened");
    }

    private void openWebsite(String url, String label) {
        stopMovement(); removeContent();
        WebView w = new WebView(this); webView = w; contentView = w;
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true);
        s.setLoadsImagesAutomatically(true); s.setMediaPlaybackRequiresUserGesture(true);
        s.setSupportZoom(true); s.setBuiltInZoomControls(false); s.setDisplayZoomControls(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        w.setBackgroundColor(Color.WHITE);
        w.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String u, android.graphics.Bitmap favicon) {
                lastPageStartMs = SystemClock.elapsedRealtime(); appendLog("PAGE_START label=" + label + " url=" + u);
            }
            @Override public void onPageFinished(WebView view, String u) {
                appendLog("PAGE_FINISH url=" + u + " elapsed=" + (SystemClock.elapsedRealtime() - lastPageStartMs) + "ms title=" + view.getTitle());
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) appendLog("PAGE_ERROR url=" + request.getUrl() + " code=" + error.getErrorCode() + " description=" + error.getDescription());
            }
        });
        w.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int progress) {
                if (progress == 100) appendLog("WEB_PROGRESS 100 url=" + view.getUrl());
            }
            @Override public boolean onConsoleMessage(ConsoleMessage m) {
                appendLog("WEB_CONSOLE " + m.messageLevel() + " " + m.message() + " line=" + m.lineNumber());
                return true;
            }
        });
        root.addView(w, 0, new FrameLayout.LayoutParams(-1, -1));
        appendLog("WEBVIEW_CREATED hardwareAccelerated=true userAgent=" + s.getUserAgentString());
        w.loadUrl(url);
        keepPointerMode();
        appendLog("POINTER_MODE_ACTIVE over WebView");
    }

    private void promptGoogleSearch() {
        EditText input = new EditText(this); input.setSingleLine(true); input.setHint("Search Google");
        new AlertDialog.Builder(this).setTitle("Google Search").setView(input)
                .setPositiveButton("Search", (d, which) -> {
                    String q = input.getText().toString().trim();
                    if (!q.isEmpty()) {
                        try {
                            String url = "https://www.google.com/search?q=" + java.net.URLEncoder.encode(q, "UTF-8");
                            appendLog("GOOGLE_SEARCH query=" + q); openWebsite(url, "GOOGLE_SEARCH");
                        } catch (Exception e) { appendLog("GOOGLE_SEARCH encoding error=" + e); }
                    }
                }).setNegativeButton("Cancel", (d, which) -> d.dismiss()).show();
    }

    private void chooseLocalVideo() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("video/*");
        try { startActivityForResult(i, REQUEST_VIDEO); appendLog("LOCAL_VIDEO_PICKER opened"); }
        catch (Exception e) { appendLog("LOCAL_VIDEO_PICKER failed=" + e); Toast.makeText(this, "No file picker available", Toast.LENGTH_LONG).show(); }
    }

    private void playLocalVideo(Uri uri) {
        stopMovement(); removeContent();
        VideoView v = new VideoView(this); videoView = v; contentView = v;
        MediaController controller = new MediaController(this);
        controller.setAnchorView(v); v.setMediaController(controller);
        root.addView(v, 0, new FrameLayout.LayoutParams(-1, -1));
        keepPointerMode();
        appendLog("LOCAL_VIDEO_SELECTED uri=" + uri);
        v.setOnPreparedListener(mp -> {
            keepPointerMode();
            appendLog("VIDEO_PREPARED durationMs=" + mp.getDuration() + " size=" + mp.getVideoWidth() + "x" + mp.getVideoHeight());
            mp.setOnVideoSizeChangedListener((player, width, height) -> appendLog("VIDEO_SIZE_CHANGED " + width + "x" + height));
            controller.show(5000);
        });
        v.setOnCompletionListener(mp -> appendLog("VIDEO_COMPLETED"));
        v.setOnErrorListener((mp, what, extra) -> { appendLog("VIDEO_ERROR what=" + what + " extra=" + extra); return false; });
        v.setOnInfoListener((mp, what, extra) -> { appendLog("VIDEO_INFO what=" + what + " extra=" + extra); return false; });
        v.setVideoURI(uri); v.start();
        keepPointerMode();
        appendLog("VIDEO_CURSOR_POLICY always-visible; no auto-hide during playback");
    }

    private void showBlackScreen() {
        stopMovement(); removeContent(); appendLog("TEST_SURFACE black screen");
        cursorView.requestFocus(); cursorView.invalidate();
    }

    private void removeContent() {
        if (webView != null) { try { webView.stopLoading(); webView.loadUrl("about:blank"); webView.clearHistory(); webView.removeAllViews(); webView.destroy(); } catch (Exception ignored) {}
            appendLog("WEBVIEW_DESTROYED"); webView = null; }
        if (videoView != null) { try { videoView.stopPlayback(); } catch (Exception ignored) {} videoView = null; appendLog("VIDEO_VIEW_RELEASED"); }
        if (contentView != null) { root.removeView(contentView); contentView = null; }
    }

    private void saveLog() {
        appendLog("LOG_SAVE requested"); Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TITLE, "ZeusMouseTest-" + fileFormat.format(new Date()) + ".txt");
        try { startActivityForResult(i, REQUEST_SAVE_LOG); }
        catch (Exception e) { appendLog("LOG_SAVE launch error=" + e); Toast.makeText(this, "Unable to open save location", Toast.LENGTH_LONG).show(); }
    }
    private void shareLog() {
        appendLog("LOG_SHARE requested"); Intent i = new Intent(Intent.ACTION_SEND); i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "Zeus Mouse Test diagnostics"); i.putExtra(Intent.EXTRA_TEXT, logSnapshot());
        try { startActivity(Intent.createChooser(i, "Share diagnostic log")); }
        catch (Exception e) { appendLog("LOG_SHARE error=" + e); Toast.makeText(this, "No sharing app available", Toast.LENGTH_LONG).show(); }
    }
    private void clearLog() { flushLog(); synchronized (logLock) { log.setLength(0); } appendLog("LOG CLEARED"); logDisplayMetrics(); Toast.makeText(this, "Log cleared", Toast.LENGTH_SHORT).show(); }
    /** Cheap on the UI thread: grabs timestamps and hands formatting/writing to the log thread. */
    private void appendLog(String message) {
        final long wallMs = System.currentTimeMillis();
        final long elapsedMs = SystemClock.elapsedRealtime();
        Handler h = logHandler;
        if (h == null || !h.post(() -> writeLogLine(wallMs, elapsedMs, message))) writeLogLine(wallMs, elapsedMs, message);
    }

    private void writeLogLine(long wallMs, long elapsedMs, String message) {
        synchronized (logLock) {
            log.append(timeFormat.format(new Date(wallMs))).append(" | +").append(elapsedMs).append("ms | ").append(message).append('\n');
            if (log.length() > MAX_LOG_CHARS) {
                int keepFrom = log.length() - MAX_LOG_CHARS * 3 / 4;
                int cut = log.indexOf("\n", keepFrom);
                log.delete(0, cut >= 0 ? cut + 1 : keepFrom);
            }
        }
        android.util.Log.d("ZeusMouseTest", message);
    }

    /** Waits (briefly) for queued log lines to be written. */
    private void flushLog() {
        Handler h = logHandler;
        if (h == null) return;
        CountDownLatch latch = new CountDownLatch(1);
        if (!h.post(latch::countDown)) return;
        try { latch.await(250, TimeUnit.MILLISECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private String logSnapshot() {
        flushLog();
        synchronized (logLock) { return log.toString(); }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQUEST_VIDEO) {
            try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
            playLocalVideo(uri);
        } else if (requestCode == REQUEST_SAVE_LOG) {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new IllegalStateException("No output stream");
                out.write(logSnapshot().getBytes(StandardCharsets.UTF_8)); out.flush();
                Toast.makeText(this, "Diagnostic log saved", Toast.LENGTH_LONG).show(); appendLog("LOG_SAVE completed");
            } catch (Exception e) { appendLog("LOG_SAVE failed=" + e); Toast.makeText(this, "Could not save log", Toast.LENGTH_LONG).show(); }
        }
    }

    @Override public void onBackPressed() {
        if (optionsDialog != null && optionsDialog.isShowing()) optionsDialog.dismiss();
        else if (webView != null && webView.canGoBack()) webView.goBack();
        else if (contentView != null) showBlackScreen();
        else super.onBackPressed();
    }
    @Override protected void onPause() { stopMovement(); centreKeyDown = false; handler.removeCallbacks(longPressRunnable); appendLog("LIFECYCLE onPause"); super.onPause(); }
    @Override protected void onResume() { super.onResume(); applyImmersiveMode(); if (cursorView != null) cursorView.requestFocus(); appendLog("LIFECYCLE onResume"); logDisplayMetrics(); }
    @Override protected void onDestroy() { stopMovement(); handler.removeCallbacksAndMessages(null); removeContent(); appendLog("SESSION END"); logHandler = null; if (logThread != null) logThread.quitSafely(); super.onDestroy(); }

    /** Small cursor sprite, positioned with translation so moving it never redraws a full-screen layer. */
    private class CursorView extends View {
        private final Paint outer = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint inner = new Paint(Paint.ANTI_ALIAS_FLAG);
        final float density;
        final int sizePx;
        float cursorX, cursorY, cursorRadius;
        boolean positioned;
        CursorView() {
            super(MainActivity.this);
            density = getResources().getDisplayMetrics().density;
            cursorRadius = 15f * density;
            outer.setStyle(Paint.Style.STROKE); outer.setStrokeWidth(3f * density); outer.setColor(Color.WHITE);
            inner.setStyle(Paint.Style.STROKE); inner.setStrokeWidth(1.5f * density); inner.setColor(Color.rgb(22, 140, 255));
            sizePx = (int) Math.ceil((cursorRadius + 1.5f * density) * 2f) + 2;
        }
        /** Puts the sprite's centre on (cursorX, cursorY). */
        void applyPosition() {
            setTranslationX(cursorX - sizePx / 2f);
            setTranslationY(cursorY - sizePx / 2f);
        }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            c.drawCircle(cx, cy, cursorRadius, outer);
            c.drawCircle(cx, cy, cursorRadius - 2f * density, inner);
        }
    }
}
