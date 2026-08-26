package lavi.minecraft.task.container.deposit.auto.trusted;

import adris.altoclef.util.Dimension;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals(new BlockPos(4, 64, 0), inspection.selection().orElseThrow().position());
        assertEquals(2, inspection.selection().orElseThrow().cachedEmptySlots());
        assertTrue(inspection.capacityState().contains("insufficient_capacity"));
        assertTrue(inspection.capacityState().contains("eligible"));
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
