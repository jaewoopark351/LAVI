package lavi.minecraft.diagnostics.container.store.deposit.event.route;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.common.StoreDepositOperationCorrelationFields;
import lavi.minecraft.diagnostics.container.store.deposit.event.common.StoreDepositOrderedFieldSupport;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Optional;

//20260902_kpopmodder: Own route-family diagnostic payload assembly behind the stable public facade.
public final class StoreDepositRouteEventFields {
    private StoreDepositRouteEventFields() {
    }

    public static Object[] parentCandidateDecisionFields(StoreDepositOperationState state,
                                                         Task task,
                                                         String selectedBranch,
                                                         BlockPos rawClosest,
                                                         boolean closestWithinRange,
                                                         boolean currentTryWithinExtraRange,
                                                         BlockPos currentChestTry,
                                                         ItemTarget[] notStored,
                                                         Object[] extraFields) {
        return StoreDepositOrderedFieldSupport.merge(
                StoreDepositOrderedFieldSupport.merge(StoreDepositOperationCorrelationFields.operationFields(state), new Object[]{
                        "diagnosticScope", "store_deposit_parent_candidate",
                        "owner", "store_deposit_parent_candidate_observer",
                        "mode", "BOUNDARY",
                        "trigger", selectedBranch,
                        "dedupe_key", "store_parent_candidate|" + StoreDepositOperationCorrelationFields.operationId(state) + "|" + selectedBranch + "|" + ChatClefDiagnostics.blockPos(rawClosest),
                        "max_emission", "state_change_only,per_operation=256,total_session=4936",
                        "correlation", "storeOperationId=" + StoreDepositOperationCorrelationFields.operationId(state) + ",storeRootTaskIdentity=" + StoreDepositOperationCorrelationFields.identity(task),
                        "payload", "flat_fields",
                        "terminal", false,
                        "behavior_effect", "none",
                        "selectedBranch", selectedBranch,
                        "rawClosestContainerPresent", rawClosest != null,
                        "rawClosestContainerPosition", ChatClefDiagnostics.blockPos(rawClosest),
                        "closestWithinRange", closestWithinRange,
                        "currentTryWithinExtraRange", currentTryWithinExtraRange,
                        "currentChestTry", ChatClefDiagnostics.blockPos(currentChestTry),
                        "notStoredTargets", ChatClefDiagnostics.itemTargets(notStored),
                        "taskSummary", ChatClefDiagnostics.taskSummaryForDiagnosticLog(task)
                }),
                extraFields
        );
    }

    public static Object[] filteredSearchResultFields(StoreDepositOperationState state,
                                                      Task task,
                                                      Optional<BlockPos> result,
                                                      Block[] targetBlocks) {
        boolean present = result != null && result.isPresent();
        String resultPosition = result == null
                ? "unavailable"
                : result.map(BlockPos::toShortString).orElse("none");
        return StoreDepositOrderedFieldSupport.merge(StoreDepositOperationCorrelationFields.operationFields(state), new Object[]{
                "diagnosticScope", "store_deposit_filtered_search",
                "owner", "store_deposit_filtered_search_observer",
                "mode", "BOUNDARY",
                "trigger", present ? "FILTERED_TARGET_PRESENT" : "FILTERED_TARGET_ABSENT",
                "dedupe_key", "store_filtered_search|" + StoreDepositOperationCorrelationFields.operationId(state) + "|" + StoreDepositOperationCorrelationFields.identity(task) + "|" + resultPosition,
                "max_emission", "state_change_only,per_operation=256,total_session=4936",
                "correlation", "storeOperationId=" + StoreDepositOperationCorrelationFields.operationId(state) + ",routeChildIdentity=" + StoreDepositOperationCorrelationFields.identity(task),
                "payload", "flat_fields",
                "terminal", false,
                "behavior_effect", "none",
                "filteredResultPresent", present,
                "filteredResultPosition", result == null
                        ? "unavailable"
                        : result.map(ChatClefDiagnostics::blockPos).orElse("none"),
                "targetBlocks", Arrays.toString(targetBlocks),
                "taskSummary", ChatClefDiagnostics.taskSummaryForDiagnosticLog(task)
        });
    }

    public static Object[] pursuitDecisionFields(StoreDepositOperationState state,
                                                 Task task,
                                                 Object currentPursuit,
                                                 Object candidate,
                                                 String returnedAction) {
        return StoreDepositOrderedFieldSupport.merge(StoreDepositOperationCorrelationFields.operationFields(state), new Object[]{
                "diagnosticScope", "store_deposit_pursuit",
                "owner", "store_deposit_pursuit_observer",
                "mode", "BOUNDARY",
                "trigger", returnedAction,
                "dedupe_key", "store_pursuit|" + StoreDepositOperationCorrelationFields.operationId(state) + "|" + StoreDepositOperationCorrelationFields.identity(task) + "|" + returnedAction + "|" + String.valueOf(candidate),
                "max_emission", "state_change_only,per_operation=256,total_session=4936",
                "correlation", "storeOperationId=" + StoreDepositOperationCorrelationFields.operationId(state) + ",routeChildIdentity=" + StoreDepositOperationCorrelationFields.identity(task),
                "payload", "flat_fields",
                "terminal", false,
                "behavior_effect", "none",
                "currentPursuit", diagnosticValue(currentPursuit),
                "candidate", diagnosticValue(candidate),
                "returnedAction", returnedAction,
                "taskSummary", ChatClefDiagnostics.taskSummaryForDiagnosticLog(task)
        });
    }

