package lavi.minecraft.task.container.deposit.auto.policy.classification.support;

import adris.altoclef.AltoClef;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.TestTask;
import lavi.minecraft.task.container.deposit.auto.policy.*;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//20260916_kpopmodder: Reuse real resource loading and snapshot/planning owners without changing game singletons.
public final class AutoDepositClassificationFixture {
    public final AutoDepositPolicyDefinition definition = new AutoDepositPolicyLoader().loadOrFailClosed();
    public final AutoDepositItemClassificationPolicy classification = new AutoDepositItemClassificationPolicy(definition);
    public final AutoDepositPlanBuilder builder = new AutoDepositPlanBuilder(definition, classification);

    public static void bootstrap() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    public List<AutoDepositStackSnapshot> read(ItemStack... stacks) {
        ClientPlayerEntity player = TestObjects.allocate(ClientPlayerEntity.class);
        PlayerInventory inventory = new PlayerInventory(player);
        inventory.selectedSlot = 8;
        for (int i = 0; i < stacks.length; i++) inventory.main.set(9 + i, stacks[i]);
        TestObjects.setField(player, PlayerEntity.class, "inventory", inventory);
        SnapshotMod mod = TestObjects.allocate(SnapshotMod.class);
        mod.player = player;
        return new AutoDepositStackSnapshotReader().read(mod);
    }

    public Map<Item, Integer> reserves(List<AutoDepositStackSnapshot> stacks) {
        return new AutoDepositCategoryReservePolicy(definition, classification).allocate(
                stacks, new AutoDepositHardProtectionPolicy(definition).evaluate(stacks), false);
    }

    public AutoDepositPlanDraft draft(List<AutoDepositStackSnapshot> stacks, int relief,
                                     Map<Item, Integer> required, long epoch) {
        Object world = new Object();
        TestTask root = new TestTask("classification-working-set");
        Map<Item, Integer> held = new LinkedHashMap<>();
        stacks.forEach(stack -> held.merge(stack.item(), stack.count(), Integer::sum));
        WorkingSetSnapshot working = required.isEmpty() ? null : new WorkingSetSnapshot(
                root, List.of(root), world, Dimension.OVERWORLD, epoch, held, held, required);
        AutoDepositContextSnapshot context = new AutoDepositContextSnapshot(
                world, Dimension.OVERWORLD, "classification-fixture-world", epoch,
                working == null ? null : root, working, List.of("classification-fixture"));
        AutoDepositHardProtectionResult hard = new AutoDepositHardProtectionPolicy(definition).evaluate(stacks);
        return builder.prepare(context, stacks, hard, reserves(stacks), 33, relief);
    }

    public AutoDepositPlan plan(List<AutoDepositStackSnapshot> stacks, int relief,
                                Map<Item, Integer> required, boolean trusted, long epoch) {
        return builder.finish(draft(stacks, relief, required, epoch),
                trusted ? List.of(trustedCandidate()) : List.of(), 1L,
                trusted ? "fixture_eligible_snapshot" : "none");
    }

    public AutoDepositTrustedDestinationCandidate trustedCandidate() {
        return new AutoDepositTrustedDestinationCandidate(new AutoDepositTrustedDestination(
                "classification-fixture-world", Dimension.OVERWORLD, new BlockPos(2, 64, 2), true),
                27, 8.0, "fixture_eligible_snapshot");
    }

    public static Map<Item, Integer> targetCounts(ItemTarget[] targets) {
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (ItemTarget target : targets) {
            if (target.getMatches().length != 1) throw new AssertionError("Expected exact-item policy target");
            counts.merge(target.getMatches()[0], target.getTargetCount(), Integer::sum);
        }
        return counts;
    }

    private static final class SnapshotMod extends AltoClef {
        private ClientPlayerEntity player;
        @Override public ClientPlayerEntity getPlayer() { return player; }
    }
}
