package lavi.minecraft.fabric.chatclef.bridge.transport;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResultSender;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultSender;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendCompletion;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;
import lavi.minecraft.fabric.chatclef.bridge.transport.result.FabricChatClefResultEnvelopeEncoder;
import lavi.minecraft.fabric.chatclef.bridge.transport.result.FabricChatClefResultEnvelopeEncoding;
import lavi.minecraft.fabric.chatclef.bridge.transport.result.FabricChatClefResultSendAdmission;
import lavi.minecraft.fabric.chatclef.bridge.transport.result.FabricChatClefResultSendAdmissionDecision;
import lavi.minecraft.fabric.chatclef.bridge.transport.result.FabricChatClefResultSendCompletionRouter;
import lavi.minecraft.fabric.chatclef.bridge.transport.result.FabricChatClefResultSendDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.transport.result.FabricChatClefResultTransportSubmission;

import java.net.http.WebSocket;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

//20260803_kpopmodder: Keep command_result envelope construction out of the WebSocket bridge listener.
public final class FabricChatClefResultEnvelopeSender implements
        FabricChatClefCommandResultSender,
        FabricChatClefStopControlResultSender {
    private final Supplier<WebSocket> socketSupplier;
    private final LongSupplier activeGenerationSupplier;
    private final FabricChatClefResultSendAdmission admission;
    private final FabricChatClefResultEnvelopeEncoder encoder;
    private final FabricChatClefResultTransportSubmission transportSubmission;

    public FabricChatClefResultEnvelopeSender(
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefBridgeJson json,
            Supplier<WebSocket> socketSupplier,
            LongSupplier activeGenerationSupplier,
            Consumer<FabricChatClefCommandResultSendCompletion> completionSink
    ) {
        this.socketSupplier = socketSupplier;
        this.activeGenerationSupplier = activeGenerationSupplier;
        FabricChatClefResultSendDiagnostics resultDiagnostics =
                new FabricChatClefResultSendDiagnostics(diagnostics);
        this.admission = new FabricChatClefResultSendAdmission(resultDiagnostics);
        this.encoder = new FabricChatClefResultEnvelopeEncoder(json, resultDiagnostics);
        this.transportSubmission = new FabricChatClefResultTransportSubmission(
                resultDiagnostics,
                new FabricChatClefResultSendCompletionRouter(completionSink)
        );
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
                null,
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
                context,
                null
        );
    }

    public FabricChatClefCommandResultSendSubmission sendCommandResult(
            String correlationId,
            String sessionId,
            long generation,
            FabricChatClefCommandResultPayload payload
    ) {
        return sendCommandResult(correlationId, sessionId, generation, payload, null, null);
    }

    //20260905_kpopmodder: Report STOP async completion to its independent result owner.
    @Override
    public FabricChatClefCommandResultSendSubmission sendStopControlResult(
            String correlationId,
            String sessionId,
            long javaSocketGeneration,
            FabricChatClefCommandResultPayload payload,
            Consumer<FabricChatClefCommandResultSendOutcome> completion
    ) {
        return sendCommandResult(
                correlationId,
                sessionId,
                javaSocketGeneration,
                payload,
                null,
                completion
        );
    }

    private FabricChatClefCommandResultSendSubmission sendCommandResult(
            String correlationId,
            String sessionId,
            long generation,
            FabricChatClefCommandResultPayload payload,
            FabricChatClefCommandContext context,
            Consumer<FabricChatClefCommandResultSendOutcome> stopCompletion
    ) {
        WebSocket socket = socketSupplier.get();
        long activeGeneration = activeGenerationSupplier.getAsLong();
        FabricChatClefResultSendAdmissionDecision admissionDecision =
                admission.evaluate(socket, generation, activeGeneration);
        if (!admissionDecision.isAdmitted()) {
            return admissionDecision.rejection();
        }
        FabricChatClefResultEnvelopeEncoding encoding =
                encoder.encode(correlationId, sessionId, payload);
        if (!encoding.succeeded()) {
            return encoding.failure();
        }
        return transportSubmission.submit(socket, encoding.message(), context, stopCompletion);
    }
}
