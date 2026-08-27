package lavi.minecraft.task.container.deposit.auto.trusted;

import adris.altoclef.AltoClef;
import adris.altoclef.trackers.storage.ContainerCache;
import lavi.minecraft.task.container.deposit.DepositAllContainerEligibility;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

//20260827_kpopmodder: Keep cache capacity as a hint until a trusted candidate is opened.
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
        boolean unreachable = mod.getBlockScanner().isUnreachable(position);
        boolean loaded = mod.getChunkTracker().isChunkLoaded(position);
        if (loaded) {
            Block block = mod.getWorld().getBlockState(position).getBlock();
            if (!AutoDepositTrustedContainerSupport.isSupported(block)) {
                return result(false, 0, distanceSquared, "container_missing");
            }
            DepositAllContainerEligibility.Evaluation eligibility =
                    new DepositAllContainerEligibility().evaluate(
                            mod,
                            position,
                            adris.altoclef.tasks.container.StoreInContainerTask.CONTAINER_BLOCKS
                    );
            if (!eligibility.accepted()
                    && eligibility.outcome()
                    != DepositAllContainerEligibility.Outcome.CONTAINER_CACHE_FULL) {
                return result(false, 0, distanceSquared,
                        "container_ineligible:" + eligibility.outcome());
            }
        }
        Optional<ContainerCache> cache = mod.getItemStorage().getContainerAtPosition(position);
        if (cache.isEmpty()) {
            return result(true, 0, distanceSquared,
                    unreachable ? "unreachable_hint:uncached" : "capacity_unverified");
        }
        int emptySlots = cache.get().getEmptySlotCount();
        String capacity = emptySlots < requiredEmptySlots
                ? "capacity_hint_insufficient:" + emptySlots
                : "capacity_hint_sufficient:" + emptySlots;
        return result(true, emptySlots, distanceSquared,
                unreachable ? "unreachable_hint:" + capacity : capacity);
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
