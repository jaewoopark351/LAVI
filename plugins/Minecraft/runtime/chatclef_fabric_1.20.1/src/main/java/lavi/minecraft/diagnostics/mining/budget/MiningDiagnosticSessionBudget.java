package lavi.minecraft.diagnostics.mining.budget;

import java.util.HashMap;
import java.util.Map;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
//20260830_kpopmodder: Reserve bounded mining capacity without changing any observed engine lifecycle.
public final class MiningDiagnosticSessionBudget {
    public static final int CORRELATION_DETAIL_LIMIT = 256;
    public static final int SESSION_HARD_CAP = 5000;
    public static final int RESERVED_CRITICAL_EVENTS = 32;
    public static final int SESSION_DETAIL_LIMIT = SESSION_HARD_CAP - RESERVED_CRITICAL_EVENTS;

    private final Map<String, Integer> correlationDetailEmissions = new HashMap<>();
    private int sessionEmissions;
    private int detailEmissions;
    private int criticalEmissions;
    private int capSignalEmissions;
    private boolean sessionCapSignalClaimed;

    public synchronized MiningDiagnosticAdmission admitDetail(String correlationKey) {
        String normalizedCorrelation = normalize(correlationKey);
        int correlationEmissions = correlationDetailEmissions.getOrDefault(normalizedCorrelation, 0);
        if (sessionEmissions >= SESSION_DETAIL_LIMIT) {
            return denied("SESSION_DETAIL_CAP", claimSessionCapSignal(), normalizedCorrelation,
                    correlationEmissions);
        }
        if (correlationEmissions >= CORRELATION_DETAIL_LIMIT) {
            return denied("CORRELATION_DETAIL_CAP", false, normalizedCorrelation, correlationEmissions);
        }

        correlationDetailEmissions.put(normalizedCorrelation, correlationEmissions + 1);
        detailEmissions++;
        sessionEmissions++;
        return admitted("DETAIL", normalizedCorrelation, correlationEmissions + 1);
    }

    public synchronized MiningDiagnosticAdmission admitCritical(String correlationKey) {
        String normalizedCorrelation = normalize(correlationKey);
        int correlationEmissions = correlationDetailEmissions.getOrDefault(normalizedCorrelation, 0);
        // Before the cap signal is claimed, one final slot remains dedicated to it.
        int criticalAdmissionLimit = sessionCapSignalClaimed
                ? SESSION_HARD_CAP
                : SESSION_HARD_CAP - 1;
        if (sessionEmissions >= criticalAdmissionLimit) {
            return denied("SESSION_HARD_CAP", claimSessionCapSignal(), normalizedCorrelation,
                    correlationEmissions);
        }
        criticalEmissions++;
        sessionEmissions++;
        return admitted("CRITICAL", normalizedCorrelation, correlationEmissions);
    }

    public synchronized MiningDiagnosticAdmission admitAdministrativeDetail(String correlationKey) {
        String normalizedCorrelation = normalize(correlationKey);
        int correlationEmissions = correlationDetailEmissions.getOrDefault(normalizedCorrelation, 0);
        if (sessionEmissions >= SESSION_DETAIL_LIMIT) {
            return denied("SESSION_DETAIL_CAP", claimSessionCapSignal(), normalizedCorrelation,
                    correlationEmissions);
        }
        detailEmissions++;
        sessionEmissions++;
        return admitted("ADMINISTRATIVE_DETAIL", normalizedCorrelation, correlationEmissions);
    }

    public synchronized boolean canAdmitDetail(String correlationKey) {
        String normalizedCorrelation = normalize(correlationKey);
        return sessionEmissions < SESSION_DETAIL_LIMIT
                && correlationDetailEmissions.getOrDefault(normalizedCorrelation, 0) < CORRELATION_DETAIL_LIMIT;
    }

    public synchronized boolean canAdmitCritical() {
        int criticalAdmissionLimit = sessionCapSignalClaimed
                ? SESSION_HARD_CAP
                : SESSION_HARD_CAP - 1;
        return sessionEmissions < criticalAdmissionLimit;
    }

    public synchronized void reset() {
        correlationDetailEmissions.clear();
        sessionEmissions = 0;
        detailEmissions = 0;
        criticalEmissions = 0;
        capSignalEmissions = 0;
        sessionCapSignalClaimed = false;
    }

    public synchronized MiningDiagnosticBudgetSnapshot snapshot() {
        return new MiningDiagnosticBudgetSnapshot(
                sessionEmissions,
                detailEmissions,
                criticalEmissions,
                capSignalEmissions,
                SESSION_HARD_CAP,
                SESSION_DETAIL_LIMIT,
                RESERVED_CRITICAL_EVENTS,
                sessionCapSignalClaimed
        );
    }

    private boolean claimSessionCapSignal() {
        if (sessionCapSignalClaimed || sessionEmissions >= SESSION_HARD_CAP) {
            return false;
        }
        sessionCapSignalClaimed = true;
        capSignalEmissions++;
        sessionEmissions++;
        return true;
    }

    private MiningDiagnosticAdmission admitted(String reason,
                                                String correlationKey,
                                                int correlationEmissions) {
        return new MiningDiagnosticAdmission(
                true,
                false,
                reason,
                correlationKey,
                correlationEmissions,
                CORRELATION_DETAIL_LIMIT,
                snapshot()
        );
    }

    private MiningDiagnosticAdmission denied(String reason,
                                             boolean reportSessionCap,
                                             String correlationKey,
                                             int correlationEmissions) {
        return new MiningDiagnosticAdmission(
                false,
                reportSessionCap,
                reason,
                correlationKey,
                correlationEmissions,
                CORRELATION_DETAIL_LIMIT,
                snapshot()
        );
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "UNAVAILABLE";
        }
        return value.length() <= 360 ? value : value.substring(0, 360) + "...";
    }

}
