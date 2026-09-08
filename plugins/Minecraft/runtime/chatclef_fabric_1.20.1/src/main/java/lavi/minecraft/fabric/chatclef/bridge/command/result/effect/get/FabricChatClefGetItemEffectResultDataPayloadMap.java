package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get;

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//20260907_kpopmodder: Own the additive GET effect keys at command_result.data.
final class FabricChatClefGetItemEffectResultDataPayloadMap {
    private static final String EFFECT_KIND = "effect_kind";
    private static final String EFFECT_PROFILE_ID = "effect_profile_id";
    private static final String EFFECT_PROFILE_VERSION = "effect_profile_version";
    private static final String EFFECT_PAYLOAD = "effect_payload";
    private static final String TARGET_ITEM = "target_item";
    private static final String TARGET_MATCH_IDS = "target_match_ids";
    private static final String QUANTITY_SEMANTICS = "quantity_semantics";
    private static final String REQUESTED_DELTA = "requested_delta";
    private static final String REQUESTED_COUNT = "requested_count";
    private static final String BEFORE_TARGET_COUNT = "before_target_count";
    private static final String AFTER_TARGET_COUNT = "after_target_count";
    private static final String TARGET_COUNT_DELTA = "target_count_delta";
    private static final String EFFECT_OBSERVATION_STATUS = "effect_observation_status";
    private static final String EFFECT_OBSERVATION_REASON = "effect_observation_reason";

    private FabricChatClefGetItemEffectResultDataPayloadMap() {
    }

    static Map<String, Object> toMap(
            FabricChatClefCommandResultDataPayload basePayload,
            FabricChatClefGetItemEffectEvidence evidence
    ) {
        Map<String, Object> payload = new HashMap<>(basePayload.toMap());
        FabricChatClefGetItemEffectProfile profile = evidence.profile();
        payload.put(EFFECT_PROFILE_ID, profile.effectProfileId());
        payload.put(EFFECT_PROFILE_VERSION, profile.effectProfileVersion());
        payload.put(EFFECT_KIND, profile.effectKind());
        payload.put(EFFECT_PAYLOAD, effectPayload(profile, evidence));
        if (profile.legacyFlatCompatible()) {
            putLegacyFlatFields(payload, profile, evidence);
        }
        return payload;
    }

    private static Map<String, Object> effectPayload(
            FabricChatClefGetItemEffectProfile profile,
            FabricChatClefGetItemEffectEvidence evidence
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(TARGET_ITEM, profile.targetItem());
        payload.put(TARGET_MATCH_IDS, List.copyOf(profile.targetMatchIds()));
        payload.put(QUANTITY_SEMANTICS, profile.quantitySemantics());
        payload.put(REQUESTED_DELTA, profile.requestedCount());
        payload.put(BEFORE_TARGET_COUNT, evidence.beforeCountOrNull());
        payload.put(AFTER_TARGET_COUNT, evidence.afterCountOrNull());
        payload.put(TARGET_COUNT_DELTA, evidence.deltaOrNull());
        payload.put(EFFECT_OBSERVATION_STATUS, evidence.observationStatus());
        payload.put(EFFECT_OBSERVATION_REASON, evidence.observationReason());
        return payload;
    }

    private static void putLegacyFlatFields(
            Map<String, Object> payload,
            FabricChatClefGetItemEffectProfile profile,
            FabricChatClefGetItemEffectEvidence evidence
    ) {
        payload.put(TARGET_ITEM, profile.targetItem());
        payload.put(REQUESTED_COUNT, profile.requestedCount());
        payload.put(BEFORE_TARGET_COUNT, evidence.beforeCountOrNull());
        payload.put(AFTER_TARGET_COUNT, evidence.afterCountOrNull());
        payload.put(TARGET_COUNT_DELTA, evidence.deltaOrNull());
        payload.put(EFFECT_OBSERVATION_STATUS, evidence.observationStatus());
        payload.put(EFFECT_OBSERVATION_REASON, evidence.observationReason());
    }
}
