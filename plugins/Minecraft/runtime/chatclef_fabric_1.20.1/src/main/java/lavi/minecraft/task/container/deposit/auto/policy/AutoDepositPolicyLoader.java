package lavi.minecraft.task.container.deposit.auto.policy;

import adris.altoclef.Debug;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;

public final class AutoDepositPolicyLoader {
    public static final String RESOURCE_PATH = "/lavi/automatic-deposit-policy.json";

    public AutoDepositPolicyDefinition loadOrFailClosed() {
        try (InputStream input = AutoDepositPolicyLoader.class.getResourceAsStream(RESOURCE_PATH)) {
            if (input == null) {
                Debug.logError("Automatic deposit policy resource is missing; automatic storage will fail closed.");
                return AutoDepositPolicyDefinition.failClosed();
            }
            JsonNode root = new ObjectMapper().readTree(input);
            int revision = root.path("revision").asInt(-1);
            if (revision < 0) {
                throw new IOException("revision must be non-negative");
            }
            JsonNode reserve = root.path("reserve");
            return new AutoDepositPolicyDefinition(
                    revision,
                    stringSet(root, "alwaysHardIds"),
                    stringSet(root, "rareFunctionalIds"),
                    stringSet(root, "valuableIds"),
                    stringSet(root, "valuablePrefixes"),
                    stringSet(root, "generalSurplusIds"),
                    stringSet(root, "generalSurplusSuffixes"),
                    stringSet(root, "safeBuildingIds"),
                    stringSet(root, "harmfulOrSpecialFoodIds"),
                    reserve.path("safeBuilding").asInt(64),
                    reserve.path("logs").asInt(16),
                    reserve.path("planksFallback").asInt(32),
                    reserve.path("fuel").asInt(16),
                    reserve.path("food").asInt(16),
                    reserve.path("torches").asInt(32),
                    reserve.path("arrows").asInt(32),
                    reserve.path("fireworks").asInt(32),
                    reserve.path("waterBuckets").asInt(1),
                    reserve.path("destinationContainers").asInt(1),
                    root.path("trustedMaximumDistance").asInt(128)
            );
        } catch (IOException | RuntimeException exception) {
            Debug.logError("Failed to load automatic deposit policy; automatic storage will fail closed: "
                    + exception.getMessage());
            return AutoDepositPolicyDefinition.failClosed();
        }
    }

    private static Set<String> stringSet(JsonNode root, String field) throws IOException {
        JsonNode values = root.path(field);
        if (!values.isArray()) {
            throw new IOException(field + " must be an array");
        }
        Set<String> result = new LinkedHashSet<>();
        for (JsonNode value : values) {
            String text = value.asText("").trim();
            if (text.isEmpty()) {
                throw new IOException(field + " contains an empty value");
            }
            result.add(text);
        }
        return result;
    }
}
