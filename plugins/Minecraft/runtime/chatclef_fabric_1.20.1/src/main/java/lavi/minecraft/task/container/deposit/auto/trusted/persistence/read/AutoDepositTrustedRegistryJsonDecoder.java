package lavi.minecraft.task.container.deposit.auto.trusted.persistence.read;

import adris.altoclef.util.Dimension;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public final class AutoDepositTrustedRegistryJsonDecoder {
    private static final Set<String> ROOT_FIELDS = Set.of("schemaVersion", "destinations");
    private static final Set<String> ENTRY_FIELDS = Set.of(
            "worldKey", "dimension", "x", "y", "z", "enabled"
    );
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

    public List<AutoDepositTrustedDestination> decode(byte[] payload) throws IOException {
        JsonNode root = mapper.readTree(payload);
        if (root == null || !root.isObject() || !hasExactFields(root, ROOT_FIELDS)) {
            throw new IOException("trusted destination root schema is invalid");
        }
        JsonNode schemaVersion = root.get("schemaVersion");
        JsonNode entries = root.get("destinations");
        if (schemaVersion == null
                || !schemaVersion.isIntegralNumber()
                || !schemaVersion.canConvertToInt()
                || schemaVersion.intValue() != 1
                || entries == null
                || !entries.isArray()) {
            throw new IOException("trusted destination schema version or collection is invalid");
        }

        List<AutoDepositTrustedDestination> destinations = new ArrayList<>();
        Set<String> identities = new HashSet<>();
        for (JsonNode entry : entries) {
            AutoDepositTrustedDestination destination = decodeEntry(entry);
            if (!identities.add(destination.key())) {
                throw new IOException("trusted destination identity is duplicated");
            }
            destinations.add(destination);
        }
        return List.copyOf(destinations);
    }

    private static AutoDepositTrustedDestination decodeEntry(JsonNode entry) throws IOException {
        if (entry == null || !entry.isObject() || !hasExactFields(entry, ENTRY_FIELDS)) {
            throw new IOException("trusted destination entry schema is invalid");
        }
        JsonNode worldKeyNode = entry.get("worldKey");
        JsonNode dimensionNode = entry.get("dimension");
        JsonNode enabledNode = entry.get("enabled");
        JsonNode xNode = entry.get("x");
        JsonNode yNode = entry.get("y");
        JsonNode zNode = entry.get("z");
        if (!isText(worldKeyNode)
                || !isText(dimensionNode)
                || enabledNode == null
                || !enabledNode.isBoolean()
                || !isInt(xNode)
                || !isInt(yNode)
                || !isInt(zNode)) {
            throw new IOException("trusted destination entry value is invalid");
        }
        String worldKey = worldKeyNode.textValue().trim();
        String dimensionText = dimensionNode.textValue().trim();
        if (worldKey.isEmpty() || dimensionText.isEmpty()) {
            throw new IOException("trusted destination requires worldKey and dimension");
        }
        Dimension dimension;
        try {
            dimension = Dimension.valueOf(dimensionText);
        } catch (IllegalArgumentException exception) {
            throw new IOException("unsupported dimension: " + dimensionText, exception);
        }
        return new AutoDepositTrustedDestination(
                worldKey,
                dimension,
                new BlockPos(xNode.intValue(), yNode.intValue(), zNode.intValue()),
                enabledNode.booleanValue()
        );
    }

    private static boolean isText(JsonNode value) {
        return value != null && value.isTextual();
    }

    private static boolean isInt(JsonNode value) {
        return value != null && value.isIntegralNumber() && value.canConvertToInt();
    }

    private static boolean hasExactFields(JsonNode node, Set<String> expected) {
        Set<String> actual = new HashSet<>();
        Iterator<String> names = node.fieldNames();
        names.forEachRemaining(actual::add);
        return actual.equals(expected);
    }
}
