//#if MC == 12001
package lavi.minecraft.task.movement.gotopreflight.diagnostics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

//20260913_kpopmodder: Exhaust detail and summaries without consuming first causal owner boundaries.
class GotoPreparationLogBudgetTest {
    @Test
    void exhaustedDetailCannotConsumeFirstPhaseHandoffOrEitherTerminalMeaning() {
        GotoPreparationLogBudget budget = new GotoPreparationLogBudget();
        for (int index = 0; index < 32; index++) {
            assertEquals("bounded_native_detail", budget.admission("NATIVE_DECISION reason=detail_" + index));
        }
        assertEquals("native_detail_suppression_summary", budget.admission("NATIVE_DECISION reason=over_limit"));
        assertNull(budget.admission("NATIVE_DECISION reason=another_detail"));
        for (String boundary : new String[]{
                "NATIVE_FIRST start=irrelevant target=irrelevant",
                "PHASE from=NATIVE to=ARRIVAL_CLEANUP player=irrelevant",
                "PHASE from=NATIVE to=DRAIN_TO_PREPARE player=irrelevant",
                "HANDOFF_CHECK quantityReady=true held=34",
                "HANDOFF_CHECK quantityReady=false held=0",
                "ARRIVED target=irrelevant",
                "FAILED reason=HANDOFF_SHORTAGE detail=irrelevant",
                "FAILED reason=ARRIVAL_LOST detail=irrelevant",
                "NATIVE_DECISION reason=MATERIALS_SUFFICIENT_NATIVE_CONTINUES held=34"}) {
            assertEquals("reserved_first_owner_boundary", budget.admission(boundary), boundary);
        }
        assertNull(budget.admission("ARRIVED target=different_detail"));
        assertNull(budget.admission("FAILED reason=HANDOFF_SHORTAGE detail=different_detail"));
    }

    @Test
    void cleanupPhaseAndCounterRoleHaveSeparateReservedSignatures() {
        GotoPreparationLogBudget budget = new GotoPreparationLogBudget();
        for (String phase : new String[]{"DRAIN_TO_PREPARE", "DRAIN_TO_FINAL", "ARRIVAL_CLEANUP"}) {
            assertEquals("reserved_first_owner_boundary", budget.admission("CLEANUP_WAIT phase=" + phase + " ticks=1"));
            assertEquals("reserved_first_owner_boundary", budget.admission("CLEANUP_WAIT phase=" + phase + " ticks=40"));
            assertNull(budget.admission("CLEANUP_WAIT phase=" + phase + " ticks=40"));
        }
    }

    @Test
    void legacyLimitAndConsecutiveReasonDedupeStaySeparateFromPhysicalReservation() {
        GotoPreparationLogBudget budget = new GotoPreparationLogBudget();
        assertEquals(GotoPreparationLogBudget.LEGACY_DETAIL, budget.legacyDecision("AIR_COLUMN_UNAVAILABLE"));
        assertEquals(GotoPreparationLogBudget.LEGACY_SUPPRESSED, budget.legacyDecision("AIR_COLUMN_UNAVAILABLE"));
        for (int index = 1; index < 32; index++) {
            assertEquals(GotoPreparationLogBudget.LEGACY_DETAIL, budget.legacyDecision("detail_" + index));
        }
        assertEquals(GotoPreparationLogBudget.LEGACY_LIMIT, budget.legacyDecision("limit"));
        assertEquals(GotoPreparationLogBudget.LEGACY_SUPPRESSED, budget.legacyDecision("late"));
        assertEquals("reserved_first_owner_boundary", budget.admission("NATIVE_DECISION reason=AIR_COLUMN_UNAVAILABLE"));
        assertNull(budget.admission("NATIVE_DECISION reason=AIR_COLUMN_UNAVAILABLE"));
        assertEquals("reserved_first_owner_boundary", budget.admission("ARRIVED target=original"));
    }
}
//#endif
