package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.diagnostics;

//20260905_kpopmodder: Preserve the canonical STOP diagnostics API as a projection/encoding facade.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlBaseRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;

import java.util.List;

public final class FabricChatClefStopControlTransitionFormatter {
    private final FabricChatClefStopControlTransitionSchema schema;
    private final FabricChatClefStopControlTransitionProjector projector;
    private final FabricChatClefStopControlTransitionLineEncoder encoder;

    public FabricChatClefStopControlTransitionFormatter() {
        this.schema = new FabricChatClefStopControlTransitionSchema();
        this.projector = new FabricChatClefStopControlTransitionProjector(schema);
        this.encoder = new FabricChatClefStopControlTransitionLineEncoder(schema);
    }

    public String wireResult(
            FabricChatClefStopControlBaseRequest base,
            FabricChatClefCommandResultPayload payload,
            String invalidTargetFieldsMask,
            String controlResultDelivery,
            String javaStopBarrierState,
            String ordinaryOwnerGateState,
            boolean quarantineActive
    ) {
        return encoder.encode(projector.wireResult(
                base,
                payload,
                invalidTargetFieldsMask,
                controlResultDelivery,
                javaStopBarrierState,
                ordinaryOwnerGateState,
                quarantineActive
        ));
    }

    public String noWire(
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
        return encoder.encode(projector.noWire(
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

    public String contextBoundary(
            FabricChatClefStopControlContext context,
            String diagnosticDisposition,
            String javaStopBarrierState,
            String ordinaryOwnerGateState,
            boolean quarantineActive,
            String retirementEvidenceKind
    ) {
        return encoder.encode(projector.contextBoundary(
                context,
                diagnosticDisposition,
                javaStopBarrierState,
                ordinaryOwnerGateState,
                quarantineActive,
                retirementEvidenceKind
        ));
    }

    public List<String> canonicalFieldOrder() {
        return schema.fieldOrder();
    }
}
