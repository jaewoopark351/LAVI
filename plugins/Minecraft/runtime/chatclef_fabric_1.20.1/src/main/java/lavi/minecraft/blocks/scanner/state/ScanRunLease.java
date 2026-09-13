//#if MC == 12001
package lavi.minecraft.blocks.scanner.state;

import lavi.minecraft.blocks.scanner.snapshot.ScanChunkSnapshot;
import net.minecraft.util.math.ChunkPos;
import java.util.concurrent.CompletableFuture;

//20260913_kpopmodder: Cancellation retires only this run and releases its one outstanding client request.
public final class ScanRunLease {
    private final ScanWorldBinding binding;
    private boolean cancelled;
    private ScanChunkRequest pending;
    private CompletableFuture<ScanChunkSnapshot> waiting;
    public ScanRunLease() { this(null); }
    public ScanRunLease(ScanWorldBinding binding) { this.binding = binding; }
    public ScanWorldBinding binding() { return binding; }
    public synchronized boolean cancelled() { return cancelled; }
    public synchronized ScanChunkRequest takeRequest() { ScanChunkRequest result = pending; pending = null; return result; }
    public ScanChunkSnapshot request(ChunkPos pos) {
        CompletableFuture<ScanChunkSnapshot> future = new CompletableFuture<>();
        synchronized (this) {
            if (cancelled) return null;
            pending = new ScanChunkRequest(pos, future);
            waiting = future;
        }
        try { return future.join(); }
        finally { synchronized (this) { if (waiting == future) waiting = null; } }
    }
    public synchronized void cancel() {
        cancelled = true;
        if (waiting != null) waiting.complete(null);
        pending = null;
    }
}

//#endif
