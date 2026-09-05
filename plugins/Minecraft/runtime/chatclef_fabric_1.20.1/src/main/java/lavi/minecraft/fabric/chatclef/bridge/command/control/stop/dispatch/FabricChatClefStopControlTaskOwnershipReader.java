package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch;

//20260905_kpopmodder: Isolate fail-closed task ownership observation for STOP execution.

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;

import java.util.function.Supplier;

public final class FabricChatClefStopControlTaskOwnershipReader {
    private final Supplier<FabricChatClefTaskOwnershipEvidence> delegate;

    public FabricChatClefStopControlTaskOwnershipReader(
            Supplier<FabricChatClefTaskOwnershipEvidence> delegate
    ) {
        this.delegate = delegate;
    }

    public FabricChatClefTaskOwnershipEvidence readOrNull() {
        try {
            return delegate.get();
        } catch (Throwable error) {
            return null;
        }
    }
}
