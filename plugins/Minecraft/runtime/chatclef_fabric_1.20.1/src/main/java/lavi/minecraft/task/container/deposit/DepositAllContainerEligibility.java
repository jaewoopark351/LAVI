package lavi.minecraft.task.container.deposit;

import adris.altoclef.AltoClef;
import adris.altoclef.trackers.storage.ContainerCache;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

//20260826_kpopmodder: Isolated deposit_all container eligibility from branch orchestration.
public final class DepositAllContainerEligibility {
    private final Set<BlockPos> dungeonChests = new HashSet<>();
    private final Set<BlockPos> nonDungeonChests = new HashSet<>();

    public Evaluation evaluate(AltoClef mod, BlockPos containerPos, Block... supportedContainers) {
        BlockPos immutablePosition = containerPos.toImmutable();
        Block containerBlock = mod.getWorld().getBlockState(containerPos).getBlock();
        if (Arrays.stream(supportedContainers).noneMatch(containerBlock::equals)) {
            return new Evaluation(immutablePosition, Outcome.UNSUPPORTED_CONTAINER);
        }

        boolean isChest = WorldHelper.isChest(containerPos);
        if (isChest && WorldHelper.isSolidBlock(containerPos.up()) && !WorldHelper.canBreak(containerPos.up())) {
            return new Evaluation(immutablePosition, Outcome.CHEST_ABOVE_BLOCKED_UNBREAKABLE);
        }

        Optional<ContainerCache> data = mod.getItemStorage().getContainerAtPosition(containerPos);
        if (data.isPresent() && data.get().isFull()) {
            return new Evaluation(immutablePosition, Outcome.CONTAINER_CACHE_FULL);
        }

        if (isChest && mod.getModSettings().shouldAvoidSearchingForDungeonChests()) {
            boolean cachedDungeon = dungeonChests.contains(containerPos) && !nonDungeonChests.contains(containerPos);
            if (cachedDungeon) {
                return new Evaluation(immutablePosition, Outcome.CACHED_DUNGEON_CHEST);
            }

            int range = 6;
            for (int dx = -range; dx <= range; ++dx) {
                for (int dz = -range; dz <= range; ++dz) {
                    BlockPos offset = containerPos.add(dx, 0, dz);
                    if (mod.getWorld().getBlockState(offset).getBlock() == Blocks.SPAWNER) {
                        dungeonChests.add(immutablePosition);
                        return new Evaluation(immutablePosition, Outcome.SPAWNER_NEAR_CHEST);
                    }
                }
            }
            nonDungeonChests.add(immutablePosition);
        }

        return new Evaluation(immutablePosition, Outcome.ACCEPTED);
    }

    public void reset() {
        dungeonChests.clear();
        nonDungeonChests.clear();
    }

    public int dungeonChestCount() {
        return dungeonChests.size();
    }

    public int nonDungeonChestCount() {
        return nonDungeonChests.size();
    }

    public enum Outcome {
        ACCEPTED(true),
        UNSUPPORTED_CONTAINER(false),
        CHEST_ABOVE_BLOCKED_UNBREAKABLE(false),
        CONTAINER_CACHE_FULL(false),
        CACHED_DUNGEON_CHEST(false),
        SPAWNER_NEAR_CHEST(false);

        private final boolean accepted;

        Outcome(boolean accepted) {
            this.accepted = accepted;
        }

        public boolean accepted() {
            return accepted;
        }
    }

    public record Evaluation(BlockPos position, Outcome outcome) {
        public boolean accepted() {
            return outcome.accepted();
        }
    }
}
