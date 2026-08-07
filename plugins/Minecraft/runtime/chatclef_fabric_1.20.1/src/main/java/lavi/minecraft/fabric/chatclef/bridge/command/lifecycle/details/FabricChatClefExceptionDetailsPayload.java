package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.FabricChatClefExceptionDetailsPayloadMap;

import java.util.Map;

public final class FabricChatClefExceptionDetailsPayload implements FabricChatClefLifecycleDetailsPayload {
    private final Throwable exception;

    public FabricChatClefExceptionDetailsPayload(Throwable exception) {
        this.exception = exception;
    }

    @Override
    public Map<String, Object> toMap() {
        return FabricChatClefExceptionDetailsPayloadMap.toMap(
                exception == null ? "" : exception.getClass().getName(),
                FabricChatClefLifecycleDetailValues.nullSafeMessage(exception)
        );
    }
}
