package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get;

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

//20260907_kpopmodder: Fix additive GET effect wire fields without weakening base lifecycle data.
class FabricChatClefGetItemEffectResultProjectorTest {
    private static final Set<String> EFFECT_PAYLOAD_KEYS = Set.of(
            "target_item",
            "target_match_ids",
            "quantity_semantics",
            "requested_delta",
            "before_target_count",
            "after_target_count",
            "target_count_delta",
            "effect_observation_status",
            "effect_observation_reason"
    );
    private static final Set<String> LEGACY_FLAT_KEYS = Set.of(
            "target_item",
            "requested_count",
            "before_target_count",
            "after_target_count",
            "target_count_delta",
            "effect_observation_status",
            "effect_observation_reason"
    );

    @Test
    void authoritativeEvidenceDecoratesMatchingCompletionData() {
        Object world = new Object();
        Object player = new Object();
        AtomicInteger reads = new AtomicInteger();
        FabricChatClefGetItemEffectTracker tracker = tracker(() ->
                FabricChatClefGetItemCountObservation.authoritative(
                        reads.getAndIncrement() + 4,
                        world,
                        player
                )
        );
        FabricChatClefCommandResultDataPayload base = base();

        Map<String, Object> result = new FabricChatClefGetItemEffectResultProjector()
                .fromMatchingCompletion(base, tracker)
                .toMap();

        assertEquals("matching_task_finished", result.get("result_reason"));
        assertEquals("fabric_chatclef_get_acquire_delta", result.get("effect_profile_id"));
        assertEquals(1, result.get("effect_profile_version"));
        assertEquals("get_acquisition_delta", result.get("effect_kind"));
        Map<?, ?> effect = (Map<?, ?>) result.get("effect_payload");
        assertEquals(EFFECT_PAYLOAD_KEYS, effect.keySet());
        assertEquals("diamond_pickaxe", effect.get("target_item"));
        assertEquals(List.of("minecraft:diamond_pickaxe"), effect.get("target_match_ids"));
        assertEquals("ACQUIRE_DELTA", effect.get("quantity_semantics"));
        assertEquals(1, effect.get("requested_delta"));
        assertEquals(4, effect.get("before_target_count"));
        assertEquals(5, effect.get("after_target_count"));
        assertEquals(1, effect.get("target_count_delta"));
        assertEquals("authoritative", effect.get("effect_observation_status"));
        assertEquals(
                "before_and_after_inventory_counts_match_world_binding",
                effect.get("effect_observation_reason")
        );
        assertEquals("diamond_pickaxe", result.get("target_item"));
        assertEquals(1, result.get("requested_count"));
        assertEquals(4, result.get("before_target_count"));
        assertEquals(5, result.get("after_target_count"));
        assertEquals(1, result.get("target_count_delta"));
        assertEquals("authoritative", result.get("effect_observation_status"));
        assertEquals(
                effect.get("effect_observation_reason"),
                result.get("effect_observation_reason")
        );
        assertEquals(base.toMap().get("ownership"), result.get("ownership"));
        assertEquals("message-effect", result.get("command_message_id"));
    }

    @Test
    void unavailableEvidenceUsesNullCountsInsteadOfInventedZero() {
        FabricChatClefGetItemEffectTracker tracker = tracker(() ->
                FabricChatClefGetItemCountObservation.unavailable("test_unavailable")
        );

        Map<String, Object> result = new FabricChatClefGetItemEffectResultProjector()
                .fromMatchingCompletion(base(), tracker)
                .toMap();
        Map<?, ?> effect = (Map<?, ?>) result.get("effect_payload");

        assertEquals("unavailable", result.get("effect_observation_status"));
        assertEquals("unavailable", effect.get("effect_observation_status"));
        assertEquals(
                "before=test_unavailable,after=test_unavailable",
                effect.get("effect_observation_reason")
        );
        assertEquals(
                effect.get("effect_observation_reason"),
                result.get("effect_observation_reason")
        );
        assertNull(result.get("before_target_count"));
        assertNull(result.get("after_target_count"));
        assertNull(result.get("target_count_delta"));
    }

