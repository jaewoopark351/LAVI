package adris.altoclef.mixins.diagnostics;

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.calc.IPath;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.PathCalculationResult;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.calc.PathNode;
import lavi.minecraft.diagnostics.mining.baritone.BaritonePathCalculationDiagnostics;
import lavi.minecraft.diagnostics.baritone.builder.BuilderPathLifecycleObserver;
import lavi.minecraft.diagnostics.mining.baritone.BaritonePathfinderSearchSnapshot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractNodeCostSearch.class, remap = false)
public abstract class AbstractNodeCostSearchDiagnosticMixin {
    @Shadow
    @Final
    protected Goal goal;

    @Shadow
    @Final
    protected BetterBlockPos realStart;

    @Shadow
    protected boolean cancelRequested;

    @Shadow
    protected PathNode startNode;

    @Shadow
    protected PathNode mostRecentConsidered;

    @Shadow
    @Final
    protected PathNode[] bestSoFar;

    @Shadow
    protected abstract int mapSize();

    @Unique
    private long lavi$calculateStartNanos;

    @Inject(method = "calculate", at = @At("HEAD"), remap = false)
    private void lavi$logCalculateStarted(long primaryTimeout, long failureTimeout, CallbackInfoReturnable<PathCalculationResult> cir) {
        lavi$calculateStartNanos = System.nanoTime();
        BaritonePathCalculationDiagnostics.logPathfinderCalculateStarted(
                (AbstractNodeCostSearch) (Object) this,
                primaryTimeout,
                failureTimeout,
                goal,
                realStart,
                cancelRequested
        );
    }

    @Inject(method = "calculate", at = @At("RETURN"), remap = false)
    private void lavi$logCalculateCompleted(long primaryTimeout,
                                            long failureTimeout,
                                            CallbackInfoReturnable<PathCalculationResult> cir) {
        BuilderPathLifecycleObserver.calculationResult(cir.getReturnValue());
        long elapsedNanos = lavi$calculateStartNanos <= 0 ? -1 : System.nanoTime() - lavi$calculateStartNanos;
        //20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
        BaritonePathCalculationDiagnostics.logPathfinderCalculateCompleted(
                (AbstractNodeCostSearch) (Object) this,
                cir.getReturnValue(),
                elapsedNanos,
                cancelRequested,
                () -> BaritonePathfinderSearchSnapshot.fields(
                        lavi$safeMapSize(), startNode, mostRecentConsidered, bestSoFar)
        );
    }

    @Unique
    private int lavi$safeMapSize() {
        try {
            return mapSize();
        } catch (RuntimeException | LinkageError error) {
            return -1;
        }
    }

    //20260913_kpopmodder: Observe real cutoff results, including the original object being returned unchanged.
    @WrapOperation(method = "calculate", at = @At(value = "INVOKE",
            target = "Lbaritone/api/pathing/calc/IPath;cutoffAtLoadedChunks(Ljava/lang/Object;)Lbaritone/api/pathing/calc/IPath;"), remap = false)
    private IPath lavi$loadedCutoff(IPath input, Object world, Operation<IPath> original) {
        IPath result = original.call(input, world);
        BuilderPathLifecycleObserver.transformed(input, null, result,
                input == result ? "LOADED_CUTOFF_UNCHANGED" : "LOADED_CUTOFF_REPLACED", -1, -1);
        return result;
    }

    @WrapOperation(method = "calculate", at = @At(value = "INVOKE",
            target = "Lbaritone/api/pathing/calc/IPath;staticCutoff(Lbaritone/api/pathing/goals/Goal;)Lbaritone/api/pathing/calc/IPath;"), remap = false)
    private IPath lavi$staticCutoff(IPath input, Goal requestedGoal, Operation<IPath> original) {
        IPath result = original.call(input, requestedGoal);
        BuilderPathLifecycleObserver.transformed(input, null, result,
                input == result ? "STATIC_CUTOFF_UNCHANGED" : "STATIC_CUTOFF_REPLACED", -1, -1);
        return result;
    }
}
