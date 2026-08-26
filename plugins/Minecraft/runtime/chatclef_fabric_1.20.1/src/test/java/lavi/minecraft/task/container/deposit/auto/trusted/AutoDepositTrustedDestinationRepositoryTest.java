package lavi.minecraft.task.container.deposit.auto.trusted;

import adris.altoclef.util.Dimension;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoDepositTrustedDestinationRepositoryTest {
    @Test
    void registrationIsInstanceOwnedAndAdvancesTheRepositoryRevision() {
        AutoDepositTrustedDestinationRepository repository =
                AutoDepositTrustedDestinationRepository.inMemoryEmpty();
        long before = repository.revision();

        repository.register(new AutoDepositTrustedDestination(
                "singleplayer:test",
                Dimension.OVERWORLD,
                new BlockPos(10, 64, 10),
                true
        ));

        assertEquals(1, repository.destinations().size());
        assertTrue(repository.revision() > before);
    }
}
