package lavi.minecraft.diagnostics.blocks.collection;

import adris.altoclef.commands.BlockScanner;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Invoke the real instrumented scanner method with no Minecraft world or constructor effects.
class BlockScannerNativeReadPassthroughTest {
    @Test
    void realNativeReadAlsoPreservesResultsAndExceptionsWithEnabledDiagnosticsInFreshJvm() throws Exception {
        Path output = Files.createTempFile(Path.of(System.getProperty("java.io.tmpdir")), "block-collection-enabled-", ".log");
        Process process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java.exe").toString(),
                "-Djava.io.tmpdir=" + System.getProperty("java.io.tmpdir"),
                "-Duser.dir=" + System.getProperty("user.dir"), "-cp", System.getProperty("java.class.path"),
                BlockScannerEnabledDiagnosticsProbe.class.getName())
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        boolean completed = process.waitFor(30, TimeUnit.SECONDS);
        if (!completed) process.destroyForcibly();
        assertTrue(completed, "Enabled block collection probe timed out: " + output);
        String transcript = Files.readString(output);
        assertEquals(0, process.exitValue(), transcript);
        assertTrue(transcript.contains("BLOCK_COLLECTION_ENABLED_NATIVE_PASS"), transcript);
    }

    @Test
    void originalNativeCopyRunsOnceAndRetainsANullElement() {
        AtomicInteger copies = new AtomicInteger();
        HashSet<BlockPos> set = new HashSet<>() {
            @Override public Object[] toArray() { copies.incrementAndGet(); return new Object[]{null}; }
        };
        List<BlockPos> result = scanner(set).getKnownLocationsIncludeUnreachable((Block) null);
        assertEquals(1, copies.get());
        assertEquals(1, result.size());
        assertNull(result.get(0));
    }

    @Test
    void originalNativeCopyFailurePropagatesByIdentityAfterDiagnosticFinally() {
        AtomicInteger copies = new AtomicInteger();
        ArrayIndexOutOfBoundsException failure = new ArrayIndexOutOfBoundsException("native-copy-fixture");
        HashSet<BlockPos> set = new HashSet<>() {
            @Override public Object[] toArray() { copies.incrementAndGet(); throw failure; }
        };
        ArrayIndexOutOfBoundsException observed = assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> scanner(set).getKnownLocationsIncludeUnreachable((Block) null));
        assertSame(failure, observed);
        assertEquals(1, copies.get());
    }

    private static BlockScanner scanner(HashSet<BlockPos> set) {
        BlockScanner scanner = TestObjects.allocate(BlockScanner.class);
        HashMap<Block, HashSet<BlockPos>> tracked = new HashMap<>();
        tracked.put(null, set);
        TestObjects.setField(scanner, BlockScanner.class, "trackedBlocks", tracked);
        TestObjects.setField(scanner, BlockScanner.class, "collectionLock", new Object());
        return scanner;
    }
}
