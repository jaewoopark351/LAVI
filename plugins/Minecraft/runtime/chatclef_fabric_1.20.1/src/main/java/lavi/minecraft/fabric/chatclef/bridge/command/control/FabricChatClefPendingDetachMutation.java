package lavi.minecraft.fabric.chatclef.bridge.command.control;

//20260815_kpopmodder: Preserve pending terminal ownership while an async terminal send is unresolved.
public final class FabricChatClefPendingDetachMutation {
    private final int removedCount;
    private final int inFlightRetainedCount;

    private FabricChatClefPendingDetachMutation(int removedCount, int inFlightRetainedCount) {
        this.removedCount = removedCount;
        this.inFlightRetainedCount = inFlightRetainedCount;
    }

    public static FabricChatClefPendingDetachMutation of(int removedCount, int inFlightRetainedCount) {
        return new FabricChatClefPendingDetachMutation(removedCount, inFlightRetainedCount);
    }

    public int removedCount() {
        return removedCount;
    }

    public int inFlightRetainedCount() {
        return inFlightRetainedCount;
    }
}
