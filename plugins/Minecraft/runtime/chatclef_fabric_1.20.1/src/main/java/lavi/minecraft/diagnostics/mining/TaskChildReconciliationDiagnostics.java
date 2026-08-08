package lavi.minecraft.diagnostics.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.resources.MineAndCollectTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.util.math.BlockPos;

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
        AltoClef mod = AltoClef.getInstance();
        MineAndCollectTask.MineOrCollectTask mineParent = (MineAndCollectTask.MineOrCollectTask) parent;
        BlockPos activeBeforeTargetPos = destroyTargetPosition(activeChildBefore);
        BlockPos candidateTargetPos = destroyTargetPosition(candidateChild);
        BlockPos activeAfterTargetPos = destroyTargetPosition(activeChildAfter);
        String activeBeforeTarget = MiningDiagnosticEmitter.destroyTarget(activeChildBefore);
        String candidateTarget = MiningDiagnosticEmitter.destroyTarget(candidateChild);
        String activeAfterTarget = MiningDiagnosticEmitter.destroyTarget(activeChildAfter);
        String activeToCandidateRelation = MineTargetPositionRelation.classify(activeBeforeTargetPos, candidateTargetPos);
        String activeBeforeScannerUnreachable = scannerUnreachable(mod, activeBeforeTargetPos);
        String candidateScannerUnreachable = scannerUnreachable(mod, candidateTargetPos);
        String activeAfterScannerUnreachable = scannerUnreachable(mod, activeAfterTargetPos);
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
                        "activeChildBeforeScannerUnreachable", activeBeforeScannerUnreachable,
                        "activeChildBeforeLocalBlacklistContains", localBlacklistContains(mineParent, activeBeforeTargetPos),
                        "activeChildBeforeActive", MiningDiagnosticEmitter.safeTaskActive(activeChildBefore),
                        "activeChildBeforeStopped", MiningDiagnosticEmitter.safeTaskStopped(activeChildBefore),
                        "candidateChildClass", MiningDiagnosticEmitter.taskClass(candidateChild),
                        "candidateChildInstanceId", MiningDiagnosticEmitter.instanceId(candidateChild),
                        "candidateTargetPosition", candidateTarget,
                        "candidateScannerUnreachable", candidateScannerUnreachable,
                        "candidateLocalBlacklistContains", localBlacklistContains(mineParent, candidateTargetPos),
                        "activeToCandidateTargetRelation", activeToCandidateRelation,
                        "activeToCandidateManhattanDistance", MineTargetPositionRelation.manhattanDistance(activeBeforeTargetPos, candidateTargetPos),
                        "activeToCandidateChebyshevDistance", MineTargetPositionRelation.chebyshevDistance(activeBeforeTargetPos, candidateTargetPos),
                        "activeToCandidateSquaredDistance", MineTargetPositionRelation.squaredDistance(activeBeforeTargetPos, candidateTargetPos),
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
                        "activeChildAfterTargetPosition", activeAfterTarget,
                        "activeChildAfterScannerUnreachable", activeAfterScannerUnreachable,
                        "activeChildAfterLocalBlacklistContains", localBlacklistContains(mineParent, activeAfterTargetPos),
                        "parentLocalBlacklistSize", mineParent.diagnosticLocalBlacklistSize()
                });
    }

    private static BlockPos destroyTargetPosition(Task task) {
        if (task instanceof DestroyBlockTask destroyBlockTask) {
            return destroyBlockTask.diagnosticTargetPosition();
        }
        return null;
    }

    private static String scannerUnreachable(AltoClef mod, BlockPos target) {
        if (target == null) {
            return "not_destroy_target";
        }
        return ChatClefDiagnostics.safeValue(() -> mod != null && mod.getBlockScanner().isUnreachable(target));
    }

    private static String localBlacklistContains(MineAndCollectTask.MineOrCollectTask parent, BlockPos target) {
        if (target == null) {
            return "not_destroy_target";
        }
        return ChatClefDiagnostics.safeValue(() -> parent.diagnosticLocalBlacklistContains(target));
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
