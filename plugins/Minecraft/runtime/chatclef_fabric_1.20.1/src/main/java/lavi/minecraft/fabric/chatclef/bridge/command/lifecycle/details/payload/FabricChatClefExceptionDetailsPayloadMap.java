package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload;

import java.util.HashMap;
import java.util.Map;

public final class FabricChatClefExceptionDetailsPayloadMap {
    private static final String EXCEPTION_TYPE = "exception_type";
    private static final String EXCEPTION_MESSAGE = "exception_message";

    private FabricChatClefExceptionDetailsPayloadMap() {
    }

    public static Map<String, Object> toMap(String exceptionType, String exceptionMessage) {
        Map<String, Object> details = new HashMap<>();
        details.put(EXCEPTION_TYPE, exceptionType);
        details.put(EXCEPTION_MESSAGE, exceptionMessage);
        return details;
    }
}
