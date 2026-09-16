package com.hsw.simonapp.engine.gameplay;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class SimonGameplayController {

    private static final float SHOW_BUTTON_SECONDS = 0.45f;
    private static final float BETWEEN_BUTTONS_SECONDS = 0.20f;
    private static final float ROUND_COMPLETE_DELAY_SECONDS = 0.50f;

    private final ButtonSource buttonSource;
    private final List<SimonButton> sequence = new ArrayList<>();
    private final Deque<SimonGameplayEvent> pendingEvents = new ArrayDeque<>();

    private SimonPhase phase = SimonPhase.IDLE;
    private int presentationIndex;
    private boolean presentationButtonLit;
    private int expectedInputIndex;
    private int score;
    private float phaseTimerSeconds;

    public SimonGameplayController() {
        this(new RandomButtonSource(new Random()));
    }

    public SimonGameplayController(Random random) {
        this(new RandomButtonSource(Objects.requireNonNull(random, "random")));
    }

    public SimonGameplayController(ButtonSource buttonSource) {
        this.buttonSource = Objects.requireNonNull(buttonSource, "buttonSource");
    }

    public void start() {
        sequence.clear();
        pendingEvents.clear();
        score = 0;
        expectedInputIndex = 0;
        appendNextButton();
        beginSequencePresentation();
    }

    public void reset() {
        sequence.clear();
        pendingEvents.clear();
        phase = SimonPhase.IDLE;
        presentationIndex = 0;
        presentationButtonLit = false;
        expectedInputIndex = 0;
        score = 0;
        phaseTimerSeconds = 0.0f;
    }

    public void advance(float deltaSeconds) {
        if (deltaSeconds <= 0.0f) {
            return;
        }
        if (phase != SimonPhase.SHOWING_SEQUENCE
                && phase != SimonPhase.ROUND_COMPLETE_DELAY) {
            return;
        }

        phaseTimerSeconds -= deltaSeconds;
        while (phaseTimerSeconds <= 0.0f) {
            if (phase == SimonPhase.SHOWING_SEQUENCE) {
                if (!advancePresentation()) {
                    return;
                }
            } else if (phase == SimonPhase.ROUND_COMPLETE_DELAY) {
                appendNextButton();
                beginSequencePresentation();
            } else {
                return;
            }
        }
    }

    public InputResult handlePlayerButton(SimonButton button) {
        SimonButton nonNullButton = Objects.requireNonNull(button, "button");
        if (phase != SimonPhase.WAITING_FOR_PLAYER) {
            return InputResult.IGNORED;
        }

        SimonButton expectedButton = sequence.get(expectedInputIndex);
        if (nonNullButton != expectedButton) {
            phase = SimonPhase.GAME_OVER;
            pendingEvents.add(SimonGameplayEvent.playerInputRejected(nonNullButton,
                    expectedInputIndex,
                    score));
            pendingEvents.add(SimonGameplayEvent.gameOver(score));
            return InputResult.WRONG;
        }

        int matchedIndex = expectedInputIndex;
        expectedInputIndex++;
        pendingEvents.add(SimonGameplayEvent.playerInputAccepted(nonNullButton,
                matchedIndex,
                expectedInputIndex,
                sequence.size()));

        if (expectedInputIndex < sequence.size()) {
            return InputResult.CORRECT;
        }

        score = sequence.size();
        phase = SimonPhase.ROUND_COMPLETE_DELAY;
        phaseTimerSeconds = ROUND_COMPLETE_DELAY_SECONDS;
        pendingEvents.add(SimonGameplayEvent.roundCompleted(score));
        return InputResult.ROUND_COMPLETE;
    }

    public SimonGameplayEvent pollEvent() {
        return pendingEvents.poll();
    }

    public List<SimonGameplayEvent> drainEvents() {
        List<SimonGameplayEvent> events = new ArrayList<>(pendingEvents);
        pendingEvents.clear();
        return Collections.unmodifiableList(events);
    }

    public SimonPhase getPhase() {
        return phase;
    }

    public List<SimonButton> getSequence() {
        return Collections.unmodifiableList(new ArrayList<>(sequence));
    }

    public int getExpectedInputIndex() {
        return expectedInputIndex;
    }

    public int getScore() {
        return score;
    }

    private boolean advancePresentation() {
        if (presentationButtonLit) {
            presentationButtonLit = false;
            phaseTimerSeconds += BETWEEN_BUTTONS_SECONDS;
            return true;
        }

        if (presentationIndex < sequence.size()) {
            showPresentationButton(presentationIndex);
            return true;
        }

        phase = SimonPhase.WAITING_FOR_PLAYER;
        phaseTimerSeconds = 0.0f;
        expectedInputIndex = 0;
        return false;
    }

    private void beginSequencePresentation() {
        phase = SimonPhase.SHOWING_SEQUENCE;
        presentationIndex = 0;
        presentationButtonLit = false;
        phaseTimerSeconds = 0.0f;
        showPresentationButton(presentationIndex);
    }

    private void showPresentationButton(int sequenceIndex) {
        SimonButton button = sequence.get(sequenceIndex);
        pendingEvents.add(SimonGameplayEvent.showButton(button, sequenceIndex));
        presentationIndex = sequenceIndex + 1;
        presentationButtonLit = true;
        phaseTimerSeconds += SHOW_BUTTON_SECONDS;
    }

    private void appendNextButton() {
        SimonButton nextButton = Objects.requireNonNull(buttonSource.nextButton(sequence.size()),
                "buttonSource.nextButton");
        sequence.add(nextButton);
    }

    public enum SimonButton {
        RED,
        GREEN,
        BLUE,
        YELLOW
    }

    public enum SimonPhase {
        IDLE,
        SHOWING_SEQUENCE,
        WAITING_FOR_PLAYER,
        ROUND_COMPLETE_DELAY,
        GAME_OVER
    }

    public enum InputResult {
        IGNORED,
        CORRECT,
        ROUND_COMPLETE,
        WRONG
    }

    public interface ButtonSource {
        SimonButton nextButton(int sequenceLength);
    }

    private static final class RandomButtonSource implements ButtonSource {
        private final Random random;

        private RandomButtonSource(Random random) {
            this.random = Objects.requireNonNull(random, "random");
        }

        @Override
        public SimonButton nextButton(int sequenceLength) {
            SimonButton[] buttons = SimonButton.values();
            return buttons[random.nextInt(buttons.length)];
        }
    }

    public static final class SimonGameplayEvent {
        private final Type type;
        private final SimonButton button;
        private final int sequenceIndex;
        private final int matchedCount;
        private final int sequenceLength;
        private final int score;

        private SimonGameplayEvent(Type type,
                                   SimonButton button,
                                   int sequenceIndex,
                                   int matchedCount,
                                   int sequenceLength,
                                   int score) {
            this.type = Objects.requireNonNull(type, "type");
            this.button = button;
            this.sequenceIndex = sequenceIndex;
            this.matchedCount = matchedCount;
            this.sequenceLength = sequenceLength;
            this.score = score;
        }

        private static SimonGameplayEvent showButton(SimonButton button, int sequenceIndex) {
            return new SimonGameplayEvent(Type.SHOW_BUTTON,
                    Objects.requireNonNull(button, "button"),
                    sequenceIndex,
                    0,
                    0,
                    0);
        }

        private static SimonGameplayEvent playerInputAccepted(SimonButton button,
                                                              int sequenceIndex,
                                                              int matchedCount,
                                                              int sequenceLength) {
            return new SimonGameplayEvent(Type.PLAYER_INPUT_ACCEPTED,
                    Objects.requireNonNull(button, "button"),
                    sequenceIndex,
                    matchedCount,
                    sequenceLength,
                    0);
        }

        private static SimonGameplayEvent roundCompleted(int score) {
            return new SimonGameplayEvent(Type.ROUND_COMPLETED,
                    null,
                    -1,
                    0,
                    0,
                    score);
        }

        private static SimonGameplayEvent playerInputRejected(SimonButton button,
                                                              int sequenceIndex,
                                                              int score) {
            return new SimonGameplayEvent(Type.PLAYER_INPUT_REJECTED,
                    Objects.requireNonNull(button, "button"),
                    sequenceIndex,
                    0,
                    0,
                    score);
        }

        private static SimonGameplayEvent gameOver(int score) {
            return new SimonGameplayEvent(Type.GAME_OVER,
                    null,
                    -1,
                    0,
                    0,
                    score);
        }

        public Type getType() {
            return type;
        }

        public SimonButton getButton() {
            return button;
        }

        public int getSequenceIndex() {
            return sequenceIndex;
        }

        public int getMatchedCount() {
            return matchedCount;
        }

        public int getSequenceLength() {
            return sequenceLength;
        }

        public int getScore() {
            return score;
        }

        public enum Type {
            SHOW_BUTTON,
            PLAYER_INPUT_ACCEPTED,
            PLAYER_INPUT_REJECTED,
            ROUND_COMPLETED,
            GAME_OVER
        }
    }
}
