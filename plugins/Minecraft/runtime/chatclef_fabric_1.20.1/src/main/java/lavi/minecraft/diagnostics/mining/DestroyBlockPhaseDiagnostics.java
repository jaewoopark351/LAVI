package lavi.minecraft.diagnostics.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import baritone.api.utils.input.Input;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.util.math.BlockPos;

//20260807_kpopmodder: Emit DestroyBlockTask phase transitions separately from lifetime events.
final class DestroyBlockPhaseDiagnostics {
    private DestroyBlockPhaseDiagnostics() {
    }

    static void log(AltoClef mod,
                    DestroyBlockTask task,
                    BlockPos target,
                    String currentPhase,
                    boolean reachPresent,
                    boolean isCloseToMoveBack) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        DestroyBlockDiagnosticState.State state = DestroyBlockDiagnosticState.getOrCreate(task);
        if (reachPresent) {
            state.reachEverPresent = true;
        }
        long tick = ChatClefDiagnostics.currentClientTickId();
        String normalizedPhase = normalize(currentPhase);
        String previousPhase = state.currentPhase == null ? "none" : state.currentPhase;
        if (normalizedPhase.equals(previousPhase)) {
            return;
        }
        long previousPhaseAgeTicks = state.phaseEnteredTick < 0 ? 0 : Math.max(0, tick - state.phaseEnteredTick);
        state.currentPhase = normalizedPhase;
        state.phaseEnteredTick = tick;
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "DESTROY_BLOCK_PHASE_TRANSITION",
                ChatClefDiagnostics.blockPos(target),
                previousPhase,
                normalizedPhase,
                Boolean.toString(reachPresent),
                Boolean.toString(isCloseToMoveBack)
        );
        MiningDiagnosticEmitter.emitLazy("DESTROY_BLOCK_PHASE_TRANSITION", "destroy_block_phase_transition", task,
                "destroy_phase|" + System.identityHashCode(task),
                fingerprint,
                () -> {
                    DestroyBlockDiagnosticState.updateObservedProgress(mod, target, state);
                    SubmittedGoalDiagnosticState.SubmittedGoal submittedGoal =
                            SubmittedGoalDiagnosticState.get(task);
                    BaritonePathDiagnosticSnapshot snapshot = BaritonePathDiagnosticSnapshot.capture(
                            mod,
                            target,
                            submittedGoal == null ? null : submittedGoal.goal,
                            SubmittedGoalDiagnosticState.matchesTarget(submittedGoal, target)
                    );
                    if (DestroyBlockDiagnosticState.isTrue(snapshot.pathPresent)) {
                        state.pathSuccessObserved = true;
                    }
                    if (DestroyBlockDiagnosticState.isTrue(snapshot.baritonePathing)) {
                        state.pathingStartedObserved = true;
                    }
                    return MiningDiagnosticEmitter.merge(new Object[]{
                        "owner", "destroy_block_phase_observer",
                        "trigger", "phase_changed",
                        "destroyTaskRunId", state.runId,
                        "destroyTaskInstanceId", MiningDiagnosticEmitter.instanceId(task),
                        "previousPhase", previousPhase,
                        "currentPhase", normalizedPhase,
                        "previousPhaseAgeTicks", previousPhaseAgeTicks,
                        "phaseAgeTicks", 0,
                        "targetPosition", ChatClefDiagnostics.blockPos(target),
                        "targetBlockState", DestroyBlockDiagnosticFields.targetBlockState(mod, target),
                        "blockStillExists", DestroyBlockDiagnosticFields.blockStillExists(mod, target),
                        "playerPosition", mod == null ? "unavailable" : ChatClefDiagnostics.playerPosition(mod),
                        "distanceSqToTarget", DestroyBlockDiagnosticFields.distanceSq(mod, target),
                        "horizontalDistanceSq", DestroyBlockDiagnosticFields.horizontalDistanceSq(mod, target),
                        "verticalDelta", DestroyBlockDiagnosticFields.verticalDelta(mod, target),
                        "reachPresent", reachPresent,
                        "isCloseToMoveBack", isCloseToMoveBack,
                        "playerOnGround", ChatClefDiagnostics.safeValue(() -> mod != null && mod.getPlayer().isOnGround()),
                        "playerTouchingWater", ChatClefDiagnostics.safeValue(() -> mod != null && mod.getPlayer().isTouchingWater()),
                        "foodChainNeedsToEat", ChatClefDiagnostics.safeValue(() -> mod != null && mod.getFoodChain().needsToEat()),
                        "safeToCancel", snapshot.safeToCancel,
                        "controllerBreakingBlock", DestroyBlockDiagnosticFields.controllerBreakingBlock(mod),
                        "breakingBlockPosition", DestroyBlockDiagnosticFields.breakingBlockPosition(mod),
                        "breakingProgress", DestroyBlockDiagnosticFields.breakingProgress(mod),
                        "leftClickForced", ChatClefDiagnostics.safeValue(() -> mod != null && mod.getClientBaritone().getInputOverrideHandler().isInputForcedDown(Input.CLICK_LEFT)),
                        "leftClickHeld", ChatClefDiagnostics.inputHeldState(Input.CLICK_LEFT),
                        "mainHandStack", ChatClefDiagnostics.safeValue(() -> mod == null || mod.getPlayer() == null ? null : mod.getPlayer().getMainHandStack()),
                        "bestToolStack", DestroyBlockDiagnosticFields.bestToolStack(mod, target),
                        "bestToolSuitable", DestroyBlockDiagnosticFields.bestToolSuitable(mod, target),
                        "savePolicyDecision", "not_evaluated_by_destroy_phase_observer",
                        "pathSuccessObserved", state.pathSuccessObserved,
                        "pathingStartedObserved", state.pathingStartedObserved,
                        "reachEverPresent", state.reachEverPresent,
                        "breakEverStarted", state.breakEverStarted,
                        "maximumBreakingProgress", state.maximumBreakingProgress,
                        "blockBecameAir", state.blockBecameAir
                    }, SubmittedGoalDiagnosticState.fields(submittedGoal, target), snapshot.fields(), new Object[]{
                            "progressObservationCoverage", "PHASE_TRANSITIONS_ONLY"
                    });
                });
    }

    private static String normalize(String value) {
        return value == null || value.isEmpty() ? "none" : value;
    }
}
