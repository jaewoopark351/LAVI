package lavi.minecraft.diagnostics.mining;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.util.math.BlockPos;

//20260901_kpopmodder: Observe only an already-applied target clear or owner-stop boundary.
final class MineTargetAbandonmentDiagnostics {
    private MineTargetAbandonmentDiagnostics() {
    }

    static boolean log(
            Task task,
            BlockPos previousMiningPosition,
            BlockPos miningPositionAfterBoundary,
            String closureKind,
            String closureReason) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return false;
        }
        String previous = ChatClefDiagnostics.blockPos(previousMiningPosition);
        String after = ChatClefDiagnostics.blockPos(miningPositionAfterBoundary);
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "MINE_TARGET_ABANDONED",
                previous,
                after,
                closureKind,
                closureReason
        );
        return MiningDiagnosticEmitter.emitLazyWithPhysicalOutcome(
                "MINE_TARGET_ABANDONED",
                "mine_target_abandoned",
                task,
                "mine_target_abandoned|" + System.identityHashCode(task),
                fingerprint,
                () -> new Object[]{
                        "owner", "mine_target_abandonment_observer",
                        "trigger", "after_applied_clear_or_at_owner_stop",
                        "parentTaskClass", MiningDiagnosticEmitter.taskClass(task),
                        "parentTaskInstanceId", MiningDiagnosticEmitter.instanceId(task),
                        "previousMiningPosition", previous,
                        "miningPositionAfterBoundary", after,
                        "closureKind", closureKind,
                        "closureReason", closureReason,
                        "clearObservedApplied", previousMiningPosition != null
                                && miningPositionAfterBoundary == null
                }
        );
    }
}
