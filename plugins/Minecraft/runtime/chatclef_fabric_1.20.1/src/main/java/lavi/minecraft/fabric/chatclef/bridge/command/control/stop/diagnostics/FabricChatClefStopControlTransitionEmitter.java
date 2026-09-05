package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.diagnostics;

//20260905_kpopmodder: Emit each canonical STOP state boundary once without affecting wire authority.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlBaseRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

import java.util.function.Supplier;

public final class FabricChatClefStopControlTransitionEmitter {
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefStopControlTransitionFormatter formatter;

    public FabricChatClefStopControlTransitionEmitter(FabricChatClefBridgeDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
        this.formatter = new FabricChatClefStopControlTransitionFormatter();
    }

    public void wireResult(
            FabricChatClefStopControlBaseRequest base,
            FabricChatClefCommandResultPayload payload,
            String invalidTargetFieldsMask,
            String controlResultDelivery,
            String javaStopBarrierState,
            String ordinaryOwnerGateState,
            boolean quarantineActive
    ) {
        emit(() -> formatter.wireResult(
                base,
                payload,
                invalidTargetFieldsMask,
                controlResultDelivery,
                javaStopBarrierState,
                ordinaryOwnerGateState,
                quarantineActive
        ));
    }

    public void noWire(
            FabricChatClefStopControlBaseRequest base,
            long javaSocketGeneration,
            String acceptedSessionId,
            Long acceptedServerConnectionGeneration,
            String invalidTargetFieldsMask,
            String diagnosticDisposition,
            String javaStopBarrierState,
            String ordinaryOwnerGateState,
            boolean quarantineActive
    ) {
        emit(() -> formatter.noWire(
                base,
                javaSocketGeneration,
                acceptedSessionId,
                acceptedServerConnectionGeneration,
                invalidTargetFieldsMask,
                diagnosticDisposition,
                javaStopBarrierState,
                ordinaryOwnerGateState,
                quarantineActive
        ));
    }

    public void contextBoundary(
            FabricChatClefStopControlContext context,
            String diagnosticDisposition,
            String javaStopBarrierState,
            String ordinaryOwnerGateState,
            boolean quarantineActive,
            String retirementEvidenceKind
    ) {
        emit(() -> formatter.contextBoundary(
                context,
                diagnosticDisposition,
                javaStopBarrierState,
                ordinaryOwnerGateState,
                quarantineActive,
                retirementEvidenceKind
        ));
    }

    private void emit(Supplier<String> lineFactory) {
        try {
            diagnostics.info(lineFactory.get());
        } catch (Throwable ignored) {
            // Diagnostics must never become STOP execution or release authority.
        }
    }
}
