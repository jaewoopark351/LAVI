package lavi.minecraft.task.container.deposit.auto.rearm;

import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.budget.AutoDepositBudgetStatus;
import lavi.minecraft.task.container.deposit.auto.budget.AutoDepositExecutionBudget;

import java.util.Objects;
import java.util.Locale;
import java.util.function.Supplier;

//20260914_kpopmodder: Preserve normal relief, same-work safety resumes and scoped bounded recovery.
public final class AutoDepositRearmPolicy {
    private enum Phase { INITIAL, RUNNING, SUSPENDED, WAIT_RISE, FOLLOWUP, BLOCKED }

    private final Supplier<AutoDepositExecutionBudget> budgetFactory;
    private final AutoDepositFailureHistory failures = new AutoDepositFailureHistory();
    private final AutoDepositFailureHistory episodeAttempts = new AutoDepositFailureHistory();
    private AutoDepositExecutionBudget budget;
    private Phase phase = Phase.INITIAL;
    private DepositAllInventoryPressureSnapshot logicalStart;
    private DepositAllInventoryPressureSnapshot reliefBaseline;
    private String lastReason = "initial";
    private String failureScope = "automatic";
    private String currentCondition;
    private long episodeSequence = 1;
    private boolean planningBlocked;
    private boolean observationUnavailable;

    public AutoDepositRearmPolicy() { this(AutoDepositExecutionBudget::new); }

    public AutoDepositRearmPolicy(Supplier<AutoDepositExecutionBudget> budgetFactory) {
        this.budgetFactory = Objects.requireNonNull(budgetFactory, "budgetFactory");
        budget = newBudget();
    }

    /** Control, survival and manual-work permission must be checked separately by the caller. */
    public AutoDepositRearmDecision observe(DepositAllInventoryPressureSnapshot pressure,
                                            String semanticConditionKey) {
        return observe(pressure, semanticConditionKey, failureScope);
    }

    /**
     * Keys describe cause-related gameplay conditions, never task/slot identities, diagnostics,
     * mere registry revisions or self-produced preparation inventory. This method spends nothing.
     */
    public AutoDepositRearmDecision observe(DepositAllInventoryPressureSnapshot pressure,
                                            String semanticConditionKey, String candidateScope) {
        String scope = Objects.requireNonNull(candidateScope, "candidateScope");
        if (pressure == null) return AutoDepositRearmDecision.UNKNOWN_PRESSURE;
        if (phase == Phase.RUNNING) return AutoDepositRearmDecision.RUNNING;
        if (logicalStart != null && pressure.totalSlots() != logicalStart.totalSlots()) {
            return AutoDepositRearmDecision.UNKNOWN_PRESSURE;
        }
        // Missing cause evidence cannot admit a new root, resume work, or spend a recovery grant.
        if (semanticConditionKey == null) return AutoDepositRearmDecision.UNKNOWN_CONDITION;
        // A safety-resumed unit may already have relieved pressure but still owe recovery/cleanup.
        if (phase == Phase.SUSPENDED) {
            if (logicalStart == null) {
                return AutoDepositRearmDecision.UNKNOWN_PRESSURE;
            }
            return available(AutoDepositRearmDecision.RESUME);
        }
        if (!pressure.isAtOrAboveThreshold() && logicalStart == null) {
            return AutoDepositRearmDecision.BELOW_THRESHOLD;
        }
        if (phase == Phase.INITIAL) return AutoDepositRearmDecision.INITIAL;
        if (phase == Phase.BLOCKED && observationUnavailable && semanticConditionKey != null) {
            if (failures.contains(scope, semanticConditionKey)) return AutoDepositRearmDecision.SAME_FAILURE;
            // The caller supplies a fresh valid plan/working-set observation before committing this check.
            return budget.canGrantRelatedChange()
                    ? AutoDepositRearmDecision.RELATED_CHANGE : AutoDepositRearmDecision.BUDGET_EXHAUSTED;
        }
        if (phase == Phase.WAIT_RISE) {
            if (failures.contains(scope, semanticConditionKey)) return AutoDepositRearmDecision.SAME_FAILURE;
            return reliefBaseline != null && pressure.totalSlots() == reliefBaseline.totalSlots()
                    ? AutoDepositRearmDecision.PRESSURE_RISE : AutoDepositRearmDecision.UNKNOWN_PRESSURE;
        }
        if (phase == Phase.FOLLOWUP && budget.status() == AutoDepositBudgetStatus.AVAILABLE) {
            if (failures.contains(scope, semanticConditionKey)) return AutoDepositRearmDecision.SAME_FAILURE;
            return AutoDepositRearmDecision.CONTINUE;
        }
        if (episodeAttempts.contains(scope, semanticConditionKey)
                || failures.contains(scope, semanticConditionKey)) return AutoDepositRearmDecision.SAME_FAILURE;
        // A different destination may use existing capacity, but never forgets the failed one.
        if (!planningBlocked && !scope.equals(failureScope)
                && budget.status() == AutoDepositBudgetStatus.AVAILABLE) {
            return AutoDepositRearmDecision.RELATED_CHANGE;
        }
        return budget.canGrantRelatedChange()
                ? AutoDepositRearmDecision.RELATED_CHANGE : AutoDepositRearmDecision.BUDGET_EXHAUSTED;
    }

