package lavi.minecraft.task.container.deposit.auto.trusted.command.target;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedContainerSupport;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;
import java.util.Optional;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
//20260904_kpopmodder: Apply the legacy trusted-container allowlist to a safe crosshair anchor.
public final class AutoDepositSingleCrosshairTargetReader
        implements AutoDepositSingleCrosshairTargetSource {
    private final AutoDepositCrosshairAnchorSource anchorSource;

    public AutoDepositSingleCrosshairTargetReader(AutoDepositCrosshairAnchorSource anchorSource) {
        this.anchorSource = Objects.requireNonNull(anchorSource, "anchorSource");
    }

    @Override
    public Optional<BlockPos> read(AltoClef mod) {
        Optional<BlockPos> anchor = anchorSource.read(mod);
        if (anchor.isEmpty() || mod == null || mod.getWorld() == null) {
            return Optional.empty();
        }
        BlockPos position = anchor.get();
        if (!AutoDepositTrustedContainerSupport.isSupported(
                mod.getWorld().getBlockState(position).getBlock())) {
            return Optional.empty();
        }
        return Optional.of(position.toImmutable());
    }
}
