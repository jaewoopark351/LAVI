package lavi.minecraft.diagnostics.container.store.deposit.budget;

public record StoreDepositTerminalReservation(String operationId,
                                              boolean reserved,
                                              boolean duplicate,
                                              boolean exhausted) {
    static StoreDepositTerminalReservation reserved(String operationId) {
        return new StoreDepositTerminalReservation(operationId, true, false, false);
    }

    static StoreDepositTerminalReservation duplicate(String operationId) {
        return new StoreDepositTerminalReservation(operationId, false, true, false);
    }

    static StoreDepositTerminalReservation exhausted(String operationId) {
        return new StoreDepositTerminalReservation(operationId, false, false, true);
    }
}
