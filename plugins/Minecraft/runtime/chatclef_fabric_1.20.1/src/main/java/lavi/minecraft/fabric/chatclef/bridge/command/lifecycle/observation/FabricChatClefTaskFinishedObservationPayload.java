package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.observation;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.FabricChatClefCommandTerminationObservationPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.observation.payload.FabricChatClefTaskFinishedObservationPayloadMap;

import java.util.Map;

//20260807_kpopmodder: Keep optional task-finished observation payload typed before lifecycle result serialization.
public final class FabricChatClefTaskFinishedObservationPayload {
    private final FabricChatClefCommandTerminationObservationPayload observation;

    private FabricChatClefTaskFinishedObservationPayload(
            FabricChatClefCommandTerminationObservationPayload observation
    ) {
        this.observation = observation;
    }

    public static FabricChatClefTaskFinishedObservationPayload from(
            FabricChatClefCommandTerminationObservation observation
    ) {
        return new FabricChatClefTaskFinishedObservationPayload(
                observation == null ? null : observation.payload()
        );
    }

    public static FabricChatClefTaskFinishedObservationPayload requiredFrom(
            FabricChatClefCommandTerminationObservation observation
    ) {
        return new FabricChatClefTaskFinishedObservationPayload(observation.payload());
    }

    public static FabricChatClefTaskFinishedObservationPayload empty() {
        return new FabricChatClefTaskFinishedObservationPayload(null);
    }

    public static FabricChatClefTaskFinishedObservationPayload orEmpty(
            FabricChatClefTaskFinishedObservationPayload observation
    ) {
        return observation == null ? empty() : observation;
    }

    public boolean received() {
        return observation != null;
    }

    public Map<String, Object> toMap() {
        return FabricChatClefTaskFinishedObservationPayloadMap.toMap(observation);
    }
}
