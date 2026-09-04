package lavi.minecraft.diagnostics.container.gui.lifecycle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

//20260904_kpopmodder: Lock diagnostics-only root terminal classification without Task control.
class ContainerGuiRootTerminalDecisionTest {
    @Test
    void classifiesNaturalFinish() {
        assertEquals(
                "USER_TASK_FINISHED",
                ContainerGuiRootTerminalDecision.reason(
                        true,
                        "root-assignment-1",
                        "root-assignment-1",
                        "natural_task_finish"
                )
        );
    }

    @Test
    void classifiesExplicitCancellation() {
        assertEquals(
                "USER_TASK_CANCELLED",
                ContainerGuiRootTerminalDecision.reason(
                        true,
                        "root-assignment-1",
                        "root-assignment-1",
                        "cancel_after_chain_stop"
                )
        );
    }

    @Test
    void classifiesRootReplacementAfterFinishCallback() {
        assertEquals(
                "USER_TASK_ROOT_REPLACED_AFTER_ON_FINISH_CALLBACK",
                ContainerGuiRootTerminalDecision.reason(
                        false,
                        "root-assignment-1",
                        "root-assignment-2",
                        "natural_task_finish"
                )
        );
    }

    @Test
    void sameLiveRootIsNotTerminal() {
        assertNull(ContainerGuiRootTerminalDecision.reason(
                false,
                "root-assignment-1",
                "root-assignment-1",
                "natural_task_finish"
        ));
    }
}
