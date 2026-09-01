package lavi.minecraft.diagnostics;

//20260731_kpopmodder: Own diagnostic trace, tick, event, and operation counters outside the log facade.
final class DiagnosticTraceState {
    private long nextTraceId = 1;
    private long nextOperationId = 1;
    private long clientTickId;
    private long eventSequence;
    private boolean traceSequenceAvailable = true;
    private boolean operationSequenceAvailable = true;
    private boolean eventSequenceAvailable = true;
    private String traceId = "unavailable";
    private boolean runtimeIdentityLogged;

    DiagnosticTraceState() {
    }

    DiagnosticTraceState(long nextTraceId,
                         long nextOperationId,
                         long clientTickId,
                         long eventSequence) {
        this.nextTraceId = nonNegative(nextTraceId);
        this.nextOperationId = nonNegative(nextOperationId);
        this.clientTickId = nonNegative(clientTickId);
        this.eventSequence = nonNegative(eventSequence);
    }

    synchronized void advanceClientTick() {
        if (clientTickId < Long.MAX_VALUE) {
            clientTickId++;
        }
    }

    synchronized String currentTraceId() {
        return traceId;
    }

    synchronized long currentClientTickId() {
        return clientTickId;
    }

    synchronized long nextEventSequence() {
        if (!eventSequenceAvailable) {
            return -1L;
        }
        eventSequence++;
        if (eventSequence == Long.MAX_VALUE) {
            eventSequenceAvailable = false;
        }
        return eventSequence;
    }

    synchronized long nextOperationId() {
        if (!operationSequenceAvailable) {
            return -1L;
        }
        long result = nextOperationId;
        if (nextOperationId == Long.MAX_VALUE) {
            operationSequenceAvailable = false;
        } else {
            nextOperationId++;
        }
        return result;
    }

    synchronized DiagnosticEventIdentity nextEventIdentity(boolean startNewTrace) {
        if (startNewTrace || "unavailable".equals(traceId)) {
            traceId = nextTraceIdentity();
        }
        return new DiagnosticEventIdentity(traceId, clientTickId, nextEventSequence());
    }

    synchronized void resetRuntimeIdentityLogged() {
        runtimeIdentityLogged = false;
    }

    synchronized boolean markRuntimeIdentityLogged() {
        if (runtimeIdentityLogged) {
            return false;
        }
        runtimeIdentityLogged = true;
        return true;
    }

    private String nextTraceIdentity() {
        if (!traceSequenceAvailable) {
            return "trace-unavailable-saturated";
        }
        long result = nextTraceId;
        if (nextTraceId == Long.MAX_VALUE) {
            traceSequenceAvailable = false;
        } else {
            nextTraceId++;
        }
        return "trace-" + result;
    }

    private static long nonNegative(long value) {
        if (value < 0L) {
            throw new IllegalArgumentException("Diagnostic trace counters must be non-negative.");
        }
        return value;
    }
}
