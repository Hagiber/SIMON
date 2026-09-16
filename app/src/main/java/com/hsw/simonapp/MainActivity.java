package com.hsw.simonapp;

import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
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
import com.hsw.simonapp.input.GameSurfaceView;
import com.hsw.simonapp.ui.GameSessionState;
import com.hsw.simonapp.ui.LifecycleStateMachine;
import com.hsw.simonapp.engine.api.EmbeddedEngine;
import com.hsw.simonapp.engine.api.EmbeddedEngine.Result;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final int BUTTON_VOLUME_SEEKBAR_MAX = 100;
    private static final int DEFAULT_BUTTON_VOLUME_PROGRESS = 100;
    private static final float GAME_MENU_OVERLAY_HEIGHT_FRACTION = 0.46f;
    private static final String GAME_STATS_PREFERENCES = "simon_game_stats";
    private static final String LEGACY_PREF_RUNNING = "running";
    private static final String LEGACY_PREF_CURRENT_RESULT = "current_result";
    private static final String PREF_HAS_LAST_RESULT = "has_last_result";
    private static final String PREF_LAST_RESULT = "last_result";
    private static final String PREF_RECORD = "record";
    private static final String INSTANCE_RUNNING = "game_session_running";
    private static final String INSTANCE_CURRENT_RESULT = "game_session_current_result";
    private static final String INSTANCE_HAS_LAST_RESULT = "game_session_has_last_result";
    private static final String INSTANCE_LAST_RESULT = "game_session_last_result";
    private static final String INSTANCE_RECORD = "game_session_record";

    private GameSurfaceView surfaceView;
    private FrameLayout gameContainer;
    private TextView statusText;
    private TextView lastResultText;
    private TextView recordText;
    private Button gameMenuPrimaryButton;
    private SeekBar buttonVolumeSeekBar;
    private View gameMenuOverlay;
    private EmbeddedEngine engine;
    private SurfaceHolder activeSurfaceHolder;
    private int surfaceWidth;
    private int surfaceHeight;
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
        gameSessionState = restoreGameSessionState(savedInstanceState);
        gameContainer = findViewById(R.id.game_container);
        surfaceView = findViewById(R.id.vulkan_surface);
        statusText = findViewById(R.id.status_text);
        lastResultText = findViewById(R.id.last_result_text);
        recordText = findViewById(R.id.record_text);
        gameMenuPrimaryButton = findViewById(R.id.start_game_button);
        buttonVolumeSeekBar = findViewById(R.id.button_volume_seekbar);
        gameMenuOverlay = findViewById(R.id.game_menu_overlay);
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
        gameMenuPrimaryButton.setOnClickListener(view -> handleGameMenuPrimaryAction());
        findViewById(R.id.exit_game_button).setOnClickListener(view -> exitApplication());
        engine.setButtonPressListener(entityId ->
                runOnUiThread(this::recordGameButtonPress));
        surfaceView.getHolder().addCallback(this);
        surfaceView.setOnTouchListener(new AndroidTouchInputAdapter(touchInputEvent -> {
            if (gameSessionState.isRunning() && !isGameMenuOverlayVisible()) {
                engine.queueTouchInput(touchInputEvent);
            }
        }));
        surfaceView.setClickable(true);
        gameContainer.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                updateGameMenuOverlayHeight());
        gameContainer.post(this::updateGameMenuOverlayHeight);
        gameMenuOverlay.bringToFront();
        initializeGameMenuState();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (gameSessionState.isRunning()) {
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
        if (gameSessionState.isRunning()) {
            showGameMenuOverlay();
        }
        saveCompletedGameStats();
        lifecycleStateMachine.onPaused();
        stopAndReleaseRenderer();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveActiveSessionState(outState);
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
        if (gameSessionState != null && gameSessionState.isRunning() && isGameMenuOverlayVisible()) {
            return;
        }
        engine.start();
    }

    private void stopAndReleaseRenderer() {
        if (engine != null) {
            engine.stopAndRelease();
        }
        lifecycleStateMachine.onRendererInitialized(false);
    }

    private void handleGameMenuPrimaryAction() {
        if (gameSessionState.isRunning()) {
            continueActiveGameRun();
        } else {
            startGameRun();
        }
    }

    private void initializeGameMenuState() {
        updateGameMenuStats();
        updateGameMenuPrimaryButton();
        if (gameSessionState.isRunning()) {
            statusText.setText(getString(R.string.game_current_result, gameSessionState.getCurrentResult()));
        } else {
            statusText.setText(R.string.game_menu_ready);
        }
    }

    private void startGameRun() {
        gameSessionState.startRun();
        hideGameMenuOverlay();
        statusText.setText(getString(R.string.game_current_result, gameSessionState.getCurrentResult()));
        maybeInitializeAndStartEngine();
    }

    private void continueActiveGameRun() {
        hideGameMenuOverlay();
        statusText.setText(getString(R.string.game_current_result, gameSessionState.getCurrentResult()));
        maybeInitializeAndStartEngine();
    }

    private void finishActiveGameRun() {
        if (!gameSessionState.finishRun()) {
            return;
        }
        saveCompletedGameStats();
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
        updateGameMenuPrimaryButton();
        gameMenuOverlay.setVisibility(View.VISIBLE);
        gameMenuOverlay.bringToFront();
        if (gameSessionState.isRunning() && engine.isInitialized()) {
            engine.stop();
        }
    }

    private void hideGameMenuOverlay() {
        gameMenuOverlay.setVisibility(View.GONE);
    }

    private boolean isGameMenuOverlayVisible() {
        return gameMenuOverlay != null && gameMenuOverlay.getVisibility() == View.VISIBLE;
    }

    private void updateGameMenuPrimaryButton() {
        if (gameSessionState.isRunning()) {
            gameMenuPrimaryButton.setText(R.string.game_continue_button_label);
        } else {
            gameMenuPrimaryButton.setText(R.string.game_start_button_label);
        }
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

    private GameSessionState restoreGameSessionState(Bundle savedInstanceState) {
        SharedPreferences preferences = getSharedPreferences(GAME_STATS_PREFERENCES, MODE_PRIVATE);
        if (savedInstanceState != null) {
            return new GameSessionState(savedInstanceState.getBoolean(INSTANCE_RUNNING, false),
                    savedInstanceState.getInt(INSTANCE_CURRENT_RESULT, 0),
                    savedInstanceState.getBoolean(INSTANCE_HAS_LAST_RESULT,
                            preferences.getBoolean(PREF_HAS_LAST_RESULT, false)),
                    savedInstanceState.getInt(INSTANCE_LAST_RESULT, preferences.getInt(PREF_LAST_RESULT, 0)),
                    savedInstanceState.getInt(INSTANCE_RECORD, preferences.getInt(PREF_RECORD, 0)));
        }
        return new GameSessionState(
                preferences.getBoolean(PREF_HAS_LAST_RESULT, false),
                preferences.getInt(PREF_LAST_RESULT, 0),
                preferences.getInt(PREF_RECORD, 0));
    }

    private void saveActiveSessionState(Bundle outState) {
        outState.putBoolean(INSTANCE_RUNNING, gameSessionState.isRunning());
        outState.putInt(INSTANCE_CURRENT_RESULT, gameSessionState.getCurrentResult());
        outState.putBoolean(INSTANCE_HAS_LAST_RESULT, gameSessionState.hasLastResult());
        outState.putInt(INSTANCE_LAST_RESULT, gameSessionState.getLastResult());
        outState.putInt(INSTANCE_RECORD, gameSessionState.getRecord());
    }

    private void saveCompletedGameStats() {
        if (gameSessionState == null) {
            return;
        }

        getSharedPreferences(GAME_STATS_PREFERENCES, MODE_PRIVATE)
                .edit()
                .remove(LEGACY_PREF_RUNNING)
                .remove(LEGACY_PREF_CURRENT_RESULT)
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

    private static float buttonVolumeProgressToVolume(int progress) {
        return Math.max(0, Math.min(BUTTON_VOLUME_SEEKBAR_MAX, progress))
                / (float) BUTTON_VOLUME_SEEKBAR_MAX;
    }
}
