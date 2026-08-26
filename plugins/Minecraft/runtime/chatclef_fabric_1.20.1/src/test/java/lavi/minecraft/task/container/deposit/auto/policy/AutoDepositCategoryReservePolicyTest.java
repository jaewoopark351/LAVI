package lavi.minecraft.task.container.deposit.auto.policy;

import lavi.minecraft.testsupport.TestItems;
import net.minecraft.item.Item;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoDepositCategoryReservePolicyTest {
    @Test
    void allocatesFuelAsOneCategoryTotalInsteadOfPerItem() {
        Item coal = TestItems.item();
        Item charcoal = TestItems.item();
        List<AutoDepositStackSnapshot> stacks = List.of(
                AutoDepositPolicyTestFixtures.stack(
                        coal, "minecraft:coal", 10, 0, false, false, false,
                        AutoDepositItemRole.NONE, 0
                ),
                AutoDepositPolicyTestFixtures.stack(
                        charcoal, "minecraft:charcoal", 20, 1, false, false, false,
                        AutoDepositItemRole.NONE, 0
                )
        );
        AutoDepositPolicyDefinition definition = AutoDepositPolicyTestFixtures.definition();
        AutoDepositItemClassificationPolicy classification =
                new AutoDepositItemClassificationPolicy(definition);
        AutoDepositHardProtectionResult hard =
                new AutoDepositHardProtectionPolicy(definition).evaluate(stacks);

        Map<Item, Integer> reserves = new AutoDepositCategoryReservePolicy(
                definition, classification
        ).allocate(stacks, hard, false);

        assertEquals(16, reserves.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(Integer.valueOf(16), reserves.get(charcoal));
    }
}
