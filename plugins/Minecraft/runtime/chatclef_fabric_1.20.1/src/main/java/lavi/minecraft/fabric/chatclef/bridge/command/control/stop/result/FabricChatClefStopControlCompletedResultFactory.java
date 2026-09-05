package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result;

//20260905_kpopmodder: Build only verified successful STOP results.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefOrdinaryCommandStopCapture;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlTargetScope;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultStatus;

import java.util.Map;

public final class FabricChatClefStopControlCompletedResultFactory {
    private final FabricChatClefStopControlResultProfileValidator validator;
    private final FabricChatClefStopControlResultDataFactory dataFactory;

    public FabricChatClefStopControlCompletedResultFactory(
            FabricChatClefStopControlResultProfileValidator validator,
            FabricChatClefStopControlResultDataFactory dataFactory
    ) {
        this.validator = validator;
        this.dataFactory = dataFactory;
    }

    public FabricChatClefCommandResultPayload create(
            FabricChatClefStopControlContext context,
            long verifiedClientTick
    ) {
        FabricChatClefOrdinaryCommandStopCapture capture = validator.validateCompleted(context, verifiedClientTick);
        String reason = successReason(context.request(), context.targetResolution(), capture.state());
        Map<String, Object> data = dataFactory.baseData(
                context.request().base(),
                context.request().targetScope(),
                context.request()
        );
        data.put("control_outcome", "stopped");
        data.put("control_reason", reason);
        data.put("target_resolution", context.targetResolution());
        dataFactory.putResolvedTarget(data, capture.context());
        data.put("target_state_before", capture.state());
        data.put("target_state_after", capture.absent() ? "none" : "retired");
        data.put("original_result_delivery", capture.absent() ? "not_applicable" : "sent");
        data.put("stop_command_invoked", true);
        data.put("executed_client_tick", context.executedClientTick());
        data.put("verified_client_tick", verifiedClientTick);
        return FabricChatClefCommandResultPayload.of(
                context.request().identity().requestId(),
                FabricChatClefCommandResultStatus.COMPLETED,
                null,
                "Registered ChatClef stop command completed with verified retirement.",
                dataFactory.immutable(data)
        );
    }

    private String successReason(
            FabricChatClefStopControlRequest request,
            String resolution,
            String state
    ) {
        if (request.targetScope() == FabricChatClefStopControlTargetScope.TRACKED_COMMAND) {
            if ("none".equals(resolution)) {
                return "tracked_target_absent_global_stop_executed";
            }
            if ("exact".equals(resolution)) {
                return "pending".equals(state) ? "tracked_pending_stopped" : "tracked_active_stopped";
            }
            return "pending".equals(state)
                    ? "tracked_target_replaced_current_pending_stopped"
                    : "tracked_target_replaced_current_active_stopped";
        }
        if ("none".equals(resolution)) {
            return "global_stop_executed_no_lavi_context";
        }
        return "pending".equals(state) ? "global_pending_stopped" : "global_active_stopped";
    }
}
