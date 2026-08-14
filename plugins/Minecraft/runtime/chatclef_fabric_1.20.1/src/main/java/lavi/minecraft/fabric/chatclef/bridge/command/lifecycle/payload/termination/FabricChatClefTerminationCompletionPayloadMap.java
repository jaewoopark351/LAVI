package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.completion.FabricChatClefTerminationCompletionSourcePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.completion.FabricChatClefTerminationKindPayloadMap;

import java.util.Map;

//20260814_kpopmodder: Keep termination completion fields separate without changing emitted keys.
public final class FabricChatClefTerminationCompletionPayloadMap {
    private FabricChatClefTerminationCompletionPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String terminationKind) {
        FabricChatClefTerminationCompletionSourcePayloadMap.writeTo(payload);
        FabricChatClefTerminationKindPayloadMap.writeTo(payload, terminationKind);
    }
}
