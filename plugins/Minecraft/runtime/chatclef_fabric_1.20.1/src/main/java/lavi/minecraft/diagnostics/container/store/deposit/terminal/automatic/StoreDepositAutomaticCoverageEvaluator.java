package lavi.minecraft.diagnostics.container.store.deposit.terminal.automatic;

import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleLedger.Snapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//20260907_kpopmodder: Keep automatic lifecycle completeness projection pure and independent of mutable state.
final class StoreDepositAutomaticCoverageEvaluator {
    private static final List<String> EXPECTED_TERMINAL_SCOPES = List.of(
            "PER_ITEM_ROOT",
            "CANDIDATE_INVALIDATION",
            "TRANSFER",
            "SLOT_ACTION",
            "SLOT_MUTATION",
            "ROUTE_RECONCILIATION",
            "ROUTE_CHILD"
    );

    private StoreDepositAutomaticCoverageEvaluator() {
    }

    static boolean terminalIdentityCoverageComplete(Snapshot snapshot, boolean stateComplete) {
        return stateComplete
                && exactExpectedIdentitiesComplete(snapshot)
                && snapshot.diagnosticCoverageGaps().isEmpty()
                && snapshot.diagnosticCoverageGapOverflowCount() == 0
                && snapshot.observedTerminalIdentities().stream()
                        .noneMatch(key -> key.endsWith("|UNAVAILABLE"));
    }

    static String missingLifecycleBoundaries(Snapshot snapshot) {
        List<String> missing = new ArrayList<>();
        int expectedPerItemRoots = Math.max(1, snapshot.registeredChildCount());
        int observedPerItemRoots = snapshot.terminalCount("PER_ITEM_ROOT");
        if (observedPerItemRoots < expectedPerItemRoots) {
            missing.add("PER_ITEM_ROOT_TERMINALS(expected="
                    + expectedPerItemRoots
                    + ",observed="
                    + observedPerItemRoots
                    + ")");
        }
        if (!snapshot.maintenanceLogicalTerminal()
                || snapshot.terminalCount("MAINTENANCE_LOGICAL_TERMINAL") == 0) {
            missing.add("MAINTENANCE_LOGICAL_TERMINAL");
        }
        if (!snapshot.pressureOwnedRunClosed()
                || snapshot.terminalCount("PRESSURE_OWNED_RUN") == 0) {
            missing.add("PRESSURE_OWNED_RUN");
        }
        if (snapshot.terminalCount("RUNNING_TO_WAIT_FOR_REARM") == 0) {
            missing.add("RUNNING_TO_WAIT_FOR_REARM");
        }
        if (!snapshot.diagnosticCoverageClosed()
                || snapshot.terminalCount("AUTOMATIC_DIAGNOSTIC_COVERAGE") == 0) {
            missing.add("AUTOMATIC_DIAGNOSTIC_COVERAGE");
        }
        if (!"UNAVAILABLE".equals(snapshot.userTaskRootIdentity())) {
            if (!snapshot.userTaskResumeObserved()
                    || snapshot.terminalCount("USER_TASK_RESUME") == 0) {
                missing.add("USER_TASK_RESUME");
            }
            if (!snapshot.userTaskNaturalCompletionObserved()
                    || snapshot.terminalCount("USER_TASK_NATURAL_COMPLETION") == 0) {
                missing.add("USER_TASK_NATURAL_COMPLETION");
            }
        }
        for (String scope : EXPECTED_TERMINAL_SCOPES) {
            int expected = snapshot.expectedTerminalCount(scope);
            int observed = snapshot.terminalCount(scope);
            if (observed < expected) {
                missing.add("EXPECTED_"
                        + scope
                        + "_TERMINALS(expected="
                        + expected
                        + ",observed="
                        + observed
                        + ")");
            }
            List<String> expectedIdentities = identitiesFor(snapshot.expectedTerminalIdentities(), scope);
            List<String> observedIdentities = identitiesFor(snapshot.observedTerminalIdentities(), scope);
            List<String> missingIdentities = expectedIdentities.stream()
                    .filter(identity -> !observedIdentities.contains(identity))
                    .toList();
            List<String> unexpectedIdentities = observedIdentities.stream()
                    .filter(identity -> !expectedIdentities.contains(identity))
                    .toList();
            if (!missingIdentities.isEmpty()) {
                missing.add("MISSING_" + scope + "_TERMINAL_IDENTITIES(count="
                        + missingIdentities.size()
                        + ",ids="
                        + summarizeIdentities(missingIdentities)
                        + ")");
            }
            if (!unexpectedIdentities.isEmpty()) {
                missing.add("UNEXPECTED_" + scope + "_TERMINAL_IDENTITIES(count="
                        + unexpectedIdentities.size()
                        + ",ids="
                        + summarizeIdentities(unexpectedIdentities)
                        + ")");
            }
        }
        List<String> unavailableIdentityScopes = snapshot.observedTerminalIdentities().stream()
                .filter(key -> key.endsWith("|UNAVAILABLE"))
                .map(key -> key.substring(0, key.indexOf('|')))
                .distinct()
                .sorted()
                .toList();
        if (!unavailableIdentityScopes.isEmpty()) {
            missing.add("UNAVAILABLE_TERMINAL_IDENTITIES(scopes="
                    + String.join("|", unavailableIdentityScopes)
                    + ")");
        }
        if (snapshot.expectedTerminalIdentityOverflowCount() > 0) {
            missing.add("EXPECTED_TERMINAL_IDENTITY_OVERFLOW(count="
                    + snapshot.expectedTerminalIdentityOverflowCount()
                    + ")");
        }
        if (!snapshot.diagnosticCoverageGaps().isEmpty()) {
            List<String> gapKeys = snapshot.diagnosticCoverageGaps().keySet().stream()
                    .sorted()
                    .toList();
            missing.add("DIAGNOSTIC_COVERAGE_GAPS(count="
                    + gapKeys.size()
                    + ",keys="
                    + summarizeIdentities(gapKeys)
                    + ")");
        }
        if (snapshot.diagnosticCoverageGapOverflowCount() > 0) {
            missing.add("DIAGNOSTIC_COVERAGE_GAP_OVERFLOW(count="
                    + snapshot.diagnosticCoverageGapOverflowCount()
                    + ")");
        }
        return missing.isEmpty() ? "NONE" : String.join(",", missing);
    }

    private static boolean exactExpectedIdentitiesComplete(Snapshot snapshot) {
        for (String scope : EXPECTED_TERMINAL_SCOPES) {
            List<String> expected = identitiesFor(snapshot.expectedTerminalIdentities(), scope);
            List<String> observed = identitiesFor(snapshot.observedTerminalIdentities(), scope);
            if (!Set.copyOf(expected).equals(Set.copyOf(observed))) {
                return false;
            }
        }
        return true;
    }

    private static List<String> identitiesFor(Set<String> keys, String scope) {
        String prefix = scope + "|";
        return keys.stream()
                .filter(key -> key.startsWith(prefix))
                .map(key -> key.substring(prefix.length()))
                .sorted()
                .toList();
    }

    private static String summarizeIdentities(List<String> identities) {
        int retained = Math.min(4, identities.size());
        List<String> summary = new ArrayList<>(retained + 1);
        for (int index = 0; index < retained; index++) {
            String identity = identities.get(index);
            summary.add(identity.length() <= 96
                    ? identity
                    : identity.substring(0, 96) + "...");
        }
        if (identities.size() > retained) {
            summary.add("+" + (identities.size() - retained) + "_MORE");
        }
        return summary.toString();
    }
}
