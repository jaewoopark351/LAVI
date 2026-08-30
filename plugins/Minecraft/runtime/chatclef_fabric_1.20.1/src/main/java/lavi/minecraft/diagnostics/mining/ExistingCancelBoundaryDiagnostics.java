package lavi.minecraft.diagnostics.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.cancel.PreCancelBaritoneState;
import net.minecraft.util.math.BlockPos;

//20260806_kpopmodder: Observe already-existing forceCancel boundaries without adding cancellation policy.
final class ExistingCancelBoundaryDiagnostics {
    private ExistingCancelBoundaryDiagnostics() {
    }

    static void log(AltoClef mod,
                    Task task,
                    BlockPos target,
                    String cancelSource,
                    PreCancelBaritoneState before) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BARITONE_EXISTING_CANCEL_BOUNDARY",
                cancelSource,
                ChatClefDiagnostics.blockPos(target),
                before.baritonePathing(),
                before.customGoalActive(),
                before.pathPresent()
        );
        MiningDiagnosticEmitter.emitLazy(
                "BARITONE_EXISTING_CANCEL_BOUNDARY", "baritone_existing_cancel_boundary", task,
                "baritone_cancel|" + cancelSource + "|" + System.identityHashCode(task),
                fingerprint,
                () -> {
                    SubmittedGoalDiagnosticState.SubmittedGoal submittedGoal =
                            SubmittedGoalDiagnosticState.get(task);
                    BaritonePathDiagnosticSnapshot after = BaritonePathDiagnosticSnapshot.capture(
                            mod,
                            target,
                            submittedGoal == null ? null : submittedGoal.goal,
                            SubmittedGoalDiagnosticState.matchesTarget(submittedGoal, target)
                    );
                    return MiningDiagnosticEmitter.merge(new Object[]{
                        "owner", "existing_bounded_cancel_observer",
                        "trigger", "existing_force_cancel_returned",
                        "cancelSource", cancelSource,
                        "targetPosition", ChatClefDiagnostics.blockPos(target)
                    }, SubmittedGoalDiagnosticState.fields(submittedGoal, target), before.fields("Before"),
                            after.fields("After"));
                });
    }
}
