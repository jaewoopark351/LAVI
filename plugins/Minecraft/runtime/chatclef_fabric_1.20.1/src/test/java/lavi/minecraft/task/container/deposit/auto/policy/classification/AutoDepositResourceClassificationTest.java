package lavi.minecraft.task.container.deposit.auto.policy.classification;

import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositItemClass;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositStackSnapshot;
import lavi.minecraft.task.container.deposit.auto.policy.classification.support.AutoDepositClassificationFixture;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

//20260916_kpopmodder: Verify exact requested item grades against the real classpath policy resource.
class AutoDepositResourceClassificationTest {
    @BeforeAll
    static void bootstrap() { AutoDepositClassificationFixture.bootstrap(); }

    @Test
    void loadedResourceUsesRevisionTwoAndClassifiesFiveBaseStonesAndSugarAsGeneral() {
        var fixture = new AutoDepositClassificationFixture();
        assertTrue(fixture.definition.loaded());
        assertEquals(2, fixture.definition.revision());
        for (AutoDepositStackSnapshot stack : fixture.read(
                new ItemStack(Items.TUFF), new ItemStack(Items.GRAVEL), new ItemStack(Items.ANDESITE),
                new ItemStack(Items.GRANITE), new ItemStack(Items.DIORITE), new ItemStack(Items.SUGAR_CANE))) {
            assertEquals(AutoDepositItemClass.GENERAL_SURPLUS, fixture.classification.classify(stack), stack.itemId());
        }
    }

    @Test
    void lapisLazuliIsExplicitlyValuableAndRemovedFromGeneralMembership() {
        var fixture = new AutoDepositClassificationFixture();
        AutoDepositStackSnapshot lapis = fixture.read(new ItemStack(Items.LAPIS_LAZULI)).get(0);
        assertTrue(fixture.definition.isValuable(lapis.itemId()));
        assertFalse(fixture.definition.isKnownGeneral(lapis.itemId()));
        assertEquals(AutoDepositItemClass.CONDITIONAL_VALUABLE, fixture.classification.classify(lapis));
    }

    @Test
    void lapisOresBlockAndUnrelatedUnknownsKeepTheirExistingGrades() {
        var fixture = new AutoDepositClassificationFixture();
        List<AutoDepositStackSnapshot> stacks = fixture.read(new ItemStack(Items.LAPIS_ORE),
                new ItemStack(Items.DEEPSLATE_LAPIS_ORE), new ItemStack(Items.LAPIS_BLOCK),
                new ItemStack(Items.SPONGE), new ItemStack(Items.POLISHED_ANDESITE));
        assertEquals(AutoDepositItemClass.GENERAL_SURPLUS, fixture.classification.classify(stacks.get(0)));
        assertEquals(AutoDepositItemClass.GENERAL_SURPLUS, fixture.classification.classify(stacks.get(1)));
        for (AutoDepositStackSnapshot stack : stacks.subList(2, stacks.size())) {
            assertEquals(AutoDepositItemClass.UNKNOWN, fixture.classification.classify(stack), stack.itemId());
            assertFalse(fixture.definition.isValuable(stack.itemId()), stack.itemId());
        }
    }

    @Test
    void storageGradeAddsNoBuildingReserveAndGravelRemainsAFallingBlock() {
        var fixture = new AutoDepositClassificationFixture();
        List<AutoDepositStackSnapshot> stacks = fixture.read(new ItemStack(Items.TUFF, 64),
                new ItemStack(Items.GRAVEL, 64), new ItemStack(Items.ANDESITE, 64),
                new ItemStack(Items.GRANITE, 64), new ItemStack(Items.DIORITE, 64),
                new ItemStack(Items.COBBLESTONE, 40), new ItemStack(Items.DIRT, 40));
        Map<Item, Integer> reserves = fixture.reserves(stacks);
        for (AutoDepositStackSnapshot stack : stacks.subList(0, 5)) {
            assertFalse(fixture.definition.isSafeBuilding(stack.itemId()), stack.itemId());
            assertFalse(fixture.classification.isSafeBuilding(stack), stack.itemId());
            assertEquals(0, reserves.getOrDefault(stack.item(), 0), stack.itemId());
        }
        assertTrue(stacks.get(1).fallingBlock());
        assertEquals(64, fixture.definition.safeBuildingReserve());
        assertEquals(Map.of(Items.COBBLESTONE, 40, Items.DIRT, 24), reserves);
    }
}
