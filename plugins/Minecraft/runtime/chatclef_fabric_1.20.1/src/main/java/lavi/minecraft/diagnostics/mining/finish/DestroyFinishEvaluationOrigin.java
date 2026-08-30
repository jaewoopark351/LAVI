package lavi.minecraft.diagnostics.mining.finish;

//20260830_kpopmodder: Classify Destroy finish callers only from direct diagnostic task-context evidence.
public enum DestroyFinishEvaluationOrigin {
    ACTIVE_TASK_LIFECYCLE,
    PARENT_DIAGNOSTIC_PROBE,
    UNKNOWN_CALLER;

    public static DestroyFinishEvaluationOrigin classify(boolean evaluatedTaskOwnsCurrentContext,
                                                         boolean exactParentProbeEvidence) {
        if (evaluatedTaskOwnsCurrentContext) {
            return ACTIVE_TASK_LIFECYCLE;
        }
        if (exactParentProbeEvidence) {
            return PARENT_DIAGNOSTIC_PROBE;
        }
        return UNKNOWN_CALLER;
    }
}
