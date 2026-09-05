package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership;

//20260905_kpopmodder: Serialize STOP admission with ordinary offer, dispatch, capture, and exact release.

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

public final class FabricChatClefStopControlAdmissionBarrier {
    private FabricChatClefStopControlAdmissionBarrierToken token;

    public synchronized boolean admitOrdinary(BooleanSupplier commit) {
        return token == null && commit.getAsBoolean();
    }

    public synchronized <T> Optional<T> dispatchOrdinary(Supplier<Optional<T>> commit) {
        return token == null ? commit.get() : Optional.empty();
    }

    public synchronized <T> Optional<T> admitStop(
            FabricChatClefStopControlIdentity identity,
            long javaSocketGeneration,
            Function<FabricChatClefStopControlAdmissionBarrierToken, T> commit
    ) {
        if (token != null) {
            return Optional.empty();
        }
        FabricChatClefStopControlAdmissionBarrierToken candidate =
                new FabricChatClefStopControlAdmissionBarrierToken(identity, javaSocketGeneration);
        token = candidate;
        T committedValue = null;
        try {
            committedValue = commit.apply(candidate);
            return Optional.ofNullable(committedValue);
        } finally {
            if (committedValue == null && token == candidate) {
                token = null;
            }
        }
    }

    public synchronized <T> Optional<T> inspectOwned(
            FabricChatClefStopControlAdmissionBarrierToken expected,
            Supplier<T> observation
    ) {
        if (expected == null || token != expected) {
            return Optional.empty();
        }
        return Optional.ofNullable(observation.get());
    }

    public synchronized boolean release(
            FabricChatClefStopControlAdmissionBarrierToken expected,
            BooleanSupplier queueCommit
    ) {
        if (expected == null || token != expected) {
            return false;
        }
        if (!queueCommit.getAsBoolean()) {
            return false;
        }
        token = null;
        return true;
    }

    public synchronized boolean blocksOrdinary() {
        return token != null;
    }

    public synchronized void resetForShutdown(Runnable queueCommit) {
        queueCommit.run();
        token = null;
    }
}
