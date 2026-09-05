package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch;

//20260905_kpopmodder: Give the STOP tick owner one explicit engine-mutation seam.
public interface FabricChatClefStopCommandExecutor {
    void executeRegisteredStop() throws Exception;
}
