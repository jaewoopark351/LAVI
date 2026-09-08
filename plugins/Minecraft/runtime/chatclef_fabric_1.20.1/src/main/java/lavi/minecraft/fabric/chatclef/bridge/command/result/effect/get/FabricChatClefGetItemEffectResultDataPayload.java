package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get;

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;

import java.util.Map;

//20260907_kpopmodder: Decorate matching completion data with typed GET effect evidence.
final class FabricChatClefGetItemEffectResultDataPayload
        implements FabricChatClefCommandResultDataPayload {
    private final FabricChatClefCommandResultDataPayload basePayload;
    private final FabricChatClefGetItemEffectEvidence evidence;

    FabricChatClefGetItemEffectResultDataPayload(
            FabricChatClefCommandResultDataPayload basePayload,
            FabricChatClefGetItemEffectEvidence evidence
    ) {
        this.basePayload = basePayload == null
                ? FabricChatClefCommandResultDataPayload.empty()
                : basePayload;
        this.evidence = evidence;
    }

    @Override
    public Map<String, Object> toMap() {
        return FabricChatClefGetItemEffectResultDataPayloadMap.toMap(
                basePayload,
                evidence
        );
    }
}
