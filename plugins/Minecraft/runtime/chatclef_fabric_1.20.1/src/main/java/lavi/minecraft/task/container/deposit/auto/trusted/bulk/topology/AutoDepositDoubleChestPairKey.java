package lavi.minecraft.task.container.deposit.auto.trusted.bulk.topology;

import net.minecraft.util.math.BlockPos;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public record AutoDepositDoubleChestPairKey(BlockPos first, BlockPos second) {
    private static final Comparator<BlockPos> POSITION_ORDER = Comparator
            .comparingInt(BlockPos::getX)
            .thenComparingInt(BlockPos::getY)
            .thenComparingInt(BlockPos::getZ);

    public AutoDepositDoubleChestPairKey {
        BlockPos immutableFirst = Objects.requireNonNull(first, "first").toImmutable();
        BlockPos immutableSecond = Objects.requireNonNull(second, "second").toImmutable();
        if (!horizontallyAdjacent(immutableFirst, immutableSecond)) {
            throw new IllegalArgumentException(
                    "double-chest positions must be distinct horizontal neighbors"
            );
        }
        if (POSITION_ORDER.compare(immutableFirst, immutableSecond) <= 0) {
            first = immutableFirst;
            second = immutableSecond;
        } else {
            first = immutableSecond;
            second = immutableFirst;
        }
    }

    public static AutoDepositDoubleChestPairKey of(BlockPos first, BlockPos second) {
        return new AutoDepositDoubleChestPairKey(first, second);
    }

    public List<BlockPos> positions() {
        return List.of(first, second);
    }

    private static boolean horizontallyAdjacent(BlockPos first, BlockPos second) {
        return first.getY() == second.getY()
                && Math.abs(first.getX() - second.getX())
                + Math.abs(first.getZ() - second.getZ()) == 1;
    }
}
