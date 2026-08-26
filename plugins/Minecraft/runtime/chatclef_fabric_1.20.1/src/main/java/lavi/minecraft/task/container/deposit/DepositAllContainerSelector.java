package lavi.minecraft.task.container.deposit;

import adris.altoclef.AltoClef;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.function.Predicate;

//20260826_kpopmodder: Isolated the single filtered nearest-container scan used by deposit_all.
public final class DepositAllContainerSelector {
    private final NearestSearch nearestSearch;

    public DepositAllContainerSelector() {
        this((mod, eligibility, targetBlocks) -> mod.getBlockScanner().getNearestBlock(
                mod.getPlayer().getPos(),
                eligibility,
                targetBlocks
        ));
    }

    DepositAllContainerSelector(NearestSearch nearestSearch) {
        this.nearestSearch = nearestSearch;
    }

    public Optional<BlockPos> select(AltoClef mod,
                                     Predicate<BlockPos> eligibility,
                                     Block... targetBlocks) {
        return nearestSearch.find(mod, eligibility, targetBlocks).map(BlockPos::toImmutable);
    }

    @FunctionalInterface
    interface NearestSearch {
        Optional<BlockPos> find(AltoClef mod, Predicate<BlockPos> eligibility, Block[] targetBlocks);
    }
}
