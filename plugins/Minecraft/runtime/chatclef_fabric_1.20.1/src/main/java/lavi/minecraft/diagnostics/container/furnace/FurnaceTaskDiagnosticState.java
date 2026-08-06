package lavi.minecraft.diagnostics.container.furnace;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

//20260807_kpopmodder: Store diagnostic-only furnace transition state outside upstream container tasks.
final class FurnaceTaskDiagnosticState {
    private static final Map<Task, State> STATES = Collections.synchronizedMap(new WeakHashMap<>());
    private static long decisionSequence;

    private FurnaceTaskDiagnosticState() {
    }

    static void markCacheUpdated(Task task, BlockPos sourceFurnacePosition) {
        if (task == null) {
            return;
        }
        State state = state(task);
        state.cacheUpdatedTick = ChatClefDiagnostics.currentClientTickId();
        state.cacheSourceFurnacePosition = sourceFurnacePosition == null ? null : sourceFurnacePosition.toImmutable();
    }

    static RouteObservation observeRoute(Task task, String effectiveBranch) {
        State state = state(task);
        long tick = ChatClefDiagnostics.currentClientTickId();
        String currentBranch = normalize(effectiveBranch);
        String previousBranch = state.effectiveBranch == null ? "none" : state.effectiveBranch;
        boolean branchChanged = !currentBranch.equals(previousBranch);
        long branchAgeTicks = state.effectiveBranchEnteredTick < 0 ? 0 : Math.max(0, tick - state.effectiveBranchEnteredTick);
        if (branchChanged) {
            if (state.effectiveBranch != null) {
                state.routeTransitionCount++;
            }
            state.effectiveBranch = currentBranch;
            state.effectiveBranchEnteredTick = tick;
            branchAgeTicks = 0;
        }
        long sequence = ++decisionSequence;
        return new RouteObservation(sequence, previousBranch, currentBranch, branchChanged,
                branchAgeTicks, state.routeTransitionCount);
    }

    static GateObservation observeGate(Task task, String currentGate) {
        State state = state(task);
        String normalizedGate = normalize(currentGate);
        String previousGate = state.operationGate == null ? "none" : state.operationGate;
        boolean gateChanged = !normalizedGate.equals(previousGate);
        if (gateChanged) {
            state.operationGate = normalizedGate;
            state.operationGateTransitionCount++;
        }
        return new GateObservation(previousGate, normalizedGate, gateChanged, state.operationGateTransitionCount);
    }

    static CostObservation observeCost(Task task, String costSource) {
        State state = state(task);
        String normalizedSource = normalize(costSource);
        String previousSource = state.costSource == null ? "none" : state.costSource;
        boolean sourceChanged = !normalizedSource.equals(previousSource);
        if (sourceChanged) {
            state.costSource = normalizedSource;
            state.costSourceTransitionCount++;
        }
        return new CostObservation(previousSource, normalizedSource, sourceChanged, state.costSourceTransitionCount);
    }

    static CacheObservation cacheObservation(Task task) {
        State state = task == null ? null : STATES.get(task);
        if (state == null || state.cacheUpdatedTick < 0) {
            return new CacheObservation(false, "unavailable", "unavailable", "unavailable");
        }
        long currentTick = ChatClefDiagnostics.currentClientTickId();
        return new CacheObservation(
                state.cacheUpdatedTick == currentTick,
                Long.toString(state.cacheUpdatedTick),
                Long.toString(Math.max(0, currentTick - state.cacheUpdatedTick)),
                ChatClefDiagnostics.blockPos(state.cacheSourceFurnacePosition)
        );
    }

    private static synchronized State state(Task task) {
        return STATES.computeIfAbsent(task, ignored -> new State());
    }

    private static String normalize(String value) {
        return value == null || value.isEmpty() ? "none" : value;
    }

    static final class RouteObservation {
        final long decisionSequence;
        final String previousEffectiveBranch;
        final String effectiveBranch;
        final boolean branchChanged;
        final long branchAgeTicks;
        final int branchTransitionCount;

        RouteObservation(long decisionSequence,
                         String previousEffectiveBranch,
                         String effectiveBranch,
                         boolean branchChanged,
                         long branchAgeTicks,
                         int branchTransitionCount) {
            this.decisionSequence = decisionSequence;
            this.previousEffectiveBranch = previousEffectiveBranch;
            this.effectiveBranch = effectiveBranch;
            this.branchChanged = branchChanged;
            this.branchAgeTicks = branchAgeTicks;
            this.branchTransitionCount = branchTransitionCount;
        }
    }

    static final class GateObservation {
        final String previousGate;
        final String currentGate;
        final boolean gateChanged;
        final int gateTransitionCount;

        GateObservation(String previousGate, String currentGate, boolean gateChanged, int gateTransitionCount) {
            this.previousGate = previousGate;
            this.currentGate = currentGate;
            this.gateChanged = gateChanged;
            this.gateTransitionCount = gateTransitionCount;
        }
    }

    static final class CostObservation {
        final String previousCostSource;
        final String currentCostSource;
        final boolean sourceChanged;
        final int sourceTransitionCount;

        CostObservation(String previousCostSource, String currentCostSource, boolean sourceChanged, int sourceTransitionCount) {
            this.previousCostSource = previousCostSource;
            this.currentCostSource = currentCostSource;
            this.sourceChanged = sourceChanged;
            this.sourceTransitionCount = sourceTransitionCount;
        }
    }

    static final class CacheObservation {
        final boolean cacheUpdatedThisTick;
        final String cacheLastUpdatedTick;
        final String cacheAgeTicks;
        final String cacheSourceFurnacePosition;

        CacheObservation(boolean cacheUpdatedThisTick,
                         String cacheLastUpdatedTick,
                         String cacheAgeTicks,
                         String cacheSourceFurnacePosition) {
            this.cacheUpdatedThisTick = cacheUpdatedThisTick;
            this.cacheLastUpdatedTick = cacheLastUpdatedTick;
            this.cacheAgeTicks = cacheAgeTicks;
            this.cacheSourceFurnacePosition = cacheSourceFurnacePosition;
        }
    }

    private static final class State {
        String effectiveBranch;
        long effectiveBranchEnteredTick = -1;
        int routeTransitionCount;
        String operationGate;
        int operationGateTransitionCount;
        String costSource;
        int costSourceTransitionCount;
        long cacheUpdatedTick = -1;
        BlockPos cacheSourceFurnacePosition;
    }
}
