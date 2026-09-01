package lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting;

//20260831_kpopmodder: Keep Store-root terminal classification in a fixed accounting domain.
public enum StoreDepositTerminalClassification {
    NATURAL_FINISH,
    EXPLICIT_STOP,
    UNKNOWN_STOP,
    UNAVAILABLE;

    public static StoreDepositTerminalClassification fromDiagnosticValue(String value) {
        if ("NATURAL_FINISH_OBSERVED".equals(value)) {
            return NATURAL_FINISH;
        }
        if ("EXPLICIT_STOP_CORRELATED".equals(value)) {
            return EXPLICIT_STOP;
        }
        if ("UNKNOWN_STOP".equals(value)) {
            return UNKNOWN_STOP;
        }
        return UNAVAILABLE;
    }

    public boolean abnormal() {
        return this != NATURAL_FINISH;
    }
}
