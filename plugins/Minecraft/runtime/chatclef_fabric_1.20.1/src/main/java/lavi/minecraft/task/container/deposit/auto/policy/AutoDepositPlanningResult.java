package lavi.minecraft.task.container.deposit.auto.policy;

import java.util.Objects;
import java.util.Optional;

public final class AutoDepositPlanningResult {
    public enum Status {
        READY,
        NO_SAFE_SURPLUS,
        CONTEXT_CHANGED,
        POLICY_UNAVAILABLE
    }

    private final Status status;
    private final String reason;
    private final AutoDepositPlan plan;
    private final AutoDepositDecisionFingerprint fingerprint;

    private AutoDepositPlanningResult(Status status,
                                      String reason,
                                      AutoDepositPlan plan,
                                      AutoDepositDecisionFingerprint fingerprint) {
        this.status = Objects.requireNonNull(status, "status");
        this.reason = Objects.requireNonNull(reason, "reason");
        this.plan = plan;
        this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
    }

    public static AutoDepositPlanningResult ready(AutoDepositPlan plan) {
        return new AutoDepositPlanningResult(Status.READY, "ready", plan, plan.fingerprint());
    }

    public static AutoDepositPlanningResult noSafe(String reason, AutoDepositPlan plan) {
        return new AutoDepositPlanningResult(Status.NO_SAFE_SURPLUS, reason, plan, plan.fingerprint());
    }

    public static AutoDepositPlanningResult failed(Status status,
                                                   String reason,
                                                   AutoDepositDecisionFingerprint fingerprint) {
        if (status == Status.READY || status == Status.NO_SAFE_SURPLUS) {
            throw new IllegalArgumentException("failed result requires a failure status");
        }
        return new AutoDepositPlanningResult(status, reason, null, fingerprint);
    }

    public Status status() {
        return status;
    }

    public String reason() {
        return reason;
    }

    public Optional<AutoDepositPlan> plan() {
        return Optional.ofNullable(plan);
    }

    public AutoDepositDecisionFingerprint fingerprint() {
        return fingerprint;
    }
}
