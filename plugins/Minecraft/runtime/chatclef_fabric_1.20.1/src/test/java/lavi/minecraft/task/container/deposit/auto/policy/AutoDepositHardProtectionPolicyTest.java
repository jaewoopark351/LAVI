package lavi.minecraft.task.container.deposit.auto.policy;

import lavi.minecraft.testsupport.TestItems;
import net.minecraft.item.Item;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoDepositHardProtectionPolicyTest {
    @Test
    void protectsSelectedMetadataBotProtectedAndBestFunctionalTool() {
        Item selectedItem = TestItems.item();
        Item metadataItem = TestItems.item();
        Item botItem = TestItems.item();
        Item pickaxeItem = TestItems.item();
        Item equippedItem = TestItems.item();
        AutoDepositStackSnapshot selected = AutoDepositPolicyTestFixtures.stack(
                selectedItem, "minecraft:cobblestone", 64, 0, true, false, false,
                AutoDepositItemRole.NONE, 0
        );
        AutoDepositStackSnapshot metadata = AutoDepositPolicyTestFixtures.stack(
                metadataItem, "minecraft:iron_ingot", 1, 1, false, false, true,
                AutoDepositItemRole.NONE, 0
        );
        AutoDepositStackSnapshot botProtected = AutoDepositPolicyTestFixtures.stack(
                botItem, "minecraft:stick", 8, 2, false, true, false,
                AutoDepositItemRole.NONE, 0
        );
        AutoDepositStackSnapshot weakerPickaxe = AutoDepositPolicyTestFixtures.stack(
                pickaxeItem, "minecraft:stone_pickaxe", 1, 3, false, false, false,
                AutoDepositItemRole.PICKAXE, 210
        );
        AutoDepositStackSnapshot strongerPickaxe = AutoDepositPolicyTestFixtures.stack(
                TestItems.item(), "minecraft:iron_pickaxe", 1, 4, false, false, false,
                AutoDepositItemRole.PICKAXE, 510
        );
        AutoDepositStackSnapshot equipped = AutoDepositPolicyTestFixtures.stackAt(
                equippedItem,
                "minecraft:iron_boots",
                1,
                0,
                AutoDepositStackLocation.ARMOR,
                false,
                false,
                false,
                AutoDepositItemRole.BOOTS,
                510
        );

        AutoDepositHardProtectionResult result = new AutoDepositHardProtectionPolicy(
                AutoDepositPolicyTestFixtures.definition()
        ).evaluate(List.of(
                selected, metadata, botProtected, weakerPickaxe, strongerPickaxe, equipped
        ));

        assertTrue(result.isProtected(selected));
        assertTrue(result.isProtected(metadata));
        assertTrue(result.isProtected(botProtected));
        assertTrue(result.isProtected(strongerPickaxe));
        assertTrue(result.isProtected(equipped));
        assertFalse(result.isProtected(weakerPickaxe));
    }
}
