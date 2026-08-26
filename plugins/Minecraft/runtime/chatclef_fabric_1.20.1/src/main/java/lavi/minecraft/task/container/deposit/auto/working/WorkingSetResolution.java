package lavi.minecraft.task.container.deposit.auto.working;

import java.util.Objects;
import java.util.Optional;

public record WorkingSetResolution(Status status, WorkingSetSnapshot snapshot, String reason) {
    public WorkingSetResolution {
        Objects.requireNonNull(status, "status");
        reason = reason == null ? "unspecified" : reason;
        if (status == Status.SUPPORTED) {
            Objects.requireNonNull(snapshot, "snapshot");
        }
    }

    public static WorkingSetResolution supported(WorkingSetSnapshot snapshot) {
        return new WorkingSetResolution(Status.SUPPORTED, snapshot, "supported");
    }

    public static WorkingSetResolution unsupported(String reason) {
        return new WorkingSetResolution(Status.UNSUPPORTED, null, reason);
    }

    public Optional<WorkingSetSnapshot> supportedSnapshot() {
        return Optional.ofNullable(snapshot);
    }

    public enum Status {
        SUPPORTED,
        UNSUPPORTED
    }
}
