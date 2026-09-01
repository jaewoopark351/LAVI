package lavi.minecraft.diagnostics.container.store.deposit.terminal;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

//20260830_kpopmodder: Keep automatic-run correlation alive after each per-item store ledger is purged.
public final class StoreDepositAutomaticLifecycleLedger {
    private static final int MAX_ACTIVE_RUNS = 16;
    private static final int MAX_PENDING_EVICTIONS = 16;
    private static final int MAX_TERMINAL_DEDUPE_IDENTITIES = 1024;
    private static final int MAX_EXPECTED_TERMINAL_IDENTITIES = 1024;
    private static final int MAX_TERMINAL_SCOPES = 32;
    private static final int MAX_DIAGNOSTIC_COVERAGE_GAPS = 256;
    private static final List<String> EXPECTED_TERMINAL_SCOPES = List.of(
            "PER_ITEM_ROOT",
            "CANDIDATE_INVALIDATION",
            "TRANSFER",
            "SLOT_ACTION",
            "SLOT_MUTATION",
            "ROUTE_RECONCILIATION",
            "ROUTE_CHILD"
    );

    private final LinkedHashMap<String, RunState> runs = new LinkedHashMap<>(16, 0.75f, true);
    private final Deque<TerminalRecord> pendingEvictions = new ArrayDeque<>();
    private final IdentityHashMap<Task, RunState> maintenanceBindings = new IdentityHashMap<>();
    private final IdentityHashMap<Task, ChildBinding> childBindings = new IdentityHashMap<>();
    private final IdentityHashMap<Task, Deque<RunState>> userRootBindings = new IdentityHashMap<>();
    private long nextAutoOperationEpoch;
    private long pendingEvictionOverflowCount;
    private long activeRunEvictionEmissionSuppressedCount;

    public synchronized StoreDepositAutomaticContext beginRun(Task maintenanceTask,
                                                               Task userTaskRoot,
                                                               long policyContextEpoch) {
        RunState existing = maintenanceBindings.get(maintenanceTask);
        if (existing != null) {
            touch(existing);
            return existing.baseContext();
        }
        evictOldestIfNeeded();
        long epoch = ++nextAutoOperationEpoch;
        String autoOperationId = "auto-deposit-" + epoch;
        RunState state = new RunState(
                epoch,
                policyContextEpoch,
                autoOperationId,
                autoOperationId + "-maintenance-1",
                autoOperationId + "-pressure-run-1",
                maintenanceTask,
                userTaskRoot
        );
        runs.put(autoOperationId, state);
        if (maintenanceTask != null) {
            maintenanceBindings.put(maintenanceTask, state);
        }
        if (userTaskRoot != null) {
            userRootBindings.computeIfAbsent(userTaskRoot, ignored -> new ArrayDeque<>()).addLast(state);
        }
        return state.baseContext();
    }

    public synchronized StoreDepositAutomaticContext registerChild(Task maintenanceTask,
                                                                    Task childTask,
                                                                    int childIndex) {
        if (maintenanceTask == null || childTask == null) {
            return StoreDepositAutomaticContext.unavailable();
        }
        RunState state = maintenanceBindings.get(maintenanceTask);
        if (state == null) {
            return StoreDepositAutomaticContext.unavailable();
        }
        touch(state);
        ChildBinding existing = childBindings.get(childTask);
        if (existing != null) {
            return existing.context();
        }
        int ordinal = state.nextChildOrdinal++;
        StoreDepositAutomaticContext context = new StoreDepositAutomaticContext(
                true,
                state.autoOperationEpoch,
                state.policyContextEpoch,
                state.autoOperationId,
                state.maintenanceGenerationId,
                state.autoOperationId + "-child-" + ordinal,
                childIndex,
                state.pressureOwnedRunId
        );
        childBindings.put(childTask, new ChildBinding(state, context));
        expectTerminalScopeIdentity(
                context,
                "PER_ITEM_ROOT",
                StoreDepositOperationContext.identity(childTask)
        );
        return context;
    }

    public synchronized StoreDepositAutomaticContext contextForChild(Task childTask) {
        ChildBinding binding = childBindings.get(childTask);
        if (binding != null) {
            touch(binding.state());
        }
        return binding == null ? StoreDepositAutomaticContext.unavailable() : binding.context();
    }

