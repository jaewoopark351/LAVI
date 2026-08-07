package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.observation;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.observation.payload.FabricChatClefTaskFinishedObservationPayloadMap;

import java.util.Map;

//20260807_kpopmodder: Keep optional task-finished observation payload typed before lifecycle result serialization.
public final class FabricChatClefTaskFinishedObservationPayload {
    private final FabricChatClefCommandTerminationObservation observation;

    private FabricChatClefTaskFinishedObservationPayload(FabricChatClefCommandTerminationObservation observation) {
        this.observation = observation;
    }

    public static FabricChatClefTaskFinishedObservationPayload from(
            FabricChatClefCommandTerminationObservation observation
    ) {
        return new FabricChatClefTaskFinishedObservationPayload(observation);
    }

    public boolean received() {
        return observation != null;
    }

    public Map<String, Object> toMap() {
        return FabricChatClefTaskFinishedObservationPayloadMap.toMap(observation);
    }
}
