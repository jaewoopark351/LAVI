package lavi.minecraft.fabric.chatclef.bridge.command;

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;

//20260801_kpopmodder: Let command dispatch report results without owning WebSocket transport.
public interface FabricChatClefCommandResultSender {
    void sendCommandResult(FabricChatClefCommandContext context, FabricChatClefCommandResultPayload payload);
}
