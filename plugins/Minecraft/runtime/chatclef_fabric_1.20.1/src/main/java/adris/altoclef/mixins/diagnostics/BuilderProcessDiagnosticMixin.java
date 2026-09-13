package adris.altoclef.mixins.diagnostics;

import baritone.api.pathing.calc.IPath;
import baritone.pathing.path.PathExecutor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import lavi.minecraft.diagnostics.baritone.builder.BuilderMovementAccessObserver;
import lavi.minecraft.diagnostics.baritone.builder.BuilderProcessObserver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

//20260913_kpopmodder: Observe the original local exec and exact operands; never guard, replace, or catch List.get.
@Mixin(targets = "baritone.process.BuilderProcess", remap = false)
public abstract class BuilderProcessDiagnosticMixin {
    @Inject(method = "clearArea", at = @At("HEAD"), remap = false)
    private void lavi$clearRequested(CallbackInfo ci) {
        BuilderProcessObserver.boundary(this, "CLEAR_AREA_REQUESTED");
    }
    @Inject(method = "onLostControl", at = @At("HEAD"), remap = false)
    private void lavi$controlLost(CallbackInfo ci) {
        BuilderProcessObserver.boundary(this, "LOST_CONTROL_ENTER");
    }
    @WrapOperation(method = "updateMovement", at = @At(value = "INVOKE",
            target = "Lbaritone/pathing/path/PathExecutor;getPath()Lbaritone/api/pathing/calc/IPath;"), remap = false)
    private IPath lavi$captureActualPath(PathExecutor executor, Operation<IPath> original,
                                        @Share("builderActualPath") LocalRef<IPath> actualPath) {
        IPath result = original.call(executor);
        actualPath.set(result);
        return result;
    }

    @WrapOperation(method = "updateMovement", at = @At(value = "INVOKE",
            target = "Ljava/util/List;get(I)Ljava/lang/Object;"), remap = false)
    private Object lavi$observeMovementAccess(List<?> receiver, int index, Operation<Object> original,
                                              @Local(index = 1) PathExecutor executor,
                                              @Share("builderActualPath") LocalRef<IPath> actualPath) {
        BuilderMovementAccessObserver.beforeAccess(this, executor, actualPath.get(), receiver, index);
        return original.call(receiver, index);
    }
}
