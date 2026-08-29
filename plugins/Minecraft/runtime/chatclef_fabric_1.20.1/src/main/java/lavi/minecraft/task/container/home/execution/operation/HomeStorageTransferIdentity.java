package lavi.minecraft.task.container.home.execution.operation;

import java.util.Objects;

//20260828_kpopmodder: Identify one requested transfer within one operation and container session.
public record HomeStorageTransferIdentity(
        long operationId,
        int containerSessionOrdinal,
        long planRevision,
        int attemptOrdinal,
        int logicalPlayerSlot,
        String destinationKey,
        Object handlerIdentity,
        int handlerSyncId) {

    public HomeStorageTransferIdentity {
        if (operationId <= 0L) {
            throw new IllegalArgumentException("operationId must be positive");
        }
        if (containerSessionOrdinal <= 0) {
            throw new IllegalArgumentException("containerSessionOrdinal must be positive");
        }
        if (planRevision <= 0L) {
            throw new IllegalArgumentException("planRevision must be positive");
        }
        if (attemptOrdinal <= 0) {
            throw new IllegalArgumentException("attemptOrdinal must be positive");
        }
        if (logicalPlayerSlot < 0 || logicalPlayerSlot >= 36) {
            throw new IllegalArgumentException("logicalPlayerSlot must be in [0, 35]");
        }
        Objects.requireNonNull(destinationKey, "destinationKey");
        if (destinationKey.isBlank()) {
            throw new IllegalArgumentException("destinationKey must not be blank");
        }
        Objects.requireNonNull(handlerIdentity, "handlerIdentity");
        if (handlerSyncId < 0) {
            throw new IllegalArgumentException("handlerSyncId must be non-negative");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof HomeStorageTransferIdentity identity)) {
            return false;
        }
        return operationId == identity.operationId
                && containerSessionOrdinal == identity.containerSessionOrdinal
                && planRevision == identity.planRevision
                && attemptOrdinal == identity.attemptOrdinal
                && logicalPlayerSlot == identity.logicalPlayerSlot
                && handlerSyncId == identity.handlerSyncId
                && destinationKey.equals(identity.destinationKey)
                && handlerIdentity == identity.handlerIdentity;
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(operationId);
        result = 31 * result + containerSessionOrdinal;
        result = 31 * result + Long.hashCode(planRevision);
        result = 31 * result + attemptOrdinal;
        result = 31 * result + logicalPlayerSlot;
        result = 31 * result + destinationKey.hashCode();
        result = 31 * result + System.identityHashCode(handlerIdentity);
        return 31 * result + handlerSyncId;
    }
}
