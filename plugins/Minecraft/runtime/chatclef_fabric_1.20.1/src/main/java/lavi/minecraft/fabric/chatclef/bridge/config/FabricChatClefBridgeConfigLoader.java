package lavi.minecraft.fabric.chatclef.bridge.config;

import java.net.URI;
import java.time.Duration;

//20260801_kpopmodder: Load only Fabric ChatClef bridge-specific Java runtime options.
public final class FabricChatClefBridgeConfigLoader {
    private static final String ENDPOINT_PROPERTY = "lavi.minecraft.fabric.chatclef.bridge.uri";
    private static final String ENDPOINT_ENV = "LAVI_MINECRAFT_FABRIC_CHATCLEF_BRIDGE_URI";
    private static final String RECONNECT_MS_PROPERTY = "lavi.minecraft.fabric.chatclef.bridge.reconnect_ms";
    private static final String RECONNECT_MS_ENV = "LAVI_MINECRAFT_FABRIC_CHATCLEF_BRIDGE_RECONNECT_MS";
    private static final long DEFAULT_RECONNECT_MS = 3000L;

    public FabricChatClefBridgeConfig load() {
        return new FabricChatClefBridgeConfig(
                URI.create(configValue(ENDPOINT_PROPERTY, ENDPOINT_ENV, FabricChatClefBridgeConfig.DEFAULT_ENDPOINT)),
                Duration.ofMillis(reconnectDelayMs())
        );
    }

    private long reconnectDelayMs() {
        String value = configValue(RECONNECT_MS_PROPERTY, RECONNECT_MS_ENV, Long.toString(DEFAULT_RECONNECT_MS));
        try {
            long parsed = Long.parseLong(value);
            return Math.max(250L, parsed);
        } catch (NumberFormatException ignored) {
            return DEFAULT_RECONNECT_MS;
        }
    }

    private String configValue(String propertyName, String envName, String defaultValue) {
        String property = System.getProperty(propertyName);
        if (property != null && !property.isBlank()) {
            return property.trim();
        }
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return defaultValue;
    }
}
