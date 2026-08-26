package lavi.minecraft.task.container.deposit.auto.trusted;

import adris.altoclef.util.Dimension;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
