package lavi.minecraft.diagnostics.container.store.deposit.event.common;

import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationContext;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;

//20260902_kpopmodder: Own the shared operation and automatic correlation payload behind the stable facade.
public final class StoreDepositOperationCorrelationFields {
    private StoreDepositOperationCorrelationFields() {
    }

    public static Object[] operationFields(StoreDepositOperationState state) {
        if (state == null || state.context() == null) {
            return new Object[]{
                    "storeContextAvailable", false,
                    "storeOperationId", "UNAVAILABLE",
                    "requestSource", "UNAVAILABLE"
            };
        }
        StoreDepositOperationContext context = state.context();
        Object[] fields = new Object[]{
                "storeContextAvailable", true,
                "storeOperationId", context.operationId(),
                "requestSource", context.requestSource(),
                "storeRootTaskIdentity", context.rootTaskIdentity(),
                "storeRootTaskClass", context.rootTaskClass(),
                "storeOperationStartTick", context.startTick(),
                "storeOperationStartEventSequence", context.startEventSequence()
        };
        if (!context.isAutomaticDepositOperation() || !state.automaticContext().available()) {
            return fields;
        }
        long selectedCandidateGeneration = state.routeState().selectedCandidateGeneration();
        if (selectedCandidateGeneration <= 0) {
            selectedCandidateGeneration = state.routeState().activeRouteCandidateGeneration();
        }
        return StoreDepositOrderedFieldSupport.merge(fields, automaticIdentityFields(
                state.automaticContext(),
                context.operationId(),
                candidateId(context.operationId(), selectedCandidateGeneration),
                storeAttemptId(
                        context.operationId(),
                        state.routeState().activeStoreAttemptSequence()
                ),
                routeChildId(
                        context.operationId(),
                        state.routeState().activeStoreAttemptRouteChildLifecycleSequence()
                ),
                "UNAVAILABLE",
                "UNAVAILABLE",
                "UNAVAILABLE"
        ));
    }

    public static Object[] automaticIdentityFields(StoreDepositAutomaticContext context,
                                                   String storeOperationId,
                                                   String selectedCandidateGenerationId,
                                                   String storeAttemptId,
                                                   String routeChildLifecycleId,
                                                   String transferAttemptId,
                                                   String slotActionId,
                                                   String slotMutationId) {
        Object[] automatic = context == null
                ? StoreDepositAutomaticContext.unavailable().fields()
                : context.fields();
        return StoreDepositOrderedFieldSupport.merge(automatic, new Object[]{
                "storeOperationId", normalizeIdentity(storeOperationId),
                "selectedCandidateGenerationId", normalizeIdentity(selectedCandidateGenerationId),
                "storeAttemptId", normalizeIdentity(storeAttemptId),
                "routeChildLifecycleId", normalizeIdentity(routeChildLifecycleId),
                "transferAttemptId", normalizeIdentity(transferAttemptId),
                "slotActionId", normalizeIdentity(slotActionId),
                "slotMutationId", normalizeIdentity(slotMutationId)
        });
    }

    public static Object[] activeRouteIdentityFields(StoreDepositOperationState state) {
        if (state == null
                || state.context() == null
                || !state.context().isAutomaticDepositOperation()
                || !state.automaticContext().available()) {
            return new Object[0];
        }
        String operationId = state.context().operationId();
        return StoreDepositOrderedFieldSupport.merge(
                operationFields(state),
                automaticIdentityFields(
                        state.automaticContext(),
                        operationId,
                        candidateId(operationId, state.routeState().activeRouteCandidateGeneration()),
                        storeAttemptId(operationId, state.routeState().activeStoreAttemptSequence()),
                        routeChildId(
                                operationId,
                                state.routeState().activeStoreAttemptRouteChildLifecycleSequence()
                        ),
                        "UNAVAILABLE",
                        "UNAVAILABLE",
                        "UNAVAILABLE"
                )
        );
    }

    public static String operationId(StoreDepositOperationState state) {
        return state == null || state.context() == null
                ? "UNAVAILABLE"
                : state.context().operationId();
    }

    public static String identity(Object value) {
        return StoreDepositOperationContext.identity(value);
    }

    private static String normalizeIdentity(String value) {
        return value == null || value.isBlank() ? "UNAVAILABLE" : value;
    }

    private static String candidateId(String operationId, long generation) {
        return generation <= 0 ? "UNAVAILABLE" : operationId + "-candidate-" + generation;
    }

    private static String storeAttemptId(String operationId, long sequence) {
        return sequence <= 0 ? "UNAVAILABLE" : operationId + "-attempt-" + sequence;
    }

    private static String routeChildId(String operationId, long sequence) {
        return sequence <= 0 ? "UNAVAILABLE" : operationId + "-route-child-" + sequence;
    }
}
