package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch;

//20260905_kpopmodder: Decide only whether the captured ordinary owner is fully retired.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefOrdinaryCommandStopCapture;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;

public final class FabricChatClefStopControlRetirementPredicate {
    private final FabricChatClefCommandQueue commandQueue;
    private final FabricChatClefStopControlTaskOwnershipReader taskOwnershipReader;

    public FabricChatClefStopControlRetirementPredicate(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefStopControlTaskOwnershipReader taskOwnershipReader
    ) {
        this.commandQueue = commandQueue;
        this.taskOwnershipReader = taskOwnershipReader;
    }

    public boolean isRetired(
            FabricChatClefStopControlContext stopContext,
            FabricChatClefOrdinaryCommandStopCapture capture,
            FabricChatClefCommandContext ordinaryContext
    ) {
        boolean retired = ordinaryContext.terminalSent()
                && !commandQueue.isPending(ordinaryContext)
                && !commandQueue.isActive(ordinaryContext);
        if (!retired || !capture.active()) {
            return retired;
        }
        FabricChatClefTaskOwnershipEvidence evidence = taskOwnershipReader.readOrNull();
        return evidence != null
                && evidence.available()
                && evidence.rootTask() != stopContext.capturedBoundRootTask();
    }
}
