package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.furnace;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceSourceEventName;
import lavi.minecraft.diagnostics.crafting.acquisition.source.furnace.CraftResourceFurnaceSourceEventListener;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceStage;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetRole;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.common.FabricChatClefCraftResourceContainerProjectionSupport;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.requirement.progress.FabricChatClefCraftResourceRequirementProgressDiagnostics;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//20260901_kpopmodder: Project already-emitted furnace evidence without choosing behavior.
public final class FabricChatClefCraftResourceFurnaceProjectionDiagnostics
        implements CraftResourceFurnaceSourceEventListener {
    private static final FabricChatClefCraftResourceFurnaceProjectionDiagnostics INSTANCE =
            new FabricChatClefCraftResourceFurnaceProjectionDiagnostics();
    private static final List<String> FURNACE_BLOCK_IDS = List.of("minecraft:furnace");

    private FabricChatClefCraftResourceFurnaceProjectionDiagnostics() {
    }

    public static FabricChatClefCraftResourceFurnaceProjectionDiagnostics instance() {
        return INSTANCE;
    }

    @Override
    public void onChildSelection(
            Task task,
            Task candidateChild,
            String currentGate,
            String candidateChildSemanticKey,
            int inventoryMaterialCount,
            int inventoryOutputCount,
            int materialsNeeded,
            double inventoryFuelCount,
            double fuelNeeded,
            boolean materialGateSatisfied,
            boolean fuelGateSatisfied) {
        CraftResourceStage stage = stageForGate(currentGate);
        CraftResourceTargetRole role = "ENTER_CONTAINER_FLOW".equals(currentGate)
                ? CraftResourceTargetRole.FURNACE_INTERACTION
                : CraftResourceTargetRole.UNKNOWN;
        Map<String, Object> optional = new LinkedHashMap<>();
        optional.put("currentGate", diagnosticValue(currentGate));
        optional.put("candidateChildSemanticKey", diagnosticValue(candidateChildSemanticKey));
        optional.put("inventoryMaterialCount", inventoryMaterialCount);
        optional.put("inventoryOutputCount", inventoryOutputCount);
        optional.put("materialsNeeded", materialsNeeded);
        optional.put("inventoryFuelCount", inventoryFuelCount);
        optional.put("fuelNeeded", fuelNeeded);
        optional.put("materialGateSatisfied", materialGateSatisfied);
        optional.put("fuelGateSatisfied", fuelGateSatisfied);
        observe(
                task,
                candidateChild,
                tuple(stage, role, "UNAVAILABLE", expectedIds(role)),
                CraftResourceSourceEventName.SMELT_CHILD_SELECTION_STATE,
                "SMELT_CHILD_SELECTION_CANDIDATE_REFERENCE",
                "CANDIDATE_CHILD_REFERENCE_ONLY",
                optional
        );
    }

    @Override
    public void onMaterialProgress(
            Task task,
            ItemTarget materialTarget,
            ItemTarget outputTarget,
            String currentGate,
            int inventoryMaterialCount,
            int inventoryOutputCount,
            int materialsNeeded,
            double inventoryFuelCount,
            double fuelNeeded,
            boolean materialGateSatisfied,
            boolean fuelGateSatisfied,
            double burningFuelCount,
            double burnPercentage,
            boolean sourceEmissionCompleted) {
        if (!isIronSmelting(materialTarget, outputTarget)) {
            return;
        }
        FabricChatClefCraftResourceRequirementProgressDiagnostics.observeMaterialProgress(
                task,
                materialTarget,
                currentGate,
                materialsNeeded,
                sourceEmissionCompleted
        );
        if (!sourceEmissionCompleted) {
            return;
        }
        Map<String, Object> optional = new LinkedHashMap<>();
        optional.put("currentGate", diagnosticValue(currentGate));
        optional.put("materialTarget", diagnosticValue(materialTarget));
        optional.put("outputTarget", diagnosticValue(outputTarget));
        optional.put("inventoryMaterialCount", inventoryMaterialCount);
        optional.put("inventoryOutputCount", inventoryOutputCount);
        optional.put("materialsNeeded", materialsNeeded);
        optional.put("inventoryFuelCount", inventoryFuelCount);
        optional.put("fuelNeeded", fuelNeeded);
        optional.put("materialGateSatisfied", materialGateSatisfied);
        optional.put("fuelGateSatisfied", fuelGateSatisfied);
        optional.put("burningFuelCount", burningFuelCount);
        optional.put("burnPercentage", burnPercentage);
        observe(
                task,
                null,
                tuple(
                        CraftResourceStage.RAW_IRON_SMELTING,
                        CraftResourceTargetRole.UNKNOWN,
                        "UNAVAILABLE",
                        List.of()
                ),
                CraftResourceSourceEventName.SMELT_MATERIAL_PROGRESS_SNAPSHOT,
                "SMELT_MATERIAL_PROGRESS_REQUIREMENT_REFERENCE",
                "REQUIREMENT_PROGRESS_REFERENCE_ONLY",
                optional
        );
    }

    @Override
    public void onOperationGate(
            Task task,
            String previousGate,
            String currentGate,
            int inventoryMaterialCount,
            int materialsNeeded,
            boolean materialGateSatisfied,
            double inventoryFuelCount,
            double fuelNeeded,
            boolean fuelGateSatisfied,
            Object materialsAccessible,
            boolean containerFlowEligible,
            int inventoryOutputCount,
            boolean sourceEmissionCompleted) {
        FabricChatClefCraftResourceRequirementProgressDiagnostics.observeOperationGate(
                task,
                currentGate,
                materialsNeeded,
                sourceEmissionCompleted
        );
        if (!sourceEmissionCompleted) {
            return;
        }
        CraftResourceTargetRole role = containerFlowEligible
                ? CraftResourceTargetRole.FURNACE_INTERACTION
                : CraftResourceTargetRole.UNKNOWN;
        Map<String, Object> optional = new LinkedHashMap<>();
        optional.put("previousGate", diagnosticValue(previousGate));
        optional.put("currentGate", diagnosticValue(currentGate));
        optional.put("inventoryMaterialCount", inventoryMaterialCount);
        optional.put("materialsNeeded", materialsNeeded);
        optional.put("materialGateSatisfied", materialGateSatisfied);
        optional.put("inventoryFuelCount", inventoryFuelCount);
        optional.put("fuelNeeded", fuelNeeded);
        optional.put("fuelGateSatisfied", fuelGateSatisfied);
        optional.put("materialsAccessible", diagnosticValue(materialsAccessible));
        optional.put("containerFlowEligible", containerFlowEligible);
        optional.put("inventoryOutputCount", inventoryOutputCount);
        observe(
                task,
                null,
                tuple(stageForGate(currentGate), role, "UNAVAILABLE", expectedIds(role)),
                CraftResourceSourceEventName.FURNACE_OPERATION_GATE_TRANSITION,
                "FURNACE_OPERATION_GATE_REFERENCE",
                "GATE_TRANSITION_REFERENCE_ONLY",
                optional
        );
    }

    @Override
    public void onContainerRoute(
            Task task,
            ItemTarget containerTarget,
            Block[] containerBlocks,
            String previousEffectiveBranch,
            String effectiveBranch,
            Task candidateChild,
            String candidateChildSemanticKey,
            BlockPos nearestPosition,
            String nearestSource,
            BlockPos cachedContainerPositionAfter,
            BlockPos placeTaskPlaced,
            boolean hasContainerBlockItem,
            int containerBlockItemCount,
            String trigger) {
        CraftResourceTargetRole role = roleForFurnaceBranch(effectiveBranch);
        CraftResourceStage stage = "PLACE_CONTAINER".equals(effectiveBranch)
                ? CraftResourceStage.PLACEMENT_SUPPORT
                : CraftResourceStage.RAW_IRON_SMELTING;
        BlockPos targetPosition = switch (diagnosticValue(effectiveBranch)) {
            case "OPEN_EXISTING_CONTAINER" -> cachedContainerPositionAfter == null
                    ? nearestPosition
                    : cachedContainerPositionAfter;
            case "PLACE_CONTAINER" -> placeTaskPlaced;
            default -> null;
        };
        Map<String, Object> optional = new LinkedHashMap<>();
        optional.put("previousEffectiveBranch", diagnosticValue(previousEffectiveBranch));
        optional.put("effectiveBranch", diagnosticValue(effectiveBranch));
        optional.put("candidateChildSemanticKey", diagnosticValue(candidateChildSemanticKey));
        optional.put("nearestSource", diagnosticValue(nearestSource));
        optional.put("hasContainerBlockItem", hasContainerBlockItem);
        optional.put("containerBlockItemCount", containerBlockItemCount);
        optional.put("trigger", diagnosticValue(trigger));
        observe(
                task,
                candidateChild,
                tuple(stage, role, position(targetPosition), FURNACE_BLOCK_IDS),
                CraftResourceSourceEventName.FURNACE_CONTAINER_ROUTE_TRANSITION,
                "FURNACE_CONTAINER_ROUTE_CANDIDATE_REFERENCE",
                "FINAL_ROUTE_CANDIDATE_REFERENCE_ONLY",
                optional
        );
    }

    private static void observe(
            Task task,
            Task candidateChild,
            CraftResourceTargetTuple tuple,
            CraftResourceSourceEventName source,
            String boundary,
            String authority,
            Map<String, Object> optional) {
        FabricChatClefCraftResourceContainerProjectionSupport.observeReference(
                task,
                candidateChild,
                tuple,
                source,
                boundary,
                authority,
                taskClass(task),
                taskClass(candidateChild),
                optional
        );
    }

    private static boolean isIronSmelting(ItemTarget materialTarget, ItemTarget outputTarget) {
        return containsItem(outputTarget, Items.IRON_INGOT)
                || containsItem(
                        materialTarget,
                        Items.RAW_IRON,
                        Items.IRON_ORE,
                        Items.DEEPSLATE_IRON_ORE
                );
    }

    private static boolean containsItem(ItemTarget target, Item... expected) {
        if (target == null || expected == null) {
            return false;
        }
        for (Item actual : target.getMatches()) {
            for (Item candidate : expected) {
                if (actual == candidate) {
                    return true;
                }
            }
        }
        return false;
    }

    private static CraftResourceStage stageForGate(String gate) {
        return "GET_MATERIAL".equals(gate)
                ? CraftResourceStage.IRON_INPUT_ACQUISITION
                : CraftResourceStage.RAW_IRON_SMELTING;
    }

    private static CraftResourceTargetRole roleForFurnaceBranch(String branch) {
        if ("GET_CONTAINER_ITEM".equals(branch)) {
            return CraftResourceTargetRole.FURNACE_ACQUISITION;
        }
        if ("OPEN_EXISTING_CONTAINER".equals(branch)) {
            return CraftResourceTargetRole.FURNACE_INTERACTION;
        }
        if ("PLACE_CONTAINER".equals(branch)) {
            return CraftResourceTargetRole.PLACEMENT_SUPPORT_BLOCK;
        }
        return CraftResourceTargetRole.UNKNOWN;
    }

    private static List<String> expectedIds(CraftResourceTargetRole role) {
        return role == CraftResourceTargetRole.FURNACE_INTERACTION
                || role == CraftResourceTargetRole.FURNACE_ACQUISITION
                || role == CraftResourceTargetRole.PLACEMENT_SUPPORT_BLOCK
                        ? FURNACE_BLOCK_IDS
                        : List.of();
    }

    private static CraftResourceTargetTuple tuple(
            CraftResourceStage stage,
            CraftResourceTargetRole role,
            String position,
            List<String> expectedBlockIds) {
        return new CraftResourceTargetTuple(stage, role, position, expectedBlockIds);
    }

    private static String position(BlockPos position) {
        return position == null ? "UNAVAILABLE" : position.toShortString();
    }

    private static String taskClass(Task task) {
        return task == null ? "UNAVAILABLE" : task.getClass().getName();
    }

    private static String diagnosticValue(Object value) {
        return value == null ? "UNAVAILABLE" : String.valueOf(value);
    }
}