    public static Object[] targetCallbackDecisionFields(StoreDepositOperationState state,
                                                        Task task,
                                                        BlockPos callbackTarget,
                                                        BlockPos currentChestTryBefore,
                                                        boolean sameReference,
                                                        boolean progressResetBecauseReferenceChanged,
                                                        ItemTarget[] boundNotStored) {
        return StoreDepositOrderedFieldSupport.merge(StoreDepositOperationCorrelationFields.operationFields(state), new Object[]{
                "diagnosticScope", "store_deposit_target_callback",
                "owner", "store_deposit_target_callback_observer",
                "mode", "BOUNDARY",
                "trigger", progressResetBecauseReferenceChanged
                        ? "REFERENCE_CHANGED_PROGRESS_RESET"
                        : "REFERENCE_RETAINED",
                "dedupe_key", "store_target_callback|" + StoreDepositOperationCorrelationFields.operationId(state) + "|" + ChatClefDiagnostics.blockPos(callbackTarget) + "|" + progressResetBecauseReferenceChanged,
                "max_emission", "state_change_only,per_operation=256,total_session=4936",
                "correlation", "storeOperationId=" + StoreDepositOperationCorrelationFields.operationId(state) + ",storeRootTaskIdentity=" + StoreDepositOperationCorrelationFields.identity(task),
                "payload", "flat_fields",
                "terminal", false,
                "behavior_effect", "none",
                "callbackTarget", ChatClefDiagnostics.blockPos(callbackTarget),
                "currentChestTryBefore", ChatClefDiagnostics.blockPos(currentChestTryBefore),
                "filteredTargetSameReferenceAsCurrentTry", sameReference,
                "progressResetBecauseReferenceChanged", progressResetBecauseReferenceChanged,
                "activeChildBoundNotStoredTargets", ChatClefDiagnostics.itemTargets(boundNotStored),
                "taskSummary", ChatClefDiagnostics.taskSummaryForDiagnosticLog(task)
        });
    }

    public static Object[] craftRouteEventFields(StoreDepositOperationState state,
                                                 Task task,
                                                 String observedEventName,
                                                 String observedReason,
                                                 Object[] branchFields) {
        return StoreDepositOrderedFieldSupport.merge(StoreDepositOperationCorrelationFields.operationFields(state), new Object[]{
                "diagnosticScope", "store_deposit_craft_route",
                "owner", "store_deposit_craft_route_observer",
                "mode", "BOUNDARY",
                "trigger", observedEventName + ":" + observedReason,
                "dedupe_key", "store_craft_route|" + StoreDepositOperationCorrelationFields.operationId(state) + "|" + StoreDepositOperationCorrelationFields.identity(task) + "|" + observedEventName + "|" + observedReason,
                "max_emission", "state_change_only,per_operation=256,total_session=4936",
                "correlation", "storeOperationId=" + StoreDepositOperationCorrelationFields.operationId(state) + ",containerTaskIdentity=" + StoreDepositOperationCorrelationFields.identity(task),
                "payload", "flat_fields",
                "terminal", false,
                "behavior_effect", "none",
                "observedEventName", observedEventName,
                "observedReason", observedReason,
                "decision", diagnosticValue(field(branchFields, "decision")),
                "costToWalk", diagnosticValue(field(branchFields, "costToWalk")),
                "costToMakeNew", diagnosticValue(field(branchFields, "costToMakeNew")),
                "nearestPresent", diagnosticValue(field(branchFields, "nearestPresent")),
                "nearestPosition", diagnosticValue(field(branchFields, "nearestPosition")),
                "cachedContainerPosition", diagnosticValue(field(branchFields, "cachedContainerPosition")),
                "openTableTask", diagnosticValue(field(branchFields, "openTableTask")),
                "taskSummary", ChatClefDiagnostics.taskSummaryForDiagnosticLog(task)
        });
    }

    private static Object field(Object[] fields, String name) {
        if (fields == null || name == null) {
            return null;
        }
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (name.equals(fields[index])) {
                return fields[index + 1];
            }
        }
        return null;
    }

    private static String diagnosticValue(Object value) {
        return value == null ? "unavailable" : String.valueOf(value);
    }
}
