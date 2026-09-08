package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.payload.FabricChatClefLifecycleEvidenceSequencePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;

import java.util.Map;

//20260907_kpopmodder: Decorate command-result data with only its execution-wide evidence sequence.
public final class FabricChatClefLifecycleEvidenceSequencePayload
        implements FabricChatClefCommandResultDataPayload {
    private final FabricChatClefCommandResultDataPayload basePayload;
    private final int evidenceSequence;

    private FabricChatClefLifecycleEvidenceSequencePayload(
            FabricChatClefCommandResultDataPayload basePayload,
            int evidenceSequence
    ) {
        this.basePayload = basePayload == null
                ? FabricChatClefCommandResultDataPayload.empty()
                : basePayload;
        this.evidenceSequence = evidenceSequence;
    }

    public static FabricChatClefLifecycleEvidenceSequencePayload of(
            FabricChatClefCommandResultDataPayload basePayload,
            int evidenceSequence
    ) {
        return new FabricChatClefLifecycleEvidenceSequencePayload(
                basePayload,
                evidenceSequence
        );
    }

    @Override
    public Map<String, Object> toMap() {
        return FabricChatClefLifecycleEvidenceSequencePayloadMap.toMap(
                basePayload,
                evidenceSequence
        );
    }
}
