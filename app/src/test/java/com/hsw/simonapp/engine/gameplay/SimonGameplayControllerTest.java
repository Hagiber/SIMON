package com.hsw.simonapp.engine.gameplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.hsw.simonapp.engine.gameplay.SimonGameplayController.InputResult;
import com.hsw.simonapp.engine.gameplay.SimonGameplayController.SimonButton;
import com.hsw.simonapp.engine.gameplay.SimonGameplayController.SimonGameplayEvent;
import com.hsw.simonapp.engine.gameplay.SimonGameplayController.SimonPhase;

import org.junit.Test;

import java.util.Arrays;

public class SimonGameplayControllerTest {

    @Test
    public void start_beginsSequencePresentationWithFirstGeneratedButton() {
        SimonGameplayController controller = new SimonGameplayController(
                fixedButtons(SimonButton.RED));

        controller.start();

        assertEquals(SimonPhase.SHOWING_SEQUENCE, controller.getPhase());
        assertEquals(Arrays.asList(SimonButton.RED), controller.getSequence());

        SimonGameplayEvent event = controller.pollEvent();
        assertEquals(SimonGameplayEvent.Type.SHOW_BUTTON, event.getType());
        assertEquals(SimonButton.RED, event.getButton());
        assertEquals(0, event.getSequenceIndex());
        assertNull(controller.pollEvent());
    }

    @Test
    public void advance_movesFromPresentationToPlayerInput() {
        SimonGameplayController controller = new SimonGameplayController(
                fixedButtons(SimonButton.GREEN));
        controller.start();
        controller.drainEvents();

        controller.advance(10.0f);

        assertEquals(SimonPhase.WAITING_FOR_PLAYER, controller.getPhase());
        assertEquals(0, controller.getExpectedInputIndex());
        assertNull(controller.pollEvent());
    }

    @Test
    public void handlePlayerButton_acceptsCorrectInputAndQueuesNextRound() {
        SimonGameplayController controller = new SimonGameplayController(
                fixedButtons(SimonButton.RED, SimonButton.BLUE));
        controller.start();
        controller.drainEvents();
        controller.advance(10.0f);

        assertEquals(InputResult.ROUND_COMPLETE, controller.handlePlayerButton(SimonButton.RED));

        assertEquals(SimonPhase.ROUND_COMPLETE_DELAY, controller.getPhase());
        assertEquals(1, controller.getScore());

        SimonGameplayEvent accepted = controller.pollEvent();
        assertEquals(SimonGameplayEvent.Type.PLAYER_INPUT_ACCEPTED, accepted.getType());
        assertEquals(SimonButton.RED, accepted.getButton());
        assertEquals(1, accepted.getMatchedCount());
        assertEquals(1, accepted.getSequenceLength());

        SimonGameplayEvent completed = controller.pollEvent();
        assertEquals(SimonGameplayEvent.Type.ROUND_COMPLETED, completed.getType());
        assertEquals(1, completed.getScore());

        controller.advance(10.0f);

        assertEquals(SimonPhase.SHOWING_SEQUENCE, controller.getPhase());
        assertEquals(Arrays.asList(SimonButton.RED, SimonButton.BLUE), controller.getSequence());
        SimonGameplayEvent nextPresentation = controller.pollEvent();
        assertEquals(SimonGameplayEvent.Type.SHOW_BUTTON, nextPresentation.getType());
        assertEquals(SimonButton.RED, nextPresentation.getButton());
    }

    @Test
    public void handlePlayerButton_wrongInputEndsGameWithoutIncreasingScore() {
        SimonGameplayController controller = new SimonGameplayController(
                fixedButtons(SimonButton.YELLOW));
        controller.start();
        controller.drainEvents();
        controller.advance(10.0f);

        assertEquals(InputResult.WRONG, controller.handlePlayerButton(SimonButton.BLUE));

        assertEquals(SimonPhase.GAME_OVER, controller.getPhase());
        assertEquals(0, controller.getScore());
        SimonGameplayEvent event = controller.pollEvent();
        assertEquals(SimonGameplayEvent.Type.GAME_OVER, event.getType());
        assertEquals(0, event.getScore());
    }

    @Test
    public void handlePlayerButton_ignoresInputDuringPresentation() {
        SimonGameplayController controller = new SimonGameplayController(
                fixedButtons(SimonButton.RED));
        controller.start();

        assertEquals(InputResult.IGNORED, controller.handlePlayerButton(SimonButton.RED));

        assertEquals(SimonPhase.SHOWING_SEQUENCE, controller.getPhase());
        assertEquals(0, controller.getScore());
    }

    private static SimonGameplayController.ButtonSource fixedButtons(SimonButton... buttons) {
        return sequenceLength -> buttons[Math.min(sequenceLength, buttons.length - 1)];
    }
}
