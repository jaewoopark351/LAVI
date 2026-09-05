package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result;

//20260905_kpopmodder: Build only fail-closed STOP uncertainty results.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefOrdinaryCommandStopCapture;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultStatus;

import java.util.Map;

public final class FabricChatClefStopControlUnknownResultFactory {
    private final FabricChatClefStopControlResultProfileValidator validator;
    private final FabricChatClefStopControlResultDataFactory dataFactory;

    public FabricChatClefStopControlUnknownResultFactory(
            FabricChatClefStopControlResultProfileValidator validator,
            FabricChatClefStopControlResultDataFactory dataFactory
    ) {
        this.validator = validator;
        this.dataFactory = dataFactory;
    }

    public FabricChatClefCommandResultPayload create(
            FabricChatClefStopControlContext context,
            String reason,
            long verifiedClientTick
    ) {
        FabricChatClefOrdinaryCommandStopCapture capture = validator.validateUnknown(
                context,
                reason,
                verifiedClientTick
        );
        boolean canonicalUnknownObservation = "target_observation_failed".equals(reason)
                && (capture == null || !capture.pending());
        String resolution = canonicalUnknownObservation
                ? "unknown"
                : context.targetResolution();
        String stateBefore = canonicalUnknownObservation ? "unknown" : capture.state();
        Map<String, Object> data = dataFactory.baseData(
                context.request().base(),
                context.request().targetScope(),
                context.request()
        );
        data.put("control_outcome", "unknown");
        data.put("control_reason", reason);
        data.put("target_resolution", resolution);
        dataFactory.putResolvedTarget(data, canonicalUnknownObservation ? null : capture.context());
        data.put("target_state_before", stateBefore);
        data.put("target_state_after", "unknown");
        data.put("original_result_delivery", originalDelivery(context, reason));
        data.put("stop_command_invoked", context.stopCommandInvoked());
        data.put("executed_client_tick", context.stopCommandInvoked() ? context.executedClientTick() : null);
        data.put("verified_client_tick", verifiedClientTick);
        String errorCode = "target_observation_failed".equals(reason) || "stop_command_exception".equals(reason)
                ? "internal_error"
                : null;
        return FabricChatClefCommandResultPayload.of(
                context.request().identity().requestId(),
                FabricChatClefCommandResultStatus.UNKNOWN,
                errorCode,
                "STOP control outcome could not be verified safely.",
                dataFactory.immutable(data)
        );
    }

    private String originalDelivery(FabricChatClefStopControlContext context, String reason) {
        if ("original_cancel_send_failed".equals(reason)) {
            return "failed";
        }
        if ("verification_timeout".equals(reason)) {
            FabricChatClefOrdinaryCommandStopCapture capture = context.capture();
            return capture != null && capture.context() != null && capture.context().terminalSent()
                    ? "sent"
                    : "unknown";
        }
        return "not_applicable";
    }
}
