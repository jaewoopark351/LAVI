package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership;

//20260905_kpopmodder: Own the exact STOP identity marker bound to one ordinary root task before mutation.

import adris.altoclef.tasksystem.Task;

public final class FabricChatClefOrdinaryCommandStopMarker {
    private FabricChatClefStopControlIdentity identity;

    public synchronized boolean bind(
            FabricChatClefStopControlIdentity candidateIdentity,
            Task boundRootTask,
            Task candidateTask
    ) {
        if (candidateIdentity == null || boundRootTask == null || candidateTask != boundRootTask) {
            return false;
        }
        if (identity != null) {
            return identity.equals(candidateIdentity);
        }
        identity = candidateIdentity;
        return true;
    }

    public synchronized boolean clear(FabricChatClefStopControlIdentity candidateIdentity) {
        if (candidateIdentity == null || identity == null || !identity.equals(candidateIdentity)) {
            return false;
        }
        identity = null;
        return true;
    }

    public synchronized boolean bound() {
        return identity != null;
    }
}
