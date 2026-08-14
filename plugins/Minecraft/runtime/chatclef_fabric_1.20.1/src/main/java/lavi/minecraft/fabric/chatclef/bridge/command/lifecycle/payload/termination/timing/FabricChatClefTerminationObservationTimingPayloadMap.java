package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing.observation.FabricChatClefTerminationObservationQueueDepthAfterPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing.observation.FabricChatClefTerminationObservationQueueDepthBeforePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing.observation.FabricChatClefTerminationObservationSequencePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing.observation.FabricChatClefTerminationObservationThreadPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing.observation.FabricChatClefTerminationObservedAtPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing.observation.FabricChatClefTerminationObservedClientTickPayloadMap;

import java.util.Map;

//20260814_kpopmodder: Keep observation capture timing fields separate without changing emitted keys.
public final class FabricChatClefTerminationObservationTimingPayloadMap {
    private FabricChatClefTerminationObservationTimingPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            long observationSequence,
            long observedAtMs,
            long observedClientTick,
            String observationThread,
            int queueDepthBefore,
            int queueDepthAfter
    ) {
        FabricChatClefTerminationObservationSequencePayloadMap.writeTo(payload, observationSequence);
        FabricChatClefTerminationObservedAtPayloadMap.writeTo(payload, observedAtMs);
        FabricChatClefTerminationObservedClientTickPayloadMap.writeTo(payload, observedClientTick);
        FabricChatClefTerminationObservationThreadPayloadMap.writeTo(payload, observationThread);
        FabricChatClefTerminationObservationQueueDepthBeforePayloadMap.writeTo(payload, queueDepthBefore);
        FabricChatClefTerminationObservationQueueDepthAfterPayloadMap.writeTo(payload, queueDepthAfter);
    }
}
