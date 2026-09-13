package adris.altoclef.mixins.diagnostics;

import baritone.api.pathing.calc.IPath;
import baritone.pathing.path.SplicedPath;
import lavi.minecraft.diagnostics.baritone.builder.BuilderPathLifecycleObserver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;

//20260913_kpopmodder: Preserve both splice inputs and distinguish an empty result from successful joining.
@Mixin(value = SplicedPath.class, remap = false)
public abstract class SplicedPathDiagnosticMixin {
    @Inject(method = "trySplice", at = @At("RETURN"), remap = false)
    private static void lavi$spliceReturned(IPath first, IPath second, boolean allowOverlap,
                                            CallbackInfoReturnable<Optional<SplicedPath>> cir) {
        Optional<SplicedPath> returned = cir.getReturnValue();
        BuilderPathLifecycleObserver.transformed(first, second, returned.orElse(null),
                returned.isPresent() ? "SPLICE_CREATED" : "SPLICE_NOT_CREATED", -1, -1);
    }
}
