package lavi.minecraft.task.container.deposit.auto.policy;

import net.minecraft.item.Item;

import java.util.Set;

final class AutoDepositPolicyTestFixtures {
    private AutoDepositPolicyTestFixtures() {
    }

    static AutoDepositPolicyDefinition definition() {
        return new AutoDepositPolicyDefinition(
                1,
                Set.of("minecraft:filled_map"),
                Set.of("minecraft:elytra", "minecraft:trident"),
                Set.of("minecraft:diamond"),
                Set.of("minecraft:diamond_", "minecraft:netherite_"),
                Set.of("minecraft:coal", "minecraft:charcoal", "minecraft:torch", "minecraft:iron_ingot"),
                Set.of("_log", "_planks"),
                Set.of("minecraft:cobblestone"),
                Set.of("minecraft:spider_eye"),
                64,
                16,
                32,
                16,
                16,
                32,
                32,
                32,
                1,
                1,
                128
        );
    }

    static AutoDepositStackSnapshot stack(Item item,
                                           String itemId,
                                           int count,
                                           int slot,
                                           boolean selected,
                                           boolean botProtected,
                                           boolean specialMetadata,
                                           AutoDepositItemRole role,
                                           int equipmentScore) {
        return stackAt(
                item,
                itemId,
                count,
                slot,
                AutoDepositStackLocation.MAIN,
                selected,
                botProtected,
                specialMetadata,
                role,
                equipmentScore
        );
    }

    static AutoDepositStackSnapshot stackAt(Item item,
                                             String itemId,
                                             int count,
                                             int slot,
                                             AutoDepositStackLocation location,
                                             boolean selected,
                                             boolean botProtected,
                                             boolean specialMetadata,
                                             AutoDepositItemRole role,
                                             int equipmentScore) {
        return new AutoDepositStackSnapshot(
                slot,
                location,
                item,
                itemId,
                count,
                64,
                selected,
                botProtected,
                specialMetadata,
                specialMetadata ? "special" : "plain",
                role,
                equipmentScore,
                false,
                false,
                false
        );
    }
}