    public synchronized StoreDepositAutomaticContext contextForMaintenance(Task maintenanceTask) {
        RunState state = maintenanceBindings.get(maintenanceTask);
        touch(state);
        return state == null ? StoreDepositAutomaticContext.unavailable() : state.baseContext();
    }

    public synchronized TerminalRecord recordScope(StoreDepositAutomaticContext context,
                                                   String terminalScope,
                                                   String closingIdentity,
                                                   String terminalReason) {
        if (context == null || !context.available()) {
            return TerminalRecord.unavailable();
        }
        RunState state = runs.get(context.autoOperationId());
        return recordScope(state, context, terminalScope, closingIdentity, terminalReason);
    }

    public synchronized void expectTerminalScopeIdentity(StoreDepositAutomaticContext context,
                                                         String terminalScope,
                                                         String closingIdentity) {
        if (context == null || !context.available()) {
            return;
        }
        RunState state = runs.get(context.autoOperationId());
        if (state == null) {
            return;
        }
        String scope = normalize(terminalScope);
        String identity = normalize(closingIdentity);
        if ("UNAVAILABLE".equals(scope) || "UNAVAILABLE".equals(identity)) {
            recordDiagnosticCoverageGap(
                    context,
                    "EXPECTED_TERMINAL_IDENTITY",
                    scope + "|" + identity,
                    "EXPECTED_TERMINAL_IDENTITY_UNAVAILABLE"
            );
            return;
        }
        touch(state);
        String dedupeKey = scope + "|" + identity;
        if (state.expectedTerminalDedupe.containsKey(dedupeKey)) {
            state.expectedTerminalDedupe.get(dedupeKey);
            return;
        }
        if (state.expectedTerminalDedupe.size() >= MAX_EXPECTED_TERMINAL_IDENTITIES) {
            state.expectedTerminalIdentityOverflowCount++;
            state.terminalIdentityCoverageComplete = false;
            return;
        }
        state.expectedTerminalDedupe.put(dedupeKey, Boolean.TRUE);
        state.expectedTerminalCounts.put(
                scope,
                state.expectedTerminalCounts.getOrDefault(scope, 0) + 1
        );
    }

    public synchronized Snapshot recordDiagnosticCoverageGap(
            StoreDepositAutomaticContext context,
            String gapScope,
            String affectedIdentity,
            String reason) {
        if (context == null || !context.available()) {
            return Snapshot.unavailable();
        }
        RunState state = runs.get(context.autoOperationId());
        if (state == null) {
            return Snapshot.unavailable();
        }
        touch(state);
        String key = normalize(gapScope) + "|" + normalize(affectedIdentity);
        if (!state.diagnosticCoverageGaps.containsKey(key)) {
            if (state.diagnosticCoverageGaps.size() >= MAX_DIAGNOSTIC_COVERAGE_GAPS) {
                state.diagnosticCoverageGapOverflowCount++;
            } else {
                state.diagnosticCoverageGaps.put(key, normalize(reason));
            }
        }
        state.terminalIdentityCoverageComplete = false;
        return state.snapshot();
    }

    public synchronized TerminalRecord recordMaintenanceTerminal(Task maintenanceTask,
                                                                 String terminalReason,
                                                                 String nextLifecycleState) {
        RunState state = maintenanceBindings.get(maintenanceTask);
        if (state == null) {
            return TerminalRecord.unavailable();
        }
        touch(state);
        state.maintenanceLogicalTerminal = true;
        state.nextLifecycleState = normalize(nextLifecycleState);
        return recordScope(
                state,
                state.baseContext(),
                "MAINTENANCE_LOGICAL_TERMINAL",
                StoreDepositOperationContext.identity(maintenanceTask),
                terminalReason
        );
    }

    public synchronized TerminalRecord recordPressureOwnedRunClose(Task maintenanceTask,
                                                                   String terminalReason) {
        RunState state = maintenanceBindings.get(maintenanceTask);
        if (state == null) {
            return TerminalRecord.unavailable();
        }
        touch(state);
        state.pressureOwnedRunClosed = true;
        return recordScope(
                state,
                state.baseContext(),
                "PRESSURE_OWNED_RUN",
                StoreDepositOperationContext.identity(maintenanceTask),
                terminalReason
        );
    }

