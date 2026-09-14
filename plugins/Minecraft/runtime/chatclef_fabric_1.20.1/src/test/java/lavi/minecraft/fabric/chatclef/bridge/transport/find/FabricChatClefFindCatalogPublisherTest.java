//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.transport.find;

import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.*;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;
import lavi.minecraft.fabric.chatclef.bridge.transport.connection.FabricChatClefWebSocketConnectionState;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;
import lavi.minecraft.find.catalog.FindCatalogSnapshot;
import lavi.minecraft.find.catalog.FindCatalogRecord;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.net.http.WebSocket;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Exercise real catalog encoding, bounded asynchronous submission and formatted output without a network or game.
class FabricChatClefFindCatalogPublisherTest {
    private static final class Fixture {
        final List<String> sent = new ArrayList<>();
        final List<CompletableFuture<WebSocket>> futures = new ArrayList<>();
        final AtomicLong clock = new AtomicLong();
        final AtomicReference<FindCatalogSnapshot> catalog = new AtomicReference<>(snapshot(1));
        final FabricChatClefFindCatalogPublisher publisher;
        final WebSocket socket;
        Fixture() {
            socket = (WebSocket) Proxy.newProxyInstance(WebSocket.class.getClassLoader(), new Class<?>[]{WebSocket.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("sendText")) {
                            sent.add(args[0].toString());
                            var future = new CompletableFuture<WebSocket>(); futures.add(future); return future;
                        }
                        if (method.getReturnType() == boolean.class) return false;
                        if (method.getReturnType() == String.class) return "";
                        return null;
                    });
            var diagnostics = new FabricChatClefBridgeDiagnostics();
            var connection = new FabricChatClefWebSocketConnectionState();
            long generation = connection.acceptOpen(socket);
            var session = new FabricChatClefSessionGuard(new FabricChatClefBridgeState(), diagnostics);
            session.beginHandshake("handshake", generation);
            var ack = new FabricChatClefBridgeEnvelope();
            ack.messageType = "handshake_ack"; ack.correlationId = "handshake"; ack.sessionId = "test-session";
            ack.payload = Map.of("accepted", true, "session_id", "test-session", "connection_generation", 41L);
            assertTrue(session.acceptHandshake(ack, generation));
            publisher = new FabricChatClefFindCatalogPublisher(new FabricChatClefFindCatalogPageSender(connection, session,
                    new FabricChatClefBridgeJson()), catalog::get, diagnostics, clock::get);
        }
        void complete(int index) { futures.get(index).complete(socket); }
    }
    @Test void nullInvalidationRetiresTheLastPublishedResourceGeneration() throws Exception {
        var fixture = new Fixture(); fixture.catalog.set(snapshot(7));
        fixture.publisher.tick(); fixture.complete(0); fixture.publisher.tick();
        fixture.complete(1); fixture.publisher.tick();
        fixture.catalog.set(null); fixture.publisher.tick();
        var event = new FabricChatClefBridgeJson().decode(fixture.sent.get(2));
        assertEquals("find_catalog_invalidated", event.payload.get("event"));
        assertEquals(7L, ((Number)event.payload.get("resource_generation")).longValue());
        assertEquals("CATALOG_INCOMPLETE", event.payload.get("reason"));
    }
    private static FindCatalogSnapshot snapshot(long generation) {
        return FindCatalogSnapshot.create(generation, List.of(new FindCatalogRecord(
                "block", "minecraft:stone", "block.minecraft.stone", "돌", "Stone", "")));
    }
    @Test void pendingPageIsSingularAndTransportCompletionIsNotReceiverAcknowledgement() throws Exception {
        var output = new ByteArrayOutputStream(); var previous = System.out;
        try (var captured = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            var fixture = new Fixture(); fixture.publisher.tick(); fixture.publisher.tick();
            assertEquals(1, fixture.sent.size());
            fixture.complete(0); fixture.publisher.tick();
            assertEquals(2, fixture.sent.size());
            var page = new FabricChatClefBridgeJson().decode(fixture.sent.get(1));
            assertEquals("event", page.messageType);
            assertEquals("find_catalog_page", page.payload.get("event"));
            assertEquals(41, ((Number)page.payload.get("connection_generation")).intValue());
            assertEquals(1, page.payload.get("record_count"));
            fixture.complete(1); fixture.publisher.tick(); fixture.publisher.tick();
            assertEquals(2, fixture.sent.size());
        } finally { System.setOut(previous); }
        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("FIND_CATALOG_EXCHANGE_COMPLETED"));
        assertTrue(text.contains("not_receiver_ack"));
        assertTrue(text.contains("resourceGeneration=1"));
    }
    @Test void resourceReplacementWaitsForOwnedPendingSendThenPublishesNewInvalidation() throws Exception {
        var fixture = new Fixture(); fixture.publisher.tick();
        fixture.catalog.set(snapshot(2)); fixture.publisher.tick();
        assertEquals(1, fixture.sent.size());
        fixture.complete(0); fixture.publisher.tick();
        assertEquals(2, fixture.sent.size());
        var replacement = new FabricChatClefBridgeJson().decode(fixture.sent.get(1));
        assertEquals("find_catalog_invalidated", replacement.payload.get("event"));
        assertEquals(2, ((Number)replacement.payload.get("resource_generation")).intValue());
    }
    @Test void transportFailureAndDeadlineHaveNoUnboundedAutomaticRetry() {
        var fixture = new Fixture(); fixture.publisher.tick();
        fixture.futures.get(0).completeExceptionally(new IllegalStateException("test-sink"));
        for (int index = 0; index < 100; index++) fixture.publisher.tick();
        assertEquals(1, fixture.sent.size());
        var deadline = new Fixture(); deadline.publisher.tick();
        deadline.clock.set(10_000_000_000L); deadline.publisher.tick();
        deadline.complete(0);
        for (int index = 0; index < 100; index++) deadline.publisher.tick();
        assertEquals(1, deadline.sent.size());
    }
    @Test void fullEncodedWireBoundPrecedesSubmission() {
        var encoder = new FabricChatClefFindCatalogEnvelopeEncoder(new FabricChatClefBridgeJson());
        assertThrows(IllegalArgumentException.class, () -> encoder.encode("session", 1, "find_catalog_page",
                Map.of("oversized", "x".repeat(65536))));
    }
    @Test void replacementDuringStuckSendStillObservesOriginalExchangeDeadline() {
        var previous = System.out; var output = new ByteArrayOutputStream();
        try (var sink = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(sink);
            var fixture = new Fixture(); fixture.publisher.tick(); fixture.catalog.set(snapshot(2));
            fixture.clock.set(10_000_000_000L);
            for (int tick = 0; tick < 100; tick++) fixture.publisher.tick();
            assertEquals(1, fixture.sent.size());
        } finally { System.setOut(previous); }
        String text = output.toString(StandardCharsets.UTF_8);
        assertEquals(1, text.split("replacement_wait_deadline", -1).length - 1);
    }
}
//#endif
