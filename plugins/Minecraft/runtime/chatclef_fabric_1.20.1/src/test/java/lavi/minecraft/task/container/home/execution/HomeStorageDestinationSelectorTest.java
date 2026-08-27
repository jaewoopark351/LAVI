package lavi.minecraft.task.container.home.execution;

import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260827_kpopmodder: Added focused tests for same-context distance ordering without a distance cap.
class HomeStorageDestinationSelectorTest {
    @Test
    void includesFarSameDimensionDestinationAndExcludesOtherContexts() {
        HomeStorageDestinationSelector selector = new HomeStorageDestinationSelector(
                AutoDepositTrustedDestinationRepository.inMemoryEmpty()
        );
        AutoDepositTrustedDestination near = destination(
                "world-a", Dimension.OVERWORLD, 8, true
        );
        AutoDepositTrustedDestination far = destination(
                "world-a", Dimension.OVERWORLD, 100000, true
        );
        AutoDepositTrustedDestination otherDimension = destination(
                "world-a", Dimension.NETHER, 1, true
        );
        AutoDepositTrustedDestination otherWorld = destination(
                "world-b", Dimension.OVERWORLD, 1, true
        );
        AutoDepositTrustedDestination disabled = destination(
                "world-a", Dimension.OVERWORLD, 2, false
        );

        List<AutoDepositTrustedDestinationCandidate> selected = selector.snapshot(
                List.of(far, otherDimension, disabled, near, otherWorld),
                "world-a",
                Dimension.OVERWORLD,
                0.0,
                64.0,
                0.0
        );

        assertEquals(List.of(near.destinationId(), far.destinationId()), selected.stream()
                .map(AutoDepositTrustedDestinationCandidate::destinationId)
                .toList());
    }

    private static AutoDepositTrustedDestination destination(
            String worldKey,
            Dimension dimension,
            int x,
            boolean enabled) {
        return new AutoDepositTrustedDestination(
                worldKey,
                dimension,
                new BlockPos(x, 64, 0),
                enabled
        );
    }
}