    @Test
    void generalizedTargetUsesOnlyTheNestedProfileRepresentation() {
        Object world = new Object();
        Object player = new Object();
        AtomicInteger reads = new AtomicInteger();
        FabricChatClefGetItemEffectTracker tracker =
                FabricChatClefGetItemTestProfiles.tracker(
                        "get iron_pickaxe 2",
                        () -> FabricChatClefGetItemCountObservation.authoritative(
                                reads.getAndIncrement() * 2,
                                world,
                                player
                        )
                );

        Map<String, Object> result = new FabricChatClefGetItemEffectResultProjector()
                .fromMatchingCompletion(base(), tracker)
                .toMap();
        Map<?, ?> effect = (Map<?, ?>) result.get("effect_payload");

        assertEquals("fabric_chatclef_get_acquire_delta", result.get("effect_profile_id"));
        assertEquals("get_acquisition_delta", result.get("effect_kind"));
        assertEquals("iron_pickaxe", effect.get("target_item"));
        assertEquals(2, effect.get("requested_delta"));
        assertEquals(2, effect.get("target_count_delta"));
        assertEquals(EFFECT_PAYLOAD_KEYS, effect.keySet());
        for (String legacyFlatKey : LEGACY_FLAT_KEYS) {
            assertFalse(result.containsKey(legacyFlatKey), legacyFlatKey);
        }
    }

    @Test
    void dynamicCatalogueGroupProjectsItsCanonicalWholeMatchSet() {
        Object world = new Object();
        Object player = new Object();
        AtomicInteger reads = new AtomicInteger();
        FabricChatClefGetItemEffectTracker tracker =
                FabricChatClefGetItemTestProfiles.tracker(
                        "@get log 3",
                        () -> FabricChatClefGetItemCountObservation.authoritative(
                                reads.getAndIncrement() == 0 ? 4 : 7,
                                world,
                                player
                        )
                );

        Map<String, Object> result = new FabricChatClefGetItemEffectResultProjector()
                .fromMatchingCompletion(base(), tracker)
                .toMap();
        Map<?, ?> effect = (Map<?, ?>) result.get("effect_payload");

        assertEquals("log", effect.get("target_item"));
        assertEquals(
                List.of("minecraft:oak_log", "minecraft:spruce_log"),
                effect.get("target_match_ids")
        );
        assertEquals(3, effect.get("requested_delta"));
        assertEquals(4, effect.get("before_target_count"));
        assertEquals(7, effect.get("after_target_count"));
        assertEquals(3, effect.get("target_count_delta"));
        assertEquals("authoritative", effect.get("effect_observation_status"));
        for (String legacyFlatKey : LEGACY_FLAT_KEYS) {
            assertFalse(result.containsKey(legacyFlatKey), legacyFlatKey);
        }
    }

    @Test
    void excludedCommandPreservesOriginalPayloadAndPerformsNoReads() {
        AtomicInteger reads = new AtomicInteger();
        FabricChatClefGetItemEffectTracker tracker =
                FabricChatClefGetItemTestProfiles.tracker(
                        "deposit diamond_pickaxe 2",
                        () -> {
                            reads.incrementAndGet();
                            return FabricChatClefGetItemCountObservation.unavailable("unexpected");
                        }
                );
        FabricChatClefCommandResultDataPayload base = base();

        FabricChatClefCommandResultDataPayload result =
                new FabricChatClefGetItemEffectResultProjector()
                        .fromMatchingCompletion(base, tracker);

        assertSame(base, result);
        assertEquals(0, reads.get());
    }

    private static FabricChatClefGetItemEffectTracker tracker(
            FabricChatClefGetItemTargetCountReader reader
    ) {
        return FabricChatClefGetItemTestProfiles.tracker(
                "get diamond_pickaxe 1",
                reader
        );
    }

    private static FabricChatClefCommandResultDataPayload base() {
        return FabricChatClefCommandResultDataPayload.fromMap(
                Map.of(
                        "result_reason", "matching_task_finished",
                        "result_fidelity", "callback_plus_matching_user_task_event",
                        "ownership", Map.of(
                                "request_id", "request-effect",
                                "correlation_id", "correlation-effect",
                                "session_id", "session-effect",
                                "connection_generation", 7L
                        ),
                        "command_message_id", "message-effect"
                )
        );
    }
}
