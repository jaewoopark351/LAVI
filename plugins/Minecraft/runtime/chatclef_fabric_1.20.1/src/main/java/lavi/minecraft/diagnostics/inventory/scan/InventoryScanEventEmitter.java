package lavi.minecraft.diagnostics.inventory.scan;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.inventory.InventoryDiagnosticFields;
import lavi.minecraft.diagnostics.inventory.InventoryScanContext;
import lavi.minecraft.diagnostics.inventory.InventoryScanEmissionDecision;
import lavi.minecraft.diagnostics.inventory.InventoryScanEventLimiter;

//20260805_kpopmodder: Emit bounded inventory scan diagnostics separate from scan observation.
public final class InventoryScanEventEmitter {
    private final InventoryScanEventLimiter limiter = new InventoryScanEventLimiter();

    public void emitBeginIfNeeded(InventoryScanContext context, String reason) {
        if (context.beginEmitted()) {
            return;
        }
        context.markBeginEmitted();
        emit("INVENTORY_SUBTRACKER_SCAN_BEGIN",
                reason,
                context,
                InventoryDiagnosticFields.scanBegin(context),
                false,
                "SCAN_BEGIN|"
                        + context.beginSnapshot().stableKey()
                        + "|"
                        + context.threadName()
                        + "|"
                        + context.scanDepthOnCurrentThread());
    }

    public void emit(String eventName,
                     String reason,
                     InventoryScanContext context,
                     Object[] fields,
                     boolean critical,
                     String fingerprint) {
        InventoryScanEmissionDecision decision = limiter.evaluate(
                fingerprint,
                ChatClefDiagnostics.currentClientTickId(),
                critical
        );
        if (decision.emitCap()) {
            ChatClefDiagnostics.logBoundary("INVENTORY_SUBTRACKER_DIAGNOSTIC_CAP_REACHED",
                    "inventory_subtracker_diagnostic_cap_reached",
                    null,
                    ChatClefDiagnostics.withCommandContextFields(
                            InventoryDiagnosticFields.cap(InventoryScanEventLimiter.SESSION_HARD_CAP)
                    ));
            return;
        }
        if (decision.emitSummary()) {
            ChatClefDiagnostics.logBoundary("INVENTORY_SUBTRACKER_DIAGNOSTIC_REPEAT_SUMMARY",
                    "inventory_subtracker_repeat_summary",
                    null,
                    ChatClefDiagnostics.withCommandContextFields(
                            InventoryDiagnosticFields.repeatSummary(fingerprint, decision.suppressedRepeatCount())
                    ));
            return;
        }
        if (!decision.emitEvent()) {
            return;
        }
        ChatClefDiagnostics.logBoundary(eventName,
                reason,
                null,
                ChatClefDiagnostics.withCommandContextFields(fields));
    }
}
