package lavi.minecraft.diagnostics.container.store.deposit.transfer;

import adris.altoclef.tasks.container.ContainerStoredTracker;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositBoundedEventLogger;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;

//20260830_kpopmodder: Describe the production notStored projection without rescanning inventory inputs.
public final class StoreDepositNotStoredDiagnostics {
    private final StoreDepositBindingRegistry bindings;
    private final StoreDepositEmissionGate emissionGate;

    public StoreDepositNotStoredDiagnostics(StoreDepositBindingRegistry bindings,
                                           StoreDepositEmissionGate emissionGate) {
        this.bindings = bindings;
        this.emissionGate = emissionGate;
    }

    public void observe(ContainerStoredTracker tracker,
                        ItemTarget input,
                        int storedCount,
                        boolean storedSatisfied,
                        boolean hasItemEvaluated,
                        boolean hasItem,
                        int firstAvailableCount,
                        boolean secondAvailableCountEvaluated,
                        int secondAvailableCount,
                        ItemTarget output) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        StoreDepositOperationState state = bindings.stateFor(tracker);
        if (state == null
                || !state.context().isAutomaticDepositOperation()
                || !state.automaticContext().available()) {
            return;
        }
        Decision decision = new Decision(
                input,
                storedCount,
                storedSatisfied,
                hasItemEvaluated,
                hasItem,
                firstAvailableCount,
                secondAvailableCountEvaluated,
                secondAvailableCount,
                output
        );
        String operationId = state.context().operationId();
        String key = operationId + "|" + decision.inputSummary() + "|" + decision.outputSummary();
        if (!emissionGate.shouldEmitDetail(operationId, "STORE_DEPOSIT_NOT_STORED_PROJECTION", key)) {
            return;
        }
        StoreDepositBoundedEventLogger.log(
                "STORE_DEPOSIT_NOT_STORED_PROJECTION",
                "store_deposit_not_stored_projection",
                null,
                ChatClefDiagnostics.withCommandContextFields(
                        StoreDepositEventFields.merge(
                                StoreDepositEventFields.operationFields(state),
                                decision.fields()
                        )
                )
        );
    }

    public record Decision(ItemTarget input,
                           int storedCount,
                           boolean storedSatisfied,
                           boolean hasItemEvaluated,
                           boolean hasItem,
                           int firstAvailableCount,
                           boolean secondAvailableCountEvaluated,
                           int secondAvailableCount,
                           ItemTarget output) {
        public int combinedAvailableCount() {
            if (!hasItemEvaluated) {
                return -1;
            }
            if (!hasItem) {
                return 0;
            }
            return secondAvailableCountEvaluated ? secondAvailableCount : firstAvailableCount;
        }

        public String inputSummary() {
            return ChatClefDiagnostics.itemTargets(input == null ? null : new ItemTarget[]{input});
        }

        public String outputSummary() {
            return ChatClefDiagnostics.itemTargets(output == null ? null : new ItemTarget[]{output});
        }

        public Object[] fields() {
            return new Object[]{
                    "rootStoredCountByTarget", storedCount < 0 ? "UNAVAILABLE" : storedCount,
                    "storedSatisfied", storedSatisfied,
                    "playerInventoryAvailableCount", "UNAVAILABLE",
                    "cursorAvailableCount", "UNAVAILABLE",
                    "conversionInputAvailableCount", "UNAVAILABLE",
                    "combinedAvailableCount", combinedAvailableCount() < 0
                            ? "UNAVAILABLE"
                            : combinedAvailableCount(),
                    "hasItemEvaluated", hasItemEvaluated,
                    "hasItem", hasItem,
                    "firstAvailableCount", firstAvailableCount < 0 ? "UNAVAILABLE" : firstAvailableCount,
                    "secondAvailableCountEvaluated", secondAvailableCountEvaluated,
                    "secondAvailableCount", secondAvailableCount < 0 ? "UNAVAILABLE" : secondAvailableCount,
                    "notStoredInput", inputSummary(),
                    "notStoredOutput", output == null ? "REMOVED" : outputSummary(),
                    "notStoredMeaning", "CURRENTLY_AVAILABLE_AND_UNSTORED_REQUEST_CAP",
                    "observationComplete", false,
                    "missingBoundaries", "PLAYER_CURSOR_CONVERSION_COMPONENT_BREAKDOWN",
                    "behavior_effect", "none"
            };
        }
    }
}
