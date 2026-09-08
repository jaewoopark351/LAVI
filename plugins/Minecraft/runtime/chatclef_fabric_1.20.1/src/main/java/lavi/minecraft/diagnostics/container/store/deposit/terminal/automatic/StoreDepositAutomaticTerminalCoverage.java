package lavi.minecraft.diagnostics.container.store.deposit.terminal.automatic;

import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleLedger.Snapshot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

//20260907_kpopmodder: Own one automatic run's bounded terminal identity and coverage accounting.
final class StoreDepositAutomaticTerminalCoverage {
    private static final int MAX_TERMINAL_DEDUPE_IDENTITIES = 1024;
    private static final int MAX_EXPECTED_TERMINAL_IDENTITIES = 1024;
    private static final int MAX_TERMINAL_SCOPES = 32;
    private static final int MAX_DIAGNOSTIC_COVERAGE_GAPS = 256;

    private final Map<String, Integer> terminalCounts = new LinkedHashMap<>();
    private final Map<String, Integer> expectedTerminalCounts = new LinkedHashMap<>();
    private final Map<String, Integer> terminalEmissionSuppressedCounts = new LinkedHashMap<>();
    private final Map<String, String> terminalEmissionSuppressedReasons = new LinkedHashMap<>();
    private final Map<String, String> diagnosticCoverageGaps = new LinkedHashMap<>();
    private final LinkedHashMap<String, Boolean> terminalDedupe =
            new LinkedHashMap<>(64, 0.75f, true);
    private final LinkedHashMap<String, Boolean> expectedTerminalDedupe =
            new LinkedHashMap<>(64, 0.75f, true);

    private boolean terminalIdentityCoverageComplete = true;
    private long terminalDedupeEvictionCount;
    private long expectedTerminalIdentityOverflowCount;
    private long diagnosticCoverageGapOverflowCount;
    private long terminalScopeOverflowCount;
    private long terminalEmissionSuppressedCount;

    void expect(String terminalScope, String closingIdentity) {
        String scope = normalize(terminalScope);
        String identity = normalize(closingIdentity);
        if ("UNAVAILABLE".equals(scope) || "UNAVAILABLE".equals(identity)) {
            recordDiagnosticGap(
                    "EXPECTED_TERMINAL_IDENTITY",
                    scope + "|" + identity,
                    "EXPECTED_TERMINAL_IDENTITY_UNAVAILABLE"
            );
            return;
        }
        String dedupeKey = scope + "|" + identity;
        if (expectedTerminalDedupe.containsKey(dedupeKey)) {
            expectedTerminalDedupe.get(dedupeKey);
            return;
        }
        if (expectedTerminalDedupe.size() >= MAX_EXPECTED_TERMINAL_IDENTITIES) {
            expectedTerminalIdentityOverflowCount++;
            terminalIdentityCoverageComplete = false;
            return;
        }
        expectedTerminalDedupe.put(dedupeKey, Boolean.TRUE);
        expectedTerminalCounts.put(scope, expectedTerminalCounts.getOrDefault(scope, 0) + 1);
    }

    TerminalScopeResult record(String terminalScope,
                               String closingIdentity,
                               String terminalReason) {
        String scope = normalize(terminalScope);
        String identity = normalize(closingIdentity);
        String reason = normalize(terminalReason);
        if ("UNAVAILABLE".equals(identity)) {
            terminalIdentityCoverageComplete = false;
        }
        String dedupeKey = scope + "|" + identity;
        if (terminalDedupe.containsKey(dedupeKey)) {
            terminalDedupe.get(dedupeKey);
            return new TerminalScopeResult(true, scope, identity, reason);
        }
        if (terminalDedupe.size() >= MAX_TERMINAL_DEDUPE_IDENTITIES) {
            String oldest = terminalDedupe.keySet().iterator().next();
            terminalDedupe.remove(oldest);
            terminalDedupeEvictionCount++;
            terminalIdentityCoverageComplete = false;
        }
        terminalDedupe.put(dedupeKey, Boolean.TRUE);
        String countedScope = scope;
        if (!terminalCounts.containsKey(countedScope)
                && terminalCounts.size() >= MAX_TERMINAL_SCOPES) {
            countedScope = "OTHER_SCOPE_OVERFLOW";
            terminalScopeOverflowCount++;
            terminalIdentityCoverageComplete = false;
        }
        terminalCounts.put(countedScope, terminalCounts.getOrDefault(countedScope, 0) + 1);
        return new TerminalScopeResult(false, scope, identity, reason);
    }

    void recordDiagnosticGap(String gapScope, String affectedIdentity, String reason) {
        String key = normalize(gapScope) + "|" + normalize(affectedIdentity);
        if (!diagnosticCoverageGaps.containsKey(key)) {
            if (diagnosticCoverageGaps.size() >= MAX_DIAGNOSTIC_COVERAGE_GAPS) {
                diagnosticCoverageGapOverflowCount++;
            } else {
                diagnosticCoverageGaps.put(key, normalize(reason));
            }
        }
        terminalIdentityCoverageComplete = false;
    }

