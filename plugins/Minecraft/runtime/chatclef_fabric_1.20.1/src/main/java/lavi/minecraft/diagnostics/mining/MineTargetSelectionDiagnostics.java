package lavi.minecraft.diagnostics.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.resources.MineAndCollectTask;
import adris.altoclef.util.helpers.WorldHelper;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

//20260806_kpopmodder: Observe MineOrCollect target selection separately from mining behavior.
final class MineTargetSelectionDiagnostics {
    private static final Map<MineAndCollectTask.MineOrCollectTask, String> LAST_SELECTED_PURSUIT =
            Collections.synchronizedMap(new WeakHashMap<>());

    private MineTargetSelectionDiagnostics() {
    }

    static void log(AltoClef mod,
                    MineAndCollectTask.MineOrCollectTask task,
                    Pair<Double, Optional<BlockPos>> closestBlock,
                    Pair<Double, Optional<ItemEntity>> closestDrop,
                    Optional<Object> selected,
                    String selectionReason,
                    boolean targetChanged,
                    boolean localBlacklistContains,
                    Block[] requestedBlocks,
                    int localBlacklistSize,
                    BlockPos currentMiningPos) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        String previous = LAST_SELECTED_PURSUIT.getOrDefault(task, "none");
        String selectedSummary = selected.map(value -> objectSummary(value)).orElse("none");
        if (selected.isPresent()) {
            LAST_SELECTED_PURSUIT.put(task, selectedSummary);
        }
        Optional<BlockPos> selectedBlock = selected.filter(BlockPos.class::isInstance).map(BlockPos.class::cast);
        String currentMiningPosition = currentMiningPos == null ? "none" : ChatClefDiagnostics.blockPos(currentMiningPos);
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "MINE_TARGET_SELECTION_TRANSITION",
                previous,
                selectedSummary,
                currentMiningPosition,
                Boolean.toString(targetChanged),
                selectionReason,
                selectedBlock.map(ChatClefDiagnostics::blockPos).orElse("none")
        );
        MiningDiagnosticEmitter.emit("MINE_TARGET_SELECTION_TRANSITION", "mine_target_selection_transition", task,
                "mine_target_selection|" + System.identityHashCode(task),
                fingerprint,
                new Object[]{
                        "owner", "mine_or_collect_task",
                        "trigger", "target_selection",
                        "previousPursuitPosition", previous,
                        "currentMiningPositionBeforeSelection", currentMiningPosition,
                        "closestBlockPosition", closestBlock.getRight().map(ChatClefDiagnostics::blockPos).orElse("none"),
                        "closestBlockDistanceSq", closestBlock.getLeft(),
                        "closestDropEntityId", closestDrop.getRight().map(entity -> Integer.toString(entity.getId())).orElse("none"),
                        "closestDropPosition", closestDrop.getRight().map(entity -> ChatClefDiagnostics.vec3d(entity.getPos())).orElse("none"),
                        "closestDropDistanceSq", closestDrop.getLeft(),
                        "selectedPursuitPosition", selectedSummary,
                        "targetChanged", targetChanged,
                        "selectedEqualsCurrentMiningPosition", selectedBlock.map(pos -> pos.equals(currentMiningPos)).orElse(false),
                        "selectionReason", selectionReason,
                        "requestedBlockIds", Arrays.toString(requestedBlocks),
                        "targetBlockId", selectedBlock.map(pos -> ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(pos).getBlock())).orElse("none"),
                        "targetBlockState", selectedBlock.map(pos -> ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(pos))).orElse("none"),
                        "blockStillMatchesRequestedType", selectedBlock.map(pos -> ChatClefDiagnostics.safeValue(() -> mod.getBlockScanner().isBlockAtPosition(pos, mod.getWorld().getBlockState(pos).getBlock()))).orElse("not_block"),
                        "selectedBlockMatchesAnyRequestedBlock", selectedBlock.map(pos -> ChatClefDiagnostics.safeValue(() -> blockMatchesAnyRequested(mod, pos, requestedBlocks))).orElse("not_block"),
                        "chunkLoaded", selectedBlock.map(pos -> ChatClefDiagnostics.safeValue(() -> mod.getChunkTracker().isChunkLoaded(pos))).orElse("not_block"),
                        "worldCanBreak", selectedBlock.map(pos -> ChatClefDiagnostics.safeValue(() -> WorldHelper.canBreak(pos))).orElse("not_block"),
                        "scannerUnreachable", selectedBlock.map(pos -> ChatClefDiagnostics.safeValue(() -> mod.getBlockScanner().isUnreachable(pos))).orElse("not_block"),
                        "localBlacklistContains", localBlacklistContains,
                        "localBlacklistSize", localBlacklistSize,
                        "heuristicCachePresent", "unavailable_from_subtask",
                        "cachedHeuristic", "unavailable_from_subtask",
                        "cachedBestDistanceSq", "unavailable_from_subtask"
                });
    }

    private static String objectSummary(Object value) {
        if (value == null) {
            return "none";
        }
        if (value instanceof BlockPos blockPos) {
            return "block#" + ChatClefDiagnostics.blockPos(blockPos);
        }
        if (value instanceof ItemEntity itemEntity) {
            return ChatClefDiagnostics.entitySummary(itemEntity);
        }
        return ChatClefDiagnostics.className(value) + "#" + ChatClefDiagnostics.safeValue(() -> value);
    }

    private static boolean blockMatchesAnyRequested(AltoClef mod, BlockPos pos, Block[] requestedBlocks) {
        if (requestedBlocks == null || requestedBlocks.length == 0) {
            return false;
        }
        Block actual = mod.getWorld().getBlockState(pos).getBlock();
        for (Block requestedBlock : requestedBlocks) {
            if (actual == requestedBlock) {
                return true;
            }
        }
        return false;
    }
}
