package lavi.minecraft.fabric.chatclef.bridge.transport.inbound.command;

//20260905_kpopmodder: Build an ordinary command context with both server and Java socket fences.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefAcceptedSessionIdentity;

public final class FabricChatClefCommandContextFactory {
    public FabricChatClefCommandContext create(
            FabricChatClefCommandRequest request,
            FabricChatClefBridgeEnvelope envelope,
            FabricChatClefAcceptedSessionIdentity acceptedIdentity,
            long javaSocketGeneration
    ) {
        return new FabricChatClefCommandContext(
                request,
                envelope.messageId,
                envelope.sessionId,
                acceptedIdentity.serverConnectionGeneration(),
                javaSocketGeneration
        );
    }
}
