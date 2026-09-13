//#if MC == 12001
package lavi.minecraft.blocks.scanner.state;

import lavi.minecraft.blocks.scanner.snapshot.ScanChunkSnapshot;
import net.minecraft.util.math.ChunkPos;
import java.util.concurrent.CompletableFuture;

//20260913_kpopmodder: One bounded mailbox request carries a client-captured immutable chunk to its worker.
public record ScanChunkRequest(ChunkPos position, CompletableFuture<ScanChunkSnapshot> result) { }

//#endif
