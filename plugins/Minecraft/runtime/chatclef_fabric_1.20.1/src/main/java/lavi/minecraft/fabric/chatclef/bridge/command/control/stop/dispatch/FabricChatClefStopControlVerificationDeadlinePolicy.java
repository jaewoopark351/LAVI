package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch;

//20260905_kpopmodder: Define only the bounded STOP retirement-verification deadline.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;

public final class FabricChatClefStopControlVerificationDeadlinePolicy {
    public static final int MAX_LATER_TICKS = 20;

    public long deadlineTick(FabricChatClefStopControlContext context) {
        return context.executedClientTick() + MAX_LATER_TICKS;
    }
}
