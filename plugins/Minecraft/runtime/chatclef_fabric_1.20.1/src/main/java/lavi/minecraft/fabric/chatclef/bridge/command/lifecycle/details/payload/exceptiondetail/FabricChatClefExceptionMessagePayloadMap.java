package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.exceptiondetail;

import java.util.Map;

public final class FabricChatClefExceptionMessagePayloadMap {
    private static final String EXCEPTION_MESSAGE = "exception_message";

    private FabricChatClefExceptionMessagePayloadMap() {
    }

    public static void writeTo(Map<String, Object> details, String exceptionMessage) {
        details.put(EXCEPTION_MESSAGE, exceptionMessage);
    }
}
