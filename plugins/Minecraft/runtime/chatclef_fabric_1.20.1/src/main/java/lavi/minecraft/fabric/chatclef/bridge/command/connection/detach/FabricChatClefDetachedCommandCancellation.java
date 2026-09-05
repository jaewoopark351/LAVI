package lavi.minecraft.fabric.chatclef.bridge.command.connection.detach;

//20260905_kpopmodder: Define the single registered seam for cancelling an owned detached task.

@FunctionalInterface
public interface FabricChatClefDetachedCommandCancellation {
    void cancel(String rootMatchReason);
}
