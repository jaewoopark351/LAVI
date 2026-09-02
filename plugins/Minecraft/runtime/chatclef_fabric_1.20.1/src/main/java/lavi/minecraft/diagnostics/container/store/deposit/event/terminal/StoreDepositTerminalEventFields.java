package lavi.minecraft.diagnostics.container.store.deposit.event.terminal;

import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.common.StoreDepositOperationCorrelationFields;
import lavi.minecraft.diagnostics.container.store.deposit.event.common.StoreDepositOrderedFieldSupport;

//20260902_kpopmodder: Own terminal-summary diagnostic payload assembly behind the stable public facade.
public final class StoreDepositTerminalEventFields {
    private StoreDepositTerminalEventFields() {
    }

    public static Object[] terminalSummaryFields(StoreDepositOperationState state,
                                                 String terminalTrigger,
                                                 String diagnosticClassification,
                                                 Object[] budgetFields) {
        return StoreDepositOrderedFieldSupport.merge(
                StoreDepositOrderedFieldSupport.merge(StoreDepositOperationCorrelationFields.operationFields(state), new Object[]{
                        "diagnosticScope", "store_deposit_terminal",
                        "owner", "store_deposit_terminal_summary_factory",
                        "mode", "BOUNDARY",
                        "trigger", terminalTrigger,
                        "dedupe_key", "store_deposit_terminal_summary|" + StoreDepositOperationCorrelationFields.operationId(state),
                        "max_emission", "one_per_operation",
                        "correlation", "storeOperationId=" + StoreDepositOperationCorrelationFields.operationId(state),
                        "payload", "flat_fields",
                        "terminal", true,
                        "behavior_effect", "none",
                        "terminalTrigger", terminalTrigger,
                        "durationMillis", state == null ? "unavailable" : state.elapsedMillis(),
                        "activationCount", state == null ? "unavailable" : state.activationCount(),
                        "interruptCount", state == null ? "unavailable" : state.interruptCount(),
                        "resumeCount", state == null ? "unavailable" : state.resumeCount(),
                        "trueStopObserved", state != null && state.trueStopObserved(),
                        "naturalFinishObserved", state != null && state.naturalFinishObserved(),
                        "explicitStopCorrelated", state != null && state.explicitStopCorrelated(),
                        "lifecycleEventCount", state == null ? "unavailable" : state.lifecycleEventCount(),
                        "childReconciliationCount", state == null ? "unavailable" : state.childReconciliationCount(),
                        "parentCandidateDecisionCount", state == null ? "unavailable" : state.parentCandidateDecisionCount(),
                        "filteredSearchResultCount", state == null ? "unavailable" : state.filteredSearchResultCount(),
                        "pursuitDecisionCount", state == null ? "unavailable" : state.pursuitDecisionCount(),
                        "targetCallbackDecisionCount", state == null ? "unavailable" : state.targetCallbackDecisionCount(),
                        "craftRouteEventCount", state == null ? "unavailable" : state.craftRouteEventCount(),
                        "transferDecisionCount", state == null ? "unavailable" : state.transferDecisionCount(),
                        "effectObservationCount", state == null ? "unavailable" : state.effectObservationCount(),
                        "expectedPositiveEffectCount", state == null ? "unavailable" : state.expectedPositiveEffectCount(),
                        "exceptionObservationCount", state == null ? "unavailable" : state.exceptionObservationCount(),
                        "boundTaskCount", state == null ? "unavailable" : state.boundTaskCount(),
                        "droppedTaskBindingCount", state == null ? "unavailable" : state.droppedTaskBindingCount(),
                        "trackerBindingCount", state == null ? "unavailable" : state.trackerBindingCount(),
                        "droppedTrackerBindingCount", state == null ? "unavailable" : state.droppedTrackerBindingCount(),
                        "diagnosticBindingEvictionCount", state == null ? "unavailable" : state.evictedOperationCount(),
                        "lastLifecycleAction", state == null ? "unavailable" : state.lastLifecycleAction(),
                        "lastLifecyclePhase", state == null ? "unavailable" : state.lastLifecyclePhase(),
                        "finalActiveChildInstanceId", state == null ? "unavailable" : state.lastActiveChildIdentity(),
                        "finalActiveChildClass", state == null ? "unavailable" : state.lastActiveChildClass(),
                        "lifecycleCounts", state == null ? "unavailable" : state.lifecycleCounts(),
                        "reconciliationOutcomeCountsByRole", state == null ? "unavailable" : state.reconciliationCounts(),
                        "parentCandidateCounts", state == null ? "unavailable" : state.parentCandidateCounts(),
                        "filteredSearchCounts", state == null ? "unavailable" : state.filteredSearchCounts(),
                        "pursuitCounts", state == null ? "unavailable" : state.pursuitCounts(),
                        "targetCallbackCounts", state == null ? "unavailable" : state.targetCallbackCounts(),
                        "craftRouteCounts", state == null ? "unavailable" : state.craftRouteCounts(),
                        "transferActionCounts", state == null ? "unavailable" : state.transferCounts(),
                        "effectObservationCounts", state == null ? "unavailable" : state.effectCounts(),
                        "exceptionCounts", state == null ? "unavailable" : state.exceptionCounts(),
                        "effectObservationComplete", false,
                        "effectObservationCompletenessAuthority", "PARTIAL_SLOT_CALLBACKS_ONLY",
                        "effectVerified", false,
                        "diagnosticClassification", diagnosticClassification,
                        "coverageComplete", false
                }),
                budgetFields
        );
    }

