package lavi.minecraft.fabric.chatclef.bridge.transport;

//20260905_kpopmodder: Verify command-result transport failures and owner-specific async completion routing.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultStatus;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendCompletion;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendStatus;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.net.http.WebSocket;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FabricChatClefResultEnvelopeSenderTest {
    private static final long ACTIVE_GENERATION = 7L;

    @Test
    void rejectsResultWhenNoSocketIsConnected() {
        AtomicInteger ordinaryCompletions = new AtomicInteger();
        FabricChatClefResultEnvelopeSender sender = sender(
                new FabricChatClefBridgeJson(),
                null,
                ACTIVE_GENERATION,
                ignored -> ordinaryCompletions.incrementAndGet()
        );

        FabricChatClefCommandResultSendSubmission submission =
                sender.sendTerminalCommandResult(context(ACTIVE_GENERATION), payload());

        assertFalse(submission.acceptedForAsyncSend());
        assertEquals(
                FabricChatClefCommandResultSendStatus.NO_SOCKET,
                submission.immediateOutcome().status()
        );
        assertEquals(0, ordinaryCompletions.get());
    }

    @Test
    void rejectsResultWhenConnectionGenerationDoesNotMatch() {
        SocketHarness socket = new SocketHarness();
        FabricChatClefResultEnvelopeSender sender = sender(
                new FabricChatClefBridgeJson(),
                socket.socket(),
                ACTIVE_GENERATION,
                ignored -> {
                }
        );

        FabricChatClefCommandResultSendSubmission submission =
                sender.sendTerminalCommandResult(context(ACTIVE_GENERATION - 1L), payload());

        assertFalse(submission.acceptedForAsyncSend());
        assertEquals(
                FabricChatClefCommandResultSendStatus.GENERATION_MISMATCH,
                submission.immediateOutcome().status()
        );
        assertEquals(0, socket.sendCount());
    }

    @Test
    void reportsEncodeFailureBeforeWebSocketSubmission() {
        SocketHarness socket = new SocketHarness();
        FabricChatClefResultEnvelopeSender sender = sender(
                null,
                socket.socket(),
                ACTIVE_GENERATION,
                ignored -> {
                }
        );

        FabricChatClefCommandResultSendSubmission submission =
                sender.sendTerminalCommandResult(context(ACTIVE_GENERATION), payload());

        assertFalse(submission.acceptedForAsyncSend());
        assertEquals(
                FabricChatClefCommandResultSendStatus.ENCODE_FAILED,
                submission.immediateOutcome().status()
        );
        assertEquals(0, socket.sendCount());
    }

    @Test
    void reportsSynchronousWebSocketSubmissionFailure() {
        SocketHarness socket = new SocketHarness();
        socket.failSynchronously(new IllegalStateException("sync-boom"));
        FabricChatClefResultEnvelopeSender sender = sender(
                new FabricChatClefBridgeJson(),
                socket.socket(),
                ACTIVE_GENERATION,
                ignored -> {
                }
        );

        FabricChatClefCommandResultSendSubmission submission =
                sender.sendTerminalCommandResult(context(ACTIVE_GENERATION), payload());

        assertFalse(submission.acceptedForAsyncSend());
        assertEquals(
                FabricChatClefCommandResultSendStatus.SEND_FAILED,
                submission.immediateOutcome().status()
        );
        assertEquals("IllegalStateException: sync-boom", submission.immediateOutcome().message());
    }

    @Test
    void routesSuccessfulTerminalCompletionOnlyToOrdinaryOwner() {
        SocketHarness socket = new SocketHarness();
        AtomicReference<FabricChatClefCommandResultSendCompletion> ordinaryCompletion =
                new AtomicReference<>();
        FabricChatClefResultEnvelopeSender sender = sender(
                new FabricChatClefBridgeJson(),
                socket.socket(),
                ACTIVE_GENERATION,
                ordinaryCompletion::set
        );
        FabricChatClefCommandContext context = context(ACTIVE_GENERATION);

        FabricChatClefCommandResultSendSubmission submission =
                sender.sendTerminalCommandResult(context, payload());
        socket.completeSuccessfully();

        assertTrue(submission.acceptedForAsyncSend());
        FabricChatClefCommandResultSendCompletion completion = ordinaryCompletion.get();
        assertNotNull(completion);
        assertSame(context, completion.context());
        assertEquals(FabricChatClefCommandResultSendStatus.SENT, completion.outcome().status());
    }

    @Test
    void routesFailedStopCompletionOnlyToStopOwner() {
        SocketHarness socket = new SocketHarness();
        AtomicInteger ordinaryCompletions = new AtomicInteger();
        AtomicReference<FabricChatClefCommandResultSendOutcome> stopCompletion =
                new AtomicReference<>();
        FabricChatClefResultEnvelopeSender sender = sender(
                new FabricChatClefBridgeJson(),
                socket.socket(),
                ACTIVE_GENERATION,
                ignored -> ordinaryCompletions.incrementAndGet()
        );

        FabricChatClefCommandResultSendSubmission submission = sender.sendStopControlResult(
                "stop-correlation",
                "session-a",
                ACTIVE_GENERATION,
                payload(),
                stopCompletion::set
        );
        socket.completeExceptionally(
                new CompletionException(new IllegalStateException("async-boom"))
        );

        assertTrue(submission.acceptedForAsyncSend());
        FabricChatClefCommandResultSendOutcome outcome = stopCompletion.get();
        assertNotNull(outcome);
        assertEquals(FabricChatClefCommandResultSendStatus.ASYNC_SEND_FAILED, outcome.status());
        assertEquals("IllegalStateException: async-boom", outcome.message());
        assertEquals(0, ordinaryCompletions.get());
    }

    private FabricChatClefResultEnvelopeSender sender(
            FabricChatClefBridgeJson json,
            WebSocket socket,
            long activeGeneration,
            java.util.function.Consumer<FabricChatClefCommandResultSendCompletion> completionSink
    ) {
        return new FabricChatClefResultEnvelopeSender(
                new FabricChatClefBridgeDiagnostics(),
                json,
                () -> socket,
                () -> activeGeneration,
                completionSink
        );
    }

    private FabricChatClefCommandContext context(long generation) {
        return new FabricChatClefCommandContext(
                null,
                "correlation-a",
                "session-a",
                generation
        );
    }

    private FabricChatClefCommandResultPayload payload() {
        return FabricChatClefCommandResultPayload.of(
                "request-a",
                FabricChatClefCommandResultStatus.COMPLETED,
                null,
                "done",
                Collections.emptyMap()
        );
    }

    private static final class SocketHarness {
        private final CompletableFuture<WebSocket> sendFuture = new CompletableFuture<>();
        private final AtomicInteger sendCount = new AtomicInteger();
        private final AtomicReference<RuntimeException> synchronousFailure = new AtomicReference<>();
        private final WebSocket socket;

        private SocketHarness() {
            socket = (WebSocket) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{WebSocket.class},
                    (proxy, method, arguments) -> {
                        if ("sendText".equals(method.getName())) {
                            sendCount.incrementAndGet();
                            RuntimeException failure = synchronousFailure.get();
                            if (failure != null) {
                                throw failure;
                            }
                            return sendFuture;
                        }
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

        private WebSocket socket() {
            return socket;
        }

        private int sendCount() {
            return sendCount.get();
        }

        private void failSynchronously(RuntimeException failure) {
            synchronousFailure.set(failure);
        }

        private void completeSuccessfully() {
            sendFuture.complete(socket);
        }

        private void completeExceptionally(Throwable failure) {
            sendFuture.completeExceptionally(failure);
        }
    }
}
