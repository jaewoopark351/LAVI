package lavi.minecraft.task.container.deposit.auto.trusted;

import net.minecraft.util.math.BlockPos;

public final class AutoDepositTrustedDestinationSelection {
    private final BlockPos position;
    private final int cachedEmptySlots;

    AutoDepositTrustedDestinationSelection(BlockPos position, int cachedEmptySlots) {
        this.position = position.toImmutable();
        this.cachedEmptySlots = cachedEmptySlots;
    }

    public BlockPos position() {
        return position;
    }

    public int cachedEmptySlots() {
        return cachedEmptySlots;
    }
}
