//#if MC == 12001
package lavi.minecraft.blocks.scanner.snapshot;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.WorldChunk;
import java.util.ArrayList;

//20260913_kpopmodder: Exactly one requested chunk is copied on the client tick, outside the shared index lock.
public final class ClientChunkSnapshotCapture {
    private ClientChunkSnapshotCapture() { }
    public static ScanChunkSnapshot capture(ClientWorld world, ChunkPos pos) {
        if (!MinecraftClient.getInstance().isOnThread()) throw new IllegalStateException("client_thread_required");
        if (!world.getChunkManager().isChunkLoaded(pos.x, pos.z)) return null;
        WorldChunk chunk = world.getChunk(pos.x, pos.z);
        ArrayList<PalettedContainer<BlockState>> sections = new ArrayList<>();
        for (var section : chunk.getSectionArray()) sections.add(section.getBlockStateContainer().copy());
        return new ScanChunkSnapshot(pos, world.getBottomY(), world.getTime(), sections);
    }
}

//#endif
