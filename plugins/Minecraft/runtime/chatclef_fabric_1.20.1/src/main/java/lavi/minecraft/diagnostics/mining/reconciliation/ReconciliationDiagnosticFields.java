package lavi.minecraft.diagnostics.mining.reconciliation;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.resources.MineAndCollectTask;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.formatting.MiningDiagnosticTaskFields;
import net.minecraft.util.math.BlockPos;

//20260830_kpopmodder: Project TASK_CHILD_RECONCILIATION detail after gate admission.
public final class ReconciliationDiagnosticFields {
    private ReconciliationDiagnosticFields() {
    }

    public static Object[] capture(AltoClef mod,
                                   MineAndCollectTask.MineOrCollectTask parent,
                                   ReconciliationTaskSnapshot activeChildBefore,
                                   ReconciliationTaskSnapshot candidateChild,
                                   ReconciliationTaskSnapshot activeChildAfter,
                                   ReconciliationOutcome outcome,
                                   boolean isEqualResult,
                                   boolean canInterruptEvaluated,
                                   boolean canInterruptPreviousChild,
                                   boolean replacementApplied,
                                   boolean previousChildStopCalled,
                                   boolean candidateDiscardedBecauseEqual) {
        BlockPos parentMiningPosition = parent.miningPos();
        return new Object[]{
                "owner", "task_child_reconciliation",
                "trigger", "child_candidate_compared",
                "parentTaskClass", MiningDiagnosticTaskFields.taskClass(parent),
                "parentTaskInstanceId", MiningDiagnosticTaskFields.instanceId(parent),
                "parentTaskSummary", ChatClefDiagnostics.taskSummary(parent),
                "parentMiningPositionAtReconciliation", ChatClefDiagnostics.blockPos(parentMiningPosition),
                "parentMiningPositionScannerUnreachable", scannerUnreachable(mod, parentMiningPosition),
                "parentMiningPositionLocalBlacklistContains", localBlacklistContains(parent, parentMiningPosition),

                "activeChildBeforeClass", activeChildBefore.taskClass(),
                "activeChildBeforeInstanceId", activeChildBefore.taskInstanceId(),
                "activeChildBeforeSummary", activeChildBefore.taskSummary(),
                "activeChildBeforeTargetPosition", activeChildBefore.targetPositionText(),
                "activeChildBeforeTargetBlockState", activeChildBefore.targetBlockState(),
                "activeChildBeforeBlockStillExists", activeChildBefore.blockStillExists(),
                "activeChildBeforeChunkLoaded", activeChildBefore.chunkLoaded(),
                "activeChildBeforeWorldCanBreak", activeChildBefore.worldCanBreak(),
                "activeChildBeforeScannerUnreachable", activeChildBefore.scannerUnreachable(),
                "activeChildBeforeLocalBlacklistContains", activeChildBefore.localBlacklistContains(),
                "activeChildBeforeActive", activeChildBefore.taskActive(),
                "activeChildBeforeStopped", activeChildBefore.taskStopped(),

                "candidateChildClass", candidateChild.taskClass(),
                "candidateChildInstanceId", candidateChild.taskInstanceId(),
                "candidateChildSummary", candidateChild.taskSummary(),
                "candidateTargetPosition", candidateChild.targetPositionText(),
                "candidateTargetBlockState", candidateChild.targetBlockState(),
                "candidateBlockStillExists", candidateChild.blockStillExists(),
                "candidateChunkLoaded", candidateChild.chunkLoaded(),
                "candidateWorldCanBreak", candidateChild.worldCanBreak(),
                "candidateScannerUnreachable", candidateChild.scannerUnreachable(),
                "candidateLocalBlacklistContains", candidateChild.localBlacklistContains(),

                "activeToCandidateTargetRelation", outcome.activeToCandidateTargetRelation(),
                "activeToCandidateManhattanDistance", outcome.activeToCandidateManhattanDistance(),
                "activeToCandidateChebyshevDistance", outcome.activeToCandidateChebyshevDistance(),
                "activeToCandidateSquaredDistance", outcome.activeToCandidateSquaredDistance(),
                "activeBeforeToAfterTargetRelation", outcome.activeBeforeToAfterTargetRelation(),
                "activeBeforeToAfterManhattanDistance", outcome.activeBeforeToAfterManhattanDistance(),
                "activeBeforeToAfterChebyshevDistance", outcome.activeBeforeToAfterChebyshevDistance(),
                "activeBeforeToAfterSquaredDistance", outcome.activeBeforeToAfterSquaredDistance(),
                "candidateToActiveAfterTargetRelation", outcome.candidateToActiveAfterTargetRelation(),
                "candidateToActiveAfterManhattanDistance", outcome.candidateToActiveAfterManhattanDistance(),
                "candidateToActiveAfterChebyshevDistance", outcome.candidateToActiveAfterChebyshevDistance(),
                "candidateToActiveAfterSquaredDistance", outcome.candidateToActiveAfterSquaredDistance(),

                "candidateOutcome", outcome.candidateOutcome(),
                "reconciliationClassification", outcome.reconciliationClassification(),
                "candidateBecameActive", outcome.candidateBecameActive(),
                "activeChildRetained", outcome.activeChildRetained(),
                "activeChildChanged", outcome.activeChildChanged(),
                "candidateAndActiveShareTarget", outcome.candidateAndActiveShareTarget(),
                "candidateDifferentInstanceSameTarget", outcome.candidateDifferentInstanceSameTarget(),
                "candidateWasActiveBefore", outcome.candidateWasActiveBefore(),
                "candidateDiscardedAsAllocationNoise", outcome.candidateDiscardedAsAllocationNoise(),
                "sameTargetReplacementApplied", outcome.sameTargetReplacementApplied(),
                "isEqualResult", isEqualResult,
                "canInterruptEvaluated", canInterruptEvaluated,
                "canInterruptPreviousChild", canInterruptPreviousChild,
                "replacementApplied", replacementApplied,
                "candidateDiscardedBecauseEqual", candidateDiscardedBecauseEqual,
                "previousChildStopCalled", previousChildStopCalled,

                "activeChildAfterClass", activeChildAfter.taskClass(),
                "activeChildAfterInstanceId", activeChildAfter.taskInstanceId(),
                "activeChildAfterSummary", activeChildAfter.taskSummary(),
                "activeChildAfterTargetPosition", activeChildAfter.targetPositionText(),
                "activeChildAfterTargetBlockState", activeChildAfter.targetBlockState(),
                "activeChildAfterBlockStillExists", activeChildAfter.blockStillExists(),
                "activeChildAfterChunkLoaded", activeChildAfter.chunkLoaded(),
                "activeChildAfterWorldCanBreak", activeChildAfter.worldCanBreak(),
                "activeChildAfterScannerUnreachable", activeChildAfter.scannerUnreachable(),
                "activeChildAfterLocalBlacklistContains", activeChildAfter.localBlacklistContains(),
                "activeChildAfterActive", activeChildAfter.taskActive(),
                "activeChildAfterStopped", activeChildAfter.taskStopped(),
                "parentLocalBlacklistSize", parent.diagnosticLocalBlacklistSize()
        };
    }

    private static String scannerUnreachable(AltoClef mod, BlockPos target) {
        if (target == null) {
            return "not_destroy_target";
        }
        return ChatClefDiagnostics.safeValue(() ->
                mod == null ? "unavailable" : mod.getBlockScanner().isUnreachable(target));
    }

    private static String localBlacklistContains(MineAndCollectTask.MineOrCollectTask parent,
                                                 BlockPos target) {
        if (target == null) {
            return "not_destroy_target";
        }
        return ChatClefDiagnostics.safeValue(() -> parent.diagnosticLocalBlacklistContains(target));
    }
}
