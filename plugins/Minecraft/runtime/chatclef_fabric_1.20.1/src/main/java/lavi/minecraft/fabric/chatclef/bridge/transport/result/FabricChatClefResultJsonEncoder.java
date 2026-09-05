package lavi.minecraft.fabric.chatclef.bridge.transport.result;

//20260905_kpopmodder: Encode only a built command-result envelope into its transport representation.

import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendStatus;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;

public final class FabricChatClefResultJsonEncoder {
    private final FabricChatClefBridgeJson json;
    private final FabricChatClefResultSendDiagnostics diagnostics;

    public FabricChatClefResultJsonEncoder(
            FabricChatClefBridgeJson json,
            FabricChatClefResultSendDiagnostics diagnostics
    ) {
        this.json = json;
        this.diagnostics = diagnostics;
    }

    public FabricChatClefResultEnvelopeEncoding encode(FabricChatClefBridgeEnvelope envelope) {
        try {
            return FabricChatClefResultEnvelopeEncoding.encoded(json.encode(envelope));
        } catch (Exception error) {
            String detail = diagnostics.encodeFailed(error);
            return FabricChatClefResultEnvelopeEncoding.failed(
                    FabricChatClefCommandResultSendSubmission.failed(
                            FabricChatClefCommandResultSendOutcome.failed(
                                    FabricChatClefCommandResultSendStatus.ENCODE_FAILED,
                                    detail
                            )
                    )
            );
        }
    }
}
