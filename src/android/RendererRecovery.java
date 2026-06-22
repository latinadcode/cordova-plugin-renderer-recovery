package com.latinad.player.plugin;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebView;

import org.apache.cordova.CordovaPlugin;

public class RendererRecovery extends CordovaPlugin {

    private static final String TAG = "RendererRecovery";
    // Trim WebView memory every 30 minutes to slow down cache growth
    private static final long TRIM_INTERVAL_MS = 30 * 60 * 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable trimTask;

    @Override
    public void pluginInitialize() {
        trimTask = new Runnable() {
            @Override
            public void run() {
                trimMemory();
                handler.postDelayed(this, TRIM_INTERVAL_MS);
            }
        };
        handler.postDelayed(trimTask, TRIM_INTERVAL_MS);
        Log.d(TAG, "Periodic memory trim scheduled every " + (TRIM_INTERVAL_MS / 60000) + " min");
    }

    private void trimMemory() {
        try {
            WebView wv = (WebView) webView.getView();
            if (wv != null) {
                // freeMemory() tells Chromium to purge its image decode cache
                wv.freeMemory();
                Log.d(TAG, "WebView.freeMemory() called");
            }
        } catch (Exception e) {
            Log.w(TAG, "freeMemory error: " + e.getMessage());
        }
    }

    @Override
    public boolean onRenderProcessGone(final WebView view, RenderProcessGoneDetail detail) {
        Log.e(TAG, "Renderer process gone (didCrash=" + detail.didCrash() + "), recovering...");

        cordova.getActivity().runOnUiThread(() -> {
            try {
                view.loadUrl("about:blank");
            } catch (Exception e) {
                Log.e(TAG, "Error blanking crashed WebView", e);
            }
            // recreate() keeps the process alive (no cached-state gap) while
            // creating a fresh Activity + WebView with a new renderer process.
            cordova.getActivity().recreate();
        });

        return true;
    }

    @Override
    public void onDestroy() {
        if (trimTask != null) handler.removeCallbacks(trimTask);
    }
}
