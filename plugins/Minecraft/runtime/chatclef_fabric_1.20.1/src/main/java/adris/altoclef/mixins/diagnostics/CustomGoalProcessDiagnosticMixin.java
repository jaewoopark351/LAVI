package adris.altoclef.mixins.diagnostics;

import baritone.api.pathing.goals.Goal;
import baritone.process.CustomGoalProcess;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.baritone.BaritoneProcessControlDiagnostics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CustomGoalProcess.class, remap = false)
public abstract class CustomGoalProcessDiagnosticMixin {
    @Shadow
    private Goal goal;

    @Shadow
    private Goal mostRecentGoal;

    @Inject(method = "onLostControl", at = @At("HEAD"), remap = false)
    private void lavi$logLostControlHead(CallbackInfo ci) {
        CustomGoalProcess process = (CustomGoalProcess) (Object) this;
        BaritoneProcessControlDiagnostics.logCustomGoalLostControl(
                process,
                "HEAD",
                goal,
                mostRecentGoal,
                "unavailable_private_state",
                ChatClefDiagnostics.safeValueForDiagnosticLog(process::isActive)
        );
    }

    @Inject(method = "onLostControl", at = @At("RETURN"), remap = false)
    private void lavi$logLostControlReturn(CallbackInfo ci) {
        CustomGoalProcess process = (CustomGoalProcess) (Object) this;
        BaritoneProcessControlDiagnostics.logCustomGoalLostControl(
                process,
                "RETURN",
                goal,
                mostRecentGoal,
                "unavailable_private_state",
                ChatClefDiagnostics.safeValueForDiagnosticLog(process::isActive)
        );
    }
}
