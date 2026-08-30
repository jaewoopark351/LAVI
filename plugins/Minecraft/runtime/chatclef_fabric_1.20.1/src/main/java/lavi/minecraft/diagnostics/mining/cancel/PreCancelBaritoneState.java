package lavi.minecraft.diagnostics.mining.cancel;

import adris.altoclef.AltoClef;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260830_kpopmodder: Capture only cheap semantic state needed to gate an existing cancel boundary.
public record PreCancelBaritoneState(String baritonePathing,
                                     String customGoalActive,
                                     String pathPresent,
                                     String coverage) {
    public static PreCancelBaritoneState capture(AltoClef mod) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return unavailable("DIAGNOSTICS_OFF");
        }
        return new PreCancelBaritoneState(
                ChatClefDiagnostics.safeValue(() ->
                        mod != null && mod.getClientBaritone().getPathingBehavior().isPathing()),
                ChatClefDiagnostics.safeValue(() ->
                        mod != null && mod.getClientBaritone().getCustomGoalProcess().isActive()),
                ChatClefDiagnostics.safeValue(() ->
                        mod != null && mod.getClientBaritone().getPathingBehavior().getPath().isPresent()),
                "CHEAP_PRE_CANCEL_SEMANTIC_STATE"
        );
    }

    public static PreCancelBaritoneState unavailable(String reason) {
        String coverage = reason == null || reason.isBlank() ? "UNAVAILABLE" : reason;
        return new PreCancelBaritoneState("unavailable", "unavailable", "unavailable", coverage);
    }

    public Object[] fields(String suffix) {
        String normalizedSuffix = suffix == null ? "" : suffix;
        return new Object[]{
                "baritonePathing" + normalizedSuffix, baritonePathing,
                "customGoalActive" + normalizedSuffix, customGoalActive,
                "pathPresent" + normalizedSuffix, pathPresent,
                "preCancelSnapshotCoverage" + normalizedSuffix, coverage,
                "preCancelDeepSnapshot" + normalizedSuffix,
                "NOT_CAPTURED_BEFORE_SEMANTIC_GATE"
        };
    }
}
