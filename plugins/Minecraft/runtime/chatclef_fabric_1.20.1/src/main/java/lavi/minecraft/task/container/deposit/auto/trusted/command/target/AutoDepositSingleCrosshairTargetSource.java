package lavi.minecraft.task.container.deposit.auto.trusted.command.target;

import adris.altoclef.AltoClef;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
//20260904_kpopmodder: Expose the existing broad single-registration crosshair target policy.
@FunctionalInterface
public interface AutoDepositSingleCrosshairTargetSource {
    Optional<BlockPos> read(AltoClef mod);
}
