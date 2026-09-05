package lavi.minecraft.fabric.chatclef.bridge.transport.connection.handshake;

//20260905_kpopmodder: Own construction of the Fabric ChatClef handshake envelope.

import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeMessageFactory;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;

public final class FabricChatClefHandshakeEnvelopeFactory {
    private final FabricChatClefBridgeState bridgeState;
    private final FabricChatClefBridgeMessageFactory messageFactory;

    public FabricChatClefHandshakeEnvelopeFactory(
            FabricChatClefBridgeState bridgeState,
            FabricChatClefBridgeMessageFactory messageFactory
    ) {
        this.bridgeState = bridgeState;
        this.messageFactory = messageFactory;
    }

    public FabricChatClefBridgeEnvelope create() {
        return messageFactory.handshake(bridgeState);
    }
}
