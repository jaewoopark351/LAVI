package adris.altoclef.mixins.diagnostics;

import baritone.api.pathing.calc.IPath;
import baritone.behavior.PathingBehavior;
import baritone.pathing.path.PathExecutor;
import lavi.minecraft.diagnostics.baritone.builder.BuilderExecutorObserver;
import lavi.minecraft.diagnostics.baritone.builder.BuilderExecutorView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//20260913_kpopmodder: Keep constructor/default and post-transfer indices distinguishable.
@Mixin(value = PathExecutor.class, remap = false)
public abstract class PathExecutorDiagnosticMixin implements BuilderExecutorView {
    @Shadow @Final private PathingBehavior behavior;
    @Shadow private int pathPosition;
    @Override public PathingBehavior lavi$pathingBehavior() { return behavior; }
    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void lavi$constructed(PathingBehavior owner, IPath path, CallbackInfo ci) {
        BuilderExecutorObserver.created(owner, (PathExecutor) (Object) this, path, pathPosition);
    }
    @Inject(method = "trySplice", at = @At("RETURN"), remap = false)
    private void lavi$spliceReturned(PathExecutor next, CallbackInfoReturnable<PathExecutor> cir) {
        BuilderExecutorObserver.transformed(behavior, (PathExecutor) (Object) this, next, cir.getReturnValue(), "TRY_SPLICE");
    }
    @Inject(method = "cutIfTooLong", at = @At("RETURN"), remap = false)
    private void lavi$cutReturned(CallbackInfoReturnable<PathExecutor> cir) {
        BuilderExecutorObserver.transformed(behavior, (PathExecutor) (Object) this, null, cir.getReturnValue(), "CUT_IF_TOO_LONG");
    }
}
