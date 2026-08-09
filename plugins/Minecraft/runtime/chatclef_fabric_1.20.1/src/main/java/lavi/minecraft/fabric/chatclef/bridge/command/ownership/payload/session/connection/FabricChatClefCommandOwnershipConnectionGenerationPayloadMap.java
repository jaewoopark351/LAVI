package lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload.session.connection;

import java.util.Map;

//20260809_kpopmodder: Split the command ownership connection generation without changing emitted keys.
public final class FabricChatClefCommandOwnershipConnectionGenerationPayloadMap {
    private static final String CONNECTION_GENERATION = "connection_generation";

    private FabricChatClefCommandOwnershipConnectionGenerationPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, long connectionGeneration) {
        payload.put(CONNECTION_GENERATION, connectionGeneration);
    }
}
