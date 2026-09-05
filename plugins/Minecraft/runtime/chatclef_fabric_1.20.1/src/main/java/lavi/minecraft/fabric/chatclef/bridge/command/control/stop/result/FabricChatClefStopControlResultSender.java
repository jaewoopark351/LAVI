package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result;

//20260905_kpopmodder: Separate STOP result delivery from ordinary command-result ownership.

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;

import java.util.function.Consumer;

public interface FabricChatClefStopControlResultSender {
    FabricChatClefCommandResultSendSubmission sendStopControlResult(
            String correlationId,
            String sessionId,
            long javaSocketGeneration,
            FabricChatClefCommandResultPayload payload,
            Consumer<FabricChatClefCommandResultSendOutcome> completion
    );
}
