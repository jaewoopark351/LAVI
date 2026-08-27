package lavi.minecraft.task.container.home.planning;

import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositItemRole;

import java.util.Objects;

//20260827_kpopmodder: Capture one immutable logical-slot stack for manual home planning.
public record HomeStorageStackSnapshot(
        int logicalSlot,
        HomeStorageStackLocation location,
        HomeStorageStackFingerprint fingerprint,
        int count,
        AutoDepositItemRole role,
        int capabilityScore,
        int enchantmentScore,
        int remainingDurability,
        int maximumDurability,
        boolean selectedMainHand,
        boolean explicitlyProtected,
        boolean safeGeneralFood,
        int foodScore) {

    public HomeStorageStackSnapshot {
        if (logicalSlot < 0) {
            throw new IllegalArgumentException("logicalSlot must be non-negative");
        }
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(fingerprint, "fingerprint");
        Objects.requireNonNull(role, "role");
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
        capabilityScore = Math.max(0, capabilityScore);
        enchantmentScore = Math.max(0, enchantmentScore);
        remainingDurability = Math.max(0, remainingDurability);
        maximumDurability = Math.max(0, maximumDurability);
        foodScore = Math.max(0, foodScore);
    }

    public boolean isMainInventory() {
        return location == HomeStorageStackLocation.MAIN;
    }

    public boolean isDurabilityCritical() {
        return maximumDurability > 0
                && (long) remainingDurability * 10L < maximumDurability;
    }

    public String itemId() {
        return fingerprint.itemId();
    }
}
