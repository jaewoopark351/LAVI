package lavi.minecraft.task.container.deposit.auto.policy;

import lavi.minecraft.testsupport.TestItems;
import net.minecraft.item.Item;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoDepositItemClassificationPolicyTest {
    @Test
    void failsClosedForUnknownVanillaBlocksAndModdedItems() {
        AutoDepositItemClassificationPolicy policy = new AutoDepositItemClassificationPolicy(
                AutoDepositPolicyTestFixtures.definition()
        );

        assertEquals(AutoDepositItemClass.UNKNOWN,
                policy.classify(stack("minecraft:unlisted_functional_block", true)));
        assertEquals(AutoDepositItemClass.UNKNOWN,
                policy.classify(stack("examplemod:unknown_part", false)));
        assertEquals(AutoDepositItemClass.GENERAL_SURPLUS,
                policy.classify(stack("minecraft:cobblestone", true)));
        assertEquals(AutoDepositItemClass.CONDITIONAL_VALUABLE,
                policy.classify(stack("minecraft:diamond", false)));
    }

    private static AutoDepositStackSnapshot stack(String itemId, boolean blockItem) {
        Item item = TestItems.item();
        return new AutoDepositStackSnapshot(
                0,
                AutoDepositStackLocation.MAIN,
                item,
                itemId,
                64,
                64,
                false,
                false,
                false,
                "plain",
                AutoDepositItemRole.NONE,
                0,
                false,
                blockItem,
                false
        );
    }
}
