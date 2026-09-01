package lavi.minecraft.diagnostics.interaction;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260831_kpopmodder: Prove OFF invalidation clears interaction pairing and local shaping state.
class BlockInteractionDiagnosticStateLifecycleTest {
    @Test
    void modeEpochInvalidatesAHeadStoredOnAnotherThread() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            BlockInteractionInvocationTracker tracker = new BlockInteractionInvocationTracker();
            BlockPos target = new BlockPos(4, 70, 9);
            BlockInteractionContext context = context(target);
            CountDownLatch headStored = new CountDownLatch(1);
            CountDownLatch continueAfterClear = new CountDownLatch(1);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<BlockInteractionContext> result = executor.submit(() -> {
                    tracker.begin(context);
                    headStored.countDown();
                    await(continueAfterClear);
                    return tracker.end(target);
                });

                await(headStored);
                tracker.clearForModeTransition();
                continueAfterClear.countDown();

                assertNull(result.get());
            } finally {
                continueAfterClear.countDown();
                executor.shutdownNow();
            }
        });
    }

    @Test
    void modeTransitionRestoresAnEmptyLocalEmissionWindow() {
        BlockInteractionEmissionLimiter limiter = new BlockInteractionEmissionLimiter();

        assertTrue(limiter.evaluate("same", 1L).emitEvent());
        assertFalse(limiter.evaluate("same", 2L).emitEvent());

        limiter.clearForModeTransition();

        assertTrue(limiter.evaluate("same", 3L).emitEvent());
    }

    private static BlockInteractionContext context(BlockPos target) {
        return new BlockInteractionContext(
                1L,
                2L,
                new BlockInteractionTargetInfo(
                        true,
                        "CONTAINER",
                        "minecraft:chest",
                        "Chest",
                        "minecraft:chest",
                        target
                ),
                null,
                null,
                BlockInteractionScreenSnapshot.current(null, null),
                true
        );
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }
}
