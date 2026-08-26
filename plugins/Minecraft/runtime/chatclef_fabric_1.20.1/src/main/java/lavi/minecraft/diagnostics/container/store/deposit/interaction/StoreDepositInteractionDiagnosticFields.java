package lavi.minecraft.diagnostics.container.store.deposit.interaction;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerRouteState;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;

public final class StoreDepositInteractionDiagnosticFields {
    private StoreDepositInteractionDiagnosticFields() {
    }

    public static Object[] fields(StoreDepositInteractionContext context,
                                  StoreDepositOperationState currentState) {
        if (context == null) {
            return new Object[]{
                    "storeContextAvailable", false,
                    "storeContextCoverageReason", "NO_EXACT_ACTIVE_ROUTE_AND_TARGET_BINDING"
            };
        }
        StoreContainerRouteState currentRoute = currentState == null ? null : currentState.routeState();
        String branchAtObservation = currentRoute == null ? "UNAVAILABLE_OPERATION_NOT_ACTIVE" : currentRoute.currentBranch();
        String routeChildClassAtObservation = currentRoute == null ? "unavailable" : currentRoute.currentRouteChildClass();
        String routeChildIdentityAtObservation = currentRoute == null ? "unavailable" : currentRoute.currentRouteChildIdentity();
        int routeChildReplacementCountAtObservation = currentRoute == null
                ? -1
                : currentRoute.rootRouteChildReplacementCount();
        return new Object[]{
                "storeContextAvailable", true,
                "storeContextCoverageReason", "EXACT_ACTIVE_ROUTE_AND_TARGET_BINDING",
                "storeOperationId", context.storeOperationId(),
                "storeAttemptId", context.storeAttemptId(),
                "storeAttemptSequence", context.storeAttemptSequence(),
                "interactionAttemptId", context.interactionId(),
                "storeInteractionCorrelation", "storeOperationId=" + context.storeOperationId()
                        + ",storeAttemptId=" + context.storeAttemptId()
                        + ",interactionAttemptId=" + context.interactionId(),
                "storeInteractionMaxEmission",
                "state_change_only,operation_family=44,operation_total=256,session_total=4936,critical_reserve=64",
                "storeInteractionBehaviorEffect", "none",
                "candidateDecisionSequenceAtAttempt", context.candidateDecisionSequenceAtAttempt(),
                "branchEpochAtAttempt", context.branchEpochAtAttempt(),
                "branchAtAttempt", context.branchAtAttempt(),
                "routeChildLifecycleIdAtAttempt", context.routeChildLifecycleIdAtAttempt(),
                "routeChildClassAtAttempt", context.routeChildClassAtAttempt(),
                "routeChildIdentityAtAttempt", context.routeChildIdentityAtAttempt(),
                "activeDescendantClassAtAttempt", context.activeDescendantClassAtAttempt(),
                "activeDescendantIdentityAtAttempt", context.activeDescendantIdentityAtAttempt(),
                "targetRole", context.targetRole(),
                "storeTargetPositionAtAttempt", ChatClefDiagnostics.blockPos(context.targetPosition()),
                "contextBindingSource", context.contextBindingSource(),
                "contextBindingConfidence", context.contextBindingConfidence(),
                "storeInteractionStartClientTickAtBinding", context.interactionStartClientTick(),
                "storeOperationActiveAtObservation", currentState != null,
                "branchAtObservation", branchAtObservation,
                "branchChangedSinceAttempt", currentRoute != null && !context.branchAtAttempt().equals(branchAtObservation),
                "routeChildClassAtObservation", routeChildClassAtObservation,
                "routeChildIdentityAtObservation", routeChildIdentityAtObservation,
                "routeChildChangedSinceAttempt", currentRoute != null
                        && !context.routeChildIdentityAtAttempt().equals(routeChildIdentityAtObservation),
                "routeChildReplacementCountAtAttempt", context.routeChildReplacementCountAtAttempt(),
                "routeChildReplacementCountAtObservation", routeChildReplacementCountAtObservation,
                "routeChildReplacementObservedSinceAttempt", currentRoute != null
                        && routeChildReplacementCountAtObservation > context.routeChildReplacementCountAtAttempt(),
                "candidateDecisionSequenceAtObservation", currentRoute == null
                        ? "unavailable"
                        : currentRoute.currentParentDecision().sequence(),
                "branchEpochAtObservation", currentRoute == null ? "unavailable" : currentRoute.branchEpoch()
        };
    }
}
