package lavi.minecraft.task.container.deposit.auto.recovery;

import net.minecraft.item.Item;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record AutoDepositRecoveryTransferPlan(Map<Item, Integer> targetInventoryCounts,
                                              Map<Item, Integer> withdrawalLimits) {
    public AutoDepositRecoveryTransferPlan {
        targetInventoryCounts = immutableCounts(targetInventoryCounts);
        withdrawalLimits = immutableCounts(withdrawalLimits);
    }

    public static AutoDepositRecoveryTransferPlan create(Map<Item, Integer> currentCounts,
                                                         Map<Item, Integer> deficits,
                                                         AutoDepositRecoveryCandidate candidate) {
        Objects.requireNonNull(currentCounts, "currentCounts");
        Objects.requireNonNull(deficits, "deficits");
        Objects.requireNonNull(candidate, "candidate");
        Map<Item, Integer> targets = new LinkedHashMap<>();
        Map<Item, Integer> limits = new LinkedHashMap<>();
        deficits.forEach((item, deficit) -> {
            int allowed = Math.min(deficit, candidate.withdrawalLimits().getOrDefault(item, 0));
            if (allowed > 0) {
                targets.put(item, currentCounts.getOrDefault(item, 0) + allowed);
                limits.put(item, allowed);
            }
        });
        return new AutoDepositRecoveryTransferPlan(targets, limits);
    }

    public boolean isEmpty() {
        return targetInventoryCounts.isEmpty();
    }

    private static Map<Item, Integer> immutableCounts(Map<Item, Integer> source) {
        Map<Item, Integer> copy = new LinkedHashMap<>();
        source.forEach((item, count) -> {
            if (item != null && count != null && count > 0) {
                copy.put(item, count);
            }
        });
        return Collections.unmodifiableMap(copy);
    }
}
