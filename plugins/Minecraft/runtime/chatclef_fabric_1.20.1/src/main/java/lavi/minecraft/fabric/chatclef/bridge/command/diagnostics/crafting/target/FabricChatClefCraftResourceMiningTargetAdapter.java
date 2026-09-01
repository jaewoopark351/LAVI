package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.target;

import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceStage;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetRole;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//20260901_kpopmodder: Convert typed mining evidence without reading the world or scanner again.
final class FabricChatClefCraftResourceMiningTargetAdapter {
    private static final String IRON_ORE_ID = "minecraft:iron_ore";
    private static final String DEEPSLATE_IRON_ORE_ID = "minecraft:deepslate_iron_ore";
    private static final int EXPECTED_BLOCK_INPUT_LIMIT = 64;

    private FabricChatClefCraftResourceMiningTargetAdapter() {
    }

    static Optional<CraftResourceTargetTuple> targetTuple(
            BlockPos target,
            Block[] requestedBlocks) {
        if (target == null) {
            return Optional.empty();
        }
        if (!expectedBlockIdsInputComplete(requestedBlocks)) {
            return Optional.empty();
        }
        List<String> expectedBlockIds = expectedBlockIds(requestedBlocks);
        boolean ironInput = containsIronOre(expectedBlockIds);
        return Optional.of(new CraftResourceTargetTuple(
                ironInput
                        ? CraftResourceStage.IRON_INPUT_ACQUISITION
                        : CraftResourceStage.UNKNOWN,
                ironInput
                        ? CraftResourceTargetRole.IRON_ORE_BLOCK
                        : CraftResourceTargetRole.UNKNOWN,
                position(target),
                expectedBlockIds
        ));
    }

    static Optional<CraftResourceTargetTuple> reconciliationTuple(
            Task activeChildAfter,
            Optional<CraftResourceTargetTuple> currentTuple) {
        if (!(activeChildAfter instanceof DestroyBlockTask destroyBlockTask)) {
            return Optional.empty();
        }
        BlockPos target = destroyBlockTask.diagnosticTargetPosition();
        if (target == null) {
            return Optional.empty();
        }
        CraftResourceTargetTuple current = currentTuple.orElse(null);
        return Optional.of(new CraftResourceTargetTuple(
                current == null ? CraftResourceStage.UNKNOWN : current.resourceStage(),
                current == null ? CraftResourceTargetRole.UNKNOWN : current.targetRole(),
                position(target),
                current == null ? List.of() : current.expectedBlockIds()
        ));
    }

    static List<String> expectedBlockIds(Block[] requestedBlocks) {
        if (requestedBlocks == null || requestedBlocks.length == 0) {
            return List.of();
        }
        int retainedInputCount = Math.min(
                requestedBlocks.length,
                EXPECTED_BLOCK_INPUT_LIMIT
        );
        ArrayList<String> blockIds = new ArrayList<>(retainedInputCount);
        for (int index = 0; index < retainedInputCount; index++) {
            Block block = requestedBlocks[index];
            if (block != null) {
                blockIds.add(String.valueOf(Registries.BLOCK.getId(block)));
            }
        }
        return List.copyOf(blockIds);
    }

    static boolean expectedBlockIdsInputComplete(Block[] requestedBlocks) {
        return requestedBlocks == null
                || requestedBlocks.length <= EXPECTED_BLOCK_INPUT_LIMIT;
    }

    static Optional<String> observedBlockId(BlockState targetState) {
        return targetState == null
                ? Optional.empty()
                : Optional.of(String.valueOf(Registries.BLOCK.getId(targetState.getBlock())));
    }

    static String blockStateSummary(BlockState targetState) {
        return targetState == null ? "UNAVAILABLE" : String.valueOf(targetState);
    }

    static String position(BlockPos position) {
        return position == null
                ? "UNAVAILABLE"
                : position.getX() + "," + position.getY() + "," + position.getZ();
    }

    static String taskClass(Task task) {
        return task == null ? "UNAVAILABLE" : task.getClass().getName();
    }

    private static boolean containsIronOre(List<String> expectedBlockIds) {
        return expectedBlockIds.contains(IRON_ORE_ID)
                || expectedBlockIds.contains(DEEPSLATE_IRON_ORE_ID);
    }
}
