package lavi.minecraft.fabric.chatclef.bridge.command.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.FabricChatClefNoEffectTracker;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;
import lavi.minecraft.fabric.chatclef.bridge.transport.result.FabricChatClefResultEnvelopeBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Pin the real initial-running serializer to one shared Java/Python wire fixture.
class FabricChatClefInitialRunningCommandResultFixtureTest {
    private static final String FIXTURE_PATH =
            "tests/minecraft_chatclef/command_lifecycle/fixtures/"
                    + "fabric_chatclef_initial_running_command_result_v1.json";
    private static final long FIXTURE_TIME_MS = 1_700_000_000_000L;

    @Test
    void initialRunningSerializationMatchesSharedFixtureWithEvidenceSequenceOne() throws IOException {
        FabricChatClefBridgeJson json = new FabricChatClefBridgeJson();
        JsonNode expected = json.decodeTree(Files.readString(locateFixture()));
        FabricChatClefCommandExecution execution = execution();
        JsonNode actual = json.decodeTree(json.encode(serializedEnvelope(execution)));

        normalizeRuntimeOwnedValues(actual);

        assertEquals(expected, actual);
        assertEquals(2, execution.nextLifecycleEvidenceSequence());
    }

    private static FabricChatClefCommandExecution execution() {
        FabricChatClefCommandRequest request = new FabricChatClefCommandRequest();
        request.requestId = "request-fixture";
        request.command = "get diamond_pickaxe 1";
        request.source = "lavi_gui";
        FabricChatClefCommandContext context = new FabricChatClefCommandContext(
                request,
                "command-message-fixture",
                "session-fixture",
                1L
        );
        return new FabricChatClefCommandExecution(
                context,
                "@get diamond_pickaxe 1",
                FabricChatClefTaskOwnershipEvidence.empty(),
                ignored -> FabricChatClefNoEffectTracker.instance()
        );
    }

    private static FabricChatClefBridgeEnvelope serializedEnvelope(
            FabricChatClefCommandExecution execution
    ) {
        FabricChatClefCommandContext context = execution.context();
        return new FabricChatClefResultEnvelopeBuilder().build(
                context.correlationId(),
                context.sessionId(),
                execution.runningResult()
        );
    }

    private static void normalizeRuntimeOwnedValues(JsonNode value) {
        ObjectNode envelope = requireObject(value, "envelope");
        String messageId = envelope.path("message_id").textValue();
        assertTrue(
                messageId != null && messageId.matches(
                        "fabric-chatclef-result-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-"
                                + "[0-9a-f]{4}-[0-9a-f]{12}"
                ),
                "initial result must use the production result-envelope identity"
        );
        assertTrue(envelope.path("timestamp_ms").isIntegralNumber());
        assertTrue(envelope.path("timestamp_ms").longValue() > 0L);

        ObjectNode data = requireObject(envelope.path("payload").path("data"), "payload.data");
        assertTrue(data.path("dispatch_started_ms").isIntegralNumber());
        assertTrue(data.path("dispatch_started_ms").longValue() > 0L);
        assertTrue(data.path("dispatch_thread").isTextual());
        assertTrue(!data.path("dispatch_thread").textValue().isBlank());
        assertTrue(data.path("evidence_sequence").isIntegralNumber());
        assertEquals(1, data.path("evidence_sequence").intValue());

        ObjectNode ownership = requireObject(data.path("ownership"), "payload.data.ownership");
        assertTrue(ownership.path("accepted_at_ms").isIntegralNumber());
        assertTrue(ownership.path("accepted_at_ms").longValue() > 0L);

        envelope.put("message_id", "fabric-chatclef-result-fixture");
        envelope.put("timestamp_ms", FIXTURE_TIME_MS);
        data.put("dispatch_started_ms", FIXTURE_TIME_MS);
        data.put("dispatch_thread", "fixture-thread");
        ownership.put("accepted_at_ms", FIXTURE_TIME_MS);
    }

    private static ObjectNode requireObject(JsonNode value, String field) {
        assertTrue(value.isObject(), field + " must be a JSON object");
        return (ObjectNode) value;
    }

    private static Path locateFixture() throws IOException {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null) {
            Path candidate = current.resolve(FIXTURE_PATH);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IOException("Unable to locate shared protocol fixture: " + FIXTURE_PATH);
    }
}
