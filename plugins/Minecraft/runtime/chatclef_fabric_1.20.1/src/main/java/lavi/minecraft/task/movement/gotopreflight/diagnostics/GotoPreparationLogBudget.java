//#if MC == 12001
package lavi.minecraft.task.movement.gotopreflight.diagnostics;

import java.util.HashSet;
import java.util.Set;

//20260913_kpopmodder: Reserve each known owner signature separately from repeated native detail.
public final class GotoPreparationLogBudget {
    static final int MAX_NATIVE_DETAIL = 32;
    static final int LEGACY_SUPPRESSED = 0;
    static final int LEGACY_DETAIL = 1;
    static final int LEGACY_LIMIT = 2;

    private final Set<String> requiredAttempts = new HashSet<>();
    private String lastLegacyReason;
    private int legacyDecisions;
    private String lastPhysicalNativeReason;
    private int nativeDetails;
    private boolean summaryAttempted;

    int legacyDecision(String reason) {
        if (reason.equals(lastLegacyReason)) return LEGACY_SUPPRESSED;
        lastLegacyReason = reason;
        if (legacyDecisions < MAX_NATIVE_DETAIL) {
            legacyDecisions++;
            return LEGACY_DETAIL;
        }
        if (legacyDecisions == MAX_NATIVE_DETAIL) {
            legacyDecisions++;
            return LEGACY_LIMIT;
        }
        return LEGACY_SUPPRESSED;
    }

    /** This decision controls diagnostic output only, never Task state or a terminal result. */
    String admission(String message) {
        boolean nativeEvent = GotoPreparationLogFormatter.event(message).equals("NATIVE_DECISION");
        String reason = nativeEvent ? GotoPreparationLogFormatter.value(message, "reason") : "";
        boolean nativeChanged = nativeEvent && !reason.equals(lastPhysicalNativeReason);
        if (nativeEvent) lastPhysicalNativeReason = reason;
        String signature = GotoPreparationLogFormatter.requiredSignature(message);
        if (signature != null && requiredAttempts.add(signature)) return "reserved_first_owner_boundary";
        if (!nativeChanged) return null;
        if (nativeDetails < MAX_NATIVE_DETAIL) {
            nativeDetails++;
            return "bounded_native_detail";
        }
        if (!summaryAttempted) {
            summaryAttempted = true;
            return "native_detail_suppression_summary";
        }
        return null;
    }
}
//#endif
