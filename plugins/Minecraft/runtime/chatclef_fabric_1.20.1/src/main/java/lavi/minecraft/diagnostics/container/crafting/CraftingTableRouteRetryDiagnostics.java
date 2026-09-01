package lavi.minecraft.diagnostics.container.crafting;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.source.craftingtable.CraftResourceCraftingTableSourceEventObserver;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Summarize repeated crafting-table route choices without changing container routing.
public final class CraftingTableRouteRetryDiagnostics {
    private static final long SUMMARY_INTERVAL_TICKS = 200;
    private static final int SESSION_HARD_CAP = 256;
    private static final int MAX_KEY_LENGTH = 320;

    private static final Map<String, RouteState> STATES = new HashMap<>();
    private static int emittedCount;
    private static boolean capLogged;

    private CraftingTableRouteRetryDiagnostics() {
    }

    public static synchronized void observe(String eventName,
                                            String reason,
                                            String stateKey,
                                            AltoClef mod,
                                            Task task,
                                            ItemTarget containerTarget,
                                            Block[] containerBlocks,
                                            Object[] branchFields) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()
                || !isCraftingTable(containerTarget, containerBlocks)
                || !isRouteRetrySignal(eventName, reason)) {
            return;
        }

        long tick = ChatClefDiagnostics.currentClientTickId();
        String routeKey = routeKey(eventName, reason, task, branchFields);
        RouteState state = STATES.get(routeKey);
        if (state == null) {
            state = new RouteState(tick);
            STATES.put(routeKey, state);
            state.observationCount = 1;
            emit("first_route_observation", eventName, reason, stateKey, routeKey, state, mod, task,
                    containerTarget, containerBlocks, branchFields);
            return;
        }

        state.observationCount++;
        if (tick - state.lastEmissionTick < SUMMARY_INTERVAL_TICKS) {
            return;
        }
        emit("route_retry_summary", eventName, reason, stateKey, routeKey, state, mod, task, containerTarget,
                containerBlocks, branchFields);
    }

    private static void emit(String trigger,
                             String eventName,
                             String reason,
                             String stateKey,
                             String routeKey,
                             RouteState state,
                             AltoClef mod,
                             Task task,
                             ItemTarget containerTarget,
                             Block[] containerBlocks,
                             Object[] branchFields) {
        if (emittedCount >= SESSION_HARD_CAP) {
            if (!capLogged) {
                capLogged = true;
                ChatClefDiagnostics.logBoundary("CRAFTING_TABLE_ROUTE_RETRY_DIAGNOSTIC_CAP_REACHED",
                        "crafting_table_route_retry_diagnostic_cap_reached",
                        task,
                        ChatClefDiagnostics.withCommandContextFields(
                                "diagnosticScope", "crafting_table_route_retry",
                                "owner", "crafting_table_route_observer",
                                "mode", "BOUNDARY",
                                "trigger", "session_cap",
                                "dedupe_key", "crafting_table_route_retry|cap",
                                "max_emission", "session=" + SESSION_HARD_CAP,
                                "correlation", "session",
                                "payload", "flat_fields",
                                "terminal", false,
                                "behavior_effect", "none",
                                "cap", SESSION_HARD_CAP
                        ));
            }
            return;
        }

        long tick = ChatClefDiagnostics.currentClientTickId();
        emittedCount++;
        state.lastEmissionTick = tick;
        boolean sourceEmissionCompleted = ChatClefDiagnostics.logBoundaryWithPhysicalOutcome(
                "CRAFTING_TABLE_ROUTE_RETRY_SUMMARY",
                "crafting_table_route_retry_summary",
                task,
                ChatClefDiagnostics.withCommandContextFields(
                        "diagnosticScope", "crafting_table_route_retry",
                        "owner", "crafting_table_route_observer",
                        "mode", "BOUNDARY",
                        "trigger", trigger,
                        "dedupe_key", routeKey,
                        "max_emission", "first_and_summary_ticks=" + SUMMARY_INTERVAL_TICKS + ",session=" + SESSION_HARD_CAP,
                        "correlation", "containerTaskIdentity=" + identity(task),
                        "payload", "flat_fields",
                        "terminal", false,
                        "behavior_effect", "none",
                        "gameTick", tick,
                        "observedEventName", eventName,
                        "observedReason", reason,
                        "observedStateKey", stateKey,
                        "containerTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(task),
                        "containerTaskIdentity", identity(task),
                        "sameRouteObservationCount", state.observationCount,
                        "sameRouteStableTicks", tick - state.firstSeenTick,
                        "containerTarget", containerTarget,
                        "containerBlocks", Arrays.toString(containerBlocks),
                        "decision", diagnosticValue(field(branchFields, "decision")),
                        "nearestPresent", diagnosticValue(field(branchFields, "nearestPresent")),
                        "nearestPosition", diagnosticValue(field(branchFields, "nearestPosition")),
                        "cachedContainerPosition", diagnosticValue(field(branchFields, "cachedContainerPosition")),
                        "costToWalk", diagnosticValue(field(branchFields, "costToWalk")),
                        "costToMakeNew", diagnosticValue(field(branchFields, "costToMakeNew")),
                        "rawCostRelation", rawCostRelation(field(branchFields, "nearestPresent"),
                                field(branchFields, "costToWalk"),
                                field(branchFields, "costToMakeNew")),
                        "hasContainerBlockItem", diagnosticValue(field(branchFields, "hasContainerBlockItem")),
                        "placeTaskActive", diagnosticValue(field(branchFields, "placeTaskActive")),
                        "placeTaskFinished", diagnosticValue(field(branchFields, "placeTaskFinished")),
                        "placeTaskPlaced", diagnosticValue(field(branchFields, "placeTaskPlaced")),
                        "openTableTask", diagnosticValue(field(branchFields, "openTableTask")),
                        "playerPosition", ChatClefDiagnostics.playerPosition(mod)
                ));
        if (sourceEmissionCompleted) {
            CraftResourceCraftingTableSourceEventObserver.observeRouteAggregate(
                    task,
                    containerTarget,
                    containerBlocks,
                    eventName,
                    reason,
                    stateKey,
                    routeKey,
                    state.observationCount,
                    tick - state.firstSeenTick,
                    branchFields
            );
        }
    }

    private static boolean isRouteRetrySignal(String eventName, String reason) {
        if ("CONTAINER_TASK_TARGET_DECISION".equals(eventName)) {
            return true;
        }
        return "CONTAINER_TASK_BRANCH".equals(eventName) && "return_open_table_task".equals(reason);
    }

    private static boolean isCraftingTable(ItemTarget containerTarget, Block[] containerBlocks) {
        boolean targetMatches = containerTarget != null && containerTarget.matches(Items.CRAFTING_TABLE);
        boolean blockMatches = containerBlocks != null
                && Arrays.stream(containerBlocks).anyMatch(block -> block == Blocks.CRAFTING_TABLE);
        return targetMatches || blockMatches;
    }

    private static String routeKey(String eventName, String reason, Task task, Object[] branchFields) {
        String key = "crafting_table_route_retry"
                + "|task=" + ChatClefDiagnostics.className(task)
                + "|taskIdentity=" + identity(task)
                + "|event=" + eventName
                + "|reason=" + reason
                + "|decision=" + diagnosticValue(field(branchFields, "decision"))
                + "|nearest=" + diagnosticValue(field(branchFields, "nearestPosition"))
                + "|cached=" + diagnosticValue(field(branchFields, "cachedContainerPosition"))
                + "|costToMakeNew=" + diagnosticValue(field(branchFields, "costToMakeNew"))
                + "|hasContainerBlockItem=" + diagnosticValue(field(branchFields, "hasContainerBlockItem"));
        if (key.length() <= MAX_KEY_LENGTH) {
            return key;
        }
        return key.substring(0, MAX_KEY_LENGTH) + "...";
    }

    private static String rawCostRelation(Object nearestPresent, Object costToWalkValue, Object costToMakeNewValue) {
        if ("false".equalsIgnoreCase(diagnosticValue(nearestPresent))) {
            return "NO_NEAREST";
        }
        Double costToWalk = number(costToWalkValue);
        Double costToMakeNew = number(costToMakeNewValue);
        if (costToWalk == null || costToMakeNew == null || !Double.isFinite(costToWalk)) {
            return "UNAVAILABLE";
        }
        int comparison = Double.compare(costToWalk, costToMakeNew);
        if (comparison < 0) {
            return "WALK_COST_LOWER";
        }
        if (comparison == 0) {
            return "WALK_COST_EQUAL";
        }
        return "WALK_COST_HIGHER";
    }

    private static Double number(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private static Object field(Object[] fields, String name) {
        if (fields == null || name == null) {
            return null;
        }
        for (int i = 0; i + 1 < fields.length; i += 2) {
            Object key = fields[i];
            if (name.equals(key)) {
                return fields[i + 1];
            }
        }
        return null;
    }

    private static String diagnosticValue(Object value) {
        return value == null ? "unavailable" : String.valueOf(value);
    }

    private static String identity(Task task) {
        return task == null ? "none" : Integer.toHexString(System.identityHashCode(task));
    }

    private static final class RouteState {
        private final long firstSeenTick;
        private long lastEmissionTick;
        private int observationCount;

        private RouteState(long tick) {
            this.firstSeenTick = tick;
            this.lastEmissionTick = tick;
        }
    }
}
