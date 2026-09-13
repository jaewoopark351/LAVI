package lavi.minecraft.blocks.scanner;

import adris.altoclef.commands.BlockScanner;
import adris.altoclef.trackers.blacklisting.WorldLocateBlacklist;
import adris.altoclef.util.time.TimerGame;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Real scanner copy/add/reset boundaries are sequenced with latches around the native HashSet copy.
class BlockScannerConsistencyTest {
    @Test void realAddCannotInterleaveInsideTheRealCollectionCopy() throws Exception { verify(false); }
    @Test void realResetCannotInterleaveInsideTheRealCollectionCopy() throws Exception { verify(true); }

    private static void verify(boolean reset) throws Exception {
        CountDownLatch copying = new CountDownLatch(1), releaseCopy = new CountDownLatch(1), writerEntered = new CountDownLatch(1);
        BlockPos before = BlockPos.ORIGIN, added = new BlockPos(2, 3, 4);
        HashSet<BlockPos> source = new HashSet<>() {
            @Override public Object[] toArray() {
                copying.countDown();
                try { assertTrue(releaseCopy.await(2, TimeUnit.SECONDS)); }
                catch (InterruptedException failure) { throw new AssertionError(failure); }
                return super.toArray();
            }
        };
        source.add(before);
        TestScanner scanner = scanner(source);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<List<BlockPos>> read = workers.submit(() -> scanner.getKnownLocationsIncludeUnreachable((Block) null));
            assertTrue(copying.await(2, TimeUnit.SECONDS));
            Future<?> write = workers.submit(() -> {
                writerEntered.countDown();
                if (reset) scanner.reset(); else scanner.addBlock(null, added);
            });
            assertTrue(writerEntered.await(2, TimeUnit.SECONDS));
            assertFalse(write.isDone());
            releaseCopy.countDown();
            assertEquals(List.of(before), read.get(2, TimeUnit.SECONDS));
            write.get(2, TimeUnit.SECONDS);
            List<BlockPos> after = scanner.getKnownLocationsIncludeUnreachable((Block) null);
            assertEquals(reset ? Set.of() : Set.of(before, added), new HashSet<>(after));
            assertFalse(after.contains(null));
        } finally { releaseCopy.countDown(); workers.shutdownNow(); }
    }
    private static TestScanner scanner(HashSet<BlockPos> source) {
        TestScanner scanner = TestObjects.allocate(TestScanner.class);
        HashMap<Block, HashSet<BlockPos>> tracked = new HashMap<>();
        tracked.put(null, source);
        TestObjects.setField(scanner, BlockScanner.class, "trackedBlocks", tracked);
        TestObjects.setField(scanner, BlockScanner.class, "collectionLock", new Object());
        TestObjects.setField(scanner, BlockScanner.class, "scannedBlocks", new HashMap<>());
        TestObjects.setField(scanner, BlockScanner.class, "scannedChunks", new HashMap<>());
        TestObjects.setField(scanner, BlockScanner.class, "blacklist", new WorldLocateBlacklist());
        TestObjects.setField(scanner, BlockScanner.class, "unreachablePositions", new HashSet<>());
        TestObjects.setField(scanner, BlockScanner.class, "rescanTimer", new FixedTimer());
        return scanner;
    }
    private static class TestScanner extends BlockScanner {
        private TestScanner() { super(null); }
        @Override public boolean isBlockAtPosition(BlockPos pos, Block... blocks) { return true; }
    }
    private static final class FixedTimer extends TimerGame {
        private FixedTimer() { super(1); }
        @Override protected double currentTime() { return 0; }
    }
}
