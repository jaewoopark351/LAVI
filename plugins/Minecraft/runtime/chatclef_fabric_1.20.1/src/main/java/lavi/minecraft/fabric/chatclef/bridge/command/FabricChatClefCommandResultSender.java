package lavi.minecraft.fabric.chatclef.bridge.command;

import java.util.Map;

//20260801_kpopmodder: Let command dispatch report results without owning WebSocket transport.
public interface FabricChatClefCommandResultSender {
    void sendCommandResult(FabricChatClefCommandContext context, Map<String, Object> payload);
}
