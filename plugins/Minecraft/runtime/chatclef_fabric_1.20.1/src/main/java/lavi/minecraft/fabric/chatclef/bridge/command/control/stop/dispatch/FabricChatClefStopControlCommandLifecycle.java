package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch;

//20260905_kpopmodder: Expose only the ordinary lifecycle operations needed by the STOP owner.

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlIdentity;

public interface FabricChatClefStopControlCommandLifecycle {
    boolean bindUserStop(
            FabricChatClefCommandContext context,
            FabricChatClefStopControlIdentity identity,
            Task currentTask
    );

    boolean clearUserStop(
            FabricChatClefCommandContext context,
            FabricChatClefStopControlIdentity identity
    );

    boolean sendUserStopCancellation(FabricChatClefCommandContext context);

    boolean matchesBoundRootTask(FabricChatClefCommandContext context, Task candidateTask);
}
