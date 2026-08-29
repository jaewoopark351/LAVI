package lavi.minecraft.task.container.home.execution.operation;

import lavi.minecraft.task.container.home.execution.StoreHomeResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

//20260828_kpopmodder: Verify cumulative A-to-B totals and exactly-once operation commits.
class StoreHomeOperationProgressTest {
    @Test
    void retainsConfirmedTotalsAcrossFreshContainerSessions() {
        StoreHomeOperationProgress progress = StoreHomeOperationProgress.start(41L)
                .activatedSession(2)
                .confirmed(
                        identity(41L, 1, 7L, 1, 4, "A", new Object()),
                        32,
                        32,
                        2
                )
                .capacityFailure()
                .activatedSession(2)
                .confirmed(
                        identity(41L, 2, 8L, 1, 4, "B", new Object()),
                        32,
                        0,
                        1
                );

        assertEquals(64, progress.storedItems());
        assertEquals(1, progress.touchedStackCount());
        assertEquals(1, progress.latestRemainingStacks());
        assertEquals(1, progress.capacityFailures());
        assertEquals(3, progress.nextContainerSessionOrdinal());
    }

    @Test
    void identicalConfirmationReplayDoesNotDoubleCount() {
        HomeStorageTransferIdentity identity =
                identity(9L, 1, 3L, 1, 6, "A", new Object());
        StoreHomeOperationProgress confirmed = StoreHomeOperationProgress.start(9L)
                .activatedSession(1)
                .confirmed(identity, 12, 0, 0);

        assertSame(confirmed, confirmed.confirmed(identity, 12, 0, 0));
        assertEquals(12, confirmed.storedItems());
    }

    @Test
    void reportingOnlyZeroRemainingDoesNotTurnPartialIntoCompleted() {
        StoreHomeOperationProgress progress = StoreHomeOperationProgress.start(5L)
                .activatedSession(1)
                .confirmed(
                        identity(5L, 1, 2L, 1, 8, "A", new Object()),
                        3,
                        0,
                        0
                )
                .capacityFailure()
                .withLatestRemainingStacks(0);

        assertEquals(
                StoreHomeResult.PARTIAL_TRUSTED_CAPACITY_EXHAUSTED,
                new StoreHomeTerminalClassifier().classifyExhausted(progress)
        );
    }

    @Test
    void transferIdentityIncludesDestinationAndExactHandlerIdentity() {
        Object handler = new Object();
        HomeStorageTransferIdentity expected =
                identity(12L, 1, 4L, 1, 3, "destination-A", handler);

        assertEquals(
                expected,
                identity(12L, 1, 4L, 1, 3, "destination-A", handler)
        );
        assertNotEquals(
                expected,
                identity(12L, 1, 4L, 1, 3, "destination-B", handler)
        );
        assertNotEquals(
                expected,
                identity(12L, 1, 4L, 1, 3, "destination-A", new Object())
        );
        assertNotEquals(
                expected,
                new HomeStorageTransferIdentity(
                        12L, 1, 4L, 1, 3,
                        "destination-A", handler, 99
                )
        );
    }

    private static HomeStorageTransferIdentity identity(
            long operationId,
            int sessionOrdinal,
            long revision,
            int attemptOrdinal,
            int logicalSlot,
            String destinationKey,
            Object handlerIdentity) {
        return new HomeStorageTransferIdentity(
                operationId,
                sessionOrdinal,
                revision,
                attemptOrdinal,
                logicalSlot,
                destinationKey,
                handlerIdentity,
                sessionOrdinal
        );
    }
}
