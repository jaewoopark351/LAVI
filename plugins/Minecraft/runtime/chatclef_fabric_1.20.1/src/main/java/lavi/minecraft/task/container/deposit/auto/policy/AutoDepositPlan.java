package lavi.minecraft.task.container.deposit.auto.policy;

import adris.altoclef.util.ItemTarget;
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
    private final BlockPos trustedDestination;
    private final Map<Item, Integer> protectedCounts;
    private final Map<Item, AutoDepositDisposition> dispositions;
    private final int startingOccupiedSlots;
    private final int targetReliefSlots;
    private final int expectedFreedSlots;
    private final AutoDepositDecisionFingerprint fingerprint;

    AutoDepositPlan(AutoDepositContextSnapshot context,
                    ItemTarget[] generalTargets,
                    ItemTarget[] trustedTargets,
                    BlockPos trustedDestination,
                    Map<Item, Integer> protectedCounts,
                    Map<Item, AutoDepositDisposition> dispositions,
                    int startingOccupiedSlots,
                    int targetReliefSlots,
                    int expectedFreedSlots,
                    AutoDepositDecisionFingerprint fingerprint) {
        this.context = context;
        this.generalTargets = generalTargets.clone();
        this.trustedTargets = trustedTargets.clone();
        this.trustedDestination = trustedDestination == null ? null : trustedDestination.toImmutable();
        this.protectedCounts = Collections.unmodifiableMap(new LinkedHashMap<>(protectedCounts));
        this.dispositions = Collections.unmodifiableMap(new LinkedHashMap<>(dispositions));
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
        return Optional.ofNullable(trustedDestination);
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
