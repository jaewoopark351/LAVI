package lavi.minecraft.diagnostics.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

//20260807_kpopmodder: Store DestroyBlock diagnostic-only run state outside the upstream task.
final class DestroyBlockDiagnosticState {
    private static final Map<DestroyBlockTask, State> STATES = Collections.synchronizedMap(new WeakHashMap<>());
    private static long nextRunId;

    private DestroyBlockDiagnosticState() {
    }

    static State initialize(AltoClef mod, DestroyBlockTask task) {
        State state = new State(++nextRunId, ChatClefDiagnostics.currentClientTickId(),
                DestroyBlockDiagnosticFields.cobblestoneCount(mod));
        STATES.put(task, state);
        return state;
    }

    static State getOrCreate(DestroyBlockTask task) {
        return STATES.computeIfAbsent(task, ignored -> new State(++nextRunId, ChatClefDiagnostics.currentClientTickId(), -1));
    }

    static void updateObservedProgress(AltoClef mod, BlockPos target, State state) {
        if (mod == null || state == null) {
            return;
        }
        boolean blockAir = Boolean.parseBoolean(ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(target).isAir()));
        if (blockAir) {
            state.blockBecameAir = true;
        }
        boolean breaking = Boolean.parseBoolean(DestroyBlockDiagnosticFields.controllerBreakingBlock(mod));
        double progress = DestroyBlockDiagnosticFields.breakingProgressValue(mod);
        if (breaking || progress > 0.0) {
            state.breakEverStarted = true;
            state.maximumBreakingProgress = Math.max(state.maximumBreakingProgress, progress);
        }
    }

    static boolean isTrue(String value) {
        return "true".equals(value);
    }

    static final class State {
        final long runId;
        final long startedAtTick;
        final int inventoryCobblestoneAtStart;
        String currentPhase;
        long phaseEnteredTick = -1;
        boolean pathSuccessObserved;
        boolean pathingStartedObserved;
        boolean reachEverPresent;
        boolean breakEverStarted;
        boolean blockBecameAir;
        double maximumBreakingProgress;

        State(long runId, long startedAtTick, int inventoryCobblestoneAtStart) {
            this.runId = runId;
            this.startedAtTick = startedAtTick;
            this.inventoryCobblestoneAtStart = inventoryCobblestoneAtStart;
        }
    }
}
