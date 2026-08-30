package lavi.minecraft.diagnostics.container.store.deposit.budget;

//20260830_kpopmodder: Keep policy and store projections under one bounded session budget owner.
public final class StoreDepositSharedBudget {
    private static final StoreDepositEmissionGate EMISSION_GATE = new StoreDepositEmissionGate();

    private StoreDepositSharedBudget() {
    }

    public static StoreDepositEmissionGate emissionGate() {
        return EMISSION_GATE;
    }
}