    public AutoDepositExecutionBudget beginUnit(DepositAllInventoryPressureSnapshot pressure,
                                               String semanticConditionKey) {
        return beginUnit(pressure, semanticConditionKey, failureScope);
    }

    /** Commit immediately before registration, after the caller revalidates current execution rights. */
    public AutoDepositExecutionBudget beginUnit(DepositAllInventoryPressureSnapshot pressure,
                                               String semanticConditionKey, String candidateScope) {
        AutoDepositRearmDecision decision = observe(pressure, semanticConditionKey, candidateScope);
        if (!decision.canEvaluate()) throw new IllegalStateException("Storage is not admitted: " + decision);
        if (decision == AutoDepositRearmDecision.PRESSURE_RISE) renewEpisode();
        if (decision == AutoDepositRearmDecision.RELATED_CHANGE) consumeRelatedChange(candidateScope);
        if (logicalStart == null) logicalStart = pressure;
        failureScope = candidateScope;
        currentCondition = semanticConditionKey;
        episodeAttempts.record(candidateScope, semanticConditionKey);
        reliefBaseline = null;
        phase = Phase.RUNNING;
        planningBlocked = false;
        observationUnavailable = false;
        lastReason = decision.name();
        return budget;
    }

    public void suspend() {
        suspend("safety_suspended");
    }

    public void suspend(String reason) {
        Objects.requireNonNull(reason, "reason");
        if (phase == Phase.RUNNING) {
            phase = Phase.SUSPENDED;
            lastReason = reason;
        }
    }

    /** Explicit control/root replacement abandons old work without inventing a storage failure. */
    public void cancelPendingUnit(String reason) {
        Objects.requireNonNull(reason, "reason");
        if (logicalStart == null) return;
        logicalStart = null;
        reliefBaseline = null;
        currentCondition = null;
        if (phase == Phase.RUNNING || phase == Phase.SUSPENDED) {
            phase = Phase.FOLLOWUP;
            lastReason = reason;
        }
    }

