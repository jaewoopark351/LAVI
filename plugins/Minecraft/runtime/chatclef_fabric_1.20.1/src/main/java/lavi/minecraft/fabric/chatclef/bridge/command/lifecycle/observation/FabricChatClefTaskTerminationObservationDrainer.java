package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.observation;

//20260905_kpopmodder: Drain a bounded number of TaskFinished observations per client tick.

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefUserTaskFinishedObserver;

public final class FabricChatClefTaskTerminationObservationDrainer {
    private static final int MAX_OBSERVATIONS_PER_TICK = 32;

    private final FabricChatClefUserTaskFinishedObserver observer;
    private final FabricChatClefTaskTerminationObservationHandler handler;

    public FabricChatClefTaskTerminationObservationDrainer(
            FabricChatClefUserTaskFinishedObserver observer,
            FabricChatClefTaskTerminationObservationHandler handler
    ) {
        this.observer = observer;
        this.handler = handler;
    }

    public void drain() {
        for (int index = 0; index < MAX_OBSERVATIONS_PER_TICK; index++) {
            FabricChatClefCommandTerminationObservation observation = observer.poll();
            if (observation == null) {
                return;
            }
            handler.handle(observation);
        }
    }
}
