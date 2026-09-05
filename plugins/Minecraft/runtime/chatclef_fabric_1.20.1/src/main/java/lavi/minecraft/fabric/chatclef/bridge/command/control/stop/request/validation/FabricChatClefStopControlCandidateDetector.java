package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation;

//20260905_kpopmodder: Claim every partial STOP discriminator before the ordinary command lane can observe it.

import com.fasterxml.jackson.databind.JsonNode;

public final class FabricChatClefStopControlCandidateDetector {
    public boolean isCandidate(JsonNode envelope) {
        if (envelope == null || !envelope.isObject()) {
            return false;
        }
        JsonNode messageType = envelope.get("message_type");
        if (!isExactText(messageType, "command_request")) {
            return false;
        }
        JsonNode payload = envelope.get("payload");
        if (payload == null || !payload.isObject()) {
            return false;
        }
        JsonNode metadata = payload.get("metadata");
        boolean metadataObject = metadata != null && metadata.isObject();
        boolean hasRequestKind = metadataObject && metadata.has("request_kind");
        boolean hasOperation = metadataObject && metadata.has("operation");
        return metadataObject && (
                isExactText(metadata.get("request_kind"), "stop_control_v1")
                        || isExactText(metadata.get("operation"), "stop_ai")
                        || (isExactText(payload.get("command"), "stop")
                        && (hasRequestKind || hasOperation))
        );
    }

    private boolean isExactText(JsonNode value, String expected) {
        return value != null && value.isTextual() && expected.equals(value.textValue());
    }
}
