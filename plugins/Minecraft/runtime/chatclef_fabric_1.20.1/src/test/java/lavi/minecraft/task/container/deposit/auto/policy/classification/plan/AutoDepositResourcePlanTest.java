package lavi.minecraft.task.container.deposit.auto.policy.classification.plan;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositDisposition;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositStackSnapshot;
import lavi.minecraft.task.container.deposit.auto.policy.classification.support.AutoDepositClassificationFixture;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static lavi.minecraft.task.container.deposit.auto.policy.classification.support.AutoDepositClassificationFixture.targetCounts;
import static org.junit.jupiter.api.Assertions.*;

//20260916_kpopmodder: Verify classification flows through existing whole-stack, protection and destination planning.
class AutoDepositResourcePlanTest {
    @BeforeAll
    static void bootstrap() { AutoDepositClassificationFixture.bootstrap(); }
    @BeforeEach
    void disableDiagnostics() { ChatClefDiagnostics.setBoundaryEnabled(false); }
    @AfterEach
    void keepDiagnosticsOff() { ChatClefDiagnostics.setBoundaryEnabled(false); }

    @Test
    void fiveStonesAndSugarCanFillGeneralReliefWhileLapisAndUnknownStayOut() {
        var fixture = new AutoDepositClassificationFixture();
        var stacks = fixture.read(new ItemStack(Items.TUFF, 26), new ItemStack(Items.GRAVEL, 45),
                new ItemStack(Items.ANDESITE, 64), new ItemStack(Items.GRANITE, 64),
                new ItemStack(Items.DIORITE, 64), new ItemStack(Items.SUGAR_CANE, 2),
                new ItemStack(Items.LAPIS_LAZULI, 14), new ItemStack(Items.SPONGE, 8));
        AutoDepositPlan plan = fixture.plan(stacks, 6, Map.of(), false, 260916201L);
        assertEquals(Map.of(Items.TUFF, 26, Items.GRAVEL, 45, Items.ANDESITE, 64,
                Items.GRANITE, 64, Items.DIORITE, 64, Items.SUGAR_CANE, 2), targetCounts(plan.generalTargets()));
        assertEquals(0, plan.trustedTargets().length);
        assertEquals(6, plan.expectedFreedSlots());
        assertEquals(AutoDepositDisposition.CONDITIONAL_VALUABLE, plan.dispositions().get(Items.LAPIS_LAZULI));
        assertEquals(AutoDepositDisposition.UNCLASSIFIED_CONSERVATIVE, plan.dispositions().get(Items.SPONGE));
    }

    @Test
    void lapisCannotUseGeneralStorageWithoutATrustedSnapshot() {
        var fixture = new AutoDepositClassificationFixture();
        AutoDepositPlan plan = fixture.plan(fixture.read(new ItemStack(Items.LAPIS_LAZULI, 14)),
                1, Map.of(), false, 260916202L);
        assertFalse(plan.hasTargets());
        assertTrue(plan.trustedCandidates().isEmpty());
        assertEquals(AutoDepositDisposition.CONDITIONAL_VALUABLE, plan.dispositions().get(Items.LAPIS_LAZULI));
    }

    @Test
    void lapisUsesOnlyTrustedStorageWhenGeneralSurplusLeavesReliefUnfilled() {
        var fixture = new AutoDepositClassificationFixture();
        AutoDepositPlan plan = fixture.plan(fixture.read(new ItemStack(Items.SUGAR_CANE, 2),
                new ItemStack(Items.LAPIS_LAZULI, 14)), 2, Map.of(), true, 260916203L);
        assertEquals(Map.of(Items.SUGAR_CANE, 2), targetCounts(plan.generalTargets()));
        assertEquals(Map.of(Items.LAPIS_LAZULI, 14), targetCounts(plan.trustedTargets()));
        assertEquals(fixture.trustedCandidate().destinationId(), plan.trustedCandidates().get(0).destinationId());
    }

    @Test
    void trustedLapisDoesNotDisplaceGeneralSurplusOnceReliefIsFilled() {
        var fixture = new AutoDepositClassificationFixture();
        AutoDepositPlan plan = fixture.plan(fixture.read(new ItemStack(Items.SUGAR_CANE, 2),
                new ItemStack(Items.LAPIS_LAZULI, 64)), 1, Map.of(), true, 260916204L);
        assertEquals(Map.of(Items.SUGAR_CANE, 2), targetCounts(plan.generalTargets()));
        assertEquals(0, plan.trustedTargets().length);
        assertTrue(plan.trustedCandidates().isEmpty());
    }

    @Test
    void preservedMetadataStillProtectsEachChangedItemIncludingItsPlainStacks() {
        var fixture = new AutoDepositClassificationFixture();
        for (Item item : List.of(Items.TUFF, Items.GRAVEL, Items.ANDESITE, Items.GRANITE,
                Items.DIORITE, Items.LAPIS_LAZULI)) {
            ItemStack named = new ItemStack(item, 16);
            named.setCustomName(Text.literal("keep classification fixture"));
            var stacks = fixture.read(named, new ItemStack(item, 64));
            AutoDepositPlan plan = fixture.plan(stacks, 2, Map.of(), true, 260916205L);
            assertFalse(plan.hasTargets(), stacks.get(0).itemId());
            assertEquals(80, plan.protectedCounts().get(item), stacks.get(0).itemId());
            assertEquals(AutoDepositDisposition.HARD_PROTECTED, plan.dispositions().get(item));
        }
    }

    @Test
    void workingSetKeepsReservedStoneAndLapisWhileOnlyWholeSurplusStacksAreSelected() {
        var fixture = new AutoDepositClassificationFixture();
        var stacks = fixture.read(new ItemStack(Items.ANDESITE, 64), new ItemStack(Items.ANDESITE, 16),
                new ItemStack(Items.LAPIS_LAZULI, 64), new ItemStack(Items.LAPIS_LAZULI, 16));
        AutoDepositPlan plan = fixture.plan(stacks, 4,
                Map.of(Items.ANDESITE, 16, Items.LAPIS_LAZULI, 16), true, 260916206L);
        assertEquals(Map.of(Items.ANDESITE, 16, Items.LAPIS_LAZULI, 16), plan.protectedCounts());
        assertEquals(Map.of(Items.ANDESITE, 64), targetCounts(plan.generalTargets()));
        assertEquals(Map.of(Items.LAPIS_LAZULI, 64), targetCounts(plan.trustedTargets()));
        assertEquals(2, plan.expectedFreedSlots());
    }

    @Test
    void aReservedSingleStackIsNotPartiallySentToEitherDestination() {
        var fixture = new AutoDepositClassificationFixture();
        List<AutoDepositStackSnapshot> stacks = fixture.read(new ItemStack(Items.ANDESITE, 64),
                new ItemStack(Items.LAPIS_LAZULI, 64));
        AutoDepositPlan plan = fixture.plan(stacks, 2,
                Map.of(Items.ANDESITE, 1, Items.LAPIS_LAZULI, 1), true, 260916207L);
        assertFalse(plan.hasTargets());
        assertEquals(Map.of(Items.ANDESITE, 1, Items.LAPIS_LAZULI, 1), plan.protectedCounts());
    }
}
