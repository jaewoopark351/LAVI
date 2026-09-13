package lavi.minecraft.diagnostics.toolselect;

import lavi.minecraft.diagnostics.toolselect.call.ToolDiagnosticInvocation;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ToolDiagnosticInvocationTest {
    @Test
    void firstInitializationAndLaterErroneousClassAccessCannotInterruptTheAction() {
        AtomicInteger disabled = new AtomicInteger();
        ToolDiagnosticInvocation first = new ToolDiagnosticInvocation("test_init_failure", disabled::incrementAndGet);
        ToolDiagnosticInvocation later = new ToolDiagnosticInvocation("test_init_failure", disabled::incrementAndGet);
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger laterCalls = new AtomicInteger();
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();
        AtomicReference<Throwable> laterFailure = new AtomicReference<>();
        assertEquals(-1L, first.call(() -> {
            firstCalls.incrementAndGet();
            try { return BrokenDiagnostic.operationId(); }
            catch (LinkageError failure) { firstFailure.set(failure); throw failure; }
        }, -1L));
        assertEquals(-1L, later.call(() -> {
            laterCalls.incrementAndGet();
            try { return BrokenDiagnostic.operationId(); }
            catch (LinkageError failure) { laterFailure.set(failure); throw failure; }
        }, -1L));
        assertInstanceOf(ExceptionInInitializerError.class, firstFailure.get());
        assertInstanceOf(NoClassDefFoundError.class, laterFailure.get());
        assertEquals(2, disabled.get());
        assertEquals(1, firstCalls.get());
        assertEquals(1, laterCalls.get());
        assertEquals(-1L, first.call(() -> { fail("Disabled diagnostic was retried"); return 42L; }, -1L));
    }

    @Test
    void diagnosticSnapshotAndCleanupFailureStillPreserveOneUnchangedActionAndItsReturn() {
        ToolDiagnosticInvocation diagnostic = new ToolDiagnosticInvocation("test_snapshot_failure", () -> {
            throw new NoClassDefFoundError("The failed diagnostic cannot be initialized for cleanup either");
        });
        AtomicInteger snapshotReads = new AtomicInteger();
        AtomicInteger actionCalls = new AtomicInteger();
        Object item = new Object();
        Object sourceSlot = new Object();
        Object sourceStack = new Object();
        long id = diagnostic.call(() -> {
            snapshotReads.incrementAndGet();
            throw new IllegalStateException("snapshot read failed");
        }, -1L);
        assertTrue(equip(item, id, sourceSlot, sourceStack, item, sourceSlot, sourceStack, actionCalls));
        diagnostic.run(() -> fail("Result diagnostic ran after owner invalidation"));
        assertEquals(1, snapshotReads.get());
        assertEquals(1, actionCalls.get());
    }

    @Test
    void originalGameExceptionIsOutsideTheDiagnosticGuard() {
        ToolDiagnosticInvocation diagnostic = new ToolDiagnosticInvocation("test_snapshot_failure", () -> { });
        IllegalStateException original = new IllegalStateException("game action failed");
        AtomicInteger actionCalls = new AtomicInteger();
        long id = diagnostic.call(() -> { throw new ExceptionInInitializerError("diagnostic"); }, -1L);
        assertEquals(-1L, id);
        IllegalStateException observed = assertThrows(IllegalStateException.class, () -> {
            actionCalls.incrementAndGet();
            throw original;
        });
        assertSame(original, observed);
        assertEquals(1, actionCalls.get());
    }

    private static boolean equip(Object item, long id, Object sourceSlot, Object sourceStack,
                                 Object expectedItem, Object expectedSlot, Object expectedStack,
                                 AtomicInteger actionCalls) {
        actionCalls.incrementAndGet();
        assertSame(expectedItem, item);
        assertSame(expectedSlot, sourceSlot);
        assertSame(expectedStack, sourceStack);
        assertEquals(-1L, id);
        return true;
    }

    private static final class BrokenDiagnostic {
        private static final long VALUE = failInitialization();
        static long operationId() { return VALUE; }
        static long failInitialization() { throw new IllegalStateException("observer capacity exhausted"); }
    }
}
