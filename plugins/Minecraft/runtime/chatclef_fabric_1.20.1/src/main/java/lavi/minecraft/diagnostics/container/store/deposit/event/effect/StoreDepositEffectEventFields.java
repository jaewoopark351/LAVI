package lavi.minecraft.diagnostics.container.store.deposit.event.effect;

import adris.altoclef.tasks.container.ContainerStoredTracker;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry.TrackerBinding;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.common.StoreDepositOperationCorrelationFields;
import lavi.minecraft.diagnostics.container.store.deposit.event.common.StoreDepositOrderedFieldSupport;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

//20260902_kpopmodder: Own effect-family diagnostic payload assembly behind the stable public facade.
public final class StoreDepositEffectEventFields {
    private StoreDepositEffectEventFields() {
    }

    public static Object[] effectObservationFields(StoreDepositOperationState state,
                                                   TrackerBinding binding,
                                                   ContainerStoredTracker tracker,
                                                   Slot slot,
                                                   ItemStack before,
                                                   ItemStack after,
                                                   boolean playerInventorySlot,
                                                   boolean acceptPredicateEvaluated,
                                                   boolean acceptPredicateResult,
                                                   String predicateMatchReason,
                                                   boolean predicateLastInteractionPresent,
                                                   BlockPos predicateLastInteractionPosition,
                                                   String observationOutcome,
                                                   boolean includeAutomaticSliceFields) {
        Delta delta = Delta.from(before, after);
        Object[] fields = StoreDepositOrderedFieldSupport.merge(
                StoreDepositOperationCorrelationFields.operationFields(state),
                new Object[]{
                        "diagnosticScope", "store_deposit_effect",
                        "owner", "store_deposit_effect_observer",
                        "mode", "BOUNDARY",
                        "trigger", observationOutcome,
                        "dedupe_key", "store_container_effect|" + StoreDepositOperationCorrelationFields.operationId(state) + "|" + trackerRole(binding) + "|" + observationOutcome + "|" + delta.component1Item,
                        "max_emission", "state_change_or_first_reason,per_operation=256",
                        "correlation", "storeOperationId=" + StoreDepositOperationCorrelationFields.operationId(state) + ",trackerIdentity=" + StoreDepositOperationCorrelationFields.identity(tracker),
                        "payload", "flat_fields",
                        "terminal", false,
                        "behavior_effect", "none",
                        "trackerInstanceId", StoreDepositOperationCorrelationFields.identity(tracker),
                        "trackerRole", trackerRole(binding),
                        "expectedContainerPosition", binding == null
                                ? "unavailable"
                                : ChatClefDiagnostics.blockPos(binding.targetContainer()),
                        "slotIdentity", StoreDepositOperationCorrelationFields.identity(slot),
                        "slotSummary", ChatClefDiagnostics.slotSummary(slot),
                        "slotStackBefore", ChatClefDiagnostics.itemStackSummary(before),
                        "slotStackAfter", ChatClefDiagnostics.itemStackSummary(after),
                        "slotInPlayerInventory", playerInventorySlot,
                        "acceptPredicateEvaluated", acceptPredicateEvaluated,
                        "acceptPredicateResult", acceptPredicateResult,
                        "predicateMatchReason", predicateMatchReason,
                        "predicateLastInteractionPresent", predicateLastInteractionPresent,
                        "predicateLastInteractionPosition", ChatClefDiagnostics.blockPos(predicateLastInteractionPosition),
                        "observationOutcome", observationOutcome,
                        "beforeItem", itemName(before),
                        "beforeCount", count(before),
                        "afterItem", itemName(after),
                        "afterCount", count(after),
                        "deltaComponentCount", delta.componentCount,
                        "deltaComponent1Item", delta.component1Item,
                        "deltaComponent1Amount", delta.component1Amount,
                        "deltaComponent2Item", delta.component2Item,
                        "deltaComponent2Amount", delta.component2Amount
                }
        );
        if (!includeAutomaticSliceFields) {
            return fields;
        }
        return StoreDepositOrderedFieldSupport.merge(fields, new Object[]{
                "trackerIdentity", StoreDepositOperationCorrelationFields.identity(tracker),
                "subscriptionGeneration", binding == null ? "UNAVAILABLE" : binding.subscriptionGeneration(),
                "subscriptionActiveAtMutation", binding == null
                        ? "UNAVAILABLE"
                        : binding.subscriptionActive(),
                "trackerTargetBinding", binding == null
                        ? "UNAVAILABLE"
                        : ChatClefDiagnostics.blockPos(binding.targetContainer()),
                "lastBlockPosInteractionAtEvent", ChatClefDiagnostics.blockPos(predicateLastInteractionPosition),
                "predicateEvaluated", acceptPredicateEvaluated,
                "predicateResult", acceptPredicateResult,
                "signedDelta", delta.componentCount == 1
                        ? delta.component1Amount
                        : delta.component1Amount + "," + delta.component2Amount
        });
    }

    public static Object[] effectSummaryFields(StoreDepositOperationState state,
                                               String terminalTrigger) {
        return StoreDepositOrderedFieldSupport.merge(StoreDepositOperationCorrelationFields.operationFields(state), new Object[]{
                "diagnosticScope", "store_deposit_effect_summary",
                "owner", "store_deposit_summary_factory",
                "mode", "BOUNDARY",
                "trigger", terminalTrigger,
                "dedupe_key", "store_deposit_effect_summary|" + StoreDepositOperationCorrelationFields.operationId(state),
                "max_emission", "one_per_operation",
                "correlation", "storeOperationId=" + StoreDepositOperationCorrelationFields.operationId(state),
                "payload", "flat_fields",
                "terminal", true,
                "behavior_effect", "none",
                "effectObservationCount", state == null ? "unavailable" : state.effectObservationCount(),
                "expectedPositiveEffectCount", state == null ? "unavailable" : state.expectedPositiveEffectCount(),
                "effectObservationCounts", state == null ? "unavailable" : state.effectCounts(),
                "effectObservationComplete", false,
                "effectObservationCompletenessAuthority", "PARTIAL_SLOT_CALLBACKS_ONLY",
                "effectVerified", false,
                "effectVerificationAuthority", "NOT_VERIFIED_NO_DURABLE_ORACLE"
        });
    }

    private static String trackerRole(TrackerBinding binding) {
        return binding == null ? "UNBOUND" : binding.trackerRole();
    }

    private static String itemName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        return String.valueOf(stack.getItem());
    }

    private static int count(ItemStack stack) {
        return stack == null ? 0 : stack.getCount();
    }

    private record Delta(int componentCount,
                         String component1Item,
                         int component1Amount,
                         String component2Item,
                         int component2Amount) {
        private static Delta from(ItemStack before, ItemStack after) {
            boolean beforeEmpty = before == null || before.isEmpty();
            boolean afterEmpty = after == null || after.isEmpty();
            if (beforeEmpty && afterEmpty) {
                return new Delta(0, "UNAVAILABLE", 0, "UNAVAILABLE", 0);
            }
            if (!beforeEmpty && !afterEmpty && before.getItem() != after.getItem()) {
                return new Delta(2, itemName(before), -count(before), itemName(after), count(after));
            }
            ItemStack reference = afterEmpty ? before : after;
            int amount = count(after) - count(before);
            return new Delta(1, itemName(reference), amount, "UNAVAILABLE", 0);
        }
    }
}
