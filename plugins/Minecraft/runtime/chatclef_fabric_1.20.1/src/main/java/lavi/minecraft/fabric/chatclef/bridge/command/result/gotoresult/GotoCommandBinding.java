//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.command.result.gotoresult;

import lavi.minecraft.task.movement.gotoresult.model.GotoTargetSnapshot;

//20260913_kpopmodder: Independently bind one admitted task to its request and both connection generations.
public record GotoCommandBinding(
        String requestId, String commandMessageId, String sessionId, long serverConnectionGeneration,
        long javaSocketGeneration, String taskOwner, String taskIdentity, String operationId,
        GotoTargetSnapshot target
) { }
//#endif
