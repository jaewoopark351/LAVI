package lavi.minecraft.fabric.chatclef.bridge.command.request;

//20260805_kpopmodder: Keep v1 command request wire keys centralized without changing the JSON shape.
public final class FabricChatClefCommandRequestFields {
    public static final String REQUEST_ID = "request_id";
    public static final String COMMAND = "command";
    public static final String SOURCE = "source";
    public static final String DEADLINE_MS = "deadline_ms";
    public static final String METADATA = "metadata";

    private FabricChatClefCommandRequestFields() {
    }
}
