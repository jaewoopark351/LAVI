package adris.altoclef.mixins.diagnostics;

import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import baritone.utils.PathingControlManager;
import lavi.minecraft.diagnostics.mining.baritone.BaritoneProcessControlDiagnostics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = PathingControlManager.class, remap = false)
public abstract class PathingControlManagerDiagnosticMixin {
    @Shadow
    @Final
    private List<IBaritoneProcess> active;

    @Shadow
    private IBaritoneProcess inControlLastTick;

    @Shadow
    private IBaritoneProcess inControlThisTick;

    @Shadow
    private PathingCommand command;

    @Inject(method = "preTick", at = @At("RETURN"), remap = false)
    private void lavi$logPreTickReturned(CallbackInfo ci) {
        BaritoneProcessControlDiagnostics.logPreTickReturned(
                this,
                inControlLastTick,
                inControlThisTick,
                command,
                active
        );
    }

    @Inject(method = "cancelEverything", at = @At("HEAD"), remap = false)
    private void lavi$logCancelEverythingHead(CallbackInfo ci) {
        BaritoneProcessControlDiagnostics.logCancelEverythingBoundary(
                this,
                "HEAD",
                inControlLastTick,
                inControlThisTick,
                command,
                active
        );
    }

    @Inject(method = "cancelEverything", at = @At("RETURN"), remap = false)
    private void lavi$logCancelEverythingReturn(CallbackInfo ci) {
        BaritoneProcessControlDiagnostics.logCancelEverythingBoundary(
                this,
                "RETURN",
                inControlLastTick,
                inControlThisTick,
                command,
                active
        );
    }
}
