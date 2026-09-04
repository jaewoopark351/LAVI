package lavi.minecraft.task.container.deposit.auto.trusted;

import adris.altoclef.util.Dimension;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lavi.minecraft.task.container.deposit.auto.trusted.persistence.AutoDepositTrustedConditionalSaveStatus;
import lavi.minecraft.task.container.deposit.auto.trusted.persistence.read.AutoDepositTrustedRegistryFileReader;
import lavi.minecraft.task.container.deposit.auto.trusted.persistence.read.AutoDepositTrustedRegistryFileSnapshot;
import lavi.minecraft.task.container.deposit.auto.trusted.persistence.read.AutoDepositTrustedRegistryProvenance;
import lavi.minecraft.task.container.deposit.auto.trusted.persistence.read.AutoDepositTrustedRegistryReadResult;
import lavi.minecraft.task.container.deposit.auto.trusted.persistence.read.AutoDepositTrustedStrictSnapshotReader;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//20260827_kpopmodder: Expose file persistence through the instance-owned trusted repository boundary.
public final class AutoDepositTrustedDestinationStore
        implements AutoDepositTrustedDestinationPersistence {
    private final Path path;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final AutoDepositTrustedStrictSnapshotReader strictSnapshotReader =
            new AutoDepositTrustedStrictSnapshotReader();
    private final AutoDepositTrustedRegistryFileReader registryFileReader =
            new AutoDepositTrustedRegistryFileReader();

    public AutoDepositTrustedDestinationStore(Path path) {
        this.path = path.toAbsolutePath().normalize();
    }

    @Override
    public List<AutoDepositTrustedDestination> load() throws IOException {
        if (!Files.exists(path)) {
            return List.of();
        }
        JsonNode root = mapper.readTree(path.toFile());
        if (root == null || !root.isObject()) {
            throw new IOException("trusted destination root must be an object");
        }
        JsonNode entries = root.path("destinations");
        if (!entries.isArray()) {
            throw new IOException("destinations must be an array");
        }
        List<AutoDepositTrustedDestination> result = new ArrayList<>();
        for (JsonNode entry : entries) {
            String worldKey = entry.path("worldKey").asText("").trim();
            String dimensionText = entry.path("dimension").asText("").trim();
            if (worldKey.isEmpty() || dimensionText.isEmpty()) {
                throw new IOException("trusted destination requires worldKey and dimension");
            }
            Dimension dimension;
            try {
                dimension = Dimension.valueOf(dimensionText);
            } catch (IllegalArgumentException exception) {
                throw new IOException("unsupported dimension: " + dimensionText, exception);
            }
            result.add(new AutoDepositTrustedDestination(
                    worldKey,
                    dimension,
                    new BlockPos(
                            entry.path("x").asInt(),
                            entry.path("y").asInt(),
                            entry.path("z").asInt()
                    ),
                    entry.path("enabled").asBoolean(true)
            ));
        }
        return List.copyOf(result);
    }

    @Override
    public void save(List<AutoDepositTrustedDestination> destinations) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        ObjectNode root = mapper.createObjectNode();
        root.put("schemaVersion", 1);
        ArrayNode entries = root.putArray("destinations");
        for (AutoDepositTrustedDestination destination : destinations) {
            ObjectNode entry = entries.addObject();
            entry.put("worldKey", destination.worldKey());
            entry.put("dimension", destination.dimension().name());
            entry.put("x", destination.position().getX());
            entry.put("y", destination.position().getY());
            entry.put("z", destination.position().getZ());
            entry.put("enabled", destination.enabled());
        }

        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        mapper.writeValue(temporary.toFile(), root);
        try {
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException atomicMoveFailure) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public long modifiedTime() {
        try {
            return Files.exists(path) ? Files.getLastModifiedTime(path).toMillis() : -1L;
        } catch (IOException ignored) {
            return -1L;
        }
    }

    @Override
    public AutoDepositTrustedRegistryReadResult readStrictSnapshot() {
        return strictSnapshotReader.read(path);
    }

    @Override
    public AutoDepositTrustedConditionalSaveStatus saveIfUnchanged(
            AutoDepositTrustedRegistryProvenance expected,
            List<AutoDepositTrustedDestination> destinations) throws IOException {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(destinations, "destinations");
        AutoDepositTrustedRegistryFileSnapshot current = registryFileReader.read(path);
        if (!expected.equals(current.provenance())) {
            return AutoDepositTrustedConditionalSaveStatus.CONFLICT;
        }
        save(destinations);
        return AutoDepositTrustedConditionalSaveStatus.SAVED;
    }

    public Path path() {
        return path;
    }
}
