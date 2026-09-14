package com.hsw.simonapp;

import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.hsw.simonapp.input.AndroidTouchInputAdapter;
import com.hsw.simonapp.ui.GameSessionState;
import com.hsw.simonapp.ui.LifecycleStateMachine;
import com.hsw.simonapp.engine.api.EmbeddedEngine;
import com.hsw.simonapp.engine.api.EmbeddedEngine.Result;

import java.io.File;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final int PAUSED_SURFACE_REBUILD_FRAMES = 4;
    private static final int BUTTON_VOLUME_SEEKBAR_MAX = 100;
    private static final int DEFAULT_BUTTON_VOLUME_PROGRESS = 100;
    private static final float GAME_MENU_OVERLAY_HEIGHT_FRACTION = 0.46f;
    private static final String GAME_STATS_PREFERENCES = "simon_game_stats";
    private static final String PREF_HAS_LAST_RESULT = "has_last_result";
    private static final String PREF_LAST_RESULT = "last_result";
    private static final String PREF_RECORD = "record";

    private SurfaceView surfaceView;
    private FrameLayout gameContainer;
    private TextView statusText;
    private TextView lastResultText;
    private TextView recordText;
    private Button selectedObjectButton;
    private CheckBox runPauseCheckbox;
    private SeekBar buttonVolumeSeekBar;
    private View gameMenuOverlay;
    private EmbeddedEngine engine;
    private SurfaceHolder activeSurfaceHolder;
    private int surfaceWidth;
    private int surfaceHeight;
    private boolean runRequested = true;
    private GameSessionState gameSessionState;

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
        gameSessionState = restoreGameSessionState();
        gameContainer = findViewById(R.id.game_container);
        surfaceView = findViewById(R.id.vulkan_surface);
        statusText = findViewById(R.id.status_text);
        lastResultText = findViewById(R.id.last_result_text);
        recordText = findViewById(R.id.record_text);
        selectedObjectButton = findViewById(R.id.button);
        runPauseCheckbox = findViewById(R.id.run_pause_checkbox);
        buttonVolumeSeekBar = findViewById(R.id.button_volume_seekbar);
        gameMenuOverlay = findViewById(R.id.game_menu_overlay);
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
        buttonVolumeSeekBar.setMax(BUTTON_VOLUME_SEEKBAR_MAX);
        buttonVolumeSeekBar.setProgress(DEFAULT_BUTTON_VOLUME_PROGRESS);
        engine.setButtonToneVolume(buttonVolumeProgressToVolume(buttonVolumeSeekBar.getProgress()));
        buttonVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                engine.setButtonToneVolume(buttonVolumeProgressToVolume(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        findViewById(R.id.save_button).setOnClickListener(view -> saveGame());
        findViewById(R.id.load_button).setOnClickListener(view -> loadGame());
        findViewById(R.id.start_game_button).setOnClickListener(view -> startGameRun());
        findViewById(R.id.exit_game_button).setOnClickListener(view -> exitApplication());
        engine.setSelectedEntityListener(entityId ->
                runOnUiThread(() -> selectedObjectButton.setText(selectedObjectButtonText(entityId))));
        engine.setButtonPressListener(entityId ->
                runOnUiThread(this::recordGameButtonPress));
        engine.setWorldCoordinateTouchListener((x, y) ->
                runOnUiThread(() -> selectedObjectButton.setText(coordinateButtonText(x, y))));
        surfaceView.getHolder().addCallback(this);
        surfaceView.setOnTouchListener(new AndroidTouchInputAdapter(touchInputEvent -> {
            if (gameSessionState.isRunning()) {
                engine.queueTouchInput(touchInputEvent);
            }
        }));
        surfaceView.setClickable(true);
        gameContainer.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                updateGameMenuOverlayHeight());
        gameContainer.post(this::updateGameMenuOverlayHeight);
        gameMenuOverlay.bringToFront();
        updateGameMenuStats();
        statusText.setText(R.string.game_menu_ready);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (gameSessionState.isRunning()) {
                    finishActiveGameRun();
                    showGameMenuOverlay();
                    return;
                }
                finish();
            }
        });
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

    private void startGameRun() {
        gameSessionState.startRun();
        gameMenuOverlay.setVisibility(View.GONE);
        selectedObjectButton.setText(selectedObjectButtonText(null));
        statusText.setText(getString(R.string.game_current_result, gameSessionState.getCurrentResult()));
        if (!runRequested) {
            runPauseCheckbox.setChecked(true);
        } else {
            maybeInitializeAndStartEngine();
        }
    }

    private void finishActiveGameRun() {
        if (!gameSessionState.finishRun()) {
            return;
        }
        saveGameSessionState();
        updateGameMenuStats();
        statusText.setText(getString(R.string.game_finished, gameSessionState.getLastResult()));
    }

    private void recordGameButtonPress() {
        if (!gameSessionState.recordButtonPress()) {
            return;
        }
        statusText.setText(getString(R.string.game_current_result, gameSessionState.getCurrentResult()));
    }

    private void showGameMenuOverlay() {
        updateGameMenuStats();
        gameMenuOverlay.setVisibility(View.VISIBLE);
        gameMenuOverlay.bringToFront();
    }

    private void exitApplication() {
        finishActiveGameRun();
        finish();
    }

    private void updateGameMenuStats() {
        if (gameSessionState.hasLastResult()) {
            lastResultText.setText(getString(R.string.game_menu_last_result,
                    gameSessionState.getLastResult()));
        } else {
            lastResultText.setText(R.string.game_menu_no_last_result);
        }
        recordText.setText(getString(R.string.game_menu_record, gameSessionState.getRecord()));
    }

    private GameSessionState restoreGameSessionState() {
        SharedPreferences preferences = getSharedPreferences(GAME_STATS_PREFERENCES, MODE_PRIVATE);
        return new GameSessionState(preferences.getBoolean(PREF_HAS_LAST_RESULT, false),
                preferences.getInt(PREF_LAST_RESULT, 0),
                preferences.getInt(PREF_RECORD, 0));
    }

    private void saveGameSessionState() {
        getSharedPreferences(GAME_STATS_PREFERENCES, MODE_PRIVATE)
                .edit()
                .putBoolean(PREF_HAS_LAST_RESULT, gameSessionState.hasLastResult())
                .putInt(PREF_LAST_RESULT, gameSessionState.getLastResult())
                .putInt(PREF_RECORD, gameSessionState.getRecord())
                .apply();
    }

    private void updateGameMenuOverlayHeight() {
        int containerHeight = gameContainer.getHeight();
        if (containerHeight <= 0) {
            return;
        }
        int minimumHeight = getResources().getDimensionPixelSize(R.dimen.game_menu_overlay_min_height);
        int targetHeight = Math.max(minimumHeight,
                Math.round(containerHeight * GAME_MENU_OVERLAY_HEIGHT_FRACTION));
        targetHeight = Math.min(targetHeight, containerHeight);

        ViewGroup.LayoutParams layoutParams = gameMenuOverlay.getLayoutParams();
        if (layoutParams.height == targetHeight) {
            return;
        }
        layoutParams.height = targetHeight;
        gameMenuOverlay.setLayoutParams(layoutParams);
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

    private static float buttonVolumeProgressToVolume(int progress) {
        return Math.max(0, Math.min(BUTTON_VOLUME_SEEKBAR_MAX, progress))
                / (float) BUTTON_VOLUME_SEEKBAR_MAX;
    }
}
