package lavi.minecraft.fabric.chatclef.bridge.state;

//20260801_kpopmodder: Track only Fabric ChatClef bridge transport lifecycle state.
public enum FabricChatClefBridgeLifecycleState {
    STOPPED,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    FAILED
}
