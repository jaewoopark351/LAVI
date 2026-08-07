package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.observation.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Folderized optional task-finished observation Map serialization without changing emitted fields.
public final class FabricChatClefTaskFinishedObservationPayloadMap {
    private FabricChatClefTaskFinishedObservationPayloadMap() {
    }

    public static Map<String, Object> toMap(FabricChatClefCommandTerminationObservation observation) {
        return observation == null ? new HashMap<>() : observation.toMap();
    }
}
