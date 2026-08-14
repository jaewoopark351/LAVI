package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.completion;

import java.util.Map;

//20260814_kpopmodder: Keep the termination kind key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationKindPayloadMap {
    private static final String TERMINATION_KIND = "termination_kind";

    private FabricChatClefTerminationKindPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String terminationKind) {
        payload.put(TERMINATION_KIND, terminationKind);
    }
}
