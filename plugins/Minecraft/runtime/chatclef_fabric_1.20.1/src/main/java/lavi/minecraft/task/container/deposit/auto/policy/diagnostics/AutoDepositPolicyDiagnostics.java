package lavi.minecraft.task.container.deposit.auto.policy.diagnostics;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositBoundedEventLogger;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositSharedBudget;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleState;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;

//20260830_kpopmodder: Emit one bounded immutable policy snapshot per already-computed planning result.
public final class AutoDepositPolicyDiagnostics {
    private static final int MAX_EVENT_UTF8_BYTES = 4096;
    private static final int MAX_ITEM_EVENT_UTF8_BYTES = 4096;
    private static final int MAX_STACK_EVENT_UTF8_BYTES = 4096;
    private static final StoreDepositEmissionGate EMISSION_GATE = StoreDepositSharedBudget.emissionGate();

    private AutoDepositPolicyDiagnostics() {
    }

    public static void log(AutoDepositPlan diagnosticPlan,
                           DepositAllInventoryPressureSnapshot pressure,
                           String planningStatus,
                           String planningReason,
                           Task task) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositAutomaticContext automaticContext =
                    StoreDepositAutomaticLifecycleState.contextForMaintenance(task);
            AutoDepositPolicySnapshot snapshot = AutoDepositPolicySnapshot.capture(
                    diagnosticPlan,
                    pressure,
                    planningStatus,
                    planningReason,
                    automaticContext
            );
            String budgetOperationId = budgetOperationId(snapshot);
            String semanticKey = snapshot.autoPlanId()
                    + "|" + snapshot.planningStatus()
                    + "|" + snapshot.planningReason();
            if (EMISSION_GATE.shouldEmitDetail(
                    budgetOperationId,
                    "AUTO_DEPOSIT_POLICY_SNAPSHOT",
                    semanticKey
            )) {
                StoreDepositBoundedEventLogger.log(
                        "AUTO_DEPOSIT_POLICY_SNAPSHOT",
                        "immutable_policy_decision_observed",
                        task,
                        MAX_EVENT_UTF8_BYTES,
                        snapshot.requiredFields(),
                        EMISSION_GATE.budgetSummaryFields(budgetOperationId)
                );
            }
            for (AutoDepositPolicyItemSnapshot item : snapshot.itemDecisions()) {
                String itemSemanticKey = snapshot.autoPlanId()
                        + "|" + item.itemDecisionId()
                        + "|" + item.summary();
                if (EMISSION_GATE.shouldEmitDetail(
                        budgetOperationId,
                        "AUTO_DEPOSIT_POLICY_ITEM_DECISION",
                        itemSemanticKey
                )) {
                    StoreDepositBoundedEventLogger.log(
                            "AUTO_DEPOSIT_POLICY_ITEM_DECISION",
                            "immutable_policy_item_decision_observed",
                            task,
                            MAX_ITEM_EVENT_UTF8_BYTES,
                            item.requiredFields(
                                    snapshot.inventorySnapshotId(),
                                    snapshot.autoPlanId(),
                                    snapshot.policyContextEpoch(),
                                    snapshot.automaticContext()
                            ),
                            EMISSION_GATE.budgetSummaryFields(budgetOperationId)
                    );
                }
                for (AutoDepositPolicyStackSnapshot stack : item.physicalStacks()) {
                    String stackSemanticKey = snapshot.autoPlanId()
                            + "|" + stack.stackFactId()
                            + "|" + stack.summary();
                    if (!EMISSION_GATE.shouldEmitDetail(
                            budgetOperationId,
                            "AUTO_DEPOSIT_POLICY_STACK_FACT",
                            stackSemanticKey
                    )) {
                        continue;
                    }
                    StoreDepositBoundedEventLogger.log(
                            "AUTO_DEPOSIT_POLICY_STACK_FACT",
                            "immutable_policy_physical_stack_observed",
                            task,
                            MAX_STACK_EVENT_UTF8_BYTES,
                            stack.requiredFields(
                                    snapshot.inventorySnapshotId(),
                                    snapshot.autoPlanId(),
                                    snapshot.policyContextEpoch(),
                                    snapshot.automaticContext()
                            ),
                            EMISSION_GATE.budgetSummaryFields(budgetOperationId)
                    );
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private static String budgetOperationId(AutoDepositPolicySnapshot snapshot) {
        if (snapshot.automaticContext().available()) {
            return snapshot.automaticContext().autoOperationId();
        }
        if (!"UNAVAILABLE".equals(snapshot.autoPlanId())) {
            return "policy-plan|" + snapshot.autoPlanId();
        }
        if (!"UNAVAILABLE".equals(snapshot.inventorySnapshotId())) {
            return "policy-inventory|" + snapshot.inventorySnapshotId();
        }
        return "policy-attempt|" + snapshot.planningStatus()
                + "|" + snapshot.planningReason()
                + "|" + snapshot.inventoryPressureState();
    }

}
