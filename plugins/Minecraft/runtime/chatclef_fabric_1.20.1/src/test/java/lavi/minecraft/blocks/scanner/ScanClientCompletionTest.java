package lavi.minecraft.blocks.scanner;

import adris.altoclef.AltoClef;
import adris.altoclef.commands.BlockScanner;
import lavi.minecraft.blocks.scanner.diagnostics.ScannerLifecycleDiagnostics;
import lavi.minecraft.blocks.scanner.snapshot.ScanResultSnapshot;
import lavi.minecraft.blocks.scanner.state.ScanRunCompletion;
import lavi.minecraft.blocks.scanner.state.ScanRunLease;
import lavi.minecraft.blocks.scanner.state.ScanWorldBinding;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Exercise the actual scanner's client commit independently of a live world or worker schedule.
class ScanClientCompletionTest {
    @Test void aCompletedWorkerDoesNotPublishUntilTheClientCommitBoundary() throws Exception {
        ScanRunLease run = lease(0);
        BlockScanner scanner = scanner(run);
        ScanResultSnapshot result = new ScanResultSnapshot(Map.of(), Map.of(new ChunkPos(5, 6), 77L));
        TestObjects.setField(scanner, BlockScanner.class, "completedRun", new ScanRunCompletion(run, result, "NONE"));
        assertEquals(Map.of(), field(scanner, "scannedChunks"));
        assertSame(run, field(scanner, "activeRun"));
        commit(scanner);
        assertEquals(Map.of(new ChunkPos(5, 6), 77L), field(scanner, "scannedChunks"));
        assertNull(field(scanner, "activeRun"));
        assertEquals(true, field(scanner, "runCompletionPending"));
    }
    @Test void delayedOldCompletionCannotPublishOrClearReplacementOwnership() throws Exception {
        ScanRunLease old = lease(0), replacement = lease(0);
        BlockScanner scanner = scanner(replacement);
        TestObjects.setField(scanner, BlockScanner.class, "completedRun",
                new ScanRunCompletion(old, new ScanResultSnapshot(Map.of(), Map.of(new ChunkPos(1, 1), 1L)), "NONE"));
        commit(scanner);
        assertSame(replacement, field(scanner, "activeRun"));
        assertEquals(Map.of(), field(scanner, "scannedChunks"));
        assertEquals(false, field(scanner, "runCompletionPending"));
    }
    @Test void sameRunWithDifferentLifetimeCannotPublishItsCompletedResult() throws Exception {
        ScanRunLease wrongLifetime = lease(1);
        BlockScanner scanner = scanner(wrongLifetime);
        TestObjects.setField(scanner, BlockScanner.class, "completedRun",
                new ScanRunCompletion(wrongLifetime, new ScanResultSnapshot(Map.of(), Map.of(new ChunkPos(1, 1), 1L)), "NONE"));
        commit(scanner);
        assertSame(wrongLifetime, field(scanner, "activeRun"));
        assertEquals(Map.of(), field(scanner, "scannedChunks"));
        assertEquals(false, field(scanner, "runCompletionPending"));
    }
    private static ScanRunLease lease(long generation) { return new ScanRunLease(new ScanWorldBinding(null, null, null, generation)); }
    private static BlockScanner scanner(ScanRunLease run) {
        BlockScanner scanner = TestObjects.allocate(BlockScanner.class);
        TestObjects.setField(scanner, BlockScanner.class, "mod", TestObjects.allocate(TestMod.class));
        TestObjects.setField(scanner, BlockScanner.class, "collectionLock", new Object());
        TestObjects.setField(scanner, BlockScanner.class, "activeRun", run);
        TestObjects.setField(scanner, BlockScanner.class, "scannedBlocks", new HashMap<>());
        TestObjects.setField(scanner, BlockScanner.class, "scannedChunks", new HashMap<>());
        TestObjects.setField(scanner, BlockScanner.class, "lifecycleDiagnostics", new ScannerLifecycleDiagnostics());
        return scanner;
    }
    private static Object field(BlockScanner scanner, String name) throws Exception {
        Field field = BlockScanner.class.getDeclaredField(name); field.setAccessible(true); return field.get(scanner);
    }
    private static void commit(BlockScanner scanner) throws Exception {
        Method method = BlockScanner.class.getDeclaredMethod("commitClientCompletion"); method.setAccessible(true); method.invoke(scanner);
    }
    private static final class TestMod extends AltoClef {
        @Override public ClientWorld getWorld() { return null; }
        @Override public ClientPlayerEntity getPlayer() { return null; }
    }
}
