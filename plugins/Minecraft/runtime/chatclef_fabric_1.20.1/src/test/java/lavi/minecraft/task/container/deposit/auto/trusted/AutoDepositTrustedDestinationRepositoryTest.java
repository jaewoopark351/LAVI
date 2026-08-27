package lavi.minecraft.task.container.deposit.auto.trusted;

import adris.altoclef.util.Dimension;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void repeatedRegistrationIsIdempotentAndStableIdRemovalWorks() {
        AutoDepositTrustedDestinationRepository repository =
                AutoDepositTrustedDestinationRepository.inMemoryEmpty();
        AutoDepositTrustedDestination destination = new AutoDepositTrustedDestination(
                "singleplayer:test",
                Dimension.OVERWORLD,
                new BlockPos(10, 64, 10),
                true
        );

        AutoDepositTrustedDestinationMutationResult first = repository.register(destination);
        AutoDepositTrustedDestinationMutationResult second = repository.register(
                new AutoDepositTrustedDestination(
                        "singleplayer:test",
                        Dimension.OVERWORLD,
                        new BlockPos(10, 64, 10),
                        true
                )
        );
        AutoDepositTrustedDestinationMutationResult removed =
                repository.unregisterById(destination.destinationId());

        assertEquals(AutoDepositTrustedDestinationMutationResult.Status.REGISTERED, first.status());
        assertEquals(AutoDepositTrustedDestinationMutationResult.Status.ALREADY_REGISTERED, second.status());
        assertEquals(AutoDepositTrustedDestinationMutationResult.Status.REMOVED, removed.status());
        assertEquals(0, repository.destinations().size());
        assertFalse(repository.containsEnabled(destination));
    }

    @Test
    void destinationIdIsDeterministicAcrossEquivalentObjects() {
        AutoDepositTrustedDestination first = new AutoDepositTrustedDestination(
                "multiplayer:example.test",
                Dimension.NETHER,
                new BlockPos(-4, 70, 18),
                true
        );
        AutoDepositTrustedDestination second = new AutoDepositTrustedDestination(
                "multiplayer:example.test",
                Dimension.NETHER,
                new BlockPos(-4, 70, 18),
                false
        );

        assertEquals(first.destinationId(), second.destinationId());
        assertTrue(first.destinationId().startsWith("td_"));
    }

    @Test
    void persistenceFailureDoesNotMutateTheRegistryOrReportSuccess() {
        AutoDepositTrustedDestinationRepository repository =
                new AutoDepositTrustedDestinationRepository(new FailingPersistence());
        long before = repository.revision();

        AutoDepositTrustedDestinationMutationResult result = repository.register(
                new AutoDepositTrustedDestination(
                        "singleplayer:test",
                        Dimension.OVERWORLD,
                        new BlockPos(10, 64, 10),
                        true
                )
        );

        assertEquals(AutoDepositTrustedDestinationMutationResult.Status.PERSISTENCE_FAILED,
                result.status());
        assertFalse(result.success());
        assertEquals(0, repository.destinations().size());
        assertEquals(before, repository.revision());
    }

    private static final class FailingPersistence
            implements AutoDepositTrustedDestinationPersistence {
        @Override
        public List<AutoDepositTrustedDestination> load() {
            return List.of();
        }

        @Override
        public void save(List<AutoDepositTrustedDestination> destinations) throws IOException {
            throw new IOException("expected test failure");
        }

        @Override
        public long modifiedTime() {
            return 0L;
        }
    }
}
