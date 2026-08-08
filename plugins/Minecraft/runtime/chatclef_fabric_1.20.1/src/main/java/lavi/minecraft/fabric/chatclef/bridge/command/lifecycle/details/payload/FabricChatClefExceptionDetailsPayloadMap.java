package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.exceptiondetail.FabricChatClefExceptionMessagePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.exceptiondetail.FabricChatClefExceptionTypePayloadMap;

import java.util.HashMap;
import java.util.Map;

public final class FabricChatClefExceptionDetailsPayloadMap {
    private FabricChatClefExceptionDetailsPayloadMap() {
    }

    public static Map<String, Object> toMap(String exceptionType, String exceptionMessage) {
        Map<String, Object> details = new HashMap<>();
        FabricChatClefExceptionTypePayloadMap.writeTo(details, exceptionType);
        FabricChatClefExceptionMessagePayloadMap.writeTo(details, exceptionMessage);
        return details;
    }
}
