package lavi.minecraft.diagnostics.toolselect;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.session.lifecycle.registration.DiagnosticOwnerRegistration;
import lavi.minecraft.diagnostics.toolselect.call.ToolEquipDiagnosticCall;
import lavi.minecraft.diagnostics.toolselect.lifecycle.ToolSelectionDiagnosticStateObserver;
import lavi.minecraft.diagnostics.toolselect.lifecycle.ToolEquipAttemptState;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ToolDiagnosticOwnerLeaseTest {
    @Test
    void offAndUnobservedAttemptSkipSnapshotArgumentsBeforeAnyGameRead() {
        boolean wasEnabled = ChatClefDiagnostics.isBoundaryEnabled();
        AtomicInteger reads = new AtomicInteger();
        try {
            ChatClefDiagnostics.setBoundaryEnabled(false);
            assertEquals(-1L, ToolEquipDiagnosticCall.selection(() -> {
                reads.incrementAndGet();
                throw new AssertionError("OFF snapshot was read");
            }));
            assertEquals("unavailable", ToolEquipDiagnosticCall.capture(2L, () -> {
                reads.incrementAndGet();
                return "unexpected";
            }, "unavailable"));
            ToolEquipDiagnosticCall.result(2L, reads::incrementAndGet);
            ChatClefDiagnostics.setBoundaryEnabled(true);
            assertEquals("unavailable", ToolEquipDiagnosticCall.capture(-1L, () -> {
                reads.incrementAndGet();
                return "unexpected";
            }, "unavailable"));
            ToolEquipDiagnosticCall.result(-1L, reads::incrementAndGet);
            assertEquals(0, reads.get());
            assertEquals(3L, ToolEquipDiagnosticCall.selection(() -> 3L));
        } finally {
            ChatClefDiagnostics.setBoundaryEnabled(wasEnabled);
        }
    }

    @Test
    void actualBestToolScanCannotWriteAfterOffOnOrTeardown() throws Exception {
        boolean wasEnabled = ChatClefDiagnostics.isBoundaryEnabled();
        try {
            ChatClefDiagnostics.setBoundaryEnabled(true);
            BestToolSlotDiagnostics.Scan beforeOff = BestToolSlotDiagnostics.start(null);
            assertTrue((boolean) field(beforeOff.getClass(), "enabled").get(beforeOff));
            ChatClefDiagnostics.setBoundaryEnabled(false);
            ChatClefDiagnostics.setBoundaryEnabled(true);
            beforeOff.observeShearsCandidate(null, null, true, true);
            beforeOff.observeToolCandidate(null, null, null, true, false, 1D, true);
            beforeOff.logReturn(null, "late_scan", 1D);
            assertEquals(0, field(beforeOff.getClass(), "candidateCount").getInt(beforeOff));

            BestToolSlotDiagnostics.Scan beforeTeardown = BestToolSlotDiagnostics.start(null);
            ToolSelectionDiagnosticStateObserver observer = (ToolSelectionDiagnosticStateObserver)
                    field(BestToolSlotDiagnostics.class, "OFF_STATE_OBSERVER").get(null);
            ChatClefDiagnostics.runIfDiagnosticsEligible(() -> observer.afterCleanTeardownSnapshotAttempt(false));
            beforeTeardown.observeToolCandidate(null, null, null, true, false, 1D, true);
            assertEquals(0, field(beforeTeardown.getClass(), "candidateCount").getInt(beforeTeardown));
            assertTrue(((DiagnosticOwnerRegistration) field(BestToolSlotDiagnostics.class, "OWNER").get(null)).isAvailable());
        } finally {
            ChatClefDiagnostics.setBoundaryEnabled(wasEnabled);
        }
    }

    @Test
    void oldEquipAttemptCannotCaptureAfterOffOrAttachItsResultToANewAttempt() throws Exception {
        boolean wasEnabled = ChatClefDiagnostics.isBoundaryEnabled();
        AtomicInteger reads = new AtomicInteger();
        try {
            ChatClefDiagnostics.setBoundaryEnabled(true);
            ToolEquipDiagnostics.withAvailableOwner(() -> null, null);
            ToolEquipAttemptState attempts = (ToolEquipAttemptState) field(ToolEquipDiagnostics.class, "ATTEMPTS").get(null);
            attempts.begin(71L);
            assertEquals(1, ToolEquipDiagnosticCall.capture(71L, reads::incrementAndGet, 0));
            ChatClefDiagnostics.setBoundaryEnabled(false);
            ChatClefDiagnostics.setBoundaryEnabled(true);
            ToolEquipDiagnosticCall.result(71L, reads::incrementAndGet);
            assertEquals(1, reads.get());
            attempts.begin(72L);
            assertEquals(0, ToolEquipDiagnosticCall.capture(71L, reads::incrementAndGet, 0));
            assertEquals(2, ToolEquipDiagnosticCall.capture(72L, reads::incrementAndGet, 0));
            attempts.finish(71L);
            assertTrue(attempts.isCurrent(72L));
            attempts.finish(72L);
            ToolEquipDiagnosticCall.result(72L, reads::incrementAndGet);
            assertEquals(2, reads.get());
        } finally {
            ChatClefDiagnostics.setBoundaryEnabled(false);
            ChatClefDiagnostics.setBoundaryEnabled(wasEnabled);
        }
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
