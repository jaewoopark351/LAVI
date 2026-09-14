//#if MC == 12001
package lavi.minecraft.diagnostics.session.runtime;

import lavi.minecraft.diagnostics.mode.DiagnosticModeController;
import lavi.minecraft.diagnostics.mode.DiagnosticOutputMode;
import lavi.minecraft.diagnostics.session.admission.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Verify deferred terminal protection, atomic exclusion, OFF settlement and foreign-session rejection.
class DiagnosticDeferredTraceTest {
    private DiagnosticSessionRuntime runtime(DiagnosticOutputMode mode) {
        return new DiagnosticSessionRuntime(new DiagnosticModeController(mode),
                new DiagnosticSessionAdmissionAuthority("find-test"));
    }

    @Test void ordinaryCeilingCannotConsumeFourReservedTracesOrTheirTerminals() {
        var runtime = runtime(DiagnosticOutputMode.BOUNDARY);
        var traces = new java.util.ArrayList<List<DiagnosticAdmissionToken>>();
        for (int op = 0; op < 4; op++) traces.add(runtime.reserveTrace(DiagnosticEventFamily.FIND_RESERVED_BOUNDARY, 32));
        for (var trace : traces) assertEquals(32, trace.size());
        for (int detail = 0; detail < DiagnosticSessionLimits.ORDINARY_CEILING + 20; detail++)
            runtime.dispatch(DiagnosticEventFamily.ORDINARY_DETAIL, "DETAIL", () -> {}, ignored -> {});
        long before = runtime.snapshot().admittedSlots();
        assertTrue(runtime.reserveTrace(DiagnosticEventFamily.FIND_RESERVED_BOUNDARY, 32).isEmpty());
        assertEquals(before, runtime.snapshot().admittedSlots(), "excluded trace must not partly reserve");
        AtomicInteger terminals = new AtomicInteger();
        for (var trace : traces) {
            assertNotNull(runtime.emitReserved(trace.get(31), terminals::incrementAndGet));
            for (int index = 0; index < 31; index++) runtime.abandonReserved(trace.get(index));
        }
        assertEquals(4, terminals.get());
        assertEquals(0, runtime.snapshot().emissionPending());
        assertTrue(runtime.snapshot().admittedSlots() <= 5000);
    }

    @Test void offAndSinkFailureNeverLeavePendingReservationsOrDuplicatePhysicalOutput() {
        var runtime = runtime(DiagnosticOutputMode.BOUNDARY);
        var trace = runtime.reserveTrace(DiagnosticEventFamily.FIND_RESERVED_BOUNDARY, 32);
        runtime.emitReserved(trace.get(0), () -> { throw new IllegalStateException("sink"); });
        runtime.setBoundaryEnabled(false);
        assertNull(runtime.emitReserved(trace.get(31), () -> fail("OFF must not reach sink")));
        for (var token : trace) runtime.abandonReserved(token);
        assertEquals(0, runtime.snapshot().emissionPending());
        assertEquals(0, runtime.snapshot().emissionCompleted());
        assertTrue(runtime.reserveTrace(DiagnosticEventFamily.FIND_RESERVED_BOUNDARY, 32).isEmpty());
    }

    @Test void oldSessionTokenCannotEmitAfterOffOnlySessionReplacement() {
        var runtime = runtime(DiagnosticOutputMode.BOUNDARY);
        var old = runtime.reserveTrace(DiagnosticEventFamily.FIND_RESERVED_BOUNDARY, 32);
        for (var token : old) runtime.abandonReserved(token);
        runtime.setBoundaryEnabled(false);
        runtime.replaceOffSessionForTests("replacement");
        runtime.setBoundaryEnabled(true);
        runtime.emitReserved(old.get(0), () -> fail("foreign token cannot emit"));
        assertEquals(0, runtime.snapshot().admittedSlots());
    }
}
//#endif
