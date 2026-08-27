package lavi.minecraft.task.container.deposit.auto.trusted.command;

import net.minecraft.util.math.BlockPos;

import java.util.Objects;

//20260827_kpopmodder: Preserve the exact target position and its evidence source.
public final class AutoDepositTrustedTarget {
    private final BlockPos position;
    private final AutoDepositTrustedTargetSource source;

    public AutoDepositTrustedTarget(BlockPos position, AutoDepositTrustedTargetSource source) {
        this.position = Objects.requireNonNull(position, "position").toImmutable();
        this.source = Objects.requireNonNull(source, "source");
    }

    public BlockPos position() {
        return position;
    }

    public AutoDepositTrustedTargetSource source() {
        return source;
    }
}
