package lavi.minecraft.fabric.chatclef.bridge.transport;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResultSender;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendStatus;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;
import lavi.minecraft.fabric.chatclef.bridge.protocol.result.FabricChatClefCommandResultEnvelopeFactory;

import java.net.http.WebSocket;
import java.util.concurrent.CompletionException;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

//20260803_kpopmodder: Keep command_result envelope construction out of the WebSocket bridge listener.
public final class FabricChatClefResultEnvelopeSender implements FabricChatClefCommandResultSender {
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefBridgeJson json;
    private final FabricChatClefCommandResultEnvelopeFactory envelopeFactory = new FabricChatClefCommandResultEnvelopeFactory();
    private final Supplier<WebSocket> socketSupplier;
    private final LongSupplier activeGenerationSupplier;

    public FabricChatClefResultEnvelopeSender(
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefBridgeJson json,
            Supplier<WebSocket> socketSupplier,
            LongSupplier activeGenerationSupplier
    ) {
        this.diagnostics = diagnostics;
        this.json = json;
        this.socketSupplier = socketSupplier;
        this.activeGenerationSupplier = activeGenerationSupplier;
    }

    @Override
    public FabricChatClefCommandResultSendOutcome sendCommandResult(
            FabricChatClefCommandContext context,
            FabricChatClefCommandResultPayload payload
    ) {
        return sendCommandResult(
                context.correlationId(),
                context.sessionId(),
                context.connectionGeneration(),
                payload
        );
    }

    public FabricChatClefCommandResultSendOutcome sendCommandResult(
            String correlationId,
            String sessionId,
            long generation,
            FabricChatClefCommandResultPayload payload
    ) {
        WebSocket socket = socketSupplier.get();
        long activeGeneration = activeGenerationSupplier.getAsLong();
        if (socket == null || generation != activeGeneration) {
            diagnostics.warn(
                    "ignored command_result for inactive generation="
                            + generation
                            + " active_generation="
                            + activeGeneration
            );
            if (socket == null) {
                return FabricChatClefCommandResultSendOutcome.failed(
                        FabricChatClefCommandResultSendStatus.NO_SOCKET,
                        "socket is not connected"
                );
            }
            return FabricChatClefCommandResultSendOutcome.failed(
                    FabricChatClefCommandResultSendStatus.GENERATION_MISMATCH,
                    "generation=" + generation + " active_generation=" + activeGeneration
            );
        }
        String message;
        try {
            message = json.encode(envelopeFactory.commandResult(correlationId, sessionId, payload));
        } catch (Exception error) {
            String detail = error.getClass().getSimpleName() + ": " + error.getMessage();
            diagnostics.warn("command_result encode failed " + detail);
            return FabricChatClefCommandResultSendOutcome.failed(
                    FabricChatClefCommandResultSendStatus.ENCODE_FAILED,
                    detail
            );
        }
        try {
            return socket.sendText(message, true)
                    .handle((ignored, error) -> {
                        if (error == null) {
                            return FabricChatClefCommandResultSendOutcome.sent();
                        }
                        String detail = throwableMessage(error);
                        diagnostics.warn("command_result async send failed " + detail);
                        return FabricChatClefCommandResultSendOutcome.failed(
                                FabricChatClefCommandResultSendStatus.ASYNC_SEND_FAILED,
                                detail
                        );
                    })
                    .toCompletableFuture()
                    .join();
        } catch (CompletionException error) {
            String detail = throwableMessage(error);
            diagnostics.warn("command_result send completion failed " + detail);
            return FabricChatClefCommandResultSendOutcome.failed(
                    FabricChatClefCommandResultSendStatus.ASYNC_SEND_FAILED,
                    detail
            );
        } catch (Exception error) {
            String detail = throwableMessage(error);
            diagnostics.warn("command_result send failed " + detail);
            return FabricChatClefCommandResultSendOutcome.failed(
                    FabricChatClefCommandResultSendStatus.SEND_FAILED,
                    detail
            );
        }
    }

    private static String throwableMessage(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null
                ? error.getCause()
                : error;
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