    public synchronized TerminalRecord recordRunToWait(Task maintenanceTask,
                                                       String terminalReason,
                                                       String nextLifecycleState) {
        RunState state = maintenanceBindings.get(maintenanceTask);
        if (state == null) {
            return TerminalRecord.unavailable();
        }
        touch(state);
        state.nextLifecycleState = normalize(nextLifecycleState);
        return recordScope(
                state,
                state.baseContext(),
                "RUNNING_TO_WAIT_FOR_REARM",
                StoreDepositOperationContext.identity(maintenanceTask),
                terminalReason
        );
    }

    public synchronized TerminalRecord recordCoverageClose(Task maintenanceTask,
                                                           String terminalReason) {
        RunState state = maintenanceBindings.get(maintenanceTask);
        if (state == null) {
            return TerminalRecord.unavailable();
        }
        touch(state);
        state.diagnosticCoverageClosed = true;
        return recordScope(
                state,
                state.baseContext(),
                "AUTOMATIC_DIAGNOSTIC_COVERAGE",
                StoreDepositOperationContext.identity(maintenanceTask),
                terminalReason
        );
    }

    public synchronized TerminalRecord recordCoverageCloseIfNoUserTask(Task maintenanceTask,
                                                                       String terminalReason) {
        RunState state = maintenanceBindings.get(maintenanceTask);
        if (state == null || state.userTaskRoot != null) {
            return TerminalRecord.unavailable();
        }
        touch(state);
        state.diagnosticCoverageClosed = true;
        return recordScope(
                state,
                state.baseContext(),
                "AUTOMATIC_DIAGNOSTIC_COVERAGE",
                StoreDepositOperationContext.identity(maintenanceTask),
                terminalReason
        );
    }

    public synchronized TerminalRecord recordCoverageCloseForUserTask(Task userTaskRoot,
                                                                      String terminalReason) {
        RunState state = firstUserRun(userTaskRoot, candidate -> !candidate.diagnosticCoverageClosed);
        if (state == null) {
            return TerminalRecord.unavailable();
        }
        touch(state);
        state.diagnosticCoverageClosed = true;
        return recordScope(
                state,
                state.baseContext(),
                "AUTOMATIC_DIAGNOSTIC_COVERAGE",
                StoreDepositOperationContext.identity(userTaskRoot),
                terminalReason
        );
    }

    public synchronized TerminalRecord observeUserTaskResume(Task task) {
        RunState state = firstUserRun(
                task,
                candidate -> candidate.pressureOwnedRunClosed && !candidate.userTaskResumeObserved
        );
        if (state == null) {
            return TerminalRecord.unavailable();
        }
        touch(state);
        state.userTaskResumeObserved = true;
        return recordScope(
                state,
                state.baseContext(),
                "USER_TASK_RESUME",
                StoreDepositOperationContext.identity(task),
                "USER_ROOT_RECONCILIATION_OBSERVED"
        );
    }

    public synchronized TerminalRecord observeUserTaskNaturalCompletion(Task task) {
        RunState state = firstUserRun(task, candidate -> !candidate.userTaskNaturalCompletionObserved);
        if (state == null) {
            return TerminalRecord.unavailable();
        }
        touch(state);
        state.userTaskNaturalCompletionObserved = true;
        return recordScope(
                state,
                state.baseContext(),
                "USER_TASK_NATURAL_COMPLETION",
                StoreDepositOperationContext.identity(task),
                "NATURAL_FINISH_OBSERVED"
        );
    }

    public synchronized List<TerminalRecord> observeUserTaskNaturalCompletions(Task task) {
        List<TerminalRecord> result = new ArrayList<>();
        for (RunState state : userRuns(task)) {
            if (state.userTaskNaturalCompletionObserved) {
                continue;
            }
            touch(state);
            state.userTaskNaturalCompletionObserved = true;
            result.add(recordScope(
                    state,
                    state.baseContext(),
                    "USER_TASK_NATURAL_COMPLETION",
                    StoreDepositOperationContext.identity(task),
                    "NATURAL_FINISH_OBSERVED"
            ));
        }
        return List.copyOf(result);
    }

