package lavi.minecraft.task.container.home.execution.transfer.click;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260829_kpopmodder: Preserve exact quick-move success and failure reasons.
class HomeStorageQuickMoveOutcomeTest {
    @Test
    void carriesIssuedStateWithoutOwningClickBehavior() {
        HomeStorageQuickMoveOutcome success = HomeStorageQuickMoveOutcome.success();
        HomeStorageQuickMoveOutcome failure =
                HomeStorageQuickMoveOutcome.failure("slot_click_exception");

        assertTrue(success.issued());
        assertEquals("quick_move_requested", success.reason());
        assertEquals("none", success.exceptionClass());
        assertFalse(failure.issued());
        assertEquals("slot_click_exception", failure.reason());
        assertEquals("RuntimeException", failure.exceptionClass());
    }
}
