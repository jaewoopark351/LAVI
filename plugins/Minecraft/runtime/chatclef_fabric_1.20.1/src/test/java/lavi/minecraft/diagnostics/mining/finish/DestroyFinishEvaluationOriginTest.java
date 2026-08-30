package lavi.minecraft.diagnostics.mining.finish;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260830_kpopmodder: Prevent ambiguous Destroy finish callers from being promoted to lifecycle evidence.
class DestroyFinishEvaluationOriginTest {
    @Test
    void requiresDirectContextEvidenceForKnownOrigins() {
        assertEquals(DestroyFinishEvaluationOrigin.ACTIVE_TASK_LIFECYCLE,
                DestroyFinishEvaluationOrigin.classify(true, false));
        assertEquals(DestroyFinishEvaluationOrigin.PARENT_DIAGNOSTIC_PROBE,
                DestroyFinishEvaluationOrigin.classify(false, true));
        assertEquals(DestroyFinishEvaluationOrigin.UNKNOWN_CALLER,
                DestroyFinishEvaluationOrigin.classify(false, false));
    }
}
