package lavi.minecraft.fabric.chatclef.bridge.transport.connection;

//20260905_kpopmodder: Verify current-socket detach effects and stale-callback quarantine.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefConnectionDetachedEvent;
import lavi.minecraft.fabric.chatclef.bridge.config.FabricChatClefBridgeConfig;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeLifecycleState;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;
import lavi.minecraft.fabric.chatclef.bridge.transport.FabricChatClefReconnectScheduler;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FabricChatClefConnectionDetachHandlerTest {
    @Test
    void staleCloseDoesNotDetachNotifyOrReconnect() {
        try (Fixture fixture = new Fixture()) {
            fixture.handler.onClose(fixture.staleSocket, 1000, "stale-close", fixture::recordReconnect);

            assertSame(fixture.currentSocket, fixture.connectionState.webSocket());
            assertEquals(fixture.generation, fixture.connectionState.activeConnectionGeneration());
            assertTrue(fixture.commandQueue.pollConnectionDetached().isEmpty());
            assertTrue(fixture.sessionGuard.handshakeAccepted());
            assertEquals(FabricChatClefBridgeLifecycleState.CONNECTED, fixture.bridgeState.lifecycleState());
            assertEquals(0, fixture.reconnectCount.get());
        }
    }

    @Test
    void currentCloseDetachesNotifiesOwnersAndReconnects() throws InterruptedException {
        try (Fixture fixture = new Fixture()) {
            fixture.handler.onClose(fixture.currentSocket, 1001, "going-away", fixture::recordReconnect);

            assertTrue(fixture.reconnectObserved.await(2, TimeUnit.SECONDS));
            assertNull(fixture.connectionState.webSocket());
            assertEquals(0L, fixture.connectionState.activeConnectionGeneration());
            FabricChatClefConnectionDetachedEvent event =
                    fixture.commandQueue.pollConnectionDetached().orElseThrow();
            assertEquals(fixture.generation, event.connectionGeneration());
            assertEquals("websocket_closed", event.reason());
            assertFalse(fixture.sessionGuard.handshakeAccepted());
            assertEquals(FabricChatClefBridgeLifecycleState.DISCONNECTED, fixture.bridgeState.lifecycleState());
            assertEquals("closed status=1001 reason=going-away", fixture.bridgeState.lastError().orElseThrow());
            assertEquals(1, fixture.reconnectCount.get());
        }
    }

    @Test
    void staleErrorDoesNotDetachNotifyOrReconnect() {
        try (Fixture fixture = new Fixture()) {
            fixture.handler.onError(
                    fixture.staleSocket,
                    new IllegalStateException("stale-error"),
                    fixture::recordReconnect
            );

            assertSame(fixture.currentSocket, fixture.connectionState.webSocket());
            assertEquals(fixture.generation, fixture.connectionState.activeConnectionGeneration());
            assertTrue(fixture.commandQueue.pollConnectionDetached().isEmpty());
            assertTrue(fixture.sessionGuard.handshakeAccepted());
            assertEquals(FabricChatClefBridgeLifecycleState.CONNECTED, fixture.bridgeState.lifecycleState());
            assertEquals(0, fixture.reconnectCount.get());
        }
    }

    @Test
    void currentErrorDetachesNotifiesOwnersAndReconnects() throws InterruptedException {
        try (Fixture fixture = new Fixture()) {
            fixture.handler.onError(
                    fixture.currentSocket,
                    new IllegalStateException("current-error"),
                    fixture::recordReconnect
            );

            assertTrue(fixture.reconnectObserved.await(2, TimeUnit.SECONDS));
            assertNull(fixture.connectionState.webSocket());
            assertEquals(0L, fixture.connectionState.activeConnectionGeneration());
            FabricChatClefConnectionDetachedEvent event =
                    fixture.commandQueue.pollConnectionDetached().orElseThrow();
            assertEquals(fixture.generation, event.connectionGeneration());
            assertEquals("websocket_error", event.reason());
            assertFalse(fixture.sessionGuard.handshakeAccepted());
            assertEquals(FabricChatClefBridgeLifecycleState.FAILED, fixture.bridgeState.lifecycleState());
            assertEquals("IllegalStateException: current-error", fixture.bridgeState.lastError().orElseThrow());
            assertEquals(1, fixture.reconnectCount.get());
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final FabricChatClefCommandQueue commandQueue = new FabricChatClefCommandQueue();
        private final FabricChatClefBridgeState bridgeState = new FabricChatClefBridgeState();
        private final FabricChatClefBridgeDiagnostics diagnostics = new FabricChatClefBridgeDiagnostics();
        private final FabricChatClefSessionGuard sessionGuard =
                new FabricChatClefSessionGuard(bridgeState, diagnostics);
        private final FabricChatClefWebSocketConnectionState connectionState =
                new FabricChatClefWebSocketConnectionState();
        private final FabricChatClefReconnectScheduler reconnectScheduler =
                new FabricChatClefReconnectScheduler();
        private final AtomicInteger reconnectCount = new AtomicInteger();
        private final CountDownLatch reconnectObserved = new CountDownLatch(1);
        private final WebSocket currentSocket = socket();
        private final WebSocket staleSocket = socket();
        private final long generation;
        private final FabricChatClefConnectionDetachHandler handler;

        private Fixture() {
            assertTrue(connectionState.beginRunning());
            generation = connectionState.acceptOpen(currentSocket);
            sessionGuard.beginHandshake("handshake-a", generation);
            assertTrue(sessionGuard.acceptHandshake(ack(generation), generation));
            FabricChatClefConnectionReconnectCoordinator reconnectCoordinator =
                    new FabricChatClefConnectionReconnectCoordinator(
                            new FabricChatClefBridgeConfig(
                                    URI.create("ws://127.0.0.1:4316"),
                                    Duration.ZERO
                            ),
                            reconnectScheduler,
                            connectionState
                    );
            handler = new FabricChatClefConnectionDetachHandler(
                    commandQueue,
                    bridgeState,
                    diagnostics,
                    sessionGuard,
                    connectionState,
                    reconnectCoordinator
            );
        }

        private void recordReconnect() {
            reconnectCount.incrementAndGet();
            reconnectObserved.countDown();
        }

        @Override
        public void close() {
            reconnectScheduler.stop();
        }

        private static FabricChatClefBridgeEnvelope ack(long javaGeneration) {
            FabricChatClefBridgeEnvelope envelope = new FabricChatClefBridgeEnvelope();
            envelope.messageType = "handshake_ack";
            envelope.correlationId = "handshake-a";
            envelope.sessionId = "session-a";
            envelope.payload = new HashMap<>();
            envelope.payload.put("accepted", true);
            envelope.payload.put("session_id", "session-a");
            envelope.payload.put("connection_generation", 41L + javaGeneration);
            return envelope;
        }

        private static WebSocket socket() {
            return (WebSocket) Proxy.newProxyInstance(
                    Fixture.class.getClassLoader(),
                    new Class<?>[]{WebSocket.class},
                    (proxy, method, arguments) -> {
                        Class<?> returnType = method.getReturnType();
                        if (returnType == boolean.class) {
                            return false;
                        }
                        if (returnType == long.class) {
                            return 0L;
                        }
                        return null;
                    }
            );
        }
    }
}
