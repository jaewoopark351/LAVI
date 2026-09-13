package lavi.minecraft.diagnostics.container.store.deposit.pressure;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

//20260913_kpopmodder: Verify skipped checks, negative decisions, and immutable wait provenance remain distinct.
class AutoDepositEvaluationTraceTest {
    @Test
    void skippedChecksAreNotInventedAsFalseOrInheritedFromAnEarlierEvaluation() {
        AutoDepositEvaluationTrace first = new AutoDepositEvaluationTrace(1, 50, "ARMED", false);
        first.record("pressureRead", "OBSERVED");
        first.record("occupiedSlots", 32);
        first.record("thresholdReached", false);
        Object[] frozen = first.fields();
        first.record("occupiedSlots", 34);

        AutoDepositEvaluationTrace skipped = new AutoDepositEvaluationTrace(2, 51, "RUNNING", false);
        assertEquals(false, map(frozen).get("thresholdReached"));
        assertEquals(32, map(frozen).get("occupiedSlots"));
        assertEquals("NOT_EVALUATED", map(skipped.fields()).get("pressureRead"));
        assertEquals("NOT_EVALUATED", map(skipped.fields()).get("thresholdReached"));
        skipped.record("pressureRead", "UNAVAILABLE");
        assertNotEquals(map(frozen).get("pressureRead"), map(skipped.fields()).get("pressureRead"));
    }

    @Test
    void waitOriginRetainsThePreviousRequestWhileElapsedTicksContinue() {
        AutoDepositWaitOrigin origin = new AutoDepositWaitOrigin("WAIT_FOR_REARM",
                "automatic_task_terminal", "maintenance-1", "previous-request", 10, 7, 4);
        assertEquals("previous-request", map(origin.fields(500)).get("waitOriginRequestId"));
        assertEquals(490L, map(origin.fields(500)).get("waitElapsedTicks"));
        assertEquals(10L, map(origin.fields(500)).get("waitEnteredTick"));
        assertEquals("automatic_task_terminal", map(origin.fields(501)).get("waitOriginReason"));
    }

    @Test
    void changingOnlyIdentityOrTimeDoesNotDefeatDedupButSelectionChangesDo() {
        String first = AutoDepositObservationFields.fingerprint("SCHEDULER", "selected", new Object[]{
                "selectedChain", "User@1", "selectedChainClass", "User", "tick", 1});
        String repeated = AutoDepositObservationFields.fingerprint("SCHEDULER", "selected", new Object[]{
                "selectedChain", "User@2", "selectedChainClass", "User", "tick", 2});
        String safety = AutoDepositObservationFields.fingerprint("SCHEDULER", "selected", new Object[]{
                "selectedChain", "Defense@3", "selectedChainClass", "Defense", "tick", 3});
        assertEquals(first, repeated);
        assertNotEquals(first, safety);
    }

    private static Map<String, Object> map(Object[] fields) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < fields.length; i += 2) result.put((String) fields[i], fields[i + 1]);
        return result;
    }
}
