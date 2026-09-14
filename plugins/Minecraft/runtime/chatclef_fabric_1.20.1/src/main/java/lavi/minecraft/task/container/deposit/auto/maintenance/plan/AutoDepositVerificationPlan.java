package lavi.minecraft.task.container.deposit.auto.maintenance.plan;

import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;

import java.util.Objects;

//20260914_kpopmodder: A completed storage purpose may resume only outstanding verification/recovery work.
public final class AutoDepositVerificationPlan {
    private AutoDepositVerificationPlan() { }

    public static AutoDepositPlan from(AutoDepositPlan current) {
        return Objects.requireNonNull(current, "current").verificationOnlyCopy();
    }
}
