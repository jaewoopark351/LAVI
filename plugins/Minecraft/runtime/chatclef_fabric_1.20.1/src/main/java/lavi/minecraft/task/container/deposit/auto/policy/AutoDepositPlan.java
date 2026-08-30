package lavi.minecraft.task.container.deposit.auto.policy;

import adris.altoclef.util.ItemTarget;
import lavi.minecraft.task.container.deposit.auto.policy.diagnostics.AutoDepositPolicyItemSnapshot;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AutoDepositPlan {
    private final AutoDepositContextSnapshot context;
    private final ItemTarget[] generalTargets;
    private final ItemTarget[] trustedTargets;
    //20260827_kpopmodder: Preserve the ordered trusted candidate snapshot for one operation.
    private final List<AutoDepositTrustedDestinationCandidate> trustedCandidates;
    private final Map<Item, Integer> protectedCounts;
    private final Map<Item, AutoDepositDisposition> dispositions;
    private final List<AutoDepositPolicyItemSnapshot> policyItemDecisions;
    private final String diagnosticInventoryFingerprint;
    private final long trustedRevision;
    private final String trustedCapacityState;
    private final int startingOccupiedSlots;
    private final int targetReliefSlots;
    private final int expectedFreedSlots;
    private final AutoDepositDecisionFingerprint fingerprint;

    AutoDepositPlan(AutoDepositContextSnapshot context,
                    ItemTarget[] generalTargets,
                    ItemTarget[] trustedTargets,
                    List<AutoDepositTrustedDestinationCandidate> trustedCandidates,
                    Map<Item, Integer> protectedCounts,
                    Map<Item, AutoDepositDisposition> dispositions,
                    List<AutoDepositPolicyItemSnapshot> policyItemDecisions,
                    String diagnosticInventoryFingerprint,
                    long trustedRevision,
                    String trustedCapacityState,
                    int startingOccupiedSlots,
                    int targetReliefSlots,
                    int expectedFreedSlots,
                    AutoDepositDecisionFingerprint fingerprint) {
        this.context = context;
        this.generalTargets = generalTargets.clone();
        this.trustedTargets = trustedTargets.clone();
        this.trustedCandidates = List.copyOf(trustedCandidates);
        this.protectedCounts = Collections.unmodifiableMap(new LinkedHashMap<>(protectedCounts));
        this.dispositions = Collections.unmodifiableMap(new LinkedHashMap<>(dispositions));
        this.policyItemDecisions = List.copyOf(policyItemDecisions);
        this.diagnosticInventoryFingerprint = diagnosticInventoryFingerprint;
        this.trustedRevision = trustedRevision;
        this.trustedCapacityState = trustedCapacityState;
        this.startingOccupiedSlots = startingOccupiedSlots;
        this.targetReliefSlots = targetReliefSlots;
        this.expectedFreedSlots = expectedFreedSlots;
        this.fingerprint = fingerprint;
    }

    public AutoDepositContextSnapshot context() {
        return context;
    }

    public ItemTarget[] generalTargets() {
        return generalTargets.clone();
    }

    public ItemTarget[] trustedTargets() {
        return trustedTargets.clone();
    }

    public Optional<BlockPos> trustedDestination() {
        return trustedCandidates.stream().findFirst()
                .map(AutoDepositTrustedDestinationCandidate::position);
    }

    public List<AutoDepositTrustedDestinationCandidate> trustedCandidates() {
        return trustedCandidates;
    }

    public ItemTarget[] allTargets() {
        List<ItemTarget> result = new ArrayList<>(generalTargets.length + trustedTargets.length);
        Collections.addAll(result, generalTargets);
        Collections.addAll(result, trustedTargets);
        return result.toArray(ItemTarget[]::new);
    }

    public boolean hasTargets() {
        return generalTargets.length > 0 || trustedTargets.length > 0;
    }

    public Map<Item, Integer> protectedCounts() {
        return protectedCounts;
    }

    public Map<Item, AutoDepositDisposition> dispositions() {
        return dispositions;
    }

    public List<AutoDepositPolicyItemSnapshot> policyItemDecisions() {
        return policyItemDecisions;
    }

    public String diagnosticInventoryFingerprint() {
        return diagnosticInventoryFingerprint;
    }

    public boolean diagnosticPolicyObservationRetained() {
        return !"UNAVAILABLE_DIAGNOSTICS_OFF".equals(diagnosticInventoryFingerprint);
    }

    public long trustedRevision() {
        return trustedRevision;
    }

    public String trustedCapacityState() {
        return trustedCapacityState;
    }

    public int startingOccupiedSlots() {
        return startingOccupiedSlots;
    }

    public int targetReliefSlots() {
        return targetReliefSlots;
    }

    public int expectedFreedSlots() {
        return expectedFreedSlots;
    }

    public AutoDepositDecisionFingerprint fingerprint() {
        return fingerprint;
    }
}
