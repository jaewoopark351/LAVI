package lavi.minecraft.task.container.deposit.auto.trusted;

import adris.altoclef.util.Dimension;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoDepositTrustedDestinationSelectorTest {
    @Test
    void selectsOnlyAnEligibleDestinationInTheRequestedWorldAndDimension() {
        AutoDepositTrustedDestinationRepository repository =
                AutoDepositTrustedDestinationRepository.inMemoryEmpty();
        repository.register(destination("other-world", Dimension.OVERWORLD, 1, true));
        repository.register(destination("test-world", Dimension.NETHER, 2, true));
        repository.register(destination("test-world", Dimension.OVERWORLD, 3, true));
        repository.register(destination("test-world", Dimension.OVERWORLD, 4, true));

        AutoDepositTrustedDestinationSelector selector = new AutoDepositTrustedDestinationSelector(
                repository,
                128,
                (mod, destination, requiredSlots, maximumDistance) -> {
                    if (destination.position().getX() == 3) {
                        return new AutoDepositTrustedDestinationEvaluation(
                                false, requiredSlots - 1, 9.0, "insufficient_capacity"
                        );
                    }
                    return new AutoDepositTrustedDestinationEvaluation(
                            true, requiredSlots, 16.0, "eligible"
                    );
                }
        );

        AutoDepositTrustedDestinationInspection inspection = selector.inspect(
                null, "test-world", Dimension.OVERWORLD, 2
        );

        assertTrue(inspection.selection().isPresent());
        assertEquals(1, inspection.candidates().size());
        assertEquals(new BlockPos(4, 64, 0), inspection.selection().orElseThrow().position());
        assertEquals(2, inspection.selection().orElseThrow().cachedEmptySlots());
        assertTrue(inspection.capacityState().contains("insufficient_capacity"));
        assertTrue(inspection.capacityState().contains("eligible"));
    }

    @Test
    void returnsAllEligibleCandidatesAsAnImmutableDistanceOrderedSnapshot() {
        AutoDepositTrustedDestinationRepository repository =
                AutoDepositTrustedDestinationRepository.inMemoryEmpty();
        repository.register(destination("test-world", Dimension.OVERWORLD, 12, true));
        repository.register(destination("test-world", Dimension.OVERWORLD, 3, true));

        AutoDepositTrustedDestinationSelector selector = new AutoDepositTrustedDestinationSelector(
                repository,
                128,
                (mod, destination, requiredSlots, maximumDistance) ->
                        new AutoDepositTrustedDestinationEvaluation(
                                true,
                                destination.position().getX() == 12 ? 0 : 1,
                                destination.position().getX() == 12 ? 144.0 : 9.0,
                                destination.position().getX() == 12
                                        ? "capacity_hint_insufficient"
                                        : "capacity_unverified"
                        )
        );

        AutoDepositTrustedDestinationInspection inspection = selector.inspect(
                null, "test-world", Dimension.OVERWORLD, 2
        );

        assertEquals(2, inspection.candidates().size());
        assertEquals(new BlockPos(3, 64, 0), inspection.candidates().get(0).position());
        assertEquals(new BlockPos(12, 64, 0), inspection.candidates().get(1).position());
    }

    @Test
    void boundsOneOperationCandidateSnapshotWithoutChangingDistanceOrder() {
        AutoDepositTrustedDestinationRepository repository =
                AutoDepositTrustedDestinationRepository.inMemoryEmpty();
        for (int x = 1; x <= 70; x++) {
            repository.register(destination("test-world", Dimension.OVERWORLD, x, true));
        }
        AutoDepositTrustedDestinationSelector selector = new AutoDepositTrustedDestinationSelector(
                repository,
                128,
                (mod, destination, requiredSlots, maximumDistance) ->
                        new AutoDepositTrustedDestinationEvaluation(
                                true,
                                0,
                                destination.position().getX() * destination.position().getX(),
                                "capacity_unverified"
                        )
        );

        AutoDepositTrustedDestinationInspection inspection = selector.inspect(
                null, "test-world", Dimension.OVERWORLD, 1
        );

        assertEquals(64, inspection.candidates().size());
        assertEquals(1, inspection.candidates().get(0).position().getX());
        assertEquals(64, inspection.candidates().get(63).position().getX());
        assertThrows(UnsupportedOperationException.class,
                () -> inspection.candidates().clear());
    }

    private static AutoDepositTrustedDestination destination(String worldKey,
                                                              Dimension dimension,
                                                              int x,
                                                              boolean enabled) {
        return new AutoDepositTrustedDestination(
                worldKey, dimension, new BlockPos(x, 64, 0), enabled
        );
    }
}
