package lavi.minecraft.fabric.chatclef.bridge.command.connection.detach;

//20260905_kpopmodder: Read only the task-ownership snapshot used around detach mutations.

import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandContextUnbindDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;

public final class FabricChatClefConnectionDetachOwnershipSnapshotReader {
    public FabricChatClefTaskOwnershipSnapshot capture() {
        return FabricChatClefCommandContextUnbindDiagnostics.captureOwnershipSnapshot();
    }
}
