package com.hsw.vulkansmokehost;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;

public final class SmokeHostActivity extends Activity implements SurfaceHolder.Callback {

    private static final int STATUS_BACKGROUND = 0x99000000;
    private static final int STATUS_MARGIN_DP = 16;
    private static final int STATUS_PADDING_HORIZONTAL_DP = 10;
    private static final int STATUS_PADDING_VERTICAL_DP = 8;
    private static final int STATUS_REFRESH_FRAME_INTERVAL = 15;
    private static final String SMOKE_TEXTURE_ASSET = "maca8x1.png";
    private static final String PING_TEXTURE_ASSET = "ping.png";
    private static final String WIFI_TEXTURE_ASSET = "wifi.png";

    private final SmokeSampleRenderer renderer = new SmokeSampleRenderer();
    private final SmokeRenderLoopController renderLoopController =
            new SmokeRenderLoopController(this::renderFrameOnConsumer);

    private SurfaceView surfaceView;
    private TextView statusText;
    private CheckBox infoToggle;
    private SurfaceHolder activeSurfaceHolder;
    private volatile boolean resumed;
    private volatile boolean rendererInitialized;
    private volatile int surfaceWidth;
    private volatile int surfaceHeight;
    private long firstFrameNanos;
    private long fpsWindowStartNanos;
    private int fpsWindowFrameCount;
    private int renderedFrameCount;
    private float currentFps;
    private String textureUploadStatus = "not requested";
    private String swapchainResizeStatus = "waiting for surface";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        surfaceView = new SurfaceView(this);
        root.addView(surfaceView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        statusText = new TextView(this);
        statusText.setTextColor(Color.WHITE);
        statusText.setTextSize(13.0f);
        statusText.setTypeface(Typeface.MONOSPACE);
        statusText.setBackgroundColor(STATUS_BACKGROUND);
        statusText.setPadding(dp(STATUS_PADDING_HORIZONTAL_DP),
                dp(STATUS_PADDING_VERTICAL_DP),
                dp(STATUS_PADDING_HORIZONTAL_DP),
                dp(STATUS_PADDING_VERTICAL_DP));
        statusText.setText(R.string.smoke_waiting_for_surface);

        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        statusParams.gravity = Gravity.TOP | Gravity.START;
        statusParams.setMargins(dp(STATUS_MARGIN_DP), dp(STATUS_MARGIN_DP), dp(STATUS_MARGIN_DP), 0);
        root.addView(statusText, statusParams);

        infoToggle = new CheckBox(this);
        infoToggle.setText(R.string.smoke_info_toggle);
        infoToggle.setTextColor(Color.WHITE);
        infoToggle.setTextSize(13.0f);
        infoToggle.setChecked(true);
        infoToggle.setBackgroundColor(STATUS_BACKGROUND);
        infoToggle.setPadding(dp(STATUS_PADDING_HORIZONTAL_DP),
                dp(STATUS_PADDING_VERTICAL_DP),
                dp(STATUS_PADDING_HORIZONTAL_DP),
                dp(STATUS_PADDING_VERTICAL_DP));
        infoToggle.setOnCheckedChangeListener((buttonView, isChecked) ->
                statusText.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        FrameLayout.LayoutParams toggleParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        toggleParams.gravity = Gravity.TOP | Gravity.END;
        toggleParams.setMargins(dp(STATUS_MARGIN_DP), dp(STATUS_MARGIN_DP), dp(STATUS_MARGIN_DP), 0);
        root.addView(infoToggle, toggleParams);

        setContentView(root);
        surfaceView.getHolder().addCallback(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        maybeStartRenderer();
    }

    @Override
    protected void onPause() {
        resumed = false;
        stopRenderer(getString(R.string.smoke_paused));
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (surfaceView != null) {
            surfaceView.getHolder().removeCallback(this);
        }
        stopRenderer(getString(R.string.smoke_stopped));
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        rememberSurface(holder);
        if (rendererInitialized) {
            stopRenderer(getString(R.string.smoke_waiting_for_surface));
        }
        maybeStartRenderer();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        rememberSurface(holder, width, height);
        if (surfaceWidth == 0 || surfaceHeight == 0) {
            stopRenderer(getString(R.string.smoke_waiting_for_surface));
            return;
        }
        if (rendererInitialized) {
            renderer.resize(surfaceWidth, surfaceHeight);
            swapchainResizeStatus = "resize requested " + surfaceWidth + "x" + surfaceHeight;
        } else {
            swapchainResizeStatus = "surface " + surfaceWidth + "x" + surfaceHeight;
        }
        maybeStartRenderer();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (activeSurfaceHolder == holder) {
            activeSurfaceHolder = null;
            surfaceWidth = 0;
            surfaceHeight = 0;
        }
        stopRenderer(getString(R.string.smoke_waiting_for_surface));
    }

    private boolean renderFrameOnConsumer(SmokeRenderLoopController.FrameContext frameContext) {
        if (!resumed || !rendererInitialized) {
            return false;
        }
        long frameTimeNanos = frameContext.getFrameTimeNanos();
        if (firstFrameNanos == 0L) {
            firstFrameNanos = frameTimeNanos;
        }

        float elapsedSeconds = (frameTimeNanos - firstFrameNanos) / 1_000_000_000.0f;
        if (!renderer.renderSmokeFrame(elapsedSeconds, surfaceWidth, surfaceHeight)) {
            String failure = getString(R.string.smoke_render_failed, renderer.getLastVulkanError());
            runOnUiThread(() -> failRenderer(failure));
            return false;
        }

        renderedFrameCount++;
        updateFps(frameTimeNanos);
        if (renderedFrameCount % STATUS_REFRESH_FRAME_INTERVAL == 1) {
            String status = buildStatusOverlay();
            runOnUiThread(() -> {
                if (rendererInitialized && statusText != null) {
                    statusText.setText(status);
                }
            });
        }
        return true;
    }

    private void maybeStartRenderer() {
        if (!resumed || rendererInitialized || !hasRenderableSurface()) {
            return;
        }

        statusText.setText(R.string.smoke_initializing);
        textureUploadStatus = "initializing";
        swapchainResizeStatus = "initializing";
        Surface surface = activeSurfaceHolder.getSurface();
        if (!renderer.init(this, surface, getAssets())) {
            failRenderer(getString(R.string.smoke_init_failed, renderer.getLastVulkanError()));
            return;
        }

        rendererInitialized = true;
        statusText.setText(R.string.smoke_uploading_texture);
        SmokeTextureAssetLoader textureLoader = new SmokeTextureAssetLoader(getAssets(), renderer);
        if (!uploadRequiredTexture(textureLoader, SMOKE_TEXTURE_ASSET, SmokeSampleRenderer.SMOKE_TEXTURE_SLOT) ||
                !uploadRequiredTexture(textureLoader, PING_TEXTURE_ASSET, SmokeSampleRenderer.PING_TEXTURE_SLOT) ||
                !uploadRequiredTexture(textureLoader, WIFI_TEXTURE_ASSET, SmokeSampleRenderer.WIFI_TEXTURE_SLOT)) {
            return;
        }
        textureUploadStatus = "queued " + SMOKE_TEXTURE_ASSET + ", " + PING_TEXTURE_ASSET + ", " + WIFI_TEXTURE_ASSET;

        firstFrameNanos = 0L;
        fpsWindowStartNanos = 0L;
        fpsWindowFrameCount = 0;
        currentFps = 0.0f;
        renderedFrameCount = 0;
        if (surfaceWidth > 0 && surfaceHeight > 0) {
            renderer.resize(surfaceWidth, surfaceHeight);
            swapchainResizeStatus = "resize requested " + surfaceWidth + "x" + surfaceHeight;
        }
        updateStatusOverlay();
        renderLoopController.start();
    }

    private boolean uploadRequiredTexture(SmokeTextureAssetLoader textureLoader, String assetName, int textureSlot) {
        textureUploadStatus = "loading " + assetName;
        SmokeTextureAssetLoader.TextureUploadResult uploadResult =
                textureLoader.uploadTextureToSlot(assetName, textureSlot);
        if (!uploadResult.isSuccess()) {
            textureUploadStatus = uploadResult.getMessage();
            failRenderer(getString(R.string.smoke_texture_upload_failed, uploadResult.getMessage()));
            return false;
        }
        return true;
    }

    private void stopRenderer(String status) {
        renderLoopController.stop();
        if (rendererInitialized) {
            renderer.cleanup();
            rendererInitialized = false;
        }
        if (statusText != null) {
            statusText.setText(status);
        }
        swapchainResizeStatus = status;
    }

    private void failRenderer(String status) {
        renderLoopController.stop();
        if (rendererInitialized) {
            renderer.cleanup();
            rendererInitialized = false;
        }
        statusText.setText(status);
        swapchainResizeStatus = status;
    }

    private void updateFps(long frameTimeNanos) {
        if (fpsWindowStartNanos == 0L) {
            fpsWindowStartNanos = frameTimeNanos;
            fpsWindowFrameCount = 0;
        }

        fpsWindowFrameCount++;
        long elapsedNanos = frameTimeNanos - fpsWindowStartNanos;
        if (elapsedNanos >= 1_000_000_000L) {
            currentFps = (fpsWindowFrameCount * 1_000_000_000.0f) / elapsedNanos;
            fpsWindowStartNanos = frameTimeNanos;
            fpsWindowFrameCount = 0;
        }
    }

    private void updateStatusOverlay() {
        statusText.setText(buildStatusOverlay());
    }

    private String buildStatusOverlay() {
        return getString(R.string.smoke_running,
                renderer.getSceneMode(),
                currentFps,
                renderedFrameCount,
                joinStatus(textureUploadStatus, renderer.getTextureUploadStatus()),
                emptyToNone(renderer.getLastVulkanError()),
                joinStatus(swapchainResizeStatus, renderer.getSwapchainStatus()),
                renderer.getCurrentAtlasFrame(),
                renderer.getClipStatus());
    }

    private String joinStatus(String javaStatus, String nativeStatus) {
        if (nativeStatus == null || nativeStatus.isEmpty()) {
            return javaStatus;
        }
        if (javaStatus == null || javaStatus.isEmpty()) {
            return nativeStatus;
        }
        return javaStatus + " | " + nativeStatus;
    }

    private String emptyToNone(String value) {
        return value == null || value.isEmpty() ? "none" : value;
    }

    private void rememberSurface(SurfaceHolder holder) {
        Rect surfaceFrame = holder.getSurfaceFrame();
        rememberSurface(holder, surfaceFrame.width(), surfaceFrame.height());
    }

    private void rememberSurface(SurfaceHolder holder, int width, int height) {
        activeSurfaceHolder = holder;
        surfaceWidth = Math.max(width, 0);
        surfaceHeight = Math.max(height, 0);
    }

    private boolean hasRenderableSurface() {
        return activeSurfaceHolder != null
                && activeSurfaceHolder.getSurface() != null
                && activeSurfaceHolder.getSurface().isValid()
                && surfaceWidth > 0
                && surfaceHeight > 0;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
