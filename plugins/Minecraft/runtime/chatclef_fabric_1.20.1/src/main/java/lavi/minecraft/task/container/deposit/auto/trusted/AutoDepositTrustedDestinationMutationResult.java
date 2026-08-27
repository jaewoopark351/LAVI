package lavi.minecraft.task.container.deposit.auto.trusted;

import java.util.Objects;
import java.util.Optional;

//20260827_kpopmodder: Report trusted registry persistence outcomes without fabricating command success.
public final class AutoDepositTrustedDestinationMutationResult {
    public enum Status {
        REGISTERED(true),
        UPDATED(true),
        ALREADY_REGISTERED(true),
        REMOVED(true),
        NOT_FOUND(false),
        AMBIGUOUS_ID(false),
        PERSISTENCE_FAILED(false);

        private final boolean success;

        Status(boolean success) {
            this.success = success;
        }

        public boolean success() {
            return success;
        }
    }

    private final Status status;
    private final AutoDepositTrustedDestination destination;
    private final String detail;

    private AutoDepositTrustedDestinationMutationResult(
            Status status,
            AutoDepositTrustedDestination destination,
            String detail) {
        this.status = Objects.requireNonNull(status, "status");
        this.destination = destination;
        this.detail = Objects.requireNonNull(detail, "detail");
    }

    public static AutoDepositTrustedDestinationMutationResult of(
            Status status,
            AutoDepositTrustedDestination destination,
            String detail) {
        return new AutoDepositTrustedDestinationMutationResult(status, destination, detail);
    }

    public Status status() {
        return status;
    }

    public boolean success() {
        return status.success();
    }

    public Optional<AutoDepositTrustedDestination> destination() {
        return Optional.ofNullable(destination);
    }

    public String detail() {
        return detail;
    }
}
