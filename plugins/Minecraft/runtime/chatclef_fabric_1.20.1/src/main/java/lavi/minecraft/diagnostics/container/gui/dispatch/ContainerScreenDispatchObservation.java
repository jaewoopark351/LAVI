package lavi.minecraft.diagnostics.container.gui.dispatch;

import lavi.minecraft.diagnostics.container.gui.screen.ContainerScreenEventSnapshot;
import lavi.minecraft.diagnostics.container.gui.lifecycle.ContainerGuiActiveFlow;

//20260904_kpopmodder: Count one existing EventBus screen dispatch without owning listener behavior.
public final class ContainerScreenDispatchObservation {
    private final ContainerScreenEventSnapshot screen;
    private final int registeredListenerCount;
    private final int eligibleListenerCount;
    private final ContainerGuiActiveFlow correlatedFlow;
    private int startedListenerCount;
    private int completedListenerCount;
    private int skippedInactiveListenerCount;
    private int classCastFailureCount;

    public ContainerScreenDispatchObservation(
            ContainerScreenEventSnapshot screen,
            int registeredListenerCount,
            int eligibleListenerCount,
            ContainerGuiActiveFlow correlatedFlow) {
        this.screen = screen;
        this.registeredListenerCount = Math.max(0, registeredListenerCount);
        this.eligibleListenerCount = Math.max(0, eligibleListenerCount);
        this.correlatedFlow = correlatedFlow;
    }

    public ContainerScreenEventSnapshot screen() {
        return screen;
    }

    public int registeredListenerCount() {
        return registeredListenerCount;
    }

    public int eligibleListenerCount() {
        return eligibleListenerCount;
    }

    public ContainerGuiActiveFlow correlatedFlow() {
        return correlatedFlow;
    }

    public int startedListenerCount() {
        return startedListenerCount;
    }

    public int completedListenerCount() {
        return completedListenerCount;
    }

    public int skippedInactiveListenerCount() {
        return skippedInactiveListenerCount;
    }

    public int classCastFailureCount() {
        return classCastFailureCount;
    }

    public void listenerStarted() {
        startedListenerCount++;
    }

    public void listenerCompleted() {
        completedListenerCount++;
    }

    public void listenerSkippedInactive() {
        skippedInactiveListenerCount++;
    }

    public void listenerClassCastFailed() {
        classCastFailureCount++;
    }
}
