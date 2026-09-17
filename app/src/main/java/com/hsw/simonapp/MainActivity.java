package com.hsw.simonapp;

import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
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
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String TAG = "MainActivity";
    private static final int BUTTON_VOLUME_SEEKBAR_MAX = 100;
    private static final int DEFAULT_BUTTON_VOLUME_PROGRESS = 100;
    private static final int GAME_START_COUNTDOWN_FROM = 3;
    private static final long GAME_START_COUNTDOWN_STEP_MILLIS = 1000L;
    private static final float GAME_MENU_OVERLAY_HEIGHT_FRACTION = 0.46f;
    private static final String GAME_STATS_PREFERENCES = "simon_game_stats";
    private static final String LEGACY_PREF_RUNNING = "running";
    private static final String LEGACY_PREF_CURRENT_RESULT = "current_result";
    private static final String PREF_HAS_LAST_RESULT = "has_last_result";
    private static final String PREF_LAST_RESULT = "last_result";
    private static final String PREF_RECORD = "record";
    private static final String INSTANCE_HAS_LAST_RESULT = "game_session_has_last_result";
    private static final String INSTANCE_LAST_RESULT = "game_session_last_result";
    private static final String INSTANCE_RECORD = "game_session_record";

    private GameSurfaceView surfaceView;
    private FrameLayout adContainer;
    private FrameLayout gameContainer;
    private TextView statusText;
    private TextView lastResultText;
    private TextView recordText;
    private Button gameMenuPrimaryButton;
    private Button privacyOptionsButton;
    private SeekBar buttonVolumeSeekBar;
    private View gameMenuOverlay;
    private EmbeddedEngine engine;
    private SurfaceHolder activeSurfaceHolder;
    private int surfaceWidth;
    private int surfaceHeight;
    private GameSessionState gameSessionState;
    private AdView bannerAdView;
    private ConsentInformation consentInformation;
    private boolean mobileAdsInitializationStarted;
    private boolean mobileAdsInitialized;
    private boolean gameStartCountdownActive;
    private boolean simonGameplayStarted;
    private Runnable gameStartCountdownRunnable;

    private final Handler gameStartCountdownHandler = new Handler(Looper.getMainLooper());
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
        adContainer = findViewById(R.id.ad_container);
        gameContainer = findViewById(R.id.game_container);
        surfaceView = findViewById(R.id.vulkan_surface);
        statusText = findViewById(R.id.status_text);
        lastResultText = findViewById(R.id.last_result_text);
        recordText = findViewById(R.id.record_text);
        gameMenuPrimaryButton = findViewById(R.id.start_game_button);
        privacyOptionsButton = findViewById(R.id.privacy_options_button);
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
        privacyOptionsButton.setOnClickListener(view -> showPrivacyOptionsForm());
        engine.setGameScoreListener(score ->
                runOnUiThread(() -> updateGameScore(score)));
        engine.setGameOverListener(score ->
                runOnUiThread(() -> finishGameRun(score)));
        surfaceView.getHolder().addCallback(this);
        surfaceView.setOnTouchListener(new AndroidTouchInputAdapter(touchInputEvent -> {
            if (gameSessionState.isRunning() && simonGameplayStarted && !isGameMenuOverlayVisible()) {
                engine.queueTouchInput(touchInputEvent);
            }
        }));
        surfaceView.setClickable(true);
        gameContainer.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                updateGameMenuOverlayHeight());
        gameContainer.post(this::updateGameMenuOverlayHeight);
        gameMenuOverlay.bringToFront();
        initializeMobileAdsAfterConsent();
        initializeGameMenuState();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (hasActiveGameSession()) {
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
        if (bannerAdView != null) {
            bannerAdView.resume();
        }
        lifecycleStateMachine.onResumed();
        maybeInitializeAndStartEngine();
    }

    @Override
    protected void onPause() {
        if (bannerAdView != null) {
            bannerAdView.pause();
        }
        if (hasActiveGameSession()) {
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
        saveRestorableGameStats(outState);
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
        cancelGameStartCountdown();
        destroyBannerAd();
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
        simonGameplayStarted = false;
        hideGameMenuOverlay();
        maybeInitializeAndStartEngine();
        startGameStartCountdown();
    }

    private void continueActiveGameRun() {
        hideGameMenuOverlay();
        maybeInitializeAndStartEngine();
        if (simonGameplayStarted) {
            statusText.setText(getString(R.string.game_current_result, gameSessionState.getCurrentResult()));
        } else {
            startGameStartCountdown();
        }
    }

    private void finishActiveGameRun() {
        cancelGameStartCountdown();
        simonGameplayStarted = false;
        if (!gameSessionState.finishRun()) {
            return;
        }
        saveCompletedGameStats();
        updateGameMenuStats();
        statusText.setText(getString(R.string.game_finished, gameSessionState.getLastResult()));
    }

    private void updateGameScore(int score) {
        if (!gameSessionState.setCurrentResult(score)) {
            return;
        }
        statusText.setText(getString(R.string.game_current_result, gameSessionState.getCurrentResult()));
    }

    private void finishGameRun(int finalScore) {
        cancelGameStartCountdown();
        simonGameplayStarted = false;
        if (!gameSessionState.finishRun(finalScore)) {
            return;
        }
        saveCompletedGameStats();
        updateGameMenuStats();
        updateGameMenuPrimaryButton();
        statusText.setText(getString(R.string.game_finished, gameSessionState.getLastResult()));
        gameMenuOverlay.setVisibility(View.VISIBLE);
        gameMenuOverlay.bringToFront();
        if (engine.isInitialized()) {
            engine.stop();
        }
    }

    private void showGameMenuOverlay() {
        cancelGameStartCountdown();
        updateGameMenuStats();
        updateGameMenuPrimaryButton();
        if (gameSessionState.isRunning()) {
            statusText.setText(getString(R.string.game_current_result, gameSessionState.getCurrentResult()));
        } else {
            statusText.setText(R.string.game_menu_ready);
        }
        gameMenuOverlay.setVisibility(View.VISIBLE);
        gameMenuOverlay.bringToFront();
        if (gameSessionState.isRunning() && engine.isInitialized()) {
            engine.stop();
        }
    }

    private void hideGameMenuOverlay() {
        gameMenuOverlay.setVisibility(View.GONE);
    }

    private void initializeMobileAdsAfterConsent() {
        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consentInformation.requestConsentInfoUpdate(
                this,
                params,
                () -> {
                    updatePrivacyOptionsButtonVisibility();
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                            this,
                            formError -> {
                                if (formError != null) {
                                    Log.w(TAG, "Consent form failed: " + formError);
                                }
                                updatePrivacyOptionsButtonVisibility();
                                initializeMobileAdsIfConsentAllows();
                            });
                    initializeMobileAdsIfConsentAllows();
                },
                requestConsentError -> {
                    Log.w(TAG, "Consent info update failed: " + requestConsentError);
                    updatePrivacyOptionsButtonVisibility();
                    initializeMobileAdsIfConsentAllows();
                });
    }

    private void initializeMobileAdsIfConsentAllows() {
        if (consentInformation == null || !consentInformation.canRequestAds()) {
            destroyBannerAd();
            return;
        }
        initializeMobileAds();
    }

    private void initializeMobileAds() {
        if (mobileAdsInitialized) {
            loadAdaptiveBannerAd();
            return;
        }
        if (mobileAdsInitializationStarted) {
            return;
        }
        mobileAdsInitializationStarted = true;
        new Thread(() -> MobileAds.initialize(this, initializationStatus ->
                runOnUiThread(() -> {
                    mobileAdsInitialized = true;
                    loadAdaptiveBannerAd();
                }))).start();
    }

    private void loadAdaptiveBannerAd() {
        if (isFinishing() || isDestroyed() || adContainer == null || bannerAdView != null) {
            return;
        }
        if (adContainer.getWidth() <= 0) {
            adContainer.post(this::loadAdaptiveBannerAd);
            return;
        }

        AdView adView = new AdView(this);
        adView.setAdUnitId(getString(R.string.admob_banner_ad_unit_id));
        AdSize adSize = createAdaptiveBannerSize();
        adView.setAdSize(adSize);
        bannerAdView = adView;

        adContainer.removeAllViews();
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                adSize.getHeightInPixels(this),
                Gravity.CENTER);
        adContainer.addView(adView, layoutParams);
        adView.loadAd(new AdRequest.Builder().build());
    }

    private AdSize createAdaptiveBannerSize() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int adWidthPixels = adContainer.getWidth();
        if (adWidthPixels <= 0) {
            adWidthPixels = displayMetrics.widthPixels;
        }
        int adWidth = (int) (adWidthPixels / displayMetrics.density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }

    private void destroyBannerAd() {
        if (bannerAdView == null) {
            return;
        }
        adContainer.removeView(bannerAdView);
        bannerAdView.destroy();
        bannerAdView = null;
    }

    private void updatePrivacyOptionsButtonVisibility() {
        if (privacyOptionsButton == null || consentInformation == null) {
            return;
        }
        boolean privacyOptionsRequired =
                consentInformation.getPrivacyOptionsRequirementStatus()
                        == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
        privacyOptionsButton.setVisibility(privacyOptionsRequired ? View.VISIBLE : View.GONE);
    }

    private void showPrivacyOptionsForm() {
        if (consentInformation == null) {
            return;
        }
        UserMessagingPlatform.showPrivacyOptionsForm(this, formError -> {
            if (formError != null) {
                Log.w(TAG, "Privacy options form failed: " + formError);
            }
            updatePrivacyOptionsButtonVisibility();
            initializeMobileAdsIfConsentAllows();
        });
    }

    private void startGameStartCountdown() {
        if (gameStartCountdownActive) {
            return;
        }
        gameStartCountdownActive = true;
        showGameStartCountdown(GAME_START_COUNTDOWN_FROM);
    }

    private void showGameStartCountdown(int countdownValue) {
        if (!gameStartCountdownActive || isGameMenuOverlayVisible()) {
            cancelGameStartCountdown();
            return;
        }

        statusText.setText(getString(R.string.game_start_countdown, countdownValue));
        if (countdownValue <= 0) {
            gameStartCountdownActive = false;
            gameStartCountdownRunnable = null;
            gameSessionState.startRun();
            simonGameplayStarted = true;
            statusText.setText(getString(R.string.game_current_result, gameSessionState.getCurrentResult()));
            engine.startSimonGame();
            return;
        }

        gameStartCountdownRunnable = () -> showGameStartCountdown(countdownValue - 1);
        gameStartCountdownHandler.postDelayed(gameStartCountdownRunnable,
                GAME_START_COUNTDOWN_STEP_MILLIS);
    }

    private void cancelGameStartCountdown() {
        if (gameStartCountdownRunnable != null) {
            gameStartCountdownHandler.removeCallbacks(gameStartCountdownRunnable);
            gameStartCountdownRunnable = null;
        }
        gameStartCountdownActive = false;
    }

    private boolean isGameMenuOverlayVisible() {
        return gameMenuOverlay != null && gameMenuOverlay.getVisibility() == View.VISIBLE;
    }

    private boolean hasActiveGameSession() {
        return gameSessionState.isRunning() || gameStartCountdownActive;
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
            return new GameSessionState(savedInstanceState.getBoolean(INSTANCE_HAS_LAST_RESULT,
                    preferences.getBoolean(PREF_HAS_LAST_RESULT, false)),
                    savedInstanceState.getInt(INSTANCE_LAST_RESULT, preferences.getInt(PREF_LAST_RESULT, 0)),
                    savedInstanceState.getInt(INSTANCE_RECORD, preferences.getInt(PREF_RECORD, 0)));
        }
        return new GameSessionState(
                preferences.getBoolean(PREF_HAS_LAST_RESULT, false),
                preferences.getInt(PREF_LAST_RESULT, 0),
                preferences.getInt(PREF_RECORD, 0));
    }

    private void saveRestorableGameStats(Bundle outState) {
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
