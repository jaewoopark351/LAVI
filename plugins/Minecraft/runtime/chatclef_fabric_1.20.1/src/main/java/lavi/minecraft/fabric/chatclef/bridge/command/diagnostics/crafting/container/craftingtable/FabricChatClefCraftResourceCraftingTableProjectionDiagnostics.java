package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.craftingtable;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceSourceEventName;
import lavi.minecraft.diagnostics.crafting.acquisition.source.craftingtable.CraftResourceCraftingTableSourceEventListener;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceStage;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetRole;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.common.FabricChatClefCraftResourceContainerProjectionSupport;
import net.minecraft.block.Block;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//20260901_kpopmodder: Bind crafting-table route aggregates without inventing a retry decision.
public final class FabricChatClefCraftResourceCraftingTableProjectionDiagnostics
        implements CraftResourceCraftingTableSourceEventListener {
    private static final FabricChatClefCraftResourceCraftingTableProjectionDiagnostics INSTANCE =
            new FabricChatClefCraftResourceCraftingTableProjectionDiagnostics();

    private FabricChatClefCraftResourceCraftingTableProjectionDiagnostics() {
    }

    public static FabricChatClefCraftResourceCraftingTableProjectionDiagnostics instance() {
        return INSTANCE;
    }

    @Override
    public void onRouteAggregate(
            Task task,
            ItemTarget containerTarget,
            Block[] containerBlocks,
            String observedEventName,
            String observedReason,
            String observedStateKey,
            String routeKey,
            int observationCount,
            long stableTicks,
            Object[] branchFields) {
        Object nearestPresent = field(branchFields, "nearestPresent");
        Object nearestPosition = field(branchFields, "nearestPosition");
        Object cachedPosition = field(branchFields, "cachedContainerPosition");
        Object placedPosition = field(branchFields, "placeTaskPlaced");
        Object hasBlockItem = field(branchFields, "hasContainerBlockItem");

        CraftResourceStage stage = CraftResourceStage.FINAL_CRAFTING;
        CraftResourceTargetRole role;
        String position;
        if (booleanValue(nearestPresent) || availablePosition(cachedPosition)) {
            role = CraftResourceTargetRole.CRAFTING_TABLE_INTERACTION;
            position = availablePosition(cachedPosition)
                    ? diagnosticValue(cachedPosition)
                    : diagnosticValue(nearestPosition);
        } else if (!booleanValue(hasBlockItem)) {
            role = CraftResourceTargetRole.CRAFTING_TABLE_ACQUISITION;
            position = "UNAVAILABLE";
        } else {
            stage = CraftResourceStage.PLACEMENT_SUPPORT;
            role = CraftResourceTargetRole.PLACEMENT_SUPPORT_BLOCK;
            position = diagnosticValue(placedPosition);
        }

        Map<String, Object> optional = new LinkedHashMap<>();
        optional.put("aggregateObservedEventName", diagnosticValue(observedEventName));
        optional.put("aggregateObservedReason", diagnosticValue(observedReason));
        optional.put("aggregateObservedStateKey", diagnosticValue(observedStateKey));
        optional.put("aggregateRouteKey", diagnosticValue(routeKey));
        optional.put("sameRouteObservationCount", observationCount);
        optional.put("sameRouteStableTicks", stableTicks);
        optional.put("decision", diagnosticValue(field(branchFields, "decision")));
        FabricChatClefCraftResourceContainerProjectionSupport.observeReference(
                task,
                null,
                new CraftResourceTargetTuple(
                        stage,
                        role,
                        position,
                        List.of("minecraft:crafting_table")
                ),
                CraftResourceSourceEventName.CRAFTING_TABLE_ROUTE_RETRY_SUMMARY,
                "CRAFTING_TABLE_ROUTE_AGGREGATE_REFERENCE",
                "AGGREGATE_REFERENCE_ONLY_NO_RETRY_AUTHORITY",
                taskClass(task),
                "UNAVAILABLE_AGGREGATE_ONLY",
                optional
        );
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

    private static String taskClass(Task task) {
        return task == null ? "UNAVAILABLE" : task.getClass().getName();
    }
}
