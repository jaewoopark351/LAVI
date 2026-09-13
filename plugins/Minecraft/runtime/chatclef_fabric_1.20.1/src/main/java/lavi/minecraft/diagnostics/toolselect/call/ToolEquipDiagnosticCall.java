package lavi.minecraft.diagnostics.toolselect.call;

import lavi.minecraft.diagnostics.toolselect.ToolEquipDiagnostics;

import java.util.function.LongSupplier;
import java.util.function.Supplier;

//20260913_kpopmodder: Keep ToolEquip class initialization inside a diagnostic-only call boundary.
public final class ToolEquipDiagnosticCall {
    private static final ToolDiagnosticInvocation INVOCATION =
            new ToolDiagnosticInvocation("tool_equip", () -> ToolEquipDiagnostics.disableOwner("CALL_INITIALIZATION_OR_OBSERVATION_FAILED"));

    private ToolEquipDiagnosticCall() { }

    public static long selection(LongSupplier diagnostic) {
        return INVOCATION.call(() -> ToolEquipDiagnostics.withAvailableOwner(diagnostic::getAsLong, -1L), -1L);
    }

    public static <T> T capture(long equipAttemptId, Supplier<T> diagnostic, T fallback) {
        if (equipAttemptId < 0L) {
            return fallback;
        }
        return INVOCATION.call(() -> ToolEquipDiagnostics.withCurrentAttempt(equipAttemptId, diagnostic, fallback), fallback);
    }

    public static void result(long equipAttemptId, Runnable diagnostic) {
        capture(equipAttemptId, () -> {
            diagnostic.run();
            return null;
        }, null);
    }
}
