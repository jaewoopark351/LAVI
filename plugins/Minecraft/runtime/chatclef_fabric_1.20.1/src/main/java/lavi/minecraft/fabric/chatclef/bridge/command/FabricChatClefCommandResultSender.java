package lavi.minecraft.fabric.chatclef.bridge.command;

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;

//20260801_kpopmodder: Let command dispatch report results without owning WebSocket transport.
public interface FabricChatClefCommandResultSender {
    FabricChatClefCommandResultSendSubmission sendCommandResult(
            FabricChatClefCommandContext context,
            FabricChatClefCommandResultPayload payload
    );

    FabricChatClefCommandResultSendSubmission sendTerminalCommandResult(
            FabricChatClefCommandContext context,
            FabricChatClefCommandResultPayload payload
    );
}
