package lavi.minecraft.task.container.home.execution.state;

import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import lavi.minecraft.task.container.home.execution.StoreHomeResult;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeOperationProgress;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260828_kpopmodder: Verify the extracted root lifecycle state without invoking game behavior.
class StoreHomeExecutionStateTest {
    @Test
    void terminalStateIsStableAfterFirstFinish() {
        StoreHomeExecutionState state = new StoreHomeExecutionState(
                StoreHomeOperationProgress.start(41L)
        );

        state.lifecycle().finish(StoreHomeResult.CONTEXT_CHANGED, "first_reason");
        state.lifecycle().finish(StoreHomeResult.COMPLETED, "second_reason");
        state.lifecycle().transitionTo(StoreHomePhase.ACCEPT_REQUEST);

        assertFalse(state.lifecycle().pending());
        assertEquals(StoreHomeResult.CONTEXT_CHANGED, state.lifecycle().result());
        assertEquals(StoreHomePhase.TERMINAL, state.lifecycle().phase());
        assertEquals("first_reason", state.lifecycle().terminalReason());
    }

    @Test
    void initializationAndPhaseAreLifecycleStateOnly() {
        StoreHomeExecutionState state = new StoreHomeExecutionState(
                StoreHomeOperationProgress.start(42L)
        );

        assertFalse(state.lifecycle().initialized());
        assertTrue(state.lifecycle().pending());
        state.lifecycle().markInitialized();
        state.lifecycle().transitionTo(StoreHomePhase.REVALIDATE_AFTER_RESUME);

        assertTrue(state.lifecycle().initialized());
        assertEquals(
                StoreHomePhase.REVALIDATE_AFTER_RESUME,
                state.lifecycle().phase()
        );
    }

    @Test
    void invalidTerminalInputsCannotPartiallyCommitLifecycleState() {
        StoreHomeExecutionState state = new StoreHomeExecutionState(
                StoreHomeOperationProgress.start(43L)
        );

        assertThrows(
                NullPointerException.class,
                () -> state.lifecycle().finish(
                        StoreHomeResult.COMPLETED, null
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> state.lifecycle().finish(
                        StoreHomeResult.PENDING, "not_terminal"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> state.lifecycle().finish(
                        StoreHomeResult.COMPLETED, " "
                )
        );

        assertTrue(state.lifecycle().pending());
        assertEquals(StoreHomeResult.PENDING, state.lifecycle().result());
        assertEquals(StoreHomePhase.ACCEPT_REQUEST, state.lifecycle().phase());
        assertEquals("", state.lifecycle().terminalReason());
    }

    @Test
    void terminalPhaseCanOnlyBeEnteredByAtomicFinish() {
        StoreHomeExecutionState state = new StoreHomeExecutionState(
                StoreHomeOperationProgress.start(44L)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> state.lifecycle().transitionTo(StoreHomePhase.TERMINAL)
        );

        assertTrue(state.lifecycle().pending());
        assertEquals(StoreHomePhase.ACCEPT_REQUEST, state.lifecycle().phase());
    }
}
