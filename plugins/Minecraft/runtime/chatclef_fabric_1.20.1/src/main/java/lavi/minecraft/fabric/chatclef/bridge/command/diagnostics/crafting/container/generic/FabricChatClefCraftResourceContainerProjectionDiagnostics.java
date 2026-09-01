package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.generic;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceSourceEventName;
import lavi.minecraft.diagnostics.crafting.acquisition.source.container.CraftResourceContainerSourceEventListener;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceStage;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetRole;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.common.FabricChatClefCraftResourceContainerProjectionSupport;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//20260901_kpopmodder: Classify generic container inputs as candidate references only.
public final class FabricChatClefCraftResourceContainerProjectionDiagnostics
        implements CraftResourceContainerSourceEventListener {
    private static final FabricChatClefCraftResourceContainerProjectionDiagnostics INSTANCE =
            new FabricChatClefCraftResourceContainerProjectionDiagnostics();

    private FabricChatClefCraftResourceContainerProjectionDiagnostics() {
    }

    public static FabricChatClefCraftResourceContainerProjectionDiagnostics instance() {
        return INSTANCE;
    }

    @Override
    public void onTargetDecision(
            Task task,
            ItemTarget containerTarget,
            Block[] containerBlocks,
            String reason,
            String stateKey,
            Object[] branchFields) {
        ContainerKind kind = containerKind(containerBlocks);
        if (kind == ContainerKind.UNKNOWN) {
            return;
        }
        Object nearestPresent = field(branchFields, "nearestPresent");
        Object nearestPosition = field(branchFields, "nearestPosition");
        Object cachedPosition = field(branchFields, "cachedContainerPosition");
        Object placedPosition = field(branchFields, "placeTaskPlaced");
        boolean hasBlockItem = booleanValue(field(branchFields, "hasContainerBlockItem"));
        boolean hasExisting = booleanValue(nearestPresent)
                || availablePosition(cachedPosition);

        CraftResourceStage stage;
        CraftResourceTargetRole role;
        String position;
        if (hasExisting) {
            stage = kind == ContainerKind.FURNACE
                    ? CraftResourceStage.RAW_IRON_SMELTING
                    : CraftResourceStage.FINAL_CRAFTING;
            role = kind.interactionRole;
            position = availablePosition(cachedPosition)
                    ? diagnosticValue(cachedPosition)
                    : diagnosticValue(nearestPosition);
        } else if (!hasBlockItem) {
            stage = kind == ContainerKind.FURNACE
                    ? CraftResourceStage.RAW_IRON_SMELTING
                    : CraftResourceStage.FINAL_CRAFTING;
            role = kind.acquisitionRole;
            position = "UNAVAILABLE";
        } else {
            stage = CraftResourceStage.PLACEMENT_SUPPORT;
            role = CraftResourceTargetRole.PLACEMENT_SUPPORT_BLOCK;
            position = diagnosticValue(placedPosition);
        }

        Map<String, Object> optional = new LinkedHashMap<>();
        optional.put("observedReason", diagnosticValue(reason));
        optional.put("observedStateKey", diagnosticValue(stateKey));
        optional.put("nearestPresent", diagnosticValue(nearestPresent));
        optional.put("cachedContainerPosition", diagnosticValue(cachedPosition));
        optional.put("hasContainerBlockItem", diagnosticValue(
                field(branchFields, "hasContainerBlockItem")
        ));
        optional.put("placeTaskActive", diagnosticValue(field(branchFields, "placeTaskActive")));
        optional.put("placeTaskFinished", diagnosticValue(field(branchFields, "placeTaskFinished")));
        optional.put("decision", diagnosticValue(field(branchFields, "decision")));
        FabricChatClefCraftResourceContainerProjectionSupport.observeReference(
                task,
                null,
                new CraftResourceTargetTuple(stage, role, position, List.of(kind.blockId)),
                CraftResourceSourceEventName.CONTAINER_TASK_TARGET_DECISION,
                "CONTAINER_TASK_PRE_BRANCH_CANDIDATE_REFERENCE",
                "PRE_BRANCH_INPUT_REFERENCE_ONLY",
                taskClass(task),
                "UNAVAILABLE_CANDIDATE_NOT_SELECTED",
                optional
        );
    }

    @Override
    public void onChildSelection(
            Task task,
            Task candidateChild,
            ItemTarget containerTarget,
            Block[] containerBlocks,
            String reason,
            String stateKey,
            Object[] branchFields,
            boolean sourceEmissionCompleted) {
        if (!sourceEmissionCompleted
                || candidateChild == null
                || !"return_open_table_task".equals(reason)
                || containerKind(containerBlocks) != ContainerKind.CRAFTING_TABLE) {
            return;
        }
        String position = blockPosition(field(branchFields, "cachedContainerPosition"));
        if ("UNAVAILABLE".equals(position)) {
            return;
        }
        Map<String, Object> optional = new LinkedHashMap<>();
        optional.put("observedReason", diagnosticValue(reason));
        optional.put("observedStateKey", diagnosticValue(stateKey));
        optional.put("candidateChildClass", taskClass(candidateChild));
        FabricChatClefCraftResourceContainerProjectionSupport.observeReference(
                task,
                candidateChild,
                new CraftResourceTargetTuple(
                        CraftResourceStage.FINAL_CRAFTING,
                        CraftResourceTargetRole.CRAFTING_TABLE_INTERACTION,
                        position,
                        List.of("minecraft:crafting_table")
                ),
                CraftResourceSourceEventName.CONTAINER_TASK_BRANCH,
                "CONTAINER_TASK_BRANCH_SELECTED_CHILD_REFERENCE",
                "PHYSICALLY_EMITTED_SELECTED_CHILD_REFERENCE",
                taskClass(task),
                taskClass(candidateChild),
                optional
        );
    }

    private static ContainerKind containerKind(Block[] blocks) {
        if (blocks != null) {
            for (Block block : blocks) {
                if (block == Blocks.FURNACE) {
                    return ContainerKind.FURNACE;
                }
                if (block == Blocks.CRAFTING_TABLE) {
                    return ContainerKind.CRAFTING_TABLE;
                }
            }
        }
        return ContainerKind.UNKNOWN;
    }

    private static Object field(Object[] fields, String key) {
        if (fields == null) {
            return null;
        }
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (key.equals(fields[index])) {
                return fields[index + 1];
            }
        }
        return null;
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean flag
                ? flag
                : Boolean.parseBoolean(String.valueOf(value));
    }

    private static boolean availablePosition(Object value) {
        String text = diagnosticValue(value);
        return !"UNAVAILABLE".equals(text)
                && !"none".equalsIgnoreCase(text)
                && !"null".equalsIgnoreCase(text);
    }

    private static String diagnosticValue(Object value) {
        return value == null ? "UNAVAILABLE" : String.valueOf(value);
    }

    private static String blockPosition(Object value) {
        if (!(value instanceof BlockPos position)) {
            return "UNAVAILABLE";
        }
        return position.getX() + "," + position.getY() + "," + position.getZ();
    }

    private static String taskClass(Task task) {
        return task == null ? "UNAVAILABLE" : task.getClass().getName();
    }

    private enum ContainerKind {
        FURNACE(
                "minecraft:furnace",
                CraftResourceTargetRole.FURNACE_ACQUISITION,
                CraftResourceTargetRole.FURNACE_INTERACTION
        ),
        CRAFTING_TABLE(
                "minecraft:crafting_table",
                CraftResourceTargetRole.CRAFTING_TABLE_ACQUISITION,
                CraftResourceTargetRole.CRAFTING_TABLE_INTERACTION
        ),
        UNKNOWN(
                "UNAVAILABLE",
                CraftResourceTargetRole.UNKNOWN,
                CraftResourceTargetRole.UNKNOWN
        );

        private final String blockId;
        private final CraftResourceTargetRole acquisitionRole;
        private final CraftResourceTargetRole interactionRole;

        ContainerKind(
                String blockId,
                CraftResourceTargetRole acquisitionRole,
                CraftResourceTargetRole interactionRole) {
            this.blockId = blockId;
            this.acquisitionRole = acquisitionRole;
            this.interactionRole = interactionRole;
        }
    }
}
