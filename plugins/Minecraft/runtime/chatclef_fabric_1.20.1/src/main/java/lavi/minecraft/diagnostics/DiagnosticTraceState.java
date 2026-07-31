package lavi.minecraft.diagnostics;

//20260731_kpopmodder: Own diagnostic trace, tick, event, and operation counters outside the log facade.
final class DiagnosticTraceState {
    private long nextTraceId = 1;
    private long nextOperationId = 1;
    private long clientTickId;
    private long eventSequence;
    private String traceId = "unavailable";
    private boolean runtimeIdentityLogged;

    synchronized void advanceClientTick() {
        clientTickId++;
    }

    synchronized String currentTraceId() {
        return traceId;
    }

    synchronized long currentClientTickId() {
        return clientTickId;
    }

    synchronized long nextEventSequence() {
        return ++eventSequence;
    }

    synchronized long nextOperationId() {
        return nextOperationId++;
    }

    synchronized DiagnosticEventIdentity nextEventIdentity(boolean startNewTrace) {
        if (startNewTrace || "unavailable".equals(traceId)) {
            traceId = "trace-" + nextTraceId++;
        }
        return new DiagnosticEventIdentity(traceId, clientTickId, ++eventSequence);
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
}