    public synchronized List<TerminalRecord> recordCoverageClosesForUserTask(
            Task userTaskRoot,
            String terminalReason) {
        List<TerminalRecord> result = new ArrayList<>();
        for (RunState state : userRuns(userTaskRoot)) {
            if (state.diagnosticCoverageClosed) {
                continue;
            }
            touch(state);
            state.diagnosticCoverageClosed = true;
            result.add(recordScope(
                    state,
                    state.baseContext(),
                    "AUTOMATIC_DIAGNOSTIC_COVERAGE",
                    StoreDepositOperationContext.identity(userTaskRoot),
                    terminalReason
            ));
        }
        return List.copyOf(result);
    }

    public synchronized Snapshot snapshotFor(Task maintenanceTask) {
        RunState state = maintenanceBindings.get(maintenanceTask);
        touch(state);
        return state == null ? Snapshot.unavailable() : state.snapshot();
    }

    public synchronized TerminalRecord takePendingEviction() {
        TerminalRecord record = pendingEvictions.pollFirst();
        return record == null ? TerminalRecord.unavailable() : record;
    }

    synchronized void purgeClosedRun(StoreDepositAutomaticContext context) {
        if (context == null || !context.available()) {
            return;
        }
        RunState state = runs.get(context.autoOperationId());
        if (state != null && state.diagnosticCoverageClosed) {
            removeRun(state);
        }
    }

    public synchronized Snapshot recordTerminalEmissionSuppressed(TerminalRecord record) {
        if (record == null || !record.available() || record.duplicate()) {
            return Snapshot.unavailable();
        }
        RunState state = runs.get(record.context().autoOperationId());
        if (state == null) {
            Snapshot snapshot = record.snapshot();
            if (snapshot.available() && snapshot.activeRunLedgerEvictionCount() > 0) {
                activeRunEvictionEmissionSuppressedCount++;
                return snapshot.withTerminalEmissionSuppressed(
                        record.terminalScope(),
                        record.terminalReason(),
                        activeRunEvictionEmissionSuppressedCount
                );
            }
            return Snapshot.unavailable();
        }
        String scope = normalize(record.terminalScope());
        String countedScope = scope;
        if (!state.terminalEmissionSuppressedCounts.containsKey(countedScope)
                && state.terminalEmissionSuppressedCounts.size() >= MAX_TERMINAL_SCOPES) {
            countedScope = "OTHER_SCOPE_OVERFLOW";
        }
        state.terminalEmissionSuppressedCounts.put(
                countedScope,
                state.terminalEmissionSuppressedCounts.getOrDefault(countedScope, 0) + 1
        );
        state.terminalEmissionSuppressedReasons.putIfAbsent(
                countedScope,
                normalize(record.terminalReason())
        );
        state.terminalEmissionSuppressedCount++;
        state.terminalIdentityCoverageComplete = false;
        return state.snapshot();
    }

    public synchronized ClearResult clearForModeTransition() {
        ClearResult result = new ClearResult(
                runs.size(),
                pendingEvictions.size(),
                maintenanceBindings.size(),
                childBindings.size(),
                userRootBindings.size()
        );
        runs.clear();
        pendingEvictions.clear();
        maintenanceBindings.clear();
        childBindings.clear();
        userRootBindings.clear();
        return result;
    }

    public synchronized int activeRunCount() {
        return runs.size();
    }

