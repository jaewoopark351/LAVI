package lavi.minecraft.fabric.chatclef.bridge.transport.connection;

//20260905_kpopmodder: Snapshot current-socket admission for one detach callback.
public final class FabricChatClefConnectionDetachAdmission {
    private final boolean currentSocket;
    private final long generation;

    public FabricChatClefConnectionDetachAdmission(boolean currentSocket, long generation) {
        this.currentSocket = currentSocket;
        this.generation = generation;
    }

    public boolean isCurrentSocket() {
        return currentSocket;
    }

    public long generation() {
        return generation;
    }
}
