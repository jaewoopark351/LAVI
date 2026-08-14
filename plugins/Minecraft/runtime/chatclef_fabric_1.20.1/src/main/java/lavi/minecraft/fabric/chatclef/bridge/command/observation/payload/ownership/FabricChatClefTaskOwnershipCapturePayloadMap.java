package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.capture.FabricChatClefTaskOwnershipCaptureAvailablePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.capture.FabricChatClefTaskOwnershipCaptureErrorPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.capture.FabricChatClefTaskOwnershipCaptureThreadPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.capture.FabricChatClefTaskOwnershipCapturedAtPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.capture.FabricChatClefTaskOwnershipCapturedClientTickPayloadMap;

import java.util.Map;

//20260808_kpopmodder: Split task ownership capture fields from the snapshot Map edge without changing keys.
public final class FabricChatClefTaskOwnershipCapturePayloadMap {
    private FabricChatClefTaskOwnershipCapturePayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            boolean available,
            String error,
            long capturedAtMs,
            long capturedClientTick,
            String captureThread
    ) {
        FabricChatClefTaskOwnershipCaptureAvailablePayloadMap.writeTo(payload, available);
        FabricChatClefTaskOwnershipCaptureErrorPayloadMap.writeTo(payload, error);
        FabricChatClefTaskOwnershipCapturedAtPayloadMap.writeTo(payload, capturedAtMs);
        FabricChatClefTaskOwnershipCapturedClientTickPayloadMap.writeTo(payload, capturedClientTick);
        FabricChatClefTaskOwnershipCaptureThreadPayloadMap.writeTo(payload, captureThread);
    }
}
