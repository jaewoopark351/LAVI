package adris.altoclef.mixins.diagnostics;

import baritone.api.pathing.goals.Goal;
import baritone.api.process.PathingCommand;
import baritone.api.utils.BetterBlockPos;
import baritone.behavior.PathingBehavior;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.path.PathExecutor;
import lavi.minecraft.diagnostics.mining.baritone.BaritonePathCalculationDiagnostics;
import net.minecraft.util.math.BlockPos;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PathingBehavior.class, remap = false)
public abstract class PathingBehaviorDiagnosticMixin {
    @Shadow
    private PathExecutor current;

    @Shadow
    private PathExecutor next;

    @Shadow
    private Goal goal;

    @Shadow
    private boolean cancelRequested;

    @Shadow
    private boolean calcFailedLastTick;

    @Shadow
    private AbstractNodeCostSearch inProgress;

    @Shadow
    private BetterBlockPos expectedSegmentStart;

    @Inject(method = "secretInternalSetGoalAndPath", at = @At("RETURN"), remap = false)
    private void lavi$logGoalRequestDecision(PathingCommand command, CallbackInfoReturnable<Boolean> cir) {
        BaritonePathCalculationDiagnostics.logGoalRequestDecision(
                (PathingBehavior) (Object) this,
                command,
                cir.getReturnValueZ(),
                current,
                next,
                inProgress,
                goal,
                expectedSegmentStart,
                cancelRequested,
                calcFailedLastTick
        );
    }

    @Inject(method = "findPathInNewThread", at = @At("RETURN"), remap = false)
    private void lavi$logCalculationScheduled(BlockPos start, boolean firstSegment, CalculationContext context, CallbackInfo ci) {
        BaritonePathCalculationDiagnostics.logCalculationScheduled(
                (PathingBehavior) (Object) this,
                start,
                firstSegment,
                context,
                goal,
                current,
                next,
                inProgress,
                expectedSegmentStart
        );
    }

    @Inject(method = "lambda$findPathInNewThread$2", at = @At("HEAD"), remap = false)
    private void lavi$logWorkerStarted(boolean firstSegment,
                                       BlockPos pathStart,
                                       Goal requestedGoal,
                                       AbstractNodeCostSearch pathfinder,
                                       long primaryTimeout,
                                       long failureTimeout,
                                       CallbackInfo ci) {
        BaritonePathCalculationDiagnostics.logWorkerStarted(
                (PathingBehavior) (Object) this,
                firstSegment,
                pathStart,
                requestedGoal,
                pathfinder,
                primaryTimeout,
                failureTimeout,
                current,
                next,
                inProgress,
                expectedSegmentStart
        );
    }

    @Inject(method = "lambda$findPathInNewThread$2",
            at = @At(value = "FIELD",
                    target = "Lbaritone/behavior/PathingBehavior;inProgress:Lbaritone/pathing/calc/AbstractNodeCostSearch;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.BEFORE,
                    remap = false),
            remap = false)
    private void lavi$logAdoptionDecisionBeforeClear(boolean firstSegment,
                                                     BlockPos pathStart,
                                                     Goal requestedGoal,
                                                     AbstractNodeCostSearch pathfinder,
                                                     long primaryTimeout,
                                                     long failureTimeout,
                                                     CallbackInfo ci) {
        BaritonePathCalculationDiagnostics.logAdoptionDecisionBeforeClear(
                (PathingBehavior) (Object) this,
                firstSegment,
                pathStart,
                requestedGoal,
                pathfinder,
                current,
                next,
                inProgress,
                expectedSegmentStart
        );
    }

    @Inject(method = "forceCancel", at = @At("HEAD"), remap = false)
    private void lavi$logForceCancelHead(CallbackInfo ci) {
        BaritonePathCalculationDiagnostics.logForceCancelBoundary(
                (PathingBehavior) (Object) this,
                "HEAD",
                current,
                next,
                inProgress,
                goal,
                cancelRequested,
                calcFailedLastTick
        );
    }

    @Inject(method = "forceCancel", at = @At("RETURN"), remap = false)
    private void lavi$logForceCancelReturn(CallbackInfo ci) {
        BaritonePathCalculationDiagnostics.logForceCancelBoundary(
                (PathingBehavior) (Object) this,
                "RETURN",
                current,
                next,
                inProgress,
                goal,
                cancelRequested,
                calcFailedLastTick
        );
    }
}
