package lavi.minecraft.diagnostics.container.gui.screen;

import lavi.minecraft.diagnostics.container.gui.budget.ContainerGuiDiagnosticAggregateSnapshot;
import lavi.minecraft.diagnostics.container.gui.emission.ContainerGuiDiagnosticFields;
import lavi.minecraft.diagnostics.container.gui.emission.ContainerGuiEvidenceFields;

//20260904_kpopmodder: Count activation-wide TAIL/EventBus evidence independently of heuristic flows.
public final class ContainerScreenTransportAggregate {
    private long firstObservedGameTick = -1L;
    private long lastObservedGameTick = -1L;
    private int sourceObservedCount;
    private int eventBusDroppedCount;
    private int eventBusDispatchStartedCount;
    private int eventBusDispatchCompletedCount;
    private int eventBusNoActiveListenerCount;
    private int eventBusListenerStartedCount;
    private int eventBusListenerCompletedCount;
    private int eventBusInactiveListenerSkipCount;
    private int eventBusListenerClassCastFailureCount;

    public void sourceObserved(long gameTick) {
        observe(gameTick);
        sourceObservedCount++;
    }

    public void dispatchStarted(long gameTick) {
        observe(gameTick);
        eventBusDispatchStartedCount++;
    }

    public void droppedNoActiveListener(long gameTick) {
        observe(gameTick);
        eventBusDroppedCount++;
        eventBusNoActiveListenerCount++;
    }

    public void listenerStarted(long gameTick) {
        observe(gameTick);
        eventBusListenerStartedCount++;
    }

    public void listenerCompleted(long gameTick) {
        observe(gameTick);
        eventBusListenerCompletedCount++;
    }

    public void listenerSkippedInactive(long gameTick) {
        observe(gameTick);
        eventBusInactiveListenerSkipCount++;
    }

    public void listenerClassCastFailed(long gameTick) {
        observe(gameTick);
        eventBusListenerClassCastFailureCount++;
    }

    public void dispatchCompleted(long gameTick) {
        observe(gameTick);
        eventBusDispatchCompletedCount++;
    }

    public Object[] fields(
            ContainerGuiDiagnosticAggregateSnapshot detail,
            String flushReason,
            String budgetScope,
            boolean includeFinalCheckpointAttempted) {
        Object[] fields = new Object[]{
                "diagnosticFlushReason", flushReason,
                "diagnosticBudgetScope", budgetScope,
                "screenTailSourceObservedCount", sourceObservedCount,
                "screenTailHubDroppedCount", eventBusDroppedCount,
                "screenTailHubDispatchStartedCount", eventBusDispatchStartedCount,
                "screenTailHubDispatchCompletedCount", eventBusDispatchCompletedCount,
                "screenTailHubNoActiveListenerCount", eventBusNoActiveListenerCount,
                "screenEventListenerStartedCount", eventBusListenerStartedCount,
                "completedGuiListenerCallbackCount", eventBusListenerCompletedCount,
                "screenTailHubInactiveAfterSnapshotSkipCount", eventBusInactiveListenerSkipCount,
                "screenEventListenerClassCastFailureCount", eventBusListenerClassCastFailureCount,
                "firstObservedGameTick", firstObservedGameTick,
                "lastObservedGameTick", lastObservedGameTick
        };
        fields = ContainerGuiDiagnosticFields.merge(
                fields,
                ContainerGuiEvidenceFields.from(
                        detail,
                        true,
                        "MODE_OFF".equals(flushReason)
                )
        );
        if (includeFinalCheckpointAttempted) {
            fields = ContainerGuiDiagnosticFields.merge(fields, new Object[]{
                    "finalCheckpointEmissionAttempted", true
            });
        }
        return detail == null
                ? fields
                : ContainerGuiDiagnosticFields.merge(fields, ContainerGuiDiagnosticFields.aggregate(detail));
    }

    public void clear() {
        firstObservedGameTick = -1L;
        lastObservedGameTick = -1L;
        sourceObservedCount = 0;
        eventBusDroppedCount = 0;
        eventBusDispatchStartedCount = 0;
        eventBusDispatchCompletedCount = 0;
        eventBusNoActiveListenerCount = 0;
        eventBusListenerStartedCount = 0;
        eventBusListenerCompletedCount = 0;
        eventBusInactiveListenerSkipCount = 0;
        eventBusListenerClassCastFailureCount = 0;
    }

    private void observe(long gameTick) {
        if (firstObservedGameTick < 0L) {
            firstObservedGameTick = gameTick;
        }
        lastObservedGameTick = gameTick;
    }
}
