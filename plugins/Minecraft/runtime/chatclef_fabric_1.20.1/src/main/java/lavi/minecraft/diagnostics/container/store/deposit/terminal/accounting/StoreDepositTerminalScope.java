package lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting;

//20260831_kpopmodder: Keep proven Store-root scopes separate from companion terminal scopes.
public enum StoreDepositTerminalScope {
    MANUAL,
    AUTOMATIC_GENERAL,
    UNAVAILABLE;

    public static StoreDepositTerminalScope fromRequestSource(String requestSource) {
        if ("BARE_DEPOSIT_COMMAND".equals(requestSource)
                || "BARE_DEPOSIT_ALL_COMMAND".equals(requestSource)) {
            return MANUAL;
        }
        if ("AUTO_DEPOSIT_ALL_CHAIN".equals(requestSource)) {
            return AUTOMATIC_GENERAL;
        }
        return UNAVAILABLE;
    }
}
