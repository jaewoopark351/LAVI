package lavi.minecraft.fabric.chatclef.bridge.config;

import java.net.URI;
import java.time.Duration;

//20260801_kpopmodder: Keep the LAVI Fabric ChatClef bridge endpoint separate from legacy Player2.
public final class FabricChatClefBridgeConfig {
    public static final String DEFAULT_ENDPOINT = "ws://127.0.0.1:4316";

    private final URI endpoint;
    private final Duration reconnectDelay;

    public FabricChatClefBridgeConfig(URI endpoint, Duration reconnectDelay) {
        this.endpoint = endpoint;
        this.reconnectDelay = reconnectDelay;
    }

    public URI endpoint() {
        return endpoint;
    }

    public Duration reconnectDelay() {
        return reconnectDelay;
    }
}
