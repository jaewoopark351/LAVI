package lavi.minecraft.fabric.chatclef.bridge.transport.result;

import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;

//20260905_kpopmodder: Carry either an encoded result envelope or its terminal encoding failure.
public final class FabricChatClefResultEnvelopeEncoding {
    private final String message;
    private final FabricChatClefCommandResultSendSubmission failure;

    private FabricChatClefResultEnvelopeEncoding(
            String message,
            FabricChatClefCommandResultSendSubmission failure
    ) {
        this.message = message;
        this.failure = failure;
    }

    public static FabricChatClefResultEnvelopeEncoding encoded(String message) {
        return new FabricChatClefResultEnvelopeEncoding(message, null);
    }

    public static FabricChatClefResultEnvelopeEncoding failed(
            FabricChatClefCommandResultSendSubmission failure
    ) {
        return new FabricChatClefResultEnvelopeEncoding(null, failure);
    }

    public boolean succeeded() {
        return message != null;
    }

    public String message() {
        return message;
    }

    public FabricChatClefCommandResultSendSubmission failure() {
        return failure;
    }
}
