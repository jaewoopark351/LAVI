package lavi.minecraft.diagnostics.command.deposit;

public enum DepositCommandVariant {
    DEPOSIT("deposit", "BARE_DEPOSIT_COMMAND"),
    DEPOSIT_ALL("deposit_all", "BARE_DEPOSIT_ALL_COMMAND");

    private final String commandName;
    private final String requestSource;

    DepositCommandVariant(String commandName, String requestSource) {
        this.commandName = commandName;
        this.requestSource = requestSource;
    }

    public String commandName() {
        return commandName;
    }

    public String requestSource() {
        return requestSource;
    }
}
