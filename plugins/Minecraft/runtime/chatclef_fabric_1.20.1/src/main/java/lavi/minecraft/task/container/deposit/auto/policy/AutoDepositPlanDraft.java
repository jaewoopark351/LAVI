package lavi.minecraft.task.container.deposit.auto.policy;

import lavi.minecraft.task.container.deposit.auto.policy.diagnostics.AutoDepositPolicyItemSnapshot;
import net.minecraft.item.Item;

import java.util.List;
import java.util.Map;

public final class AutoDepositPlanDraft {
    private final AutoDepositContextSnapshot context;
    private final List<AutoDepositPlannedItem> generalItems;
    private final List<AutoDepositPlannedItem> conditionalItems;
    private final Map<Item, Integer> protectedCounts;
    private final Map<Item, AutoDepositDisposition> dispositions;
    private final List<AutoDepositPolicyItemSnapshot> policyItemDecisions;
    private final String diagnosticInventoryFingerprint;
    private final boolean diagnosticPolicyObservationRetained;
    private final List<String> semanticEntries;
    private final int startingOccupiedSlots;
    private final int targetReliefSlots;

    AutoDepositPlanDraft(AutoDepositContextSnapshot context,
                         List<AutoDepositPlannedItem> generalItems,
                         List<AutoDepositPlannedItem> conditionalItems,
                         Map<Item, Integer> protectedCounts,
                         Map<Item, AutoDepositDisposition> dispositions,
                         List<AutoDepositPolicyItemSnapshot> policyItemDecisions,
                         String diagnosticInventoryFingerprint,
                         boolean diagnosticPolicyObservationRetained,
                         List<String> semanticEntries,
                         int startingOccupiedSlots,
                         int targetReliefSlots) {
        this.context = context;
        this.generalItems = List.copyOf(generalItems);
        this.conditionalItems = List.copyOf(conditionalItems);
        this.protectedCounts = Map.copyOf(protectedCounts);
        this.dispositions = Map.copyOf(dispositions);
        this.policyItemDecisions = List.copyOf(policyItemDecisions);
        this.diagnosticInventoryFingerprint = diagnosticInventoryFingerprint;
        this.diagnosticPolicyObservationRetained = diagnosticPolicyObservationRetained;
        this.semanticEntries = List.copyOf(semanticEntries);
        this.startingOccupiedSlots = startingOccupiedSlots;
        this.targetReliefSlots = targetReliefSlots;
    }

    public AutoDepositContextSnapshot context() {
        return context;
    }

    List<AutoDepositPlannedItem> generalItems() {
        return generalItems;
    }

    List<AutoDepositPlannedItem> conditionalItems() {
        return conditionalItems;
    }

    Map<Item, Integer> protectedCounts() {
        return protectedCounts;
    }

    Map<Item, AutoDepositDisposition> dispositions() {
        return dispositions;
    }

    List<AutoDepositPolicyItemSnapshot> policyItemDecisions() {
        return policyItemDecisions;
    }

    String diagnosticInventoryFingerprint() {
        return diagnosticInventoryFingerprint;
    }

    boolean diagnosticPolicyObservationRetained() {
        return diagnosticPolicyObservationRetained;
    }

    List<String> semanticEntries() {
        return semanticEntries;
    }

    public int trustedEmptySlotsRequired() {
        int remainingRelief = Math.max(0, targetReliefSlots - generalItems.size());
        return Math.min(remainingRelief, conditionalItems.size());
    }

    public int startingOccupiedSlots() {
        return startingOccupiedSlots;
    }

    public int targetReliefSlots() {
        return targetReliefSlots;
    }
}
