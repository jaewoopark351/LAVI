package lavi.minecraft.diagnostics.observation;

import lavi.minecraft.diagnostics.observation.state.ObservationEmission;
import lavi.minecraft.diagnostics.observation.state.ObservationLedger;
import lavi.minecraft.diagnostics.observation.state.ObservationRegistry;
import lavi.minecraft.diagnostics.observation.state.checkpoint.ObservationCheckpointState;
import lavi.minecraft.diagnostics.session.admission.*;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticEventFamilyClassifier;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ObservationCheckpointTest {
    @Test void onlyARealOrdinaryBudgetRejectionOpensCheckpointCapacity() {
        for (DiagnosticAdmissionDecision.RejectionReason reason : DiagnosticAdmissionDecision.RejectionReason.values()) {
            ObservationLedger ledger = new ObservationLedger();
            ObservationEmission first = sample(ledger, 0, 0);
            ledger.settle(first, false, false, "failure", reason);
            ObservationEmission detail = sample(ledger, 200, 10_000_000_000L);
            assertEquals("DETAIL", detail.tier(), "A first-event rejection must not authorize summary reserve");
            boolean admitted = reason == DiagnosticAdmissionDecision.RejectionReason.NONE;
            ledger.settle(detail, admitted, false, "explicit rejection or sink failure", reason);
            ObservationEmission next = sample(ledger, 400, 20_000_000_000L);
            boolean budgetRejected = reason == DiagnosticAdmissionDecision.RejectionReason.ORDINARY_CEILING_REACHED
                    || reason == DiagnosticAdmissionDecision.RejectionReason.SHARED_HARD_CAP_REACHED;
            assertEquals(budgetRejected ? "SUMMARY" : "DETAIL", next.tier(), reason.name());
            assertEquals(0L, fields(next.required()).get("priorEmissionCallsReturned"));
        }
    }

    @Test void checkpointsNeedBothClocksStopAt32AndLeaveFirstAndTerminalEvidenceUntouched() {
        ObservationLedger ledger = rejectedLedger();
        assertNull(sample(ledger, 399, 20_000_000_000L));
        assertNull(sample(ledger, 400, 19_999_999_999L));
        for (int n = 1; n <= ObservationCheckpointState.LIMIT; n++) {
            ObservationEmission summary = ledger.capture("LOOP", "SAME_TARGET", "same", false,
                    200L + n * 200L, (1L + n) * 10_000_000_000L,
                    new Object[]{"destroyChildRunCount", n * 80L, "firstCausalLink", "first_equip_to_prepare_to_stop"});
            assertEquals("SUMMARY", summary.tier());
            assertEquals(n, fields(summary.required()).get("summaryAttemptedCount"));
            assertEquals(n * 80L, fields(summary.required()).get("destroyChildRunCount"));
            assertEquals("first_equip_to_prepare_to_stop", fields(summary.required()).get("firstCausalLink"));
            assertEquals(n == 32, fields(summary.required()).get("summaryAllowanceExhausted"));
            ledger.settle(summary, true, n % 2 == 0, "sink result", DiagnosticAdmissionDecision.RejectionReason.NONE);
        }
        assertNull(sample(ledger, 100_000, 100_000_000_000_000L));
        ObservationEmission first = ledger.capture("DIFFERENT_OWNER", "FIRST_FAILURE", "fixed", false,
                100_001, 100_000_000_000_001L, "new evidence");
        assertEquals("FIRST", first.tier());
        ObservationEmission terminal = ledger.capture("CLOSE", "USER_STOP", "closed", true,
                100_002, 100_000_000_000_002L, "terminal evidence");
        assertEquals("TERMINAL", terminal.tier());
        assertEquals(32, fields(terminal.required()).get("summaryAttemptedCount"));
        assertEquals(32, fields(terminal.required()).get("summaryAdmittedCount"));
        assertEquals(16, fields(terminal.required()).get("summaryEmissionCallsReturned"));
        assertEquals("NOT_VERIFIED", fields(terminal.required()).get("summaryFilePersistence"));
    }

    @Test void allFiveNewPoolsCannotSpendAnyFirstOrTerminalReservation() {
        DiagnosticSessionAdmissionAuthority authority = new DiagnosticSessionAdmissionAuthority("summary-partitions");
        for (int i = 0; i < DiagnosticSessionLimits.ORDINARY_CEILING; i++)
            assertTrue(authority.admit(DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.ORDINARY_DETAIL)).admitted());
        assertFalse(authority.admit(DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.ORDINARY_DETAIL)).admitted());
        for (DiagnosticEventFamily family : new DiagnosticEventFamily[]{DiagnosticEventFamily.RESOURCE_MINING_SUMMARY,
                DiagnosticEventFamily.RESOURCE_DEPOSIT_SUMMARY, DiagnosticEventFamily.RESOURCE_BUILDER_SUMMARY,
                DiagnosticEventFamily.BLOCK_COLLECTION_FIRST, DiagnosticEventFamily.BLOCK_COLLECTION_SUMMARY}) {
            for (int i = 0; i < family.slotQuota(); i++)
                assertTrue(authority.admit(DiagnosticAdmissionRequest.eligible(family)).admitted());
            assertFalse(authority.admit(DiagnosticAdmissionRequest.eligible(family)).admitted());
        }
        for (DiagnosticEventFamily first : new DiagnosticEventFamily[]{DiagnosticEventFamily.RESOURCE_MINING_FIRST,
                DiagnosticEventFamily.RESOURCE_DEPOSIT_FIRST, DiagnosticEventFamily.RESOURCE_BUILDER_FIRST,
                DiagnosticEventFamily.RESOURCE_OBSERVATION_TERMINAL, DiagnosticEventFamily.FINAL_SNAPSHOT})
            assertEquals(0L, authority.snapshot().family(first).admittedRequests(), first.name());
        assertTrue(authority.admit(DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.RESOURCE_MINING_FIRST)).admitted());
        assertTrue(authority.admit(DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.RESOURCE_OBSERVATION_TERMINAL)).admitted());
        assertTrue(authority.admitFinalSnapshot(true).admitted());
        assertTrue(authority.snapshot().admittedSlots() <= 5_000);
    }

    @Test void onlyExactWrappersSelectTheNewFamilies() {
        assertEquals(DiagnosticEventFamily.RESOURCE_MINING_SUMMARY,
                DiagnosticEventFamilyClassifier.classify("RESOURCE_OBSERVATION_MINING_SUMMARY"));
        assertEquals(DiagnosticEventFamily.RESOURCE_DEPOSIT_SUMMARY,
                DiagnosticEventFamilyClassifier.classify("RESOURCE_OBSERVATION_DEPOSIT_SUMMARY"));
        assertEquals(DiagnosticEventFamily.RESOURCE_BUILDER_SUMMARY,
                DiagnosticEventFamilyClassifier.classify("RESOURCE_OBSERVATION_BUILDER_SUMMARY"));
        assertEquals(DiagnosticEventFamily.BLOCK_COLLECTION_FIRST, DiagnosticEventFamilyClassifier.classify("BLOCK_COLLECTION_FIRST"));
        assertEquals(DiagnosticEventFamily.BLOCK_COLLECTION_SUMMARY, DiagnosticEventFamilyClassifier.classify("BLOCK_COLLECTION_SUMMARY"));
        assertEquals(DiagnosticEventFamily.ORDINARY_DETAIL, DiagnosticEventFamilyClassifier.classify("RESOURCE_OBSERVATION_DETAIL"));
        assertNotEquals(DiagnosticEventFamily.RESOURCE_MINING_SUMMARY, DiagnosticEventFamilyClassifier.classify("GOLD_TOOL_LOOP_SUMMARY"));
    }

    @Test void depositEntryCannotDisplaceTheLatestEvaluatedDecisionFromASummarySlot() {
        ObservationLedger ledger = new ObservationLedger();
        ledger.capture("AUTO_DEPOSIT_RUNTIME_ENTRY", "entry", "entry", false, 0, 0, new Object[0]);
        ledger.capture("AUTO_DEPOSIT_DECISION", "low_water", "low_water", false, 1, 1,
                new Object[]{"occupiedSlots", 28, "freeSlots", 8});
        ObservationEmission detail = ledger.capture("AUTO_DEPOSIT_RUNTIME_ENTRY", "entry", "entry", false,
                201, 10_000_000_001L, new Object[0]);
        ledger.settle(detail, false, false, "ordinary rejected", DiagnosticAdmissionDecision.RejectionReason.ORDINARY_CEILING_REACHED);
        assertNull(ledger.capture("AUTO_DEPOSIT_RUNTIME_ENTRY", "entry", "entry", false,
                401, 20_000_000_001L, new Object[0]));
        ObservationEmission decision = ledger.capture("AUTO_DEPOSIT_DECISION", "low_water", "low_water", false,
                401, 20_000_000_001L, new Object[]{"occupiedSlots", 27, "freeSlots", 9});
        assertEquals("SUMMARY", decision.tier());
        assertEquals(27, fields(decision.required()).get("occupiedSlots"));
        assertEquals(9, fields(decision.required()).get("freeSlots"));
        assertEquals(1, fields(decision.required()).get("summaryAttemptedCount"));
    }

    @Test void finalRegistrySnapshotRetainsCheckpointAccountingAfterInvalidation() {
        ObservationRegistry registry = new ObservationRegistry();
        Object instance = new Object(), world = new Object();
        ObservationActivation activation = registry.capture(0, instance, world);
        ObservationScope scope = registry.open(activation, "mining", "first-command", "context");
        ObservationLedger ledger = scope.ledger();
        sample(ledger, 0, 0);
        ObservationEmission detail = sample(ledger, 200, 10_000_000_000L);
        ledger.settle(detail, false, false, "ordinary refused", DiagnosticAdmissionDecision.RejectionReason.ORDINARY_CEILING_REACHED);
        ObservationEmission summary = sample(ledger, 400, 20_000_000_000L);
        ledger.settle(summary, true, false, "sink failed", DiagnosticAdmissionDecision.RejectionReason.NONE);
        registry.invalidate();
        Map<String, Object> snapshot = fields(registry.snapshot());
        assertEquals(1L, snapshot.get("resourceSummaryAttempted"));
        assertEquals(1L, snapshot.get("resourceSummaryAdmitted"));
        assertEquals(0L, snapshot.get("resourceSummaryEmissionCallsReturned"));
        assertFalse(activation.live(0));
    }

    private static ObservationLedger rejectedLedger() {
        ObservationLedger ledger = new ObservationLedger();
        sample(ledger, 0, 0);
        ObservationEmission detail = sample(ledger, 200, 10_000_000_000L);
        ledger.settle(detail, false, false, "ordinary refused", DiagnosticAdmissionDecision.RejectionReason.ORDINARY_CEILING_REACHED);
        return ledger;
    }

    private static ObservationEmission sample(ObservationLedger ledger, long tick, long nanos) {
        return ledger.capture("LOOP", "SAME_TARGET", "same", false, tick, nanos, "payload");
    }

    private static Map<String, Object> fields(Object[] fields) {
        Map<String, Object> result = new HashMap<>();
        for (int i = 0; i + 1 < fields.length; i += 2) result.put(String.valueOf(fields[i]), fields[i + 1]);
        return result;
    }
}
