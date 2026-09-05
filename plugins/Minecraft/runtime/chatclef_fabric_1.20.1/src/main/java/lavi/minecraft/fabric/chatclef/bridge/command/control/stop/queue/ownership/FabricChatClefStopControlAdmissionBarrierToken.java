package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership;

//20260905_kpopmodder: Bind the Java STOP barrier to one exact control and socket identity.
public final class FabricChatClefStopControlAdmissionBarrierToken {
    private final FabricChatClefStopControlIdentity identity;
    private final long javaSocketGeneration;

    FabricChatClefStopControlAdmissionBarrierToken(
            FabricChatClefStopControlIdentity identity,
            long javaSocketGeneration
    ) {
        this.identity = identity;
        this.javaSocketGeneration = javaSocketGeneration;
    }

    public FabricChatClefStopControlIdentity identity() {
        return identity;
    }

    public long javaSocketGeneration() {
        return javaSocketGeneration;
    }
}
