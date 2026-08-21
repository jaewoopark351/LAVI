package lavi.minecraft.fabric.chatclef.bridge.command.observation;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

import java.util.Map;

//20260822_kpopmodder: Latch the first command finish callback without reclassifying duplicates.
public final class FabricChatClefFinishCallbackObservation {
    public enum Timing {
        BEFORE_DISPATCH_RETURN("before_dispatch_return", true),
        AFTER_DISPATCH_RETURN("after_dispatch_return", false);

        private final String wireValue;
        private final boolean beforeDispatchReturn;

        Timing(String wireValue, boolean beforeDispatchReturn) {
            this.wireValue = wireValue;
            this.beforeDispatchReturn = beforeDispatchReturn;
        }

        public String wireValue() {
            return wireValue;
        }

        public boolean beforeDispatchReturn() {
            return beforeDispatchReturn;
        }
    }

    private final Timing timing;
    private final Task task;
    private final FabricChatClefTaskSnapshot taskSnapshot;
    private final long observedAtMs;
    private final long observedAtNanos;
    private final long observedClientTick;
    private final String threadName;

    private FabricChatClefFinishCallbackObservation(
            Timing timing,
            Task task,
            FabricChatClefTaskSnapshot taskSnapshot,
            long observedAtMs,
            long observedAtNanos,
            long observedClientTick,
            String threadName
    ) {
        this.timing = timing;
        this.task = task;
        this.taskSnapshot = taskSnapshot;
        this.observedAtMs = observedAtMs;
        this.observedAtNanos = observedAtNanos;
        this.observedClientTick = observedClientTick;
        this.threadName = threadName == null ? "" : threadName;
    }

    public static FabricChatClefFinishCallbackObservation capture(
            Task task,
            boolean executorExecuteInvocationOpen
    ) {
        return new FabricChatClefFinishCallbackObservation(
                executorExecuteInvocationOpen ? Timing.BEFORE_DISPATCH_RETURN : Timing.AFTER_DISPATCH_RETURN,
                task,
                FabricChatClefTaskSnapshot.capture(task),
                System.currentTimeMillis(),
                System.nanoTime(),
                ChatClefDiagnostics.currentClientTickId(),
                Thread.currentThread().getName()
        );
    }

    public Timing timing() {
        return timing;
    }

    public boolean observedBeforeDispatchReturn() {
        return timing.beforeDispatchReturn();
    }

    public Task task() {
        return task;
    }

    public FabricChatClefTaskSnapshot taskSnapshot() {
        return taskSnapshot;
    }

    public long observedAtMs() {
        return observedAtMs;
    }

    public long observedAtNanos() {
        return observedAtNanos;
    }

    public long observedClientTick() {
        return observedClientTick;
    }

    public String threadName() {
        return threadName;
    }

    public Map<String, Object> toMap(int duplicateCount) {
        return Map.of(
                "timing", timing.wireValue(),
                "before_dispatch_return", observedBeforeDispatchReturn(),
                "observed_at_ms", observedAtMs,
                "observed_at_nanos", observedAtNanos,
                "observed_client_tick", observedClientTick,
                "thread", threadName,
                "duplicate_count", duplicateCount,
                "task", taskSnapshot.toMap()
        );
    }
}
