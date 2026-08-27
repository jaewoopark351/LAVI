package lavi.minecraft.task.container.deposit.auto.trusted.interaction;

import adris.altoclef.util.Dimension;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoDepositOpenContainerCorrelationWindowTest {
    @Test
    void consumesOnlyTheExactCurrentWorldAndDimensionInteraction() {
        Object world = new Object();
        BlockPos position = new BlockPos(10, 64, 20);
        AutoDepositOpenContainerCorrelationWindow window =
                new AutoDepositOpenContainerCorrelationWindow();
        window.record(world, Dimension.OVERWORLD, position, 100L);

        assertEquals(position, window.consume(
                world, Dimension.OVERWORLD, 101L, 20L
        ).orElseThrow());
        assertTrue(window.consume(
                world, Dimension.OVERWORLD, 102L, 20L
        ).isEmpty());
    }

    @Test
    void rejectsStaleOrCrossWorldInteractionsInsteadOfGuessing() {
        Object firstWorld = new Object();
        AutoDepositOpenContainerCorrelationWindow window =
                new AutoDepositOpenContainerCorrelationWindow();
        window.record(firstWorld, Dimension.OVERWORLD, new BlockPos(1, 2, 3), 10L);

        assertTrue(window.consume(
                new Object(), Dimension.OVERWORLD, 11L, 20L
        ).isEmpty());

        window.record(firstWorld, Dimension.OVERWORLD, new BlockPos(1, 2, 3), 10L);
        assertTrue(window.consume(
                firstWorld, Dimension.OVERWORLD, 31L, 20L
        ).isEmpty());
    }
}
