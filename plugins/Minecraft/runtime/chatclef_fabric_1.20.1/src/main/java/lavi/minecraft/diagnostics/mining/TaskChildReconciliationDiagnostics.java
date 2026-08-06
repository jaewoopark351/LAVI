package lavi.minecraft.diagnostics.mining;

import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.resources.MineAndCollectTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260806_kpopmodder: Observe child candidate reconciliation without altering Task scheduling.
final class TaskChildReconciliationDiagnostics {
    private TaskChildReconciliationDiagnostics() {
    }

    static void log(Task parent,
                    Task activeChildBefore,
                    Task candidateChild,
                    boolean isEqualResult,
                    boolean canInterruptEvaluated,
                    boolean canInterruptPreviousChild,
                    boolean replacementApplied,
                    boolean previousChildStopCalled,
                    Task activeChildAfter,
                    boolean candidateDiscardedBecauseEqual) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || !(parent instanceof MineAndCollectTask.MineOrCollectTask)) {
            return;
        }
        if (!(activeChildBefore instanceof DestroyBlockTask)
                && !(candidateChild instanceof DestroyBlockTask)
                && !(activeChildAfter instanceof DestroyBlockTask)) {
            return;
        }
        String activeBeforeTarget = MiningDiagnosticEmitter.destroyTarget(activeChildBefore);
        String candidateTarget = MiningDiagnosticEmitter.destroyTarget(candidateChild);
        String activeAfterTarget = MiningDiagnosticEmitter.destroyTarget(activeChildAfter);
        boolean candidateBecameActive = candidateChild != null && candidateChild == activeChildAfter;
        boolean activeChildRetained = activeChildBefore != null && activeChildBefore == activeChildAfter;
        boolean activeChildChanged = activeChildBefore != activeChildAfter;
        boolean candidateAndActiveShareTarget = !"none".equals(candidateTarget)
                && candidateTarget.equals(activeAfterTarget);
        boolean candidateDifferentInstanceSameTarget = candidateChild != null
                && activeChildBefore != null
                && candidateChild != activeChildBefore
                && !"none".equals(candidateTarget)
                && candidateTarget.equals(activeBeforeTarget);
        String candidateOutcome = candidateOutcome(candidateChild, candidateBecameActive, replacementApplied,
                candidateDiscardedBecauseEqual, canInterruptEvaluated, canInterruptPreviousChild);
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "TASK_CHILD_RECONCILIATION",
                MiningDiagnosticEmitter.taskClass(parent),
                MiningDiagnosticEmitter.taskClass(activeChildBefore),
                activeBeforeTarget,
                MiningDiagnosticEmitter.taskClass(activeChildAfter),
                activeAfterTarget,
                candidateOutcome,
                Boolean.toString(isEqualResult),
                Boolean.toString(replacementApplied),
                Boolean.toString(candidateDiscardedBecauseEqual)
        );
        MiningDiagnosticEmitter.emit("TASK_CHILD_RECONCILIATION", "task_child_reconciliation", parent,
                "task_child_reconciliation|" + System.identityHashCode(parent),
                fingerprint,
                new Object[]{
                        "owner", "task_child_reconciliation",
                        "trigger", "child_candidate_compared",
                        "parentTaskClass", MiningDiagnosticEmitter.taskClass(parent),
                        "parentTaskInstanceId", MiningDiagnosticEmitter.instanceId(parent),
                        "activeChildBeforeClass", MiningDiagnosticEmitter.taskClass(activeChildBefore),
                        "activeChildBeforeInstanceId", MiningDiagnosticEmitter.instanceId(activeChildBefore),
                        "activeChildBeforeTargetPosition", activeBeforeTarget,
                        "activeChildBeforeActive", MiningDiagnosticEmitter.safeTaskActive(activeChildBefore),
                        "activeChildBeforeStopped", MiningDiagnosticEmitter.safeTaskStopped(activeChildBefore),
                        "candidateChildClass", MiningDiagnosticEmitter.taskClass(candidateChild),
                        "candidateChildInstanceId", MiningDiagnosticEmitter.instanceId(candidateChild),
                        "candidateTargetPosition", candidateTarget,
                        "candidateOutcome", candidateOutcome,
                        "candidateBecameActive", candidateBecameActive,
                        "activeChildRetained", activeChildRetained,
                        "activeChildChanged", activeChildChanged,
                        "candidateAndActiveShareTarget", candidateAndActiveShareTarget,
                        "candidateDifferentInstanceSameTarget", candidateDifferentInstanceSameTarget,
                        "isEqualResult", isEqualResult,
                        "canInterruptEvaluated", canInterruptEvaluated,
                        "canInterruptPreviousChild", canInterruptPreviousChild,
                        "replacementApplied", replacementApplied,
                        "candidateDiscardedBecauseEqual", candidateDiscardedBecauseEqual,
                        "previousChildStopCalled", previousChildStopCalled,
                        "activeChildAfterClass", MiningDiagnosticEmitter.taskClass(activeChildAfter),
                        "activeChildAfterInstanceId", MiningDiagnosticEmitter.instanceId(activeChildAfter),
                        "activeChildAfterTargetPosition", activeAfterTarget
                });
    }

    private static String candidateOutcome(Task candidateChild,
                                           boolean candidateBecameActive,
                                           boolean replacementApplied,
                                           boolean candidateDiscardedBecauseEqual,
                                           boolean canInterruptEvaluated,
                                           boolean canInterruptPreviousChild) {
        if (candidateChild == null) {
            return "NO_CANDIDATE";
        }
        if (candidateBecameActive) {
            return replacementApplied ? "REPLACED_ACTIVE_CHILD" : "CANDIDATE_ALREADY_ACTIVE";
        }
        if (replacementApplied) {
            return "REPLACEMENT_APPLIED_BUT_CANDIDATE_NOT_ACTIVE";
        }
        if (candidateDiscardedBecauseEqual) {
            return "REUSED_ACTIVE_CHILD_CANDIDATE_EQUAL";
        }
        if (canInterruptEvaluated && !canInterruptPreviousChild) {
            return "REPLACEMENT_BLOCKED_PREVIOUS_CHILD_NOT_INTERRUPTIBLE";
        }
        return "CANDIDATE_NOT_ADOPTED";
    }
}
