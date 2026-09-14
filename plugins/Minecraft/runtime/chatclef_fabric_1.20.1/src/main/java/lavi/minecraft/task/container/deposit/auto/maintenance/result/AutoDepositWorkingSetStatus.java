package lavi.minecraft.task.container.deposit.auto.maintenance.result;

//20260914_kpopmodder: Keep missing working-set evidence distinct from legitimate idle non-application.
public enum AutoDepositWorkingSetStatus {
    NOT_EVALUATED, NOT_APPLICABLE, SATISFIED, DEFICIT, UNAVAILABLE;

    public boolean satisfiedOrNotApplicable() {
        return this == SATISFIED || this == NOT_APPLICABLE;
    }
}
