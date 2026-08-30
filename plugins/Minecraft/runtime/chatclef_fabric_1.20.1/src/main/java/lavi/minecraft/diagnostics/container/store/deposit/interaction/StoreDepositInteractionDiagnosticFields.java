package lavi.minecraft.diagnostics.container.store.deposit.interaction;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerRouteState;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;

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
        String observationBindingVerdict = observationBindingVerdict(context, currentRoute);
        Object[] fields = new Object[]{
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
                "headBindingVerdict", context.contextBindingConfidence(),
                "observationBindingVerdict", observationBindingVerdict,
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
        StoreDepositAutomaticContext automaticContext = context.automaticContextAtAttempt();
        if (!automaticContext.available()
                && currentState != null
                && currentState.context().isAutomaticDepositOperation()) {
            automaticContext = currentState.automaticContext();
        }
        return automaticContext.available()
                ? StoreDepositEventFields.merge(
                        fields,
                        automaticIdentityFields(context, automaticContext)
                )
                : fields;
    }

    public static Object[] unavailableObservationFields(
            StoreDepositInteractionContext context,
            StoreDepositInteractionBindingRegistry.LookupStatus status) {
        if (context == null) {
            return fields(null, null);
        }
        String verdict = status == null ? "UNAVAILABLE" : "BINDING_" + status.name();
        Object[] fields = new Object[]{
                "storeContextAvailable", false,
                "storeHeadContextAvailable", true,
                "storeContextCoverageReason", verdict,
                "storeOperationId", context.storeOperationId(),
                "storeAttemptId", context.storeAttemptId(),
                "interactionAttemptId", context.interactionId(),
                "headBindingVerdict", context.contextBindingConfidence(),
                "observationBindingVerdict", verdict,
                "observationComplete", false,
                "missingBoundaries", "OBSERVATION_BINDING_LOOKUP",
                "behavior_effect", "none"
        };
        return context.automaticContextAtAttempt().available()
                ? StoreDepositEventFields.merge(
                        fields,
                        automaticIdentityFields(
                                context,
                                context.automaticContextAtAttempt()
                        )
                )
                : fields;
    }

    private static Object[] automaticIdentityFields(
            StoreDepositInteractionContext context,
            StoreDepositAutomaticContext automaticContext) {
        String routeChildLifecycleId = context.routeChildLifecycleIdAtAttempt() <= 0
                ? "UNAVAILABLE"
                : context.storeOperationId()
                        + "-route-child-"
                        + context.routeChildLifecycleIdAtAttempt();
        return StoreDepositEventFields.automaticIdentityFields(
                automaticContext,
                context.storeOperationId(),
                context.selectedCandidateGenerationIdAtAttempt(),
                context.storeAttemptId(),
                routeChildLifecycleId,
                "UNAVAILABLE",
                "UNAVAILABLE",
                "UNAVAILABLE"
        );
    }

    private static String observationBindingVerdict(StoreDepositInteractionContext context,
                                                    StoreContainerRouteState currentRoute) {
        if (currentRoute == null) {
            return "OPERATION_EXPIRED";
        }
        if (!context.branchAtAttempt().equals(currentRoute.currentBranch())) {
            return "ROUTE_BRANCH_CHANGED";
        }
        if (!context.routeChildIdentityAtAttempt().equals(currentRoute.currentRouteChildIdentity())) {
            return "ROUTE_CHILD_CHANGED";
        }
        boolean currentTarget = context.targetPosition().equals(currentRoute.currentPursuit())
                || context.targetPosition().equals(currentRoute.currentFilteredCandidate());
        return currentTarget ? "STILL_EXACT" : "TARGET_MISMATCH";
    }
}
