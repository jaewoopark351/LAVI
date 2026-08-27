package lavi.minecraft.task.container.home.planning;

import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositItemRole;

import java.util.Objects;

//20260827_kpopmodder: Added this type file to carry policy-neutral equipment and food facts.
public record HomeStorageItemFacts(
        AutoDepositItemRole role,
        int capabilityScore,
        int enchantmentScore,
        int remainingDurability,
        int maximumDurability,
        boolean safeGeneralFood,
        int foodScore) {

    public HomeStorageItemFacts {
        Objects.requireNonNull(role, "role");
        capabilityScore = Math.max(0, capabilityScore);
        enchantmentScore = Math.max(0, enchantmentScore);
        remainingDurability = Math.max(0, remainingDurability);
        maximumDurability = Math.max(0, maximumDurability);
        foodScore = Math.max(0, foodScore);
    }
}
