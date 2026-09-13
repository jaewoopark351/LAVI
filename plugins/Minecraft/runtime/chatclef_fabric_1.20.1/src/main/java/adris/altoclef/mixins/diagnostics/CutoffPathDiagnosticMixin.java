package adris.altoclef.mixins.diagnostics;

import baritone.api.pathing.calc.IPath;
import lavi.minecraft.diagnostics.baritone.builder.BuilderPathLifecycleObserver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//20260913_kpopmodder: The canonical three-argument constructor observes both calculation and execution cutoffs.
@Mixin(targets = "baritone.pathing.path.CutoffPath", remap = false)
public abstract class CutoffPathDiagnosticMixin {
    @Inject(method = "<init>(Lbaritone/api/pathing/calc/IPath;II)V", at = @At("RETURN"), remap = false)
    private void lavi$cutoffCreated(IPath original, int first, int last, CallbackInfo ci) {
        BuilderPathLifecycleObserver.transformed(original, null, (IPath) (Object) this, "CUTOFF_CREATED", first, last);
    }
}
