package adris.altoclef.mixins.diagnostics;

import baritone.api.pathing.goals.Goal;
import baritone.api.process.PathingCommand;
import baritone.api.utils.BetterBlockPos;
import baritone.behavior.PathingBehavior;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.path.PathExecutor;
import lavi.minecraft.diagnostics.mining.baritone.executor.BaritoneExecutorProgressDiagnostics;
import lavi.minecraft.diagnostics.mining.baritone.BaritonePathCalculationDiagnostics;
import lavi.minecraft.diagnostics.baritone.builder.BuilderExecutorObserver;
import lavi.minecraft.diagnostics.baritone.correlation.BuilderTraceRegistry;
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

    @Inject(method = "tickPath", at = @At("HEAD"), remap = false)
    private void lavi$logExecutorProgressTickPathHead(CallbackInfo ci) {
        BaritoneExecutorProgressDiagnostics.logTickPathHead(
                (PathingBehavior) (Object) this,
                current,
                next,
                inProgress,
                goal,
                expectedSegmentStart,
                cancelRequested,
                calcFailedLastTick
        );
    }

    @Inject(method = "tickPath", at = @At("RETURN"), remap = false)
    private void lavi$logExecutorProgressTickPathReturn(CallbackInfo ci) {
        BaritoneExecutorProgressDiagnostics.logTickPathReturn(
                (PathingBehavior) (Object) this,
                current,
                next,
                inProgress,
                goal,
                expectedSegmentStart,
                cancelRequested,
                calcFailedLastTick
        );
    }

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
        BuilderTraceRegistry.workerEnter((PathingBehavior) (Object) this, pathfinder);
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
        BuilderExecutorObserver.adopted((PathingBehavior) (Object) this, current, next, "BEFORE_LEGACY_CALCULATION_COMPLETE");
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

    //20260913_kpopmodder: Bind the exact finder before the existing Executor.execute can start its worker.
    @Inject(method = "findPathInNewThread", at = @At(value = "FIELD",
            target = "Lbaritone/behavior/PathingBehavior;inProgress:Lbaritone/pathing/calc/AbstractNodeCostSearch;",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER), remap = false)
    private void lavi$bindBeforeScheduling(CallbackInfo ci) {
        try { BuilderTraceRegistry.scheduled((PathingBehavior) (Object) this, inProgress, goal); }
        catch (RuntimeException | LinkageError ignored) { }
    }

    @Inject(method = "lambda$findPathInNewThread$2", at = @At("RETURN"), remap = false)
    private void lavi$workerReturned(CallbackInfo ci) { BuilderTraceRegistry.workerExit(); }

    @Inject(method = {"tickPath", "softCancelIfSafe", "secretInternalSegmentCancel", "lambda$findPathInNewThread$2"},
            at = @At(value = "FIELD", target = "Lbaritone/behavior/PathingBehavior;current:Lbaritone/pathing/path/PathExecutor;",
                    opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER), remap = false)
    private void lavi$currentAssigned(CallbackInfo ci) {
        BuilderExecutorObserver.adopted((PathingBehavior) (Object) this, current, next, "CURRENT_FIELD_WRITE_AFTER");
    }

    @Inject(method = {"tickPath", "softCancelIfSafe", "secretInternalSegmentCancel", "lambda$findPathInNewThread$2"},
            at = @At(value = "FIELD", target = "Lbaritone/behavior/PathingBehavior;next:Lbaritone/pathing/path/PathExecutor;",
                    opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER), remap = false)
    private void lavi$nextAssigned(CallbackInfo ci) {
        BuilderExecutorObserver.adopted((PathingBehavior) (Object) this, current, next, "NEXT_FIELD_WRITE_AFTER");
    }
}
