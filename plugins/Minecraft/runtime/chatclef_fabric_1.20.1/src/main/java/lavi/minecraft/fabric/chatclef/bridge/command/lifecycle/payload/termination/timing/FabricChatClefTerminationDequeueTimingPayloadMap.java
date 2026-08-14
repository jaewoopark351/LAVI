package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing.dequeue.FabricChatClefTerminationDequeuedAtPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing.dequeue.FabricChatClefTerminationDequeuedClientTickPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing.dequeue.FabricChatClefTerminationObservationAgePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing.dequeue.FabricChatClefTerminationQueueDepthAfterDequeuePayloadMap;

import java.util.Map;

//20260814_kpopmodder: Keep dequeue timing fields separate without changing emitted keys.
public final class FabricChatClefTerminationDequeueTimingPayloadMap {
    private FabricChatClefTerminationDequeueTimingPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            long dequeuedAtMs,
            long dequeuedClientTick,
            long observationAgeMs,
            int queueDepthAfterDequeue
    ) {
        FabricChatClefTerminationDequeuedAtPayloadMap.writeTo(payload, dequeuedAtMs);
        FabricChatClefTerminationDequeuedClientTickPayloadMap.writeTo(payload, dequeuedClientTick);
        FabricChatClefTerminationObservationAgePayloadMap.writeTo(payload, observationAgeMs);
        FabricChatClefTerminationQueueDepthAfterDequeuePayloadMap.writeTo(payload, queueDepthAfterDequeue);
    }
}
