package lavi.minecraft.fabric.chatclef.bridge.transport;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResultSender;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendCompletion;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendStatus;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;
import lavi.minecraft.fabric.chatclef.bridge.protocol.result.FabricChatClefCommandResultEnvelopeFactory;

import java.net.http.WebSocket;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

//20260803_kpopmodder: Keep command_result envelope construction out of the WebSocket bridge listener.
public final class FabricChatClefResultEnvelopeSender implements FabricChatClefCommandResultSender {
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefBridgeJson json;
    private final FabricChatClefCommandResultEnvelopeFactory envelopeFactory = new FabricChatClefCommandResultEnvelopeFactory();
    private final Supplier<WebSocket> socketSupplier;
    private final LongSupplier activeGenerationSupplier;
    private final Consumer<FabricChatClefCommandResultSendCompletion> completionSink;

    public FabricChatClefResultEnvelopeSender(
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefBridgeJson json,
            Supplier<WebSocket> socketSupplier,
            LongSupplier activeGenerationSupplier,
            Consumer<FabricChatClefCommandResultSendCompletion> completionSink
    ) {
        this.diagnostics = diagnostics;
        this.json = json;
        this.socketSupplier = socketSupplier;
        this.activeGenerationSupplier = activeGenerationSupplier;
        this.completionSink = completionSink;
    }

    @Override
    public FabricChatClefCommandResultSendSubmission sendCommandResult(
            FabricChatClefCommandContext context,
            FabricChatClefCommandResultPayload payload
    ) {
        return sendCommandResult(
                context.correlationId(),
                context.sessionId(),
                context.connectionGeneration(),
                payload,
                null
        );
    }

    @Override
    public FabricChatClefCommandResultSendSubmission sendTerminalCommandResult(
            FabricChatClefCommandContext context,
            FabricChatClefCommandResultPayload payload
    ) {
        return sendCommandResult(
                context.correlationId(),
                context.sessionId(),
                context.connectionGeneration(),
                payload,
                context
        );
    }

    public FabricChatClefCommandResultSendSubmission sendCommandResult(
            String correlationId,
            String sessionId,
            long generation,
            FabricChatClefCommandResultPayload payload
    ) {
        return sendCommandResult(correlationId, sessionId, generation, payload, null);
    }

    private FabricChatClefCommandResultSendSubmission sendCommandResult(
            String correlationId,
            String sessionId,
            long generation,
            FabricChatClefCommandResultPayload payload,
            FabricChatClefCommandContext context
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
                return failedSubmission(
                        FabricChatClefCommandResultSendOutcome.failed(
                                FabricChatClefCommandResultSendStatus.NO_SOCKET,
                                "socket is not connected"
                        )
                );
            }
            return failedSubmission(
                    FabricChatClefCommandResultSendOutcome.failed(
                            FabricChatClefCommandResultSendStatus.GENERATION_MISMATCH,
                            "generation=" + generation + " active_generation=" + activeGeneration
                    )
            );
        }
        String message;
        try {
            message = json.encode(envelopeFactory.commandResult(correlationId, sessionId, payload));
        } catch (Exception error) {
            String detail = error.getClass().getSimpleName() + ": " + error.getMessage();
            diagnostics.warn("command_result encode failed " + detail);
            return failedSubmission(
                    FabricChatClefCommandResultSendOutcome.failed(
                            FabricChatClefCommandResultSendStatus.ENCODE_FAILED,
                            detail
                    )
            );
        }
        try {
            socket.sendText(message, true)
                    .whenComplete((ignored, error) -> {
                        FabricChatClefCommandResultSendOutcome outcome;
                        if (error == null) {
                            outcome = FabricChatClefCommandResultSendOutcome.sent();
                        } else {
                            String detail = throwableMessage(error);
                            diagnostics.warn("command_result async send failed " + detail);
                            outcome = FabricChatClefCommandResultSendOutcome.failed(
                                    FabricChatClefCommandResultSendStatus.ASYNC_SEND_FAILED,
                                    detail
                            );
                        }
                        enqueueCompletion(context, outcome);
                    });
            return FabricChatClefCommandResultSendSubmission.accepted();
        } catch (Exception error) {
            String detail = throwableMessage(error);
            diagnostics.warn("command_result send failed " + detail);
            return failedSubmission(
                    FabricChatClefCommandResultSendOutcome.failed(
                            FabricChatClefCommandResultSendStatus.SEND_FAILED,
                            detail
                    )
            );
        }
    }

    private FabricChatClefCommandResultSendSubmission failedSubmission(
            FabricChatClefCommandResultSendOutcome outcome
    ) {
        return FabricChatClefCommandResultSendSubmission.failed(outcome);
    }

    private void enqueueCompletion(
            FabricChatClefCommandContext context,
            FabricChatClefCommandResultSendOutcome outcome
    ) {
        if (context == null || completionSink == null) {
            return;
        }
        completionSink.accept(FabricChatClefCommandResultSendCompletion.of(context, outcome));
    }

    private static String throwableMessage(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null
                ? error.getCause()
                : error;
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
