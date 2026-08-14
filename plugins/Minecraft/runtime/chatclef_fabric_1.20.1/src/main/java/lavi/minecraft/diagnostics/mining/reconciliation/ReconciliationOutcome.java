package lavi.minecraft.diagnostics.mining.reconciliation;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.mining.MineTargetPositionRelation;

//20260814_kpopmodder: Keep active-child reconciliation classification separate from emitted diagnostic fields.
public record ReconciliationOutcome(
        String candidateOutcome,
        String reconciliationClassification,
        String activeToCandidateTargetRelation,
        Object activeToCandidateManhattanDistance,
        Object activeToCandidateChebyshevDistance,
        Object activeToCandidateSquaredDistance,
        String activeBeforeToAfterTargetRelation,
        Object activeBeforeToAfterManhattanDistance,
        Object activeBeforeToAfterChebyshevDistance,
        Object activeBeforeToAfterSquaredDistance,
        String candidateToActiveAfterTargetRelation,
        Object candidateToActiveAfterManhattanDistance,
        Object candidateToActiveAfterChebyshevDistance,
        Object candidateToActiveAfterSquaredDistance,
        boolean candidateBecameActive,
        boolean activeChildRetained,
        boolean activeChildChanged,
        boolean candidateAndActiveShareTarget,
        boolean candidateDifferentInstanceSameTarget,
        boolean candidateWasActiveBefore,
        boolean candidateDiscardedAsAllocationNoise,
        boolean sameTargetReplacementApplied
) {
    public static ReconciliationOutcome classify(ReconciliationTaskSnapshot activeChildBefore,
                                                 ReconciliationTaskSnapshot candidateChild,
                                                 ReconciliationTaskSnapshot activeChildAfter,
                                                 boolean isEqualResult,
                                                 boolean canInterruptEvaluated,
                                                 boolean canInterruptPreviousChild,
                                                 boolean replacementApplied,
                                                 boolean candidateDiscardedBecauseEqual) {
        Task candidateTask = candidateChild.task();
        boolean candidateBecameActive = candidateTask != null && candidateChild.sameTaskAs(activeChildAfter);
        boolean activeChildRetained = activeChildBefore.task() != null && activeChildBefore.sameTaskAs(activeChildAfter);
        boolean activeChildChanged = activeChildBefore.task() != activeChildAfter.task();
        boolean candidateAndActiveShareTarget = candidateChild.sameTargetAs(activeChildAfter);
        boolean candidateDifferentInstanceSameTarget = candidateTask != null
                && activeChildBefore.task() != null
                && candidateTask != activeChildBefore.task()
                && candidateChild.sameTargetAs(activeChildBefore);
        boolean candidateWasActiveBefore = candidateTask != null && candidateChild.sameTaskAs(activeChildBefore);
        boolean candidateDiscardedAsAllocationNoise = candidateDifferentInstanceSameTarget
                && isEqualResult
                && activeChildRetained
                && candidateDiscardedBecauseEqual
                && !replacementApplied;
        boolean sameTargetReplacementApplied = replacementApplied
                && activeChildBefore.targetPosition() != null
                && activeChildBefore.sameTargetAs(activeChildAfter)
                && activeChildBefore.task() != activeChildAfter.task();
        String candidateOutcome = candidateOutcome(candidateTask, candidateBecameActive, replacementApplied,
                candidateDiscardedBecauseEqual, canInterruptEvaluated, canInterruptPreviousChild);
        String classification = reconciliationClassification(candidateDiscardedAsAllocationNoise,
                sameTargetReplacementApplied, activeChildRetained, activeChildChanged, replacementApplied,
                canInterruptEvaluated, canInterruptPreviousChild, candidateOutcome);
        return new ReconciliationOutcome(
                candidateOutcome,
                classification,
                MineTargetPositionRelation.classify(activeChildBefore.targetPosition(), candidateChild.targetPosition()),
                MineTargetPositionRelation.manhattanDistance(activeChildBefore.targetPosition(), candidateChild.targetPosition()),
                MineTargetPositionRelation.chebyshevDistance(activeChildBefore.targetPosition(), candidateChild.targetPosition()),
                MineTargetPositionRelation.squaredDistance(activeChildBefore.targetPosition(), candidateChild.targetPosition()),
                MineTargetPositionRelation.classify(activeChildBefore.targetPosition(), activeChildAfter.targetPosition()),
                MineTargetPositionRelation.manhattanDistance(activeChildBefore.targetPosition(), activeChildAfter.targetPosition()),
                MineTargetPositionRelation.chebyshevDistance(activeChildBefore.targetPosition(), activeChildAfter.targetPosition()),
                MineTargetPositionRelation.squaredDistance(activeChildBefore.targetPosition(), activeChildAfter.targetPosition()),
                MineTargetPositionRelation.classify(candidateChild.targetPosition(), activeChildAfter.targetPosition()),
                MineTargetPositionRelation.manhattanDistance(candidateChild.targetPosition(), activeChildAfter.targetPosition()),
                MineTargetPositionRelation.chebyshevDistance(candidateChild.targetPosition(), activeChildAfter.targetPosition()),
                MineTargetPositionRelation.squaredDistance(candidateChild.targetPosition(), activeChildAfter.targetPosition()),
                candidateBecameActive,
                activeChildRetained,
                activeChildChanged,
                candidateAndActiveShareTarget,
                candidateDifferentInstanceSameTarget,
                candidateWasActiveBefore,
                candidateDiscardedAsAllocationNoise,
                sameTargetReplacementApplied
        );
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

    private static String reconciliationClassification(boolean candidateDiscardedAsAllocationNoise,
                                                       boolean sameTargetReplacementApplied,
                                                       boolean activeChildRetained,
                                                       boolean activeChildChanged,
                                                       boolean replacementApplied,
                                                       boolean canInterruptEvaluated,
                                                       boolean canInterruptPreviousChild,
                                                       String candidateOutcome) {
        if (candidateDiscardedAsAllocationNoise) {
            return "CANDIDATE_ALLOCATION_NO_ACTIVE_CHURN";
        }
        if (sameTargetReplacementApplied) {
            return "SAME_TARGET_ACTIVE_CHILD_REPLACED";
        }
        if (replacementApplied) {
            return "ACTIVE_CHILD_REPLACED";
        }
        if (activeChildRetained) {
            return "ACTIVE_CHILD_RETAINED";
        }
        if (canInterruptEvaluated && !canInterruptPreviousChild) {
            return "REPLACEMENT_BLOCKED";
        }
        if (activeChildChanged) {
            return "ACTIVE_CHILD_CHANGED_WITHOUT_REPLACEMENT";
        }
        return candidateOutcome;
    }
}
