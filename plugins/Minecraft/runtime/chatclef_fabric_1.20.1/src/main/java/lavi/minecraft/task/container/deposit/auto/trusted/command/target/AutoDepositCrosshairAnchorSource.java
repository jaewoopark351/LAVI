package lavi.minecraft.task.container.deposit.auto.trusted.command.target;

import adris.altoclef.AltoClef;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
//20260904_kpopmodder: Expose only a safe immutable crosshair block anchor to registration flows.
@FunctionalInterface
public interface AutoDepositCrosshairAnchorSource {
    Optional<BlockPos> read(AltoClef mod);
}
