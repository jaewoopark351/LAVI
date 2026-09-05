package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.outbox;

//20260905_kpopmodder: Own terminal send-attempt admission and payload commit.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;

import java.util.function.Supplier;

public final class FabricChatClefTerminalResultCommitter {
    public FabricChatClefCommandResultPayload commit(
            FabricChatClefCommandContext context,
            Supplier<FabricChatClefCommandResultPayload> resultFactory
    ) {
        if (!context.beginTerminalSend(System.currentTimeMillis())) {
            return null;
        }
        try {
            return context.commitTerminalPayload(resultFactory);
        } catch (RuntimeException error) {
            context.cancelTerminalSendAttempt("terminal_result_factory_failed");
            throw error;
        }
    }
}
