package lavi.minecraft.task.container.deposit.auto.trusted;

import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.trusted.persistence.AutoDepositTrustedConditionalSaveStatus;
import lavi.minecraft.task.container.deposit.auto.trusted.persistence.read.AutoDepositTrustedRegistryReadResult;
import lavi.minecraft.task.container.deposit.auto.trusted.persistence.read.AutoDepositTrustedRegistryReadStatus;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoDepositTrustedDestinationStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void persistsWorldDimensionPositionAndEnabledState() throws Exception {
        AutoDepositTrustedDestinationStore store = new AutoDepositTrustedDestinationStore(
                temporaryDirectory.resolve("trusted.json")
        );
        AutoDepositTrustedDestination expected = new AutoDepositTrustedDestination(
                "multiplayer:example.test",
                Dimension.NETHER,
                new BlockPos(12, 70, -4),
                false
        );

        store.save(List.of(expected));
        AutoDepositTrustedDestination loaded = store.load().get(0);

        assertEquals(expected.worldKey(), loaded.worldKey());
        assertEquals(expected.dimension(), loaded.dimension());
        assertEquals(expected.position(), loaded.position());
        assertEquals(expected.enabled(), loaded.enabled());
    }

    @Test
    void strictReadDistinguishesMissingEmptyAndPopulatedRegistries() throws Exception {
        AutoDepositTrustedDestinationStore store = new AutoDepositTrustedDestinationStore(
                temporaryDirectory.resolve("trusted.json")
        );

        AutoDepositTrustedRegistryReadResult missing = store.readStrictSnapshot();
        store.save(List.of());
        AutoDepositTrustedRegistryReadResult empty = store.readStrictSnapshot();
        store.save(List.of(new AutoDepositTrustedDestination(
                "singleplayer:test",
                Dimension.OVERWORLD,
                new BlockPos(1, 64, 1),
                true
        )));
        AutoDepositTrustedRegistryReadResult populated = store.readStrictSnapshot();

        assertEquals(AutoDepositTrustedRegistryReadStatus.FILE_MISSING_VALID_EMPTY, missing.status());
        assertEquals(AutoDepositTrustedRegistryReadStatus.VALID_EMPTY, empty.status());
        assertEquals(AutoDepositTrustedRegistryReadStatus.VALID_POPULATED, populated.status());
        assertTrue(missing.snapshot().orElseThrow().destinations().isEmpty());
        assertEquals(1, populated.snapshot().orElseThrow().destinations().size());
        assertFalse(missing.snapshot().orElseThrow().provenance().exists());
        assertTrue(populated.snapshot().orElseThrow().provenance().exists());
        assertEquals(64, populated.snapshot().orElseThrow().provenance().sha256().length());
    }

    @Test
    void strictReadRejectsMalformedCoercedDuplicateAndWrongSchemaContent() throws Exception {
        Path path = temporaryDirectory.resolve("trusted.json");
        AutoDepositTrustedDestinationStore store = new AutoDepositTrustedDestinationStore(path);
        List<String> invalidPayloads = List.of(
                "",
                "not-json",
                "{\"schemaVersion\":2,\"destinations\":[]}",
                "{\"schemaVersion\":1,\"destinations\":[],\"extra\":true}",
                "{\"schemaVersion\":1,\"destinations\":[{\"worldKey\":\"w\",\"dimension\":\"OVERWORLD\",\"x\":\"1\",\"y\":64,\"z\":1,\"enabled\":true}]}",
                "{\"schemaVersion\":1,\"destinations\":[{\"worldKey\":\"w\",\"dimension\":\"OVERWORLD\",\"x\":1,\"y\":64,\"z\":1,\"enabled\":true},{\"worldKey\":\"w\",\"dimension\":\"OVERWORLD\",\"x\":1,\"y\":64,\"z\":1,\"enabled\":false}]}"
        );

        for (String payload : invalidPayloads) {
            Files.writeString(path, payload);
            AutoDepositTrustedRegistryReadResult result = store.readStrictSnapshot();
            assertEquals(AutoDepositTrustedRegistryReadStatus.REGISTRY_READ_FAILED, result.status());
            assertTrue(result.snapshot().isEmpty());
        }
    }

    @Test
    void conditionalSaveRejectsExternalContentChangeWithoutOverwritingIt() throws Exception {
        Path path = temporaryDirectory.resolve("trusted.json");
        AutoDepositTrustedDestinationStore store = new AutoDepositTrustedDestinationStore(path);
        store.save(List.of());
        AutoDepositTrustedRegistryReadResult initial = store.readStrictSnapshot();
        String externalPayload = "{\"schemaVersion\":1,\"destinations\":[{"
                + "\"worldKey\":\"singleplayer:external\","
                + "\"dimension\":\"OVERWORLD\",\"x\":9,\"y\":64,\"z\":9,"
                + "\"enabled\":true}]}";
        Files.writeString(path, externalPayload);

        AutoDepositTrustedConditionalSaveStatus status = store.saveIfUnchanged(
                initial.snapshot().orElseThrow().provenance(),
                List.of(new AutoDepositTrustedDestination(
                        "singleplayer:ours",
                        Dimension.OVERWORLD,
                        new BlockPos(1, 64, 1),
                        true
                ))
        );

        assertEquals(AutoDepositTrustedConditionalSaveStatus.CONFLICT, status);
        assertEquals("singleplayer:external", store.load().get(0).worldKey());
    }
}
