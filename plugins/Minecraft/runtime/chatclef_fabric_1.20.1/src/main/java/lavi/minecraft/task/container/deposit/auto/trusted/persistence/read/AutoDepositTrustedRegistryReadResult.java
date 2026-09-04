package lavi.minecraft.task.container.deposit.auto.trusted.persistence.read;

import java.util.Objects;
import java.util.Optional;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public final class AutoDepositTrustedRegistryReadResult {
    private final AutoDepositTrustedRegistryReadStatus status;
    private final AutoDepositTrustedRegistrySnapshot snapshot;
    private final String reason;

    private AutoDepositTrustedRegistryReadResult(
            AutoDepositTrustedRegistryReadStatus status,
            AutoDepositTrustedRegistrySnapshot snapshot,
            String reason) {
        this.status = Objects.requireNonNull(status, "status");
        this.snapshot = snapshot;
        this.reason = Objects.requireNonNull(reason, "reason");
        if (status.usable() != (snapshot != null)) {
            throw new IllegalArgumentException("usable read status must have exactly one snapshot");
        }
    }

    public static AutoDepositTrustedRegistryReadResult success(
            AutoDepositTrustedRegistryReadStatus status,
            AutoDepositTrustedRegistrySnapshot snapshot) {
        if (!Objects.requireNonNull(status, "status").usable()) {
            throw new IllegalArgumentException("success requires a usable read status");
        }
        return new AutoDepositTrustedRegistryReadResult(status, snapshot, status.name().toLowerCase());
    }

    public static AutoDepositTrustedRegistryReadResult failure(String reason) {
        String normalizedReason = reason == null || reason.isBlank()
                ? "registry_read_failed"
                : reason.trim();
        return new AutoDepositTrustedRegistryReadResult(
                AutoDepositTrustedRegistryReadStatus.REGISTRY_READ_FAILED,
                null,
                normalizedReason
        );
    }

    public AutoDepositTrustedRegistryReadStatus status() {
        return status;
    }

    public boolean usable() {
        return status.usable();
    }

    public Optional<AutoDepositTrustedRegistrySnapshot> snapshot() {
        return Optional.ofNullable(snapshot);
    }

    public String reason() {
        return reason;
    }
}
