package lavi.minecraft.task.container.home.execution.session;

import java.util.Objects;
import java.util.Optional;

//20260828_kpopmodder: Carry exact handler identity only when all activation gates pass.
public final class HomeStorageContainerActivation {
    private final HomeStorageContainerActivationStatus status;
    private final String reason;
    private final Object handlerIdentity;
    private final int syncId;

    private HomeStorageContainerActivation(
            HomeStorageContainerActivationStatus status,
            String reason,
            Object handlerIdentity,
            int syncId) {
        this.status = Objects.requireNonNull(status, "status");
        this.reason = Objects.requireNonNull(reason, "reason");
        this.handlerIdentity = handlerIdentity;
        this.syncId = syncId;
        if (status == HomeStorageContainerActivationStatus.READY
                && (handlerIdentity == null || syncId < 0)) {
            throw new IllegalArgumentException("ready activation requires a handler binding");
        }
    }

    public static HomeStorageContainerActivation ready(
            Object handlerIdentity,
            int syncId) {
        return new HomeStorageContainerActivation(
                HomeStorageContainerActivationStatus.READY,
                "ready",
                Objects.requireNonNull(handlerIdentity, "handlerIdentity"),
                syncId
        );
    }

    public static HomeStorageContainerActivation refused(
            HomeStorageContainerActivationStatus status,
            String reason) {
        if (status == HomeStorageContainerActivationStatus.READY) {
            throw new IllegalArgumentException("refusal cannot be READY");
        }
        return new HomeStorageContainerActivation(status, reason, null, -1);
    }

    public boolean ready() {
        return status == HomeStorageContainerActivationStatus.READY;
    }

    public HomeStorageContainerActivationStatus status() {
        return status;
    }

    public String reason() {
        return reason;
    }

    public Optional<Object> handlerIdentity() {
        return Optional.ofNullable(handlerIdentity);
    }

    public int syncId() {
        return syncId;
    }
}