    public static Object[] baritoneSummaryFields(StoreDepositOperationState state,
                                                 String terminalTrigger) {
        return StoreDepositOrderedFieldSupport.merge(StoreDepositOperationCorrelationFields.operationFields(state), new Object[]{
                "diagnosticScope", "store_deposit_baritone_summary",
                "owner", "store_deposit_summary_factory",
                "mode", "BOUNDARY",
                "trigger", terminalTrigger,
                "dedupe_key", "store_deposit_baritone_summary|" + StoreDepositOperationCorrelationFields.operationId(state),
                "max_emission", "one_per_operation",
                "correlation", "storeOperationId=" + StoreDepositOperationCorrelationFields.operationId(state),
                "payload", "flat_fields",
                "terminal", true,
                "behavior_effect", "none",
                "baritoneContextImplemented", false,
                "baritoneContextCoverage", "UNOBSERVED_IN_FIRST_DIAGNOSTIC_PASS",
                "coverageComplete", false
        });
    }

    public static Object[] coverageSummaryFields(StoreDepositOperationState state,
                                                 String terminalTrigger,
                                                 Object[] budgetFields) {
        boolean depositAll = state != null
                && state.context() != null
                && state.context().isDepositAllOperation();
        String implementedFamilies = "lifecycle,child_reconciliation,parent_candidate,filtered_search,pursuit,"
                + "target_callback,craft_route,transfer,effect,user_block_null_input"
                + (depositAll ? ",predicate_rejection_aggregate,branch_epoch,operation_budget,checkpoint" : "");
        String unimplementedFamilies = "baritone_generation_context,container_access_context,durable_effect_oracle"
                + (depositAll ? ",block_scanner_internal_filter" : "");
        return StoreDepositOrderedFieldSupport.merge(
                StoreDepositOrderedFieldSupport.merge(StoreDepositOperationCorrelationFields.operationFields(state), new Object[]{
                        "diagnosticScope", "store_deposit_diagnostic_coverage",
                        "owner", "store_deposit_summary_factory",
                        "mode", "BOUNDARY",
                        "trigger", terminalTrigger,
                        "dedupe_key", "store_deposit_coverage_summary|" + StoreDepositOperationCorrelationFields.operationId(state),
                        "max_emission", "one_per_operation",
                        "correlation", "storeOperationId=" + StoreDepositOperationCorrelationFields.operationId(state),
                        "payload", "flat_fields",
                        "terminal", true,
                        "behavior_effect", "none",
                        "implementedFamilies", implementedFamilies,
                        "unimplementedFamilies", unimplementedFamilies,
                        "lifecycleEventCount", state == null ? "unavailable" : state.lifecycleEventCount(),
                        "childReconciliationCount", state == null ? "unavailable" : state.childReconciliationCount(),
                        "parentCandidateDecisionCount", state == null ? "unavailable" : state.parentCandidateDecisionCount(),
                        "filteredSearchResultCount", state == null ? "unavailable" : state.filteredSearchResultCount(),
                        "pursuitDecisionCount", state == null ? "unavailable" : state.pursuitDecisionCount(),
                        "targetCallbackDecisionCount", state == null ? "unavailable" : state.targetCallbackDecisionCount(),
                        "craftRouteEventCount", state == null ? "unavailable" : state.craftRouteEventCount(),
                        "transferDecisionCount", state == null ? "unavailable" : state.transferDecisionCount(),
                        "effectObservationCount", state == null ? "unavailable" : state.effectObservationCount(),
                        "exceptionObservationCount", state == null ? "unavailable" : state.exceptionObservationCount(),
                        "coverageComplete", false
                }),
                budgetFields
        );
    }

    public static Object[] terminalReserveExhaustedFields(StoreDepositOperationState state,
                                                          String terminalTrigger,
                                                          Object[] budgetFields) {
        return StoreDepositOrderedFieldSupport.merge(
                StoreDepositOrderedFieldSupport.merge(StoreDepositOperationCorrelationFields.operationFields(state), new Object[]{
                        "diagnosticScope", "store_deposit_terminal_control",
                        "owner", "store_deposit_critical_reserve_budget",
                        "mode", "BOUNDARY",
                        "trigger", terminalTrigger,
                        "dedupe_key", "store_deposit_terminal_reserve_exhausted|" + StoreDepositOperationCorrelationFields.operationId(state),
                        "max_emission", "one_per_operation,session_control_reserve=8",
                        "correlation", "storeOperationId=" + StoreDepositOperationCorrelationFields.operationId(state),
                        "payload", "flat_fields",
                        "terminal", true,
                        "behavior_effect", "none",
                        "terminalGroupEmitted", false,
                        "reservedTerminalSummarySlots", 0,
                        "coverageComplete", false
                }),
                budgetFields
        );
    }
}
