package lavi.minecraft.fabric.chatclef.bridge.command.connection.detach;

//20260905_kpopmodder: Own only the test-visible projection of the latest active detach decision.

public final class FabricChatClefConnectionDetachProjectionState {
    private volatile String lastBoundRootOwnership = "";
    private volatile String lastCancelAction = "";

    public void record(FabricChatClefConnectionDetachDecision decision) {
        lastBoundRootOwnership = decision.boundRootOwnership();
        lastCancelAction = decision.cancelAction();
    }

    public String lastBoundRootOwnership() {
        return lastBoundRootOwnership;
    }

    public String lastCancelAction() {
        return lastCancelAction;
    }
}
