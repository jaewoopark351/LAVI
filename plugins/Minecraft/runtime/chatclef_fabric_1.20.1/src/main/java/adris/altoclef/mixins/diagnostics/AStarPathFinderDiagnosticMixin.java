package adris.altoclef.mixins.diagnostics;

import baritone.api.pathing.calc.IPath;
import baritone.pathing.calc.AStarPathFinder;
import baritone.pathing.calc.AbstractNodeCostSearch;
import lavi.minecraft.diagnostics.mining.baritone.BaritonePathCalculationDiagnostics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = AStarPathFinder.class, remap = false)
public abstract class AStarPathFinderDiagnosticMixin {
    @Inject(method = "calculate0", at = @At("HEAD"), remap = false)
    private void lavi$logCalculate0Enter(long primaryTimeout,
                                         long failureTimeout,
                                         CallbackInfoReturnable<Optional<IPath>> cir) {
        BaritonePathCalculationDiagnostics.logCalculate0Enter(
                (AbstractNodeCostSearch) (Object) this,
                primaryTimeout,
                failureTimeout
        );
    }

    @Inject(method = "calculate0", at = @At("RETURN"), remap = false)
    private void lavi$logCalculate0Return(long primaryTimeout,
                                          long failureTimeout,
                                          CallbackInfoReturnable<Optional<IPath>> cir) {
        BaritonePathCalculationDiagnostics.logCalculate0Return(
                (AbstractNodeCostSearch) (Object) this,
                cir.getReturnValue()
        );
    }
}
