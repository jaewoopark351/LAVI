package lavi.minecraft.task.container.deposit.auto.trusted.command;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.container.deposit.auto.trusted.command.target.AutoDepositCrosshairAnchorReader;
import lavi.minecraft.task.container.deposit.auto.trusted.command.target.AutoDepositSingleCrosshairTargetReader;
import lavi.minecraft.task.container.deposit.auto.trusted.command.target.AutoDepositSingleCrosshairTargetSource;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;
import java.util.Optional;

//20260827_kpopmodder: Resolve only exact-open or current crosshair trusted container targets.
public final class AutoDepositTrustedTargetResolver {
    private final AutoDepositExactOpenContainerBinding openBinding;
    private final AutoDepositSingleCrosshairTargetSource crosshairTargetSource;

    public AutoDepositTrustedTargetResolver(
            AutoDepositExactOpenContainerBinding openBinding) {
        this(
                openBinding,
                new AutoDepositSingleCrosshairTargetReader(
                        new AutoDepositCrosshairAnchorReader()
                )
        );
    }

    public AutoDepositTrustedTargetResolver(
            AutoDepositExactOpenContainerBinding openBinding,
            AutoDepositSingleCrosshairTargetSource crosshairTargetSource) {
        this.openBinding = Objects.requireNonNull(openBinding, "openBinding");
        this.crosshairTargetSource = Objects.requireNonNull(
                crosshairTargetSource,
                "crosshairTargetSource"
        );
    }

    public Optional<AutoDepositTrustedTarget> resolve(AltoClef mod) {
        Optional<BlockPos> openPosition = openBinding.currentExactPosition();
        if (openPosition.isPresent()) {
            return Optional.of(new AutoDepositTrustedTarget(
                    openPosition.get(), AutoDepositTrustedTargetSource.EXACT_OPEN_CONTAINER
            ));
        }
        return crosshairTargetSource.read(mod).map(position -> new AutoDepositTrustedTarget(
                position, AutoDepositTrustedTargetSource.CROSSHAIR
        ));
    }
}
