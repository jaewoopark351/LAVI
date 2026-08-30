package lavi.minecraft.diagnostics.container.store.deposit.terminal;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
//20260830_kpopmodder: Preserve the automatic-deposit identity chain without changing task ownership.
public record StoreDepositAutomaticContext(boolean available,
                                           long autoOperationEpoch,
                                           long policyContextEpoch,
                                           String autoOperationId,
                                           String maintenanceGenerationId,
                                           String autoChildOperationId,
                                           int autoChildIndex,
                                           String pressureOwnedRunId) {
    private static final String UNAVAILABLE = "UNAVAILABLE";

    public StoreDepositAutomaticContext {
        autoOperationId = normalize(autoOperationId);
        maintenanceGenerationId = normalize(maintenanceGenerationId);
        autoChildOperationId = normalize(autoChildOperationId);
        pressureOwnedRunId = normalize(pressureOwnedRunId);
    }

    public static StoreDepositAutomaticContext unavailable() {
        return new StoreDepositAutomaticContext(
                false,
                -1L,
                -1L,
                UNAVAILABLE,
                UNAVAILABLE,
                UNAVAILABLE,
                -1,
                UNAVAILABLE
        );
    }

    public Object[] fields() {
        return new Object[]{
                "autoContextAvailable", available,
                "autoOperationEpoch", available ? autoOperationEpoch : UNAVAILABLE,
                "policyContextEpoch", available ? policyContextEpoch : UNAVAILABLE,
                "autoOperationId", autoOperationId,
                "maintenanceGenerationId", maintenanceGenerationId,
                "autoChildOperationId", autoChildOperationId,
                "autoChildIndex", available ? autoChildIndex : UNAVAILABLE,
                "pressureOwnedRunId", pressureOwnedRunId
        };
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? UNAVAILABLE : value;
    }
}
