package lavi.minecraft.diagnostics;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.mode.*;
import lavi.minecraft.diagnostics.session.admission.*;
import lavi.minecraft.diagnostics.session.runtime.*;

/** Test-only facade adapter. The emitter, mode, admission, encoding and output below are production sources. */
public final class FindDiagnosticEmitterBridge {
    public static boolean enabled;
    private static DiagnosticSessionRuntime session;
    private static DiagnosticEventEmitter emitter;
    public static void reset(boolean boundary) {
        session = new DiagnosticSessionRuntime(new DiagnosticModeController(
                boundary ? DiagnosticOutputMode.BOUNDARY : DiagnosticOutputMode.OFF),
                new DiagnosticSessionAdmissionAuthority("find-emitter-verification"));
        emitter = new DiagnosticEventEmitter(new DiagnosticTraceState(), new DiagnosticTaskRegistry(), session);
        enabled = true;
    }
    public static void emit(String name, String reason, Object task, Object[] fields) {
        if (enabled) emitter.emitEvent("BOUNDARY", "[LAVI ChatClefBoundary]", name, reason, (Task) task, fields, false);
    }
    public static void exhaustOrdinaryBudget() {
        // Settle real admissions without output; leave critical terminal capacity available.
        for (int i=0; i<DiagnosticSessionLimits.ORDINARY_CEILING; i++)
            session.dispatch(DiagnosticEventFamily.ORDINARY_DETAIL, "FIND", () -> {}, c -> {});
    }
    public static long completed() { return session.snapshot().emissionCompleted(); }
}
