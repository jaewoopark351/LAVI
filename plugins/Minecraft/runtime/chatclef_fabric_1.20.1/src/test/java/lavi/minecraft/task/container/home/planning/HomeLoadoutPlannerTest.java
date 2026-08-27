package lavi.minecraft.task.container.home.planning;

import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositItemRole;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260827_kpopmodder: Added focused tests for exact-slot loadout and SAFE reserve planning.
class HomeLoadoutPlannerTest {
    private final HomeLoadoutPlanner planner = new HomeLoadoutPlanner();

    @Test
    void keepsTheHealthyExactPickaxeSlotAndStoresTheSpare() {
        HomeStorageStackSnapshot healthy = equipment(
                5, "minecraft:diamond_pickaxe", AutoDepositItemRole.PICKAXE,
                6, 1400, 1561, false
        );
        HomeStorageStackSnapshot critical = equipment(
                17, "minecraft:diamond_pickaxe", AutoDepositItemRole.PICKAXE,
                6, 50, 1561, true
        );

        HomeStoragePlan plan = planner.plan(List.of(healthy, critical));

        assertEquals(HomeStorageDisposition.KEEP_LOADOUT, disposition(plan, 5));
        assertEquals(HomeStorageDisposition.STORE_HOME, disposition(plan, 17));
        assertEquals(List.of(17), plan.manifest().steps().stream()
                .map(HomeStorageManifestStep::logicalPlayerInventorySlot)
                .toList());
    }

    @Test
    void nonCriticalToolBeatsHigherTierCriticalTool() {
        HomeStorageStackSnapshot criticalDiamond = equipment(
                2, "minecraft:diamond_shovel", AutoDepositItemRole.SHOVEL,
                6, 20, 1561, true
        );
        HomeStorageStackSnapshot healthyIron = equipment(
                9, "minecraft:iron_shovel", AutoDepositItemRole.SHOVEL,
                5, 200, 250, false
        );

        HomeStoragePlan plan = planner.plan(List.of(criticalDiamond, healthyIron));

        assertEquals(HomeStorageDisposition.STORE_HOME, disposition(plan, 2));
        assertEquals(HomeStorageDisposition.KEEP_LOADOUT, disposition(plan, 9));
    }

    @Test
    void keepsEquippedArmorAndStoresUnwornArmor() {
        HomeStorageStackSnapshot equipped = snapshot(
                2, HomeStorageStackLocation.ARMOR,
                "minecraft:diamond_chestplate", 1,
                AutoDepositItemRole.CHESTPLATE, 6, 0, 500, 528,
                false, false, false, 0, null
        );
        HomeStorageStackSnapshot unworn = equipment(
                12, "minecraft:iron_chestplate", AutoDepositItemRole.CHESTPLATE,
                5, 200, 240, false
        );

        HomeStoragePlan plan = planner.plan(List.of(equipped, unworn));

        assertEquals(HomeStorageDisposition.KEEP_LOADOUT,
                plan.entries().get(0).disposition());
        assertEquals(HomeStorageDisposition.STORE_HOME, disposition(plan, 12));
    }

    @Test
    void safeReserveKeepsWholeStacksAndRequiresRetainedRangedWeaponForArrows() {
        HomeStorageStackSnapshot bread = snapshot(
                0, HomeStorageStackLocation.MAIN, "minecraft:bread", 64,
                AutoDepositItemRole.NONE, 0, 0, 0, 0,
                false, false, true, 500, null
        );
        HomeStorageStackSnapshot torches = plain(1, "minecraft:torch", 64);
        HomeStorageStackSnapshot arrows = plain(2, "minecraft:arrow", 64);
        HomeStorageStackSnapshot offhandBow = snapshot(
                0, HomeStorageStackLocation.OFFHAND, "minecraft:bow", 1,
                AutoDepositItemRole.BOW, 4, 0, 300, 384,
                false, false, false, 0, null
        );

        HomeStoragePlan plan = planner.plan(List.of(bread, torches, arrows, offhandBow));

        assertEquals(HomeStorageDisposition.KEEP_RESERVE, disposition(plan, 0));
        assertEquals(HomeStorageDisposition.KEEP_RESERVE, disposition(plan, 1));
        assertEquals(HomeStorageDisposition.KEEP_RESERVE, disposition(plan, 2));
        assertTrue(plan.manifest().steps().isEmpty());

        HomeStoragePlan withoutBow = planner.plan(List.of(arrows));
        assertEquals(HomeStorageDisposition.STORE_HOME, disposition(withoutBow, 2));
    }

    @Test
    void exactMetadataFingerprintDistinguishesOtherwiseEqualItems() {
        NbtCompound enchantedNbt = new NbtCompound();
        enchantedNbt.putString("marker", "enchanted");
        HomeStorageStackFingerprint plain = HomeStorageStackFingerprint.of(
                "minecraft:diamond", 0, null
        );
        HomeStorageStackFingerprint enchanted = HomeStorageStackFingerprint.of(
                "minecraft:diamond", 0, enchantedNbt
        );

        assertFalse(plain.equals(enchanted));
        assertTrue(enchanted.equals(HomeStorageStackFingerprint.of(
                "minecraft:diamond", 0, enchantedNbt
        )));
    }

    private static HomeStorageDisposition disposition(HomeStoragePlan plan, int logicalSlot) {
        return plan.entries().stream()
                .filter(entry -> entry.snapshot().isMainInventory())
                .filter(entry -> entry.snapshot().logicalSlot() == logicalSlot)
                .findFirst()
                .orElseThrow()
                .disposition();
    }

    private static HomeStorageStackSnapshot equipment(
            int slot,
            String itemId,
            AutoDepositItemRole role,
            int capability,
            int remaining,
            int maximum,
            boolean selected) {
        return snapshot(
                slot, HomeStorageStackLocation.MAIN, itemId, 1,
                role, capability, 0, remaining, maximum,
                selected, false, false, 0, null
        );
    }

    private static HomeStorageStackSnapshot plain(int slot, String itemId, int count) {
        return snapshot(
                slot, HomeStorageStackLocation.MAIN, itemId, count,
                AutoDepositItemRole.NONE, 0, 0, 0, 0,
                false, false, false, 0, null
        );
    }

    private static HomeStorageStackSnapshot snapshot(
            int slot,
            HomeStorageStackLocation location,
            String itemId,
            int count,
            AutoDepositItemRole role,
            int capability,
            int enchantment,
            int remaining,
            int maximum,
            boolean selected,
            boolean explicitlyProtected,
            boolean safeFood,
            int foodScore,
            NbtCompound nbt) {
        return new HomeStorageStackSnapshot(
                slot,
                location,
                HomeStorageStackFingerprint.of(itemId, 0, nbt),
                count,
                role,
                capability,
                enchantment,
                remaining,
                maximum,
                selected,
                explicitlyProtected,
                safeFood,
                foodScore
        );
    }
}
