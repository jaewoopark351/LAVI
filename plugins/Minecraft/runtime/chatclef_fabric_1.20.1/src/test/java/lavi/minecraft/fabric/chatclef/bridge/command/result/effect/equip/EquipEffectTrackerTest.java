package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.observation.EquipSlotObservation;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class EquipEffectTrackerTest {
    static FabricChatClefCommandContext context() {
        var request = new FabricChatClefCommandRequest();
        request.requestId = "equip-req";
        request.command = "equip [helmet,shield]";
        return new FabricChatClefCommandContext(request, "equip-corr", "equip-session", 2, 3);
    }
    static Map<String, Object> base(FabricChatClefCommandContext context) {
        var base = new HashMap<String, Object>();
        base.put("ownership", context.ownershipPayload().toMap());
        base.put("result_reason", "matching_task_finished");
        base.put("result_fidelity", "callback_plus_matching_user_task_event");
        base.put("dispatch_returned", true);
        base.put("finish_callback_received", true);
        base.put("task_finished_event_received", true);
        base.put("failure_type", "");
        base.put("bound_root_task", Map.of("available", true, "class_name", "adris.altoclef.tasks.misc.EquipArmorTask", "identity", "a1b2"));
        return base;
    }
    static Map<?, ?> effect(FabricChatClefCommandResultDataPayload value) { return (Map<?, ?>) value.toMap().get("effect_payload"); }
    @Test void realPayloadPreservesTerminalProofAndCachesOneReadAndOneLogPerBoundary() {
        var context = context(); var reads = new AtomicInteger();
        PrintStream previous = System.out; var bytes = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));
            var tracker = new EquipEffectTracker(EquipEffectEvidenceTest.profile(), context, () ->
                    EquipEffectEvidenceTest.slots(10 + reads.getAndIncrement(), reads.get() == 1 ? Map.of()
                            : Map.of("head", "minecraft:iron_helmet", "offhand", "minecraft:shield")));
            var payload = tracker.fromMatchingCompletion(FabricChatClefCommandResultDataPayload.fromMap(base(context)));
            assertSame(payload, tracker.fromMatchingCompletion(FabricChatClefCommandResultDataPayload.empty()));
            assertEquals(2, reads.get());
            assertEquals("satisfied", effect(payload).get("effect_observation_status"));
            assertEquals("matching_task_finished", payload.toMap().get("result_reason"));
            assertEquals("equip-req", effect(payload).get("request_id"));
            assertEquals("a1b2", effect(payload).get("task_identity"));
        } finally { System.setOut(previous); }
        String output = bytes.toString(StandardCharsets.UTF_8);
        assertEquals(2, output.lines().count());
        assertTrue(output.contains("[LAVI Fabric ChatClef Bridge] equip_effect phase=capture"));
        assertTrue(output.contains("phase=terminal request=equip-req session=equip-session"));
        assertTrue(output.contains("task=a1b2 status=satisfied reason=all_targets_equipped"));
        assertTrue(output.contains("head=minecraft:iron_helmet:1"));
    }
    @Test void staleRequestSessionGenerationAndDetachedContextRejectBeforeTerminalRead() {
        for (String changed : new String[]{"request_id", "session_id", "connection_generation", "detached"}) {
            var context = context(); var reads = new AtomicInteger();
            var tracker = new EquipEffectTracker(EquipEffectEvidenceTest.profile(), context, () -> { reads.incrementAndGet(); return EquipEffectEvidenceTest.slots(10, Map.of()); });
            var data = base(context);
            var owner = new HashMap<String, Object>(context.ownershipPayload().toMap());
            owner.put(changed, changed.equals("detached") ? true : "mismatch"); data.put("ownership", owner);
            assertEquals("unavailable", effect(tracker.fromMatchingCompletion(FabricChatClefCommandResultDataPayload.fromMap(data))).get("effect_observation_status"));
            assertEquals(1, reads.get());
        }
        var context = context(); var tracker = new EquipEffectTracker(EquipEffectEvidenceTest.profile(), context, () -> EquipEffectEvidenceTest.slots(10, Map.of()));
        context.markDetached("replaced");
        assertEquals("context_detached", effect(tracker.fromMatchingCompletion(FabricChatClefCommandResultDataPayload.fromMap(base(context)))).get("effect_observation_reason"));
    }
    @Test void callbackOnlyFailureAndMissingTaskCannotAuthorizeSlotSuccess() {
        for (String key : new String[]{"finish_callback_received", "task_finished_event_received", "dispatch_returned", "bound_root_task", "failure_type", "result_reason"}) {
            var context = context(); var tracker = new EquipEffectTracker(EquipEffectEvidenceTest.profile(), context, () -> EquipEffectEvidenceTest.slots(10, Map.of()));
            var data = base(context); data.remove(key);
            assertEquals("terminal_binding_mismatch", effect(tracker.fromMatchingCompletion(FabricChatClefCommandResultDataPayload.fromMap(data))).get("effect_observation_reason"));
        }
    }
    @Test void readerAndProductionSinkFailuresDoNotEscapeOrFabricateSuccess() {
        var context = context(); PrintStream previous = System.out;
        try {
            System.setOut(new PrintStream(new ByteArrayOutputStream()) { @Override public void println(String line) { throw new IllegalStateException("sink unavailable"); } });
            var tracker = assertDoesNotThrow(() -> new EquipEffectTracker(EquipEffectEvidenceTest.profile(), context,
                    () -> { throw new IllegalStateException("reader unavailable"); }));
            var payload = assertDoesNotThrow(() -> tracker.fromMatchingCompletion(FabricChatClefCommandResultDataPayload.fromMap(base(context))));
            assertEquals("unavailable", effect(payload).get("effect_observation_status"));
        } finally { System.setOut(previous); }
    }
    @Test void nullReaderResultDegradesInsteadOfReturningMissingPayload() {
        var context = context(); var tracker = new EquipEffectTracker(EquipEffectEvidenceTest.profile(), context, () -> null);
        assertEquals("unavailable", effect(tracker.fromMatchingCompletion(FabricChatClefCommandResultDataPayload.fromMap(base(context)))).get("effect_observation_status"));
    }
}
