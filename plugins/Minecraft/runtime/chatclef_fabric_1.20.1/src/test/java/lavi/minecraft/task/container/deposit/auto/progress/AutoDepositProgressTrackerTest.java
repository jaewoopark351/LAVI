package lavi.minecraft.task.container.deposit.auto.progress;

import org.junit.jupiter.api.Test;

//20260915_kpopmodder: Run the offline-compatible production contract checks in the established Gradle suite too.
class AutoDepositProgressTrackerTest {
    @Test void preparationProgressAndFiniteBudgetContracts() {
        AutoDepositProgressContractChecks.main(new String[0]);
    }
}
