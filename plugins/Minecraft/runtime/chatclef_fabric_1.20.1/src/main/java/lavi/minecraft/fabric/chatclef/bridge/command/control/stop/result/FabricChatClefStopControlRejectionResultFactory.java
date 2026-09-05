package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result;

//20260905_kpopmodder: Build only no-mutation STOP rejection results.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlBaseRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlTargetScope;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultStatus;

import java.util.Map;

public final class FabricChatClefStopControlRejectionResultFactory {
    private final FabricChatClefStopControlResultProfileValidator validator;
    private final FabricChatClefStopControlResultDataFactory dataFactory;

    public FabricChatClefStopControlRejectionResultFactory(
            FabricChatClefStopControlResultProfileValidator validator,
            FabricChatClefStopControlResultDataFactory dataFactory
    ) {
        this.validator = validator;
        this.dataFactory = dataFactory;
    }

    public FabricChatClefCommandResultPayload create(
            FabricChatClefStopControlBaseRequest base,
            String reason,
            FabricChatClefStopControlTargetScope scope,
            FabricChatClefStopControlRequest request,
            Long verifiedClientTick
    ) {
        validator.validateRejection(base, reason, scope, request, verifiedClientTick);
        boolean deadline = "deadline_exceeded".equals(reason);
        Map<String, Object> data = dataFactory.baseData(base, scope, request);
        data.put("control_outcome", "rejected");
        data.put("control_reason", reason);
        data.put("target_resolution", "not_evaluated");
        dataFactory.putResolvedTarget(data, null);
        data.put("target_state_before", "not_evaluated");
        data.put("target_state_after", "not_evaluated");
        data.put("original_result_delivery", "not_applicable");
        data.put("stop_command_invoked", false);
        data.put("executed_client_tick", null);
        data.put("verified_client_tick", verifiedClientTick);
        return FabricChatClefCommandResultPayload.of(
                base.identity().requestId(),
                deadline
                        ? FabricChatClefCommandResultStatus.DEADLINE_EXCEEDED
                        : FabricChatClefCommandResultStatus.REJECTED,
                deadline ? "deadline_exceeded" : "invalid_request",
                deadline
                        ? "STOP control deadline expired before execution."
                        : "STOP control request was rejected without Minecraft mutation.",
                dataFactory.immutable(data)
        );
    }
}