    private TerminalRecord recordScope(RunState state,
                                       StoreDepositAutomaticContext context,
                                       String terminalScope,
                                       String closingIdentity,
                                       String terminalReason) {
        if (state == null) {
            return TerminalRecord.unavailable();
        }
        String scope = normalize(terminalScope);
        String identity = normalize(closingIdentity);
        if ("UNAVAILABLE".equals(identity)) {
            state.terminalIdentityCoverageComplete = false;
        }
        String dedupeKey = scope + "|" + identity;
        if (state.terminalDedupe.containsKey(dedupeKey)) {
            state.terminalDedupe.get(dedupeKey);
            return TerminalRecord.duplicate(context, scope, identity, terminalReason, state.snapshot());
        }
        if (state.terminalDedupe.size() >= MAX_TERMINAL_DEDUPE_IDENTITIES) {
            String oldest = state.terminalDedupe.keySet().iterator().next();
            state.terminalDedupe.remove(oldest);
            state.terminalDedupeEvictionCount++;
            state.terminalIdentityCoverageComplete = false;
        }
        state.terminalDedupe.put(dedupeKey, Boolean.TRUE);
        String countedScope = scope;
        if (!state.terminalCounts.containsKey(countedScope)
                && state.terminalCounts.size() >= MAX_TERMINAL_SCOPES) {
            countedScope = "OTHER_SCOPE_OVERFLOW";
            state.terminalScopeOverflowCount++;
            state.terminalIdentityCoverageComplete = false;
        }
        state.terminalCounts.put(
                countedScope,
                state.terminalCounts.getOrDefault(countedScope, 0) + 1
        );
        return new TerminalRecord(
                true,
                false,
                context,
                scope,
                normalize(terminalReason),
                identity,
                state.snapshot()
        );
    }

    private void evictOldestIfNeeded() {
        if (runs.size() < MAX_ACTIVE_RUNS) {
            return;
        }
        RunState oldest = runs.values().iterator().next();
        if (pendingEvictions.size() >= MAX_PENDING_EVICTIONS) {
            pendingEvictions.removeFirst();
            pendingEvictionOverflowCount++;
        }
        oldest.diagnosticCoverageClosed = true;
        oldest.terminalIdentityCoverageComplete = false;
        oldest.activeRunLedgerEvictionCount++;
        oldest.activeRunLedgerEvictionReason = "ACTIVE_RUN_LEDGER_CAP_EVICTION";
        oldest.activeRunEvictionQueueOverflowCount = pendingEvictionOverflowCount;
        pendingEvictions.addLast(recordScope(
                oldest,
                oldest.baseContext(),
                "AUTOMATIC_DIAGNOSTIC_COVERAGE",
                StoreDepositOperationContext.identity(oldest.maintenanceTask),
                oldest.activeRunLedgerEvictionReason
        ));
        removeRun(oldest);
    }