    /** Root-generation validation and successful owned cleanup are prerequisites at the integration boundary. */
    public void finishUnit(boolean normalValidated, DepositAllInventoryPressureSnapshot end,
                           String reason, String failedScope, String semanticConditionKey) {
        if (phase != Phase.RUNNING && phase != Phase.SUSPENDED) return;
        Objects.requireNonNull(failedScope, "failureScope");
        Objects.requireNonNull(reason, "reason");
        budget.completeUnit();
        failureScope = failedScope;
        lastReason = reason;
        episodeAttempts.record(failedScope, semanticConditionKey);
        episodeAttempts.record(failedScope, currentCondition);
        boolean comparable = logicalStart != null && end != null
                && logicalStart.totalSlots() == end.totalSlots();
        String reasonKind = reason.split(":", 2)[0].toLowerCase(Locale.ROOT);
        boolean queryFailure = reasonKind.contains("unavailable");
        observationUnavailable = queryFailure || !comparable && (normalValidated || reasonKind.equals("normal"));
        if (!comparable) {
            phase = Phase.BLOCKED;
            // Missing pressure cannot erase a separately confirmed child/cleanup/storage failure.
            if (observationUnavailable && !queryFailure) lastReason = "pressure_unavailable_or_scope_mismatch";
        } else if (observationUnavailable) {
            phase = Phase.BLOCKED;
        } else if (normalValidated && logicalStart.occupiedSlots() > end.occupiedSlots()) {
            failures.remove(failedScope, semanticConditionKey);
            failures.remove(failedScope, currentCondition);
            if (!end.isAtOrAboveThreshold()) {
                reliefBaseline = end;
                phase = Phase.WAIT_RISE;
            } else {
                phase = Phase.FOLLOWUP;
                // Budget status is reported separately; changing reason would change the retry key family.
            }
        } else {
            phase = Phase.BLOCKED;
            if (normalValidated) lastReason = "no_slot_relief";
        }
        if (phase == Phase.BLOCKED && !observationUnavailable) {
            failures.record(failedScope, semanticConditionKey);
            failures.record(failedScope, currentCondition);
        }
        planningBlocked = false;
        logicalStart = null;
    }

    /** Records a completed planning evaluation, not a storage execution or fabricated zero-progress run. */
    public void markPlanBlocked(String reason, String scope, String semanticConditionKey) {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(reason, "reason");
        if (phase == Phase.RUNNING) throw new IllegalStateException("Cannot replace an active storage result");
        if (phase == Phase.WAIT_RISE) renewEpisode();
        if ((phase == Phase.BLOCKED || phase == Phase.FOLLOWUP)
                && !episodeAttempts.contains(scope, semanticConditionKey)
                && !budget.grantRelatedChange()) {
            lastReason = "related_change_limit";
            phase = Phase.BLOCKED;
            return;
        }
        failureScope = scope;
        observationUnavailable = reason.toLowerCase(Locale.ROOT).contains("unavailable");
        if (!observationUnavailable) failures.record(scope, semanticConditionKey);
        episodeAttempts.record(scope, semanticConditionKey);
        lastReason = reason;
        phase = Phase.BLOCKED;
        planningBlocked = true;
    }

    /** Session/world replacement invalidates old results; it is not proof of a successful storage run. */
    public void resetContext() {
        renewEpisode();
        failures.clear();
        phase = Phase.INITIAL;
        logicalStart = null;
        reliefBaseline = null;
        failureScope = "automatic";
        currentCondition = null;
        planningBlocked = false;
        observationUnavailable = false;
        lastReason = "context_reset";
    }

    public AutoDepositExecutionBudget activeBudget() { return budget; }
    public DepositAllInventoryPressureSnapshot logicalStart() { return logicalStart; }
    public String lastReason() { return lastReason; }
    public long episodeSequence() { return episodeSequence; }
    public boolean hasPendingUnit() { return logicalStart != null; }

    private AutoDepositRearmDecision available(AutoDepositRearmDecision decision) {
        return budget.status() == AutoDepositBudgetStatus.AVAILABLE
                ? decision : AutoDepositRearmDecision.BUDGET_EXHAUSTED;
    }

    private void consumeRelatedChange(String scope) {
        if (!observationUnavailable && !planningBlocked && !scope.equals(failureScope)
                && budget.status() == AutoDepositBudgetStatus.AVAILABLE) return;
        if (!budget.grantRelatedChange()) throw new IllegalStateException("Related-change capacity exhausted");
    }

    private void renewEpisode() {
        budget = newBudget();
        episodeAttempts.clear();
        episodeSequence++;
    }

    private AutoDepositExecutionBudget newBudget() {
        return Objects.requireNonNull(budgetFactory.get(), "budgetFactory result");
    }
}
