package adris.altoclef.mixins.diagnostics;

import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.utils.BetterBlockPos;
import baritone.pathing.calc.PathNode;
import baritone.pathing.movement.CalculationContext;
import lavi.minecraft.diagnostics.mining.baritone.BaritonePathCalculationDiagnostics;
import lavi.minecraft.diagnostics.baritone.builder.BuilderPathLifecycleObserver;
import lavi.minecraft.diagnostics.baritone.builder.BuilderPathView;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "baritone.pathing.calc.Path", remap = false)
public abstract class PathDiagnosticMixin implements BuilderPathView {
    //20260913_kpopmodder: Read raw fields rather than the getter which rejects pre-verification paths.
    @Shadow private java.util.List<?> path;
    @Shadow private java.util.List<?> movements;
    @Shadow private volatile boolean verified;
    @Override public int lavi$positionsCount() { return path == null ? -1 : path.size(); }
    @Override public int lavi$movementsCount() { return movements == null ? -1 : movements.size(); }
    @Override public boolean lavi$verified() { return verified; }
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
        BuilderPathLifecycleObserver.phase((IPath) (Object) this, null, "RAW_CREATED");
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
        BuilderPathLifecycleObserver.phase((IPath) (Object) this, null, "POST_PROCESS_ENTER");
        BaritonePathCalculationDiagnostics.logPathPostProcessEnter((IPath) (Object) this);
    }

    @Inject(method = "postProcess", at = @At("RETURN"), remap = false)
    private void lavi$logPathPostProcessReturn(CallbackInfoReturnable<IPath> cir) {
        BuilderPathLifecycleObserver.phase((IPath) (Object) this, cir.getReturnValue(), "POST_PROCESS_RETURN");
        BaritonePathCalculationDiagnostics.logPathPostProcessReturn((IPath) (Object) this, cir.getReturnValue());
    }

    @Inject(method = "assembleMovements", at = @At("RETURN"), remap = false)
    private void lavi$assemblyReturned(CallbackInfoReturnable<Boolean> cir) {
        BuilderPathLifecycleObserver.phase((IPath) (Object) this, null,
                cir.getReturnValueZ() ? "ASSEMBLY_RETURN_PARTIAL" : "ASSEMBLY_RETURN_COMPLETE");
    }
}
