package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

//20260822_kpopmodder: Classify whether a post-dispatch root is owned by the current command.
public enum FabricChatClefRootOwnershipClassification {
    COMMAND_OWNED_ROOT,
    PREEXISTING_UNCHANGED_IDLE_ROOT,
    NO_ROOT_VISIBLE,
    OWNERSHIP_UNKNOWN
}
