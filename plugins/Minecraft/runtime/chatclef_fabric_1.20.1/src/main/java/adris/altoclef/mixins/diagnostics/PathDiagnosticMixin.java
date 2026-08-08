package adris.altoclef.mixins.diagnostics;

import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.utils.BetterBlockPos;
import baritone.pathing.calc.PathNode;
import baritone.pathing.movement.CalculationContext;
import lavi.minecraft.diagnostics.mining.baritone.BaritonePathCalculationDiagnostics;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "baritone.pathing.calc.Path", remap = false)
public abstract class PathDiagnosticMixin {
    @Inject(method = "<init>",
            at = @At(value = "FIELD",
                    target = "Lbaritone/pathing/calc/Path;end:Lbaritone/api/utils/BetterBlockPos;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.BEFORE,
                    remap = false),
            remap = false)
    private void lavi$logPathBuildEnter(BetterBlockPos realStart,
                                        PathNode startNode,
                                        PathNode endNode,
                                        int numNodes,
                                        Goal goal,
                                        CalculationContext context,
                                        CallbackInfo ci) {
        BaritonePathCalculationDiagnostics.logPathBuildEnter(
                this,
                realStart,
                startNode,
                endNode,
                numNodes,
                goal,
                context
        );
    }

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void lavi$logPathBuildReturn(BetterBlockPos realStart,
                                         PathNode startNode,
                                         PathNode endNode,
                                         int numNodes,
                                         Goal goal,
                                         CalculationContext context,
                                         CallbackInfo ci) {
        BaritonePathCalculationDiagnostics.logPathBuildReturn(
                this,
                realStart,
                startNode,
                endNode,
                numNodes,
                goal,
                context
        );
    }

    @Inject(method = "postProcess", at = @At("HEAD"), remap = false)
    private void lavi$logPathPostProcessEnter(CallbackInfoReturnable<IPath> cir) {
        BaritonePathCalculationDiagnostics.logPathPostProcessEnter((IPath) (Object) this);
    }

    @Inject(method = "postProcess", at = @At("RETURN"), remap = false)
    private void lavi$logPathPostProcessReturn(CallbackInfoReturnable<IPath> cir) {
        BaritonePathCalculationDiagnostics.logPathPostProcessReturn((IPath) (Object) this, cir.getReturnValue());
    }
}
