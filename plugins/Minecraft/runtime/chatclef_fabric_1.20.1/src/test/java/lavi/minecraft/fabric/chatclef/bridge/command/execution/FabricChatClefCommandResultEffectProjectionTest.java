package lavi.minecraft.fabric.chatclef.bridge.command.execution;

import adris.altoclef.eventbus.events.TaskFinishedEvent;
import adris.altoclef.tasks.movement.IdleTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandOutcomeClassifier;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminalDecision;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.FabricChatClefCommandEffectTracker;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.FabricChatClefNoEffectTracker;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get.FabricChatClefGetItemTestProfiles;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Verify execution wiring limits GET effect data to exact matching completion.
class FabricChatClefCommandResultEffectProjectionTest {
    @Test
    void classifierProjectsExactEffectOnlyForMatchingTaskCompletion() {
        IdleTask root = new IdleTask();
        FabricChatClefCommandTerminalDecision decision = classify(
                "get diamond_pickaxe 1",
                root,
                root
        );
        Map<?, ?> data = data(decision.result().toMap());

        assertTrue(decision.terminal());
        assertEquals("matching_task_finished", decision.reason());
        assertEquals("matching_task_finished", data.get("result_reason"));
        assertEquals("fabric_chatclef_get_acquire_delta", data.get("effect_profile_id"));
        assertEquals(1, data.get("effect_profile_version"));
        assertEquals("get_acquisition_delta", data.get("effect_kind"));
        Map<?, ?> effect = (Map<?, ?>) data.get("effect_payload");
        assertEquals("diamond_pickaxe", effect.get("target_item"));
        assertEquals("ACQUIRE_DELTA", effect.get("quantity_semantics"));
        assertEquals(1, effect.get("requested_delta"));
        assertEquals("diamond_pickaxe", data.get("target_item"));
        assertEquals(1, data.get("requested_count"));
        assertFalse("".equals(data.get("effect_observation_status")));
    }

    @Test
    void classifierDoesNotProjectEffectForNonmatchingTaskCompletion() {
        FabricChatClefCommandTerminalDecision decision = classify(
                "get diamond_pickaxe 1",
                new IdleTask(),
                new IdleTask()
        );
        Map<String, Object> result = decision.result().toMap();
        Map<?, ?> data = data(result);

        assertTrue(decision.terminal());
        assertEquals("task_finished_event_identity_mismatch", decision.reason());
        assertEquals("unknown", result.get("status"));
        assertEquals("task_identity_mismatch", data.get("result_reason"));
        assertFalse(data.containsKey("effect_kind"));
        assertFalse(data.containsKey("effect_observation_status"));
    }

    @Test
    void classifierProjectsNestedEffectForAnySingleCatalogueTarget() {
        IdleTask root = new IdleTask();
        FabricChatClefCommandTerminalDecision decision = classify(
                "get iron_pickaxe 2",
                root,
                root
        );
        Map<?, ?> data = data(decision.result().toMap());
        Map<?, ?> effect = (Map<?, ?>) data.get("effect_payload");

        assertTrue(decision.terminal());
        assertEquals("matching_task_finished", decision.reason());
        assertEquals("fabric_chatclef_get_acquire_delta", data.get("effect_profile_id"));
        assertEquals("get_acquisition_delta", data.get("effect_kind"));
        assertEquals("iron_pickaxe", effect.get("target_item"));
        assertEquals(2, effect.get("requested_delta"));
        assertFalse(data.containsKey("target_item"));
        assertFalse(data.containsKey("effect_observation_status"));
    }

    @Test
    void atPrefixedRawRequestProjectsFromItsNormalizedOakLogCommand() {
        IdleTask root = new IdleTask();
        FabricChatClefCommandTerminalDecision decision = classify(
                "get oak_log 2",
                root,
                root
        );
        Map<String, Object> result = decision.result().toMap();
        Map<?, ?> data = data(result);
        Map<?, ?> effect = (Map<?, ?>) data.get("effect_payload");
        Map<?, ?> ownership = (Map<?, ?>) data.get("ownership");

        assertEquals("req-effect", result.get("request_id"));
        assertEquals("matching_task_finished", decision.reason());
        assertEquals("oak_log", effect.get("target_item"));
        assertEquals(2, effect.get("requested_delta"));
        assertEquals(2, effect.get("target_count_delta"));
        assertEquals("req-effect", ownership.get("request_id"));
        assertEquals("corr-effect", ownership.get("correlation_id"));
        assertEquals("session-effect", ownership.get("session_id"));
        assertEquals(1L, ownership.get("connection_generation"));
        assertEquals("callback_plus_matching_user_task_event", data.get("result_fidelity"));
    }

