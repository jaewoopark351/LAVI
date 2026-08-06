package lavi.minecraft.diagnostics.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.blockpos.BlockPosVer;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.StorageHelper;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.util.math.BlockPos;

//20260806_kpopmodder: Observe block unreachable requests before existing blacklist mutation.
final class BlockUnreachableRequestDiagnostics {
    private BlockUnreachableRequestDiagnostics() {
    }

    static void log(AltoClef mod,
                    Task task,
                    BlockPos target,
                    int requestedAllowedFailures,
                    String requestSource,
                    Task activeDestroyTask,
                    Task candidateDestroyTask) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        BaritonePathDiagnosticSnapshot snapshot = BaritonePathDiagnosticSnapshot.capture(mod, target, null, "unavailable");
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BLOCK_UNREACHABLE_REQUEST",
                requestSource,
                ChatClefDiagnostics.blockPos(target),
                Integer.toString(requestedAllowedFailures),
                snapshot.customGoalActive,
                snapshot.baritonePathing,
                snapshot.pathPresent
        );
        MiningDiagnosticEmitter.emit("BLOCK_UNREACHABLE_REQUEST", "block_unreachable_request", task,
                "block_unreachable_request|" + requestSource + "|" + ChatClefDiagnostics.blockPos(target),
                fingerprint,
                MiningDiagnosticEmitter.merge(new Object[]{
                        "owner", "block_unreachable_request_observer",
                        "trigger", "before_request_block_unreachable",
                        "requestSource", requestSource,
                        "targetPosition", ChatClefDiagnostics.blockPos(target),
                        "targetBlockId", ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(target).getBlock()),
                        "targetBlockState", ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(target)),
                        "requestedAllowedFailures", requestedAllowedFailures,
                        "scannerUnreachableBefore", ChatClefDiagnostics.safeValue(() -> mod.getBlockScanner().isUnreachable(target)),
                        "playerPosition", mod == null ? "unavailable" : ChatClefDiagnostics.playerPosition(mod),
                        "distanceSq", ChatClefDiagnostics.safeValue(() -> BlockPosVer.getSquaredDistance(target, mod.getPlayer().getPos())),
                        "currentMiningRequirement", ChatClefDiagnostics.safeValue(StorageHelper::getCurrentMiningRequirement),
                        "activeDestroyTaskInstanceId", MiningDiagnosticEmitter.instanceId(activeDestroyTask),
                        "candidateDestroyTaskInstanceId", MiningDiagnosticEmitter.instanceId(candidateDestroyTask)
                }, snapshot.fields()));
    }
}
