package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload;

import java.util.HashMap;
import java.util.Map;

public final class FabricChatClefEmptyDetailsPayloadMap {
    private FabricChatClefEmptyDetailsPayloadMap() {
    }

    public static Map<String, Object> toMap() {
        return new HashMap<>();
    }
}