    @Test
    void classifierProjectsTheWholeDynamicCatalogueGroupMatchSet() {
        IdleTask root = new IdleTask();
        FabricChatClefCommandTerminalDecision decision = classify(
                "get log 3",
                root,
                root
        );
        Map<?, ?> data = data(decision.result().toMap());
        Map<?, ?> effect = (Map<?, ?>) data.get("effect_payload");

        assertEquals("matching_task_finished", decision.reason());
        assertEquals("log", effect.get("target_item"));
        assertEquals(
                java.util.List.of("minecraft:oak_log", "minecraft:spruce_log"),
                effect.get("target_match_ids")
        );
        assertEquals(3, effect.get("requested_delta"));
        assertEquals(3, effect.get("target_count_delta"));
        assertFalse(data.containsKey("target_item"));
        assertFalse(data.containsKey("effect_observation_status"));
    }

    @Test
    void classifierDoesNotProjectEffectForUnsupportedCommandFamily() {
        IdleTask root = new IdleTask();
        FabricChatClefCommandTerminalDecision decision = classify(
                "deposit diamond_pickaxe 2",
                root,
                root
        );
        Map<?, ?> data = data(decision.result().toMap());

        assertTrue(decision.terminal());
        assertEquals("matching_task_finished", decision.reason());
        assertFalse(data.containsKey("effect_profile_id"));
        assertFalse(data.containsKey("effect_kind"));
        assertFalse(data.containsKey("effect_payload"));
    }

    private static FabricChatClefCommandTerminalDecision classify(
            String command,
            Task boundRoot,
            Task observedTask
    ) {
        FabricChatClefCommandExecution execution = execution(command);
        execution.markFinishCallbackReceived(boundRoot);
        execution.markDispatchReturned(
                evidence(boundRoot),
                FabricChatClefRootOwnershipClassification.COMMAND_OWNED_ROOT
        );
        execution.markTaskFinishedObservation(observation(observedTask));
        return new FabricChatClefCommandOutcomeClassifier().classify(execution);
    }

    private static FabricChatClefCommandExecution execution(String command) {
        FabricChatClefCommandRequest request = new FabricChatClefCommandRequest();
        request.requestId = "req-effect";
        request.command = "@" + command;
        request.source = "test";
        return new FabricChatClefCommandExecution(
                new FabricChatClefCommandContext(
                        request,
                        "corr-effect",
                        "session-effect",
                        1L
                ),
                "@" + command,
                FabricChatClefTaskOwnershipEvidence.empty(),
                FabricChatClefCommandResultEffectProjectionTest::effectTracker
        );
    }

    private static FabricChatClefCommandEffectTracker effectTracker(String command) {
        String prefixless = command.startsWith("@")
                ? command.substring(1)
                : command;
        if (prefixless.startsWith("get diamond_pickaxe")) {
            return FabricChatClefGetItemTestProfiles.authoritativeTracker(
                    command,
                    0,
                    1
            );
        }
        if (prefixless.startsWith("get iron_pickaxe")) {
            return FabricChatClefGetItemTestProfiles.authoritativeTracker(
                    command,
                    0,
                    2
            );
        }
        if (prefixless.startsWith("get oak_log")) {
            return FabricChatClefGetItemTestProfiles.authoritativeTracker(
                    command,
                    0,
                    2
            );
        }
        if (prefixless.startsWith("get log")) {
            return FabricChatClefGetItemTestProfiles.authoritativeTracker(
                    command,
                    0,
                    3
            );
        }
        return FabricChatClefNoEffectTracker.instance();
    }

    private static FabricChatClefCommandTerminationObservation observation(Task task) {
        return FabricChatClefCommandTerminationObservation.fromTaskFinishedEvent(
                new TaskFinishedEvent(0.25, task)
        );
    }

    private static FabricChatClefTaskOwnershipEvidence evidence(Task root) {
        FabricChatClefTaskSnapshot snapshot = FabricChatClefTaskSnapshot.capture(root);
        return FabricChatClefTaskOwnershipEvidence.of(
                root,
                snapshot,
                FabricChatClefTaskOwnershipSnapshot.empty(),
                1000L,
                1000L,
                1L,
                "test"
        );
    }

    private static Map<?, ?> data(Map<String, Object> result) {
        return (Map<?, ?>) result.get("data");
    }
}
