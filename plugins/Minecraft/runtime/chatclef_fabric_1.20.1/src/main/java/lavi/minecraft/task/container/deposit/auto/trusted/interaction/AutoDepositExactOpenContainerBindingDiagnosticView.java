package lavi.minecraft.task.container.deposit.auto.trusted.interaction;

import net.minecraft.util.math.BlockPos;

import java.util.Optional;

//20260828_kpopmodder: Expose a non-mutating exact-binding view to diagnostics only.
public interface AutoDepositExactOpenContainerBindingDiagnosticView {
    Optional<BlockPos> peekCurrentExactPositionForDiagnostics();
}
