package lavi.minecraft.blocks.scanner;

import lavi.minecraft.blocks.scanner.state.ScanRunLease;
import lavi.minecraft.blocks.scanner.state.ScanChunkRequest;
import lavi.minecraft.blocks.scanner.state.ScanWorldBinding;
import net.minecraft.util.math.ChunkPos;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Cancellation releases both queued and already-taken requests without affecting another run.
class ScanRunLeaseTest {
    @Test void identicalDimensionNamesDoNotMatchAnotherWorldPlayerOrGeneration() {
        Object world = new Object(), player = new Object();
        ScanWorldBinding binding = new ScanWorldBinding(world, player, "overworld", 4);
        assertTrue(binding.matches(world, player, "overworld", 4));
        assertFalse(binding.matches(new Object(), player, "overworld", 4));
        assertFalse(binding.matches(world, new Object(), "overworld", 4));
        assertFalse(binding.matches(world, player, "overworld", 5));
        assertFalse(binding.matches(world, player, "nether", 4));
    }
    @Test void cancellationReleasesARequestEvenAfterTheClientHasTakenIt() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            ScanRunLease old = new ScanRunLease(), replacement = new ScanRunLease();
            Future<?> result = worker.submit(() -> old.request(new ChunkPos(0, 0)));
            ScanChunkRequest request = awaitRequest(old);
            old.cancel();
            assertNull(result.get(2, TimeUnit.SECONDS));
            assertTrue(request.result().isDone());
            assertFalse(replacement.cancelled());
            assertNull(old.request(new ChunkPos(1, 1)));
        } finally { worker.shutdownNow(); }
    }
    @Test void clientFailureCompletesTheWaitingWorkerAndDoesNotRequireAnotherClientTick() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            ScanRunLease run = new ScanRunLease();
            Future<?> result = worker.submit(() -> run.request(new ChunkPos(0, 0)));
            ScanChunkRequest request = awaitRequest(run);
            IllegalStateException expected = new IllegalStateException("client capture");
            request.result().completeExceptionally(expected);
            ExecutionException observed = assertThrows(ExecutionException.class, () -> result.get(2, TimeUnit.SECONDS));
            assertSame(expected, observed.getCause().getCause());
        } finally { worker.shutdownNow(); }
    }
    private static ScanChunkRequest awaitRequest(ScanRunLease run) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            ScanChunkRequest request = run.takeRequest();
            if (request != null) return request;
            Thread.onSpinWait();
        }
        throw new AssertionError("worker did not publish request");
    }
}
