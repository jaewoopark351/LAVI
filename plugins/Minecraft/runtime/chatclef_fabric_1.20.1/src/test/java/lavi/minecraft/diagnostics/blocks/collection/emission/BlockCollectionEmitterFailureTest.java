package lavi.minecraft.diagnostics.blocks.collection.emission;

import lavi.minecraft.diagnostics.blocks.collection.context.BlockCollectionContext;
import lavi.minecraft.diagnostics.blocks.collection.state.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Failed diagnostics in finally cannot replace the native exception already propagating.
class BlockCollectionEmitterFailureTest {
    @Test
    void diagnosticLinkageFailureDoesNotMaskTheOriginalNativeFailure() {
        BlockCollectionRegistry registry = new BlockCollectionRegistry();
        var transition = registry.begin(new Object(), "map", "set", BlockCollectionOperation.READ_COPY, "copy",
                new BlockCollectionContext(0, 0, 1, "worker", "request", "correlation", "session", "1", "true", "none"));
        IllegalStateException nativeFailure = new IllegalStateException("native");
        IllegalStateException observed = assertThrows(IllegalStateException.class, () -> {
            try { throw nativeFailure; }
            finally { BlockCollectionEmitter.emit(registry, transition.events().get(0), event -> { throw new NoClassDefFoundError("diagnostic"); }); }
        });
        assertSame(nativeFailure, observed);
        assertEquals(1L, field(registry.finalSnapshotFields(), "blockCollectionEmissionFailures"));
        assertEquals(0L, field(registry.finalSnapshotFields(), "blockCollectionEmissionCallsReturned"));
    }

    @Test
    void runtimeOutputFailureIsAccountedWithoutClaimingFilePersistence() {
        BlockCollectionRegistry registry = new BlockCollectionRegistry();
        var transition = registry.begin(new Object(), "map", "set", BlockCollectionOperation.READ_COPY, "copy",
                new BlockCollectionContext(0, 0, 1, "worker", "request", "correlation", "session", "1", "true", "none"));
        assertDoesNotThrow(() -> BlockCollectionEmitter.emit(registry, transition.events().get(0), event -> { throw new IllegalStateException("sink"); }));
        assertEquals("NOT_VERIFIED", field(BlockCollectionEmitter.fields(transition.events().get(0)), "filePersistence"));
        assertEquals("THREW:IllegalStateException", field(registry.finalSnapshotFields(), "blockCollectionLastOutputRejection"));
    }
    private static Object field(Object[] fields, String key) {
        for (int i = 0; i + 1 < fields.length; i += 2) if (key.equals(fields[i])) return fields[i + 1];
        fail("Missing field " + key); return null;
    }
}
