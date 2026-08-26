package lavi.minecraft.task.container.deposit.auto.trusted;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.container.StoreInContainerTask;
import adris.altoclef.trackers.storage.ContainerCache;
import lavi.minecraft.task.container.deposit.DepositAllContainerEligibility;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public final class AutoDepositTrustedDestinationRuntimeEvaluator
        implements AutoDepositTrustedDestinationEvaluator {
    @Override
    public AutoDepositTrustedDestinationEvaluation evaluate(AltoClef mod,
                                                            AutoDepositTrustedDestination destination,
                                                            int requiredEmptySlots,
                                                            int maximumDistance) {
        if (mod == null || mod.getPlayer() == null || mod.getWorld() == null) {
            return result(false, 0, Double.POSITIVE_INFINITY, "runtime_unavailable");
        }
        BlockPos position = destination.position();
        double distanceSquared = mod.getPlayer().getPos().squaredDistanceTo(
                position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5
        );
        if (distanceSquared > (double) maximumDistance * maximumDistance) {
            return result(false, 0, distanceSquared, "outside_distance");
        }
        if (mod.getBlockScanner().isUnreachable(position)) {
            return result(false, 0, distanceSquared, "unreachable");
        }
        Block block = mod.getWorld().getBlockState(position).getBlock();
        boolean supported = false;
        for (Block supportedBlock : StoreInContainerTask.CONTAINER_BLOCKS) {
            if (supportedBlock.equals(block)) {
                supported = true;
                break;
            }
        }
        if (!supported) {
            return result(false, 0, distanceSquared, "container_missing");
        }
        DepositAllContainerEligibility eligibility = new DepositAllContainerEligibility();
        if (!eligibility.evaluate(mod, position, StoreInContainerTask.CONTAINER_BLOCKS).accepted()) {
            return result(false, 0, distanceSquared, "container_ineligible");
        }
        Optional<ContainerCache> cache = mod.getItemStorage().getContainerAtPosition(position);
        if (cache.isEmpty()) {
            return result(false, 0, distanceSquared, "capacity_unverified");
        }
        int emptySlots = cache.get().getEmptySlotCount();
        if (emptySlots < requiredEmptySlots) {
            return result(false, emptySlots, distanceSquared, "insufficient_capacity");
        }
        return result(true, emptySlots, distanceSquared, "eligible:" + emptySlots);
    }

    private static AutoDepositTrustedDestinationEvaluation result(boolean eligible,
                                                                  int emptySlots,
                                                                  double distanceSquared,
                                                                  String state) {
        return new AutoDepositTrustedDestinationEvaluation(
                eligible, emptySlots, distanceSquared, state
        );
    }
}
