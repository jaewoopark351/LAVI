package lavi.minecraft.diagnostics.blocks.collection;

import adris.altoclef.commands.BlockScanner;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.blocks.collection.state.BlockCollectionRegistry;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//20260913_kpopmodder: Fresh process proves active diagnostics reached the real native read and its finally.
public final class BlockScannerEnabledDiagnosticsProbe {
    public static void main(String[] args) throws Exception {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        require(BlockCollectionDiagnostics.install().accepted(), "Collection owner registration rejected");
        BlockScanner scanner = TestObjects.allocate(BlockScanner.class);
        AtomicInteger copies = new AtomicInteger();
        HashSet<BlockPos> source = new HashSet<>() {
            @Override public Object[] toArray() { copies.incrementAndGet(); return new Object[]{null}; }
        };
        HashMap<Block, HashSet<BlockPos>> tracked = new HashMap<>();
        tracked.put(null, source);
        TestObjects.setField(scanner, BlockScanner.class, "trackedBlocks", tracked);
        TestObjects.setField(scanner, BlockScanner.class, "collectionLock", new Object());
        List<BlockPos> result = scanner.getKnownLocationsIncludeUnreachable((Block) null);
        require(copies.get() == 1 && result.size() == 1 && result.get(0) == null, "Native copy result changed");
        require(BlockCollectionDiagnostics.readOrigin(result) != null, "Enabled diagnostic read did not retain origin");
        ArrayIndexOutOfBoundsException nativeFailure = new ArrayIndexOutOfBoundsException("enabled-native-copy");
        tracked.put(null, new HashSet<>() {
            @Override public Object[] toArray() { copies.incrementAndGet(); throw nativeFailure; }
        });
        try {
            scanner.getKnownLocationsIncludeUnreachable((Block) null);
            throw new AssertionError("Native failure did not propagate");
        } catch (ArrayIndexOutOfBoundsException observed) {
            require(observed == nativeFailure, "Native exception identity changed");
        }
        require(copies.get() == 2, "Native copy was repeated");
        Field registryField = BlockCollectionRuntime.class.getDeclaredField("REGISTRY");
        registryField.setAccessible(true);
        BlockCollectionRegistry registry = (BlockCollectionRegistry) registryField.get(null);
        Object[] totals = registry.finalSnapshotFields();
        require(Long.valueOf(4).equals(field(totals, "blockCollectionObservedEntries")), "Native diagnostic entry was skipped");
        require(Long.valueOf(2).equals(field(totals, "blockCollectionAbnormalExits")), "Diagnostic finally did not observe both native failure exits");
        require(((Number) field(totals, "blockCollectionEmissionAttempted")).longValue() >= 4, "Enabled output never attempted");
        System.out.println("BLOCK_COLLECTION_ENABLED_NATIVE_PASS");
    }

    private static Object field(Object[] fields, String key) {
        for (int i = 0; i + 1 < fields.length; i += 2) if (key.equals(fields[i])) return fields[i + 1];
        throw new AssertionError("Missing field " + key);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
