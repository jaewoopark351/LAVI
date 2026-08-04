package lavi.minecraft.diagnostics.toolselect.support;

import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260805_kpopmodder: Share tool candidate diagnostic line formatting without changing emitted fields.
public final class ToolCandidateDiagnosticFormatter {
    private ToolCandidateDiagnosticFormatter() {
    }

    public static String equipCandidate(
            Slot slot,
            String stackDetails,
            String suitable,
            String defaultStackSuitable,
            String shouldSave,
            String selectionOutcome,
            String saveDecision,
            String speed
    ) {
        return ChatClefDiagnostics.slotSummary(slot)
                + "#stack=" + stackDetails
                + "#suitable=" + suitable
                + "#defaultStackSuitable=" + defaultStackSuitable
                + "#shouldSave=" + shouldSave
                + "#selectionOutcome=" + selectionOutcome
                + "#saveDecision=" + saveDecision
                + "#speed=" + speed;
    }

    public static String bestToolCandidate(
            Slot slot,
            String stackDetails,
            boolean defaultSuitable,
            String shouldSave,
            String selectionOutcome,
            String saveDecision,
            String speed
    ) {
        return ChatClefDiagnostics.slotSummary(slot)
                + "#stack=" + stackDetails
                + "#defaultStackSuitable=" + defaultSuitable
                + "#shouldSave=" + shouldSave
                + "#selectionOutcome=" + selectionOutcome
                + "#saveDecision=" + saveDecision
                + "#speed=" + speed;
    }

    public static String shearsCandidate(
            Slot slot,
            String stackDetails,
            boolean effective,
            boolean selected
    ) {
        return ChatClefDiagnostics.slotSummary(slot)
                + "#stack=" + stackDetails
                + "#selectionOutcome=" + shearsOutcome(effective, selected)
                + "#saveDecision=not_applicable#reason=SHEARS"
                + "#speed=not_evaluated";
    }

    private static String shearsOutcome(boolean effective, boolean selected) {
        if (!effective) {
            return "SHEARS_NOT_EFFECTIVE";
        }
        return selected ? "SHEARS_SELECTED" : "SHEARS_EFFECTIVE";
    }
}
