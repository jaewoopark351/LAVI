package lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting;

import java.util.Objects;

//20260831_kpopmodder: Expose bounded authoritative-terminal acceptance without conflating overflow causes.
public record StoreDepositTerminalRegistrationResult(
        Status status,
        StoreDepositTerminalAccountingToken token) {

    public StoreDepositTerminalRegistrationResult {
        Objects.requireNonNull(status, "status");
        if ((status == Status.ACCEPTED) != (token != null)) {
            throw new IllegalArgumentException("Only an accepted terminal registration may carry a token.");
        }
    }

    static StoreDepositTerminalRegistrationResult accepted(
            StoreDepositTerminalAccountingToken token) {
        return new StoreDepositTerminalRegistrationResult(
                Status.ACCEPTED,
                Objects.requireNonNull(token, "token")
        );
    }

    static StoreDepositTerminalRegistrationResult rejected(Status status) {
        if (status == Status.ACCEPTED) {
            throw new IllegalArgumentException("An accepted registration requires its token.");
        }
        return new StoreDepositTerminalRegistrationResult(status, null);
    }

    public boolean accepted() {
        return status == Status.ACCEPTED;
    }

    public enum Status {
        ACCEPTED,
        SEQUENCE_UNAVAILABLE,
        ACTIVE_TOKEN_REGISTRY_FULL
    }
}
