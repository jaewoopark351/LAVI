//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefUserTaskFinishedObserver;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.capture.UserRootSnapshotReader;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.diagnostics.RootTerminationTrace;

//20260913_kpopmodder: Observe after bounded drain and before terminal evaluation in the existing client tick order.
public final class RootLifetimeTickObserver {
    private final FabricChatClefUserTaskFinishedObserver observations;
    private final FabricChatClefCommandDiagnostics diagnostics;
    private final UserRootSnapshotReader reader = new UserRootSnapshotReader();
    private final RootTerminationTrace trace = new RootTerminationTrace();
    public RootLifetimeTickObserver(FabricChatClefUserTaskFinishedObserver observations,
                                    FabricChatClefCommandDiagnostics diagnostics) {
        this.observations = observations;
        this.diagnostics = diagnostics;
    }
    public void observe(FabricChatClefCommandExecution execution, FabricChatClefCommandContext activeContext) {
        if (execution == null || activeContext != execution.context() || !execution.rootTermination().bound()
                || execution.context().terminalPayloadCommitted()) return;
        var snapshot = reader.capture();
        execution.rootTermination().observe(snapshot, observations.acceptedThrough(), observations.dequeuedThrough());
        trace.observe(diagnostics, execution, snapshot);
    }
}
//#endif
