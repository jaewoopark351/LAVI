//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.command.result.gotoresult;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.task.movement.gotoresult.model.GotoTaskResultSource;

import java.util.UUID;

//20260913_kpopmodder: Bridge-local projection owns correlation, while the movement task owns terminal semantics.
public final class GotoCommandResultTracker {
    private Task boundRoot;
    private GotoTaskResultSource source;
    private GotoCommandBinding binding;

    public void bind(FabricChatClefCommandContext context, String command, Task root) {
        if (binding != null || !(root instanceof GotoTaskResultSource candidate)
                || !GotoCommandTargetMatcher.matches(command, candidate.gotoTarget())) return;
        boundRoot = root;
        source = candidate;
        binding = new GotoCommandBinding(context.requestId(), context.correlationId(), context.sessionId(),
                context.serverConnectionGeneration(), context.connectionGeneration(), root.getClass().getName(),
                Integer.toHexString(System.identityHashCode(root)), UUID.randomUUID().toString(), candidate.gotoTarget());
    }

    public FabricChatClefCommandResultDataPayload bindingData(FabricChatClefCommandResultDataPayload base) {
        return binding == null ? base : new GotoCommandResultDataPayload(base, binding, null);
    }

    public boolean hasBinding() { return binding != null; }

    public FabricChatClefCommandResultPayload matchingCompletion(
            FabricChatClefCommandResultDataPayload base,
            FabricChatClefCommandTerminationObservation observation, boolean userStopBound
    ) {
        if (binding == null || observation == null || observation.task() != boundRoot
                || !observation.stopStateAvailable() || observation.taskStopped() || userStopBound) return null;
        return GotoCommandTerminalProjector.project(base, binding, source.gotoTerminal());
    }
}
//#endif
