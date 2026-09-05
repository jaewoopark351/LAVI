package lavi.minecraft.fabric.chatclef.bridge.command.result.send;

//20260905_kpopmodder: Own only the immutable commit of one ordinary terminal-result payload.

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class FabricChatClefCommandTerminalPayloadCommitState {
    private final AtomicReference<FabricChatClefCommandResultPayload> committedPayload =
            new AtomicReference<>();

    public FabricChatClefCommandResultPayload commit(
            Supplier<FabricChatClefCommandResultPayload> factory
    ) {
        FabricChatClefCommandResultPayload existing = committedPayload.get();
        if (existing != null) {
            return existing;
        }
        FabricChatClefCommandResultPayload candidate = factory.get();
        if (candidate == null) {
            throw new IllegalStateException("terminal result factory returned null");
        }
        if (committedPayload.compareAndSet(null, candidate)) {
            return candidate;
        }
        return committedPayload.get();
    }

    public boolean committed() {
        return committedPayload.get() != null;
    }
}