    private void removeRun(RunState state) {
        runs.remove(state.autoOperationId);
        maintenanceBindings.entrySet().removeIf(entry -> entry.getValue() == state);
        childBindings.entrySet().removeIf(entry -> entry.getValue().state() == state);
        userRootBindings.values().forEach(states -> states.removeIf(candidate -> candidate == state));
        userRootBindings.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    private void touch(RunState state) {
        if (state != null) {
            runs.get(state.autoOperationId);
        }
    }

    private RunState firstUserRun(Task task, Predicate<RunState> predicate) {
        for (RunState state : userRuns(task)) {
            if (predicate.test(state)) {
                touch(state);
                return state;
            }
        }
        return null;
    }

    private List<RunState> userRuns(Task task) {
        Deque<RunState> states = userRootBindings.get(task);
        return states == null ? List.of() : List.copyOf(states);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "UNAVAILABLE" : value;
    }

    private record ChildBinding(RunState state, StoreDepositAutomaticContext context) {
    }

    private final class RunState {
        private final long autoOperationEpoch;
        private final long policyContextEpoch;
        private final String autoOperationId;
        private final String maintenanceGenerationId;
        private final String pressureOwnedRunId;
        private final Task maintenanceTask;
        private final Task userTaskRoot;
        private final Map<String, Integer> terminalCounts = new LinkedHashMap<>();
        private final Map<String, Integer> expectedTerminalCounts = new LinkedHashMap<>();
        private final Map<String, Integer> terminalEmissionSuppressedCounts = new LinkedHashMap<>();
        private final Map<String, String> terminalEmissionSuppressedReasons = new LinkedHashMap<>();
        private final Map<String, String> diagnosticCoverageGaps = new LinkedHashMap<>();
        private final LinkedHashMap<String, Boolean> terminalDedupe =
                new LinkedHashMap<>(64, 0.75f, true);
        private final LinkedHashMap<String, Boolean> expectedTerminalDedupe =
                new LinkedHashMap<>(64, 0.75f, true);
        private int nextChildOrdinal = 1;
        private boolean maintenanceLogicalTerminal;
        private boolean pressureOwnedRunClosed;
        private boolean diagnosticCoverageClosed;
        private boolean userTaskResumeObserved;
        private boolean userTaskNaturalCompletionObserved;
        private boolean terminalIdentityCoverageComplete = true;
        private long terminalDedupeEvictionCount;
        private long expectedTerminalIdentityOverflowCount;
        private long diagnosticCoverageGapOverflowCount;
        private long terminalScopeOverflowCount;
        private long terminalEmissionSuppressedCount;
        private long activeRunLedgerEvictionCount;
        private long activeRunEvictionQueueOverflowCount;
        private String activeRunLedgerEvictionReason = "NONE";
        private String nextLifecycleState = "UNAVAILABLE";

        private RunState(long autoOperationEpoch,
                         long policyContextEpoch,
                         String autoOperationId,
                         String maintenanceGenerationId,
                         String pressureOwnedRunId,
                         Task maintenanceTask,
                         Task userTaskRoot) {
            this.autoOperationEpoch = autoOperationEpoch;
            this.policyContextEpoch = policyContextEpoch;
            this.autoOperationId = autoOperationId;
            this.maintenanceGenerationId = maintenanceGenerationId;
            this.pressureOwnedRunId = pressureOwnedRunId;
            this.maintenanceTask = maintenanceTask;
            this.userTaskRoot = userTaskRoot;
        }

        private StoreDepositAutomaticContext baseContext() {
            return new StoreDepositAutomaticContext(
                    true,
                    autoOperationEpoch,
                    policyContextEpoch,
                    autoOperationId,
                    maintenanceGenerationId,
                    "UNAVAILABLE",
                    -1,
                    pressureOwnedRunId
            );
        }

        private Snapshot snapshot() {
            return new Snapshot(
                    true,
                    baseContext(),
                    Map.copyOf(terminalCounts),
                    Map.copyOf(expectedTerminalCounts),
                    Set.copyOf(expectedTerminalDedupe.keySet()),
                    Set.copyOf(terminalDedupe.keySet()),
                    expectedTerminalIdentityOverflowCount,
                    Map.copyOf(diagnosticCoverageGaps),
                    diagnosticCoverageGapOverflowCount,
                    nextChildOrdinal - 1,
                    maintenanceLogicalTerminal,
                    pressureOwnedRunClosed,
                    diagnosticCoverageClosed,
                    userTaskResumeObserved,
                    userTaskNaturalCompletionObserved,
                    terminalIdentityCoverageComplete,
                    terminalDedupeEvictionCount,
                    terminalScopeOverflowCount,
                    terminalEmissionSuppressedCount,
                    Map.copyOf(terminalEmissionSuppressedCounts),
                    Map.copyOf(terminalEmissionSuppressedReasons),
                    activeRunLedgerEvictionCount,
                    activeRunEvictionQueueOverflowCount,
                    activeRunEvictionEmissionSuppressedCount,
                    activeRunLedgerEvictionReason,
                    nextLifecycleState,
                    StoreDepositOperationContext.identity(maintenanceTask),
                    StoreDepositOperationContext.identity(userTaskRoot)
            );
        }
    }

    public record Snapshot(boolean available,
                           StoreDepositAutomaticContext context,
                           Map<String, Integer> terminalCounts,
                           Map<String, Integer> expectedTerminalCounts,
                           Set<String> expectedTerminalIdentities,
                           Set<String> observedTerminalIdentities,
                           long expectedTerminalIdentityOverflowCount,
                           Map<String, String> diagnosticCoverageGaps,
                           long diagnosticCoverageGapOverflowCount,
                           int registeredChildCount,
                           boolean maintenanceLogicalTerminal,
                           boolean pressureOwnedRunClosed,
                           boolean diagnosticCoverageClosed,
                           boolean userTaskResumeObserved,
                           boolean userTaskNaturalCompletionObserved,
                           boolean terminalIdentityCoverageComplete,
                           long terminalDedupeEvictionCount,
                           long terminalScopeOverflowCount,
                           long terminalEmissionSuppressedCount,
                           Map<String, Integer> terminalEmissionSuppressedCounts,
                           Map<String, String> terminalEmissionSuppressedReasons,
                           long activeRunLedgerEvictionCount,
                           long activeRunEvictionQueueOverflowCount,
                           long activeRunEvictionEmissionSuppressedCount,
                           String activeRunLedgerEvictionReason,
                           String nextLifecycleState,
                           String maintenanceIdentity,
                           String userTaskRootIdentity) {
        public static Snapshot unavailable() {
            return new Snapshot(
                    false,
                    StoreDepositAutomaticContext.unavailable(),
                    Map.of(),
                    Map.of(),
                    Set.of(),
                    Set.of(),
                    0L,
                    Map.of(),
                    0L,
                    0,
                    false,
                    false,
                    false,
                    false,
                    false,
                    true,
                    0L,
                    0L,
                    0L,
                    Map.of(),
                    Map.of(),
                    0L,
                    0L,
                    0L,
                    "NONE",
                    "UNAVAILABLE",
                    "UNAVAILABLE",
                    "UNAVAILABLE"
            );
        }

        public int terminalCount(String scope) {
            return terminalCounts.getOrDefault(scope, 0);
        }

        public int expectedTerminalCount(String scope) {
            return expectedTerminalCounts.getOrDefault(scope, 0);
        }

        @Override
        public boolean terminalIdentityCoverageComplete() {
            return terminalIdentityCoverageComplete
                    && exactExpectedIdentitiesComplete()
                    && diagnosticCoverageGaps.isEmpty()
                    && diagnosticCoverageGapOverflowCount == 0
                    && observedTerminalIdentities.stream()
                            .noneMatch(key -> key.endsWith("|UNAVAILABLE"));
        }

        public boolean lifecycleCoverageComplete() {
            return "NONE".equals(missingLifecycleBoundaries());
        }

        public String missingLifecycleBoundaries() {
            List<String> missing = new ArrayList<>();
            int expectedPerItemRoots = Math.max(1, registeredChildCount);
            int observedPerItemRoots = terminalCount("PER_ITEM_ROOT");
            if (observedPerItemRoots < expectedPerItemRoots) {
                missing.add("PER_ITEM_ROOT_TERMINALS(expected="
                        + expectedPerItemRoots
                        + ",observed="
                        + observedPerItemRoots
                        + ")");
            }
            if (!maintenanceLogicalTerminal
                    || terminalCount("MAINTENANCE_LOGICAL_TERMINAL") == 0) {
                missing.add("MAINTENANCE_LOGICAL_TERMINAL");
            }
            if (!pressureOwnedRunClosed || terminalCount("PRESSURE_OWNED_RUN") == 0) {
                missing.add("PRESSURE_OWNED_RUN");
            }
            if (terminalCount("RUNNING_TO_WAIT_FOR_REARM") == 0) {
                missing.add("RUNNING_TO_WAIT_FOR_REARM");
            }
            if (!diagnosticCoverageClosed
                    || terminalCount("AUTOMATIC_DIAGNOSTIC_COVERAGE") == 0) {
                missing.add("AUTOMATIC_DIAGNOSTIC_COVERAGE");
            }
            if (!"UNAVAILABLE".equals(userTaskRootIdentity)) {
                if (!userTaskResumeObserved || terminalCount("USER_TASK_RESUME") == 0) {
                    missing.add("USER_TASK_RESUME");
                }
                if (!userTaskNaturalCompletionObserved
                        || terminalCount("USER_TASK_NATURAL_COMPLETION") == 0) {
                    missing.add("USER_TASK_NATURAL_COMPLETION");
                }
            }
            for (String scope : EXPECTED_TERMINAL_SCOPES) {
                int expected = expectedTerminalCount(scope);
                int observed = terminalCount(scope);
                if (observed < expected) {
                    missing.add("EXPECTED_"
                            + scope
                            + "_TERMINALS(expected="
                            + expected
                            + ",observed="
                            + observed
                            + ")");
                }
                List<String> expectedIdentities = identitiesFor(expectedTerminalIdentities, scope);
                List<String> observedIdentities = identitiesFor(observedTerminalIdentities, scope);
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
            List<String> unavailableIdentityScopes = observedTerminalIdentities.stream()
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
            if (expectedTerminalIdentityOverflowCount > 0) {
                missing.add("EXPECTED_TERMINAL_IDENTITY_OVERFLOW(count="
                        + expectedTerminalIdentityOverflowCount
                        + ")");
            }
            if (!diagnosticCoverageGaps.isEmpty()) {
                List<String> gapKeys = diagnosticCoverageGaps.keySet().stream()
                        .sorted()
                        .toList();
                missing.add("DIAGNOSTIC_COVERAGE_GAPS(count="
                        + gapKeys.size()
                        + ",keys="
                        + summarizeIdentities(gapKeys)
                        + ")");
            }
            if (diagnosticCoverageGapOverflowCount > 0) {
                missing.add("DIAGNOSTIC_COVERAGE_GAP_OVERFLOW(count="
                        + diagnosticCoverageGapOverflowCount
                        + ")");
            }
            return missing.isEmpty() ? "NONE" : String.join(",", missing);
        }

        private boolean exactExpectedIdentitiesComplete() {
            for (String scope : EXPECTED_TERMINAL_SCOPES) {
                List<String> expected = identitiesFor(expectedTerminalIdentities, scope);
                if (!Set.copyOf(expected).equals(Set.copyOf(
                        identitiesFor(observedTerminalIdentities, scope)
                ))) {
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

        private Snapshot withTerminalEmissionSuppressed(String scope,
                                                        String reason,
                                                        long activeRunSuppressedCount) {
            Map<String, Integer> counts = new LinkedHashMap<>(terminalEmissionSuppressedCounts);
            Map<String, String> reasons = new LinkedHashMap<>(terminalEmissionSuppressedReasons);
            String normalizedScope = normalize(scope);
            String countedScope = normalizedScope;
            if (!counts.containsKey(countedScope) && counts.size() >= MAX_TERMINAL_SCOPES) {
                countedScope = "OTHER_SCOPE_OVERFLOW";
            }
            counts.put(countedScope, counts.getOrDefault(countedScope, 0) + 1);
            reasons.putIfAbsent(countedScope, normalize(reason));
            return new Snapshot(
                    available,
                    context,
                    terminalCounts,
                    expectedTerminalCounts,
                    expectedTerminalIdentities,
                    observedTerminalIdentities,
                    expectedTerminalIdentityOverflowCount,
                    diagnosticCoverageGaps,
                    diagnosticCoverageGapOverflowCount,
                    registeredChildCount,
                    maintenanceLogicalTerminal,
                    pressureOwnedRunClosed,
                    diagnosticCoverageClosed,
                    userTaskResumeObserved,
                    userTaskNaturalCompletionObserved,
                    false,
                    terminalDedupeEvictionCount,
                    terminalScopeOverflowCount,
                    terminalEmissionSuppressedCount + 1,
                    Map.copyOf(counts),
                    Map.copyOf(reasons),
                    activeRunLedgerEvictionCount,
                    activeRunEvictionQueueOverflowCount,
                    activeRunSuppressedCount,
                    activeRunLedgerEvictionReason,
                    nextLifecycleState,
                    maintenanceIdentity,
                    userTaskRootIdentity
            );
        }
    }

    public record TerminalRecord(boolean available,
                                 boolean duplicate,
                                 StoreDepositAutomaticContext context,
                                 String terminalScope,
                                 String terminalReason,
                                 String closingIdentity,
                                 Snapshot snapshot) {
        public static TerminalRecord unavailable() {
            return new TerminalRecord(
                    false,
                    false,
                    StoreDepositAutomaticContext.unavailable(),
                    "UNAVAILABLE",
                    "UNAVAILABLE",
                    "UNAVAILABLE",
                    Snapshot.unavailable()
            );
        }

        private static TerminalRecord duplicate(StoreDepositAutomaticContext context,
                                                String scope,
                                                String identity,
                                                String reason,
                                                Snapshot snapshot) {
            return new TerminalRecord(
                    true,
                    true,
                    context,
                    scope,
                    normalize(reason),
                    identity,
                    snapshot
            );
        }
    }

    public record ClearResult(int activeRunCount,
                              int pendingEvictionCount,
                              int maintenanceBindingCount,
                              int childBindingCount,
                              int userRootBindingCount) {
        public int totalEntryCount() {
            return activeRunCount
                    + pendingEvictionCount
                    + maintenanceBindingCount
                    + childBindingCount
                    + userRootBindingCount;
        }
    }
}
