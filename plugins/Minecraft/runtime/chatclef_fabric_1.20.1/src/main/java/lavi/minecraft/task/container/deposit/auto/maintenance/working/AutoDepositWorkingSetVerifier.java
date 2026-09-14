package lavi.minecraft.task.container.deposit.auto.maintenance.working;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositWorkingSetStatus;
import lavi.minecraft.task.container.deposit.auto.recovery.AutoDepositWorkingSetRecovery;

import java.util.Objects;

//20260914_kpopmodder: Validate the existing read-only working-set boundary independently of recovery execution.
public final class AutoDepositWorkingSetVerifier {
    private final AutoDepositWorkingSetRecovery recovery;

    public AutoDepositWorkingSetVerifier(AutoDepositWorkingSetRecovery recovery) {
        this.recovery = Objects.requireNonNull(recovery, "recovery");
    }

    public AutoDepositWorkingSetVerdict verify(AltoClef mod) {
        if (!recovery.available()) {
            return new AutoDepositWorkingSetVerdict(AutoDepositWorkingSetStatus.NOT_APPLICABLE, 0);
        }
        try {
            if (mod == null || mod.getPlayer() == null || mod.getPlayer().getInventory() == null) {
                return unavailable();
            }
            int deficit = recovery.deficitTypeCount(mod);
            return new AutoDepositWorkingSetVerdict(deficit == 0
                    ? AutoDepositWorkingSetStatus.SATISFIED : AutoDepositWorkingSetStatus.DEFICIT, deficit);
        } catch (RuntimeException | LinkageError unavailable) {
            // Read-only evidence boundary; no recovery or other gameplay action is enclosed.
            return unavailable();
        }
    }

    private static AutoDepositWorkingSetVerdict unavailable() {
        return new AutoDepositWorkingSetVerdict(AutoDepositWorkingSetStatus.UNAVAILABLE, -1);
    }
}
