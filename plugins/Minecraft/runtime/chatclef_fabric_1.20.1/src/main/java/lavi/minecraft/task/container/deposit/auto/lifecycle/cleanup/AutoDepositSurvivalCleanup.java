package lavi.minecraft.task.container.deposit.auto.lifecycle.cleanup;

import adris.altoclef.AltoClef;

import java.util.Objects;

//20260914_kpopmodder: Preserve already-evaluated survival claims across synchronous automatic-owned cleanup.
public final class AutoDepositSurvivalCleanup {
    private AutoDepositSurvivalCleanup() { }

    /**
     * Run only at the automatic chain's synchronous handoff, before the selected chain ticks.
     * Native survival priority evaluation already selects weapons and holds inputs, but its
     * selected movement child has not ticked or published a new path. The old automatic tree
     * must therefore cancel its own path now; deferred cleanup could cancel the new defense path.
     * This wrapper restores only explicit survival inputs, never a former storage weapon or path.
     */
    public static void run(AltoClef mod, Runnable cleanup) {
        run(new MinecraftAutoDepositSurvivalInputPort(mod), cleanup);
    }

    static void run(AutoDepositSurvivalInputPort inputs, Runnable cleanup) {
        Objects.requireNonNull(inputs, "inputs");
        Objects.requireNonNull(cleanup, "cleanup");
        AutoDepositSurvivalInputClaims claims = AutoDepositSurvivalClaimCapture.capture(inputs);
        Throwable cleanupFailure = null;
        try {
            cleanup.run();
        } catch (RuntimeException | Error failure) {
            cleanupFailure = failure;
            throw failure;
        } finally {
            try {
                AutoDepositSurvivalClaimRestore.restore(inputs, claims);
            } catch (RuntimeException | Error restoreFailure) {
                if (cleanupFailure == null) throw restoreFailure;
                if (restoreFailure != cleanupFailure) cleanupFailure.addSuppressed(restoreFailure);
            }
        }
    }
}