    void recordEmissionSuppressed(String terminalScope, String terminalReason) {
        String scope = normalize(terminalScope);
        String countedScope = scope;
        if (!terminalEmissionSuppressedCounts.containsKey(countedScope)
                && terminalEmissionSuppressedCounts.size() >= MAX_TERMINAL_SCOPES) {
            countedScope = "OTHER_SCOPE_OVERFLOW";
        }
        terminalEmissionSuppressedCounts.put(
                countedScope,
                terminalEmissionSuppressedCounts.getOrDefault(countedScope, 0) + 1
        );
        terminalEmissionSuppressedReasons.putIfAbsent(countedScope, normalize(terminalReason));
        terminalEmissionSuppressedCount++;
        terminalIdentityCoverageComplete = false;
    }

    void markIncomplete() {
        terminalIdentityCoverageComplete = false;
    }

    static Snapshot withEmissionSuppressed(Snapshot snapshot,
                                           String terminalScope,
                                           String terminalReason,
                                           long activeRunSuppressedCount) {
        Map<String, Integer> counts = new LinkedHashMap<>(snapshot.terminalEmissionSuppressedCounts());
        Map<String, String> reasons = new LinkedHashMap<>(snapshot.terminalEmissionSuppressedReasons());
        String countedScope = normalize(terminalScope);
        if (!counts.containsKey(countedScope) && counts.size() >= MAX_TERMINAL_SCOPES) {
            countedScope = "OTHER_SCOPE_OVERFLOW";
        }
        counts.put(countedScope, counts.getOrDefault(countedScope, 0) + 1);
        reasons.putIfAbsent(countedScope, normalize(terminalReason));
        return new Snapshot(
                snapshot.available(),
                snapshot.context(),
                snapshot.terminalCounts(),
                snapshot.expectedTerminalCounts(),
                snapshot.expectedTerminalIdentities(),
                snapshot.observedTerminalIdentities(),
                snapshot.expectedTerminalIdentityOverflowCount(),
                snapshot.diagnosticCoverageGaps(),
                snapshot.diagnosticCoverageGapOverflowCount(),
                snapshot.registeredChildCount(),
                snapshot.maintenanceLogicalTerminal(),
                snapshot.pressureOwnedRunClosed(),
                snapshot.diagnosticCoverageClosed(),
                snapshot.userTaskResumeObserved(),
                snapshot.userTaskNaturalCompletionObserved(),
                false,
                snapshot.terminalDedupeEvictionCount(),
                snapshot.terminalScopeOverflowCount(),
                snapshot.terminalEmissionSuppressedCount() + 1,
                Map.copyOf(counts),
                Map.copyOf(reasons),
                snapshot.activeRunLedgerEvictionCount(),
                snapshot.activeRunEvictionQueueOverflowCount(),
                activeRunSuppressedCount,
                snapshot.activeRunLedgerEvictionReason(),
                snapshot.nextLifecycleState(),
                snapshot.maintenanceIdentity(),
                snapshot.userTaskRootIdentity()
        );
    }

    Map<String, Integer> terminalCounts() {
        return Map.copyOf(terminalCounts);
    }

    Map<String, Integer> expectedTerminalCounts() {
        return Map.copyOf(expectedTerminalCounts);
    }

    Set<String> expectedTerminalIdentities() {
        return Set.copyOf(expectedTerminalDedupe.keySet());
    }

    Set<String> observedTerminalIdentities() {
        return Set.copyOf(terminalDedupe.keySet());
    }

    long expectedTerminalIdentityOverflowCount() {
        return expectedTerminalIdentityOverflowCount;
    }

    Map<String, String> diagnosticCoverageGaps() {
        return Map.copyOf(diagnosticCoverageGaps);
    }

    long diagnosticCoverageGapOverflowCount() {
        return diagnosticCoverageGapOverflowCount;
    }

    boolean terminalIdentityCoverageComplete() {
        return terminalIdentityCoverageComplete;
    }

    long terminalDedupeEvictionCount() {
        return terminalDedupeEvictionCount;
    }

    long terminalScopeOverflowCount() {
        return terminalScopeOverflowCount;
    }

    long terminalEmissionSuppressedCount() {
        return terminalEmissionSuppressedCount;
    }

    Map<String, Integer> terminalEmissionSuppressedCounts() {
        return Map.copyOf(terminalEmissionSuppressedCounts);
    }

    Map<String, String> terminalEmissionSuppressedReasons() {
        return Map.copyOf(terminalEmissionSuppressedReasons);
    }

    static String normalize(String value) {
        return value == null || value.isBlank() ? "UNAVAILABLE" : value;
    }

    record TerminalScopeResult(boolean duplicate,
                               String scope,
                               String identity,
                               String reason) {
    }
}
