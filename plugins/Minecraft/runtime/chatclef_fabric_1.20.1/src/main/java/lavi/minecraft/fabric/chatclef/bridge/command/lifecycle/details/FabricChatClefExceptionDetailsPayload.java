package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;

import java.util.HashMap;
import java.util.Map;

public final class FabricChatClefExceptionDetailsPayload implements FabricChatClefLifecycleDetailsPayload {
    private static final String EXCEPTION_TYPE = "exception_type";
    private static final String EXCEPTION_MESSAGE = "exception_message";

    private final Throwable exception;

    public FabricChatClefExceptionDetailsPayload(Throwable exception) {
        this.exception = exception;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> details = new HashMap<>();
        details.put(EXCEPTION_TYPE, exception == null ? "" : exception.getClass().getName());
        details.put(EXCEPTION_MESSAGE, FabricChatClefLifecycleDetailValues.nullSafeMessage(exception));
        return details;
    }
}
