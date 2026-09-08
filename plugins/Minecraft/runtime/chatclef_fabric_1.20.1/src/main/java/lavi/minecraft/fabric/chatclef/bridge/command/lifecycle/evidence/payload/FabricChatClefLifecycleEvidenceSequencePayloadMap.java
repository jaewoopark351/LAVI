package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;

import java.util.HashMap;
import java.util.Map;

//20260907_kpopmodder: Own the lifecycle evidence sequence wire key at command_result.data.
public final class FabricChatClefLifecycleEvidenceSequencePayloadMap {
    private static final String EVIDENCE_SEQUENCE = "evidence_sequence";

    private FabricChatClefLifecycleEvidenceSequencePayloadMap() {
    }

    public static Map<String, Object> toMap(
            FabricChatClefCommandResultDataPayload basePayload,
            int evidenceSequence
    ) {
        Map<String, Object> payload = new HashMap<>(basePayload.toMap());
        payload.put(EVIDENCE_SEQUENCE, evidenceSequence);
        return payload;
    }
}
