package lavi.minecraft.diagnostics.container.store.deposit.interaction;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerRouteState;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationContext;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import net.minecraft.util.math.BlockPos;

public record StoreDepositInteractionContext(long interactionId,
                                             String storeOperationId,
                                             String storeAttemptId,
                                             long storeAttemptSequence,
                                             long candidateDecisionSequenceAtAttempt,
                                             long branchEpochAtAttempt,
                                             String branchAtAttempt,
                                             long routeChildLifecycleIdAtAttempt,
                                             String routeChildClassAtAttempt,
                                             String routeChildIdentityAtAttempt,
                                             String activeDescendantClassAtAttempt,
                                             String activeDescendantIdentityAtAttempt,
                                             int routeChildReplacementCountAtAttempt,
                                             String targetRole,
                                             BlockPos targetPosition,
                                             String contextBindingSource,
                                             String contextBindingConfidence,
                                             long interactionStartClientTick) {
    public static StoreDepositInteractionContext capture(StoreDepositOperationState state,
                                                         Task activeTask,
                                                         BlockInteractionContext interaction) {
        if (state == null
                || state.context() == null
                || !state.context().isDepositAllOperation()
                || activeTask == null
                || interaction == null
                || interaction.targetPosition() == null) {
            return null;
        }
        StoreContainerRouteState route = state.routeState();
        if (!"OPEN_EXISTING".equals(route.currentBranch())
                || route.activeStoreAttemptSequence() <= 0
                || !route.isInCurrentRoute(activeTask)) {
            return null;
        }
        String targetRole = targetRole(interaction.targetPosition(), route);
        if ("UNRELATED_POSITION".equals(targetRole)) {
            return null;
        }
        String operationId = state.context().operationId();
        return new StoreDepositInteractionContext(
                interaction.interactionId(),
                operationId,
                operationId + "-attempt-" + route.activeStoreAttemptSequence(),
                route.activeStoreAttemptSequence(),
                route.activeStoreAttemptCandidateDecisionSequence(),
                route.activeStoreAttemptBranchEpoch(),
                route.activeStoreAttemptBranch(),
                route.activeStoreAttemptRouteChildLifecycleSequence(),
                route.currentRouteChildClass(),
                route.currentRouteChildIdentity(),
                activeTask.getClass().getName(),
                StoreDepositOperationContext.identity(activeTask),
                route.activeStoreAttemptRouteChildReplacementCount(),
                targetRole,
                interaction.targetPosition().toImmutable(),
                "ACTIVE_TASK_IDENTITY_BINDING_AND_TARGET_VALUE",
                "EXACT",
                interaction.startClientTickId()
        );
    }

    private static String targetRole(BlockPos target, StoreContainerRouteState route) {
        boolean pursuit = target.equals(route.currentPursuit());
        boolean filtered = target.equals(route.currentFilteredCandidate());
        if (pursuit && filtered) {
            return "CURRENT_PURSUIT_AND_FILTERED_CANDIDATE";
        }
        if (pursuit) {
            return "CURRENT_PURSUIT";
        }
        if (filtered) {
            return "CURRENT_FILTERED_CANDIDATE";
        }
        return "UNRELATED_POSITION";
    }
}
