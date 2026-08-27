package lavi.minecraft.task.container.deposit.auto.trusted.execution;

import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoDepositTrustedCandidateQueueTest {
    @Test
    void rejectionMovesForwardOnceAndNeverReturnsTheFailedCandidate() {
        AutoDepositTrustedDestinationCandidate first = candidate(1);
        AutoDepositTrustedDestinationCandidate second = candidate(2);
        AutoDepositTrustedCandidateQueue queue = new AutoDepositTrustedCandidateQueue(
                List.of(first, second)
        );

        assertEquals(first.destinationId(), queue.current().orElseThrow().destinationId());
        queue.rejectCurrent("live_gui_full");
        assertEquals(second.destinationId(), queue.current().orElseThrow().destinationId());
        assertEquals("live_gui_full",
                queue.rejected().get(first.destination().key()));
        queue.rejectCurrent("unreachable");

        assertTrue(queue.current().isEmpty());
        assertEquals(0, queue.remainingCandidateCount());
    }

    private static AutoDepositTrustedDestinationCandidate candidate(int x) {
        return new AutoDepositTrustedDestinationCandidate(
                new AutoDepositTrustedDestination(
                        "test-world",
                        Dimension.OVERWORLD,
                        new BlockPos(x, 64, 0),
                        true
                ),
                0,
                x * x,
                "test"
        );
    }
}
