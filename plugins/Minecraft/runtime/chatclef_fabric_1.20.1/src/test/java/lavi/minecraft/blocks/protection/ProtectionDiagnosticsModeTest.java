package lavi.minecraft.blocks.protection;

import lavi.minecraft.blocks.protection.diagnostics.ProtectionPublicationDiagnostics;
import lavi.minecraft.blocks.protection.state.ProtectionSnapshot;
import lavi.minecraft.blocks.scanner.diagnostics.ScannerLifecycleDiagnostics;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: OFF must not spend local capacity; a new ON activation still observes an unchanged world/status.
class ProtectionDiagnosticsModeTest {
    @Test void disabledObservationDoesNotCollectStateOrSpendEitherLocalAllowance() throws Exception {
        boolean enabled = ChatClefDiagnostics.isBoundaryEnabled();
        try {
            ChatClefDiagnostics.setBoundaryEnabled(false);
            ProtectionPublicationDiagnostics protection = new ProtectionPublicationDiagnostics();
            ScannerLifecycleDiagnostics scanner = new ScannerLifecycleDiagnostics();
            for (int n = 0; n < 5; n++) {
                protection.observe(snapshot(), 100, 1000);
                scanner.observe("RESET_INVALIDATED", n, n, "test");
            }
            assertEquals(0, field(protection, "emitted"));
            assertEquals(0L, field(protection, "observedLifetime"));
            assertEquals(0, field(scanner, "attempts"));
        } finally { ChatClefDiagnostics.setBoundaryEnabled(enabled); }
    }
    @Test void reenablingObservesTheSamePublicationWithoutResettingTheProcessAllowance() throws Exception {
        boolean enabled = ChatClefDiagnostics.isBoundaryEnabled();
        try {
            ProtectionPublicationDiagnostics protection = new ProtectionPublicationDiagnostics();
            ProtectionSnapshot snapshot = snapshot();
            ChatClefDiagnostics.setBoundaryEnabled(true);
            protection.observe(snapshot, 0, 0);
            protection.observe(snapshot, 0, 0);
            assertEquals(1, field(protection, "emitted"));
            ChatClefDiagnostics.setBoundaryEnabled(false);
            protection.observe(snapshot, 0, 0);
            assertEquals(1, field(protection, "emitted"));
            ChatClefDiagnostics.setBoundaryEnabled(true);
            protection.observe(snapshot, 0, 0);
            assertEquals(2, field(protection, "emitted"));
        } finally { ChatClefDiagnostics.setBoundaryEnabled(enabled); }
    }
    private static ProtectionSnapshot snapshot() {
        return new ProtectionSnapshot(new Object(), new Object(), "overworld", 1, 1, "READY", Set.of(), Set.of());
    }
    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }
}
