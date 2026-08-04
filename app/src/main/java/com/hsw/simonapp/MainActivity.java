package com.hsw.simonapp;

import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.hsw.simonapp.input.AndroidTouchInputAdapter;
import com.hsw.simonapp.ui.LifecycleStateMachine;
import com.hsw.simonapp.engine.api.EmbeddedEngine;
import com.hsw.simonapp.engine.api.EmbeddedEngine.Result;

import java.io.File;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final int PAUSED_SURFACE_REBUILD_FRAMES = 4;

    private SurfaceView surfaceView;
    private TextView statusText;
    private Button selectedObjectButton;
    private CheckBox runPauseCheckbox;
    private EmbeddedEngine engine;
    private SurfaceHolder activeSurfaceHolder;
    private int surfaceWidth;
    private int surfaceHeight;
    private boolean runRequested = true;

    private final LifecycleStateMachine lifecycleStateMachine = new LifecycleStateMachine();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
        }


        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        engine = new EmbeddedEngine(getAssets());
        surfaceView = findViewById(R.id.vulkan_surface);
        statusText = findViewById(R.id.status_text);
        selectedObjectButton = findViewById(R.id.button);
        runPauseCheckbox = findViewById(R.id.run_pause_checkbox);
        selectedObjectButton.setText(selectedObjectButtonText(null));
        selectedObjectButton.setOnClickListener(view -> engine.reverseSelectedEntityDirection());
        runPauseCheckbox.setChecked(runRequested);
        runPauseCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            runRequested = isChecked;
            if (isChecked) {
                maybeInitializeAndStartEngine();
            } else if (engine.isInitialized()) {
                engine.stop();
            }
        });
        findViewById(R.id.save_button).setOnClickListener(view -> saveGame());
        findViewById(R.id.load_button).setOnClickListener(view -> loadGame());
        engine.setSelectedEntityListener(entityId ->
                runOnUiThread(() -> selectedObjectButton.setText(selectedObjectButtonText(entityId))));
        engine.setWorldCoordinateTouchListener((x, y) ->
                runOnUiThread(() -> selectedObjectButton.setText(coordinateButtonText(x, y))));
        surfaceView.getHolder().addCallback(this);
        surfaceView.setOnTouchListener(new AndroidTouchInputAdapter(engine::queueTouchInput));
        surfaceView.setClickable(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        lifecycleStateMachine.onResumed();
        maybeInitializeAndStartEngine();
    }

    @Override
    protected void onPause() {
        lifecycleStateMachine.onPaused();
        stopAndReleaseRenderer();
        super.onPause();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        rememberSurface(holder);
        lifecycleStateMachine.onSurfaceAvailable(surfaceWidth, surfaceHeight);

        if (engine.isInitialized()) {
            stopAndReleaseRenderer();
        }

        maybeInitializeAndStartEngine();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        rememberSurface(holder, width, height);
        lifecycleStateMachine.onSurfaceAvailable(surfaceWidth, surfaceHeight);
        if (surfaceWidth == 0 || surfaceHeight == 0) {
            stopAndReleaseRenderer();
            return;
        }
        if (engine.isInitialized() && width > 0 && height > 0) {
            engine.resize(surfaceWidth, surfaceHeight);
        }
        maybeInitializeAndStartEngine();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        lifecycleStateMachine.onSurfaceDestroyed();
        clearSurface(holder);
        stopAndReleaseRenderer();
    }

    @Override
    protected void onDestroy() {
        if (surfaceView != null) {
            surfaceView.getHolder().removeCallback(this);
        }
        stopAndReleaseRenderer();
        super.onDestroy();
    }

    private void maybeInitializeAndStartEngine() {
        if (!lifecycleStateMachine.shouldOwnRenderer() || !hasRenderableSurface()) {
            return;
        }

        if (!engine.isInitialized()) {
            Result initializationResult = engine.initialize(this, activeSurfaceHolder.getSurface());
            lifecycleStateMachine.onRendererInitialized(initializationResult.isSuccess());
            if (!initializationResult.isSuccess()) {
                String diagnostic = initializationResult.getErrorCode() + ": " + initializationResult.getMessage();
                statusText.setText(getString(R.string.vulkan_failed, diagnostic));
                return;
            }
        } else {
            lifecycleStateMachine.onRendererInitialized(true);
        }

        if (surfaceWidth > 0 && surfaceHeight > 0) {
            engine.resize(surfaceWidth, surfaceHeight);
        }
        maybeStartEngine();
    }

    private void maybeStartEngine() {
        if (!lifecycleStateMachine.shouldRender() || !engine.isInitialized()) {
            return;
        }
        if (runRequested) {
            engine.start();
        } else {
            engine.renderCurrentFrames(PAUSED_SURFACE_REBUILD_FRAMES);
        }
    }

    private void stopAndReleaseRenderer() {
        if (engine != null) {
            engine.stopAndRelease();
        }
        lifecycleStateMachine.onRendererInitialized(false);
    }

    private void saveGame() {
        boolean saved = engine.saveGame(saveFile());
        statusText.setText(saved ? R.string.save_success : R.string.save_failed);
    }

    private void loadGame() {
        boolean loaded = engine.loadGame(saveFile());
        statusText.setText(loaded ? R.string.load_success : R.string.load_failed);
        if (loaded) {
            maybeInitializeAndStartEngine();
        }
    }

    private File saveFile() {
        return new File(new File(getFilesDir(), "saves"), "slot_1.json");
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

    private void clearSurface(SurfaceHolder holder) {
        if (activeSurfaceHolder == holder) {
            activeSurfaceHolder = null;
            surfaceWidth = 0;
            surfaceHeight = 0;
        }
    }

    private boolean hasRenderableSurface() {
        return activeSurfaceHolder != null
                && activeSurfaceHolder.getSurface() != null
                && activeSurfaceHolder.getSurface().isValid()
                && surfaceWidth > 0
                && surfaceHeight > 0;
    }

    private static String selectedObjectButtonText(Integer entityId) {
        if (entityId == null) {
            return "Obj: -";
        }
        return "Obj: " + entityId;
    }

    private static String coordinateButtonText(float x, float y) {
        return String.format(Locale.US, "X: %.2f Y: %.2f", x, y);
    }
}
