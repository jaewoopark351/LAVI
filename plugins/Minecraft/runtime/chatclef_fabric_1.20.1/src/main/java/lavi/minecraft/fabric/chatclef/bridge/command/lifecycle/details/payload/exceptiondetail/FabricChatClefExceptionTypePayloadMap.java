package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.exceptiondetail;

import java.util.Map;

public final class FabricChatClefExceptionTypePayloadMap {
    private static final String EXCEPTION_TYPE = "exception_type";

    private FabricChatClefExceptionTypePayloadMap() {
    }

    public static void writeTo(Map<String, Object> details, String exceptionType) {
        details.put(EXCEPTION_TYPE, exceptionType);
    }
}
