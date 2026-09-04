package lavi.minecraft.diagnostics.container.gui.screen;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.gui.dispatch.ContainerScreenDispatchObservation;
import lavi.minecraft.diagnostics.container.gui.dispatch.ContainerScreenDispatchProbe;
import lavi.minecraft.diagnostics.container.gui.emission.ContainerGuiDiagnosticEmitter;
import lavi.minecraft.diagnostics.container.gui.emission.ContainerGuiDiagnosticFields;
import lavi.minecraft.diagnostics.container.gui.emission.ContainerGuiSemanticFingerprint;
import lavi.minecraft.diagnostics.container.gui.lifecycle.ContainerGuiActiveFlow;

import java.util.function.Supplier;

//20260904_kpopmodder: Observe the existing EventBus transport through the canonical GUI transport schema.
public final class ContainerScreenTransportDiagnostics {
    private final ContainerGuiDiagnosticEmitter uncorrelatedEmitter;
    private final ContainerScreenTransportAggregate uncorrelatedAggregate;
    private final Supplier<ContainerGuiActiveFlow> activeFlow;

    public ContainerScreenTransportDiagnostics(
            ContainerGuiDiagnosticEmitter uncorrelatedEmitter,
            ContainerScreenTransportAggregate uncorrelatedAggregate,
            Supplier<ContainerGuiActiveFlow> activeFlow) {
        this.uncorrelatedEmitter = uncorrelatedEmitter;
        this.uncorrelatedAggregate = uncorrelatedAggregate;
        this.activeFlow = activeFlow;
    }

    public void emitTailSource(ContainerScreenEventSnapshot snapshot, long gameTick) {
        uncorrelatedAggregate.sourceObserved(gameTick);
        uncorrelatedEmitter.detail(
                "EXACT_GUI_SCREEN_TAIL_SOURCE_OBSERVED",
                "screen_tail_source_pre_publish",
                ContainerGuiSemanticFingerprint.of(
                        "SOURCE",
                        snapshot.eventPreOpen(),
                        snapshot.targetFamily(),
                        snapshot.screenTypeActual(),
                        snapshot.handlerTypeActual(),
                        snapshot.tickWindow().state(),
                        snapshot.diagnosticCorrelationReason()
                ),
                gameTick,
                null,
                screenFields(snapshot, "TAIL_SOURCE_PRE_PUBLISH", "OBSERVED", "NONE"),
                new Object[0]
        );
    }

    public ContainerScreenDispatchProbe beginDispatch(
            ContainerScreenEventSnapshot snapshot,
            int registeredListenerCount,
            int eligibleListenerCount) {
        ContainerGuiActiveFlow flow = matchingFlow(snapshot);
        ContainerScreenDispatchObservation observation =
                new ContainerScreenDispatchObservation(
                        snapshot,
                        registeredListenerCount,
                        eligibleListenerCount,
                        flow
                );
        boolean dropped = eligibleListenerCount <= 0;
        String disposition = dropped ? "DROPPED" : "DISPATCH_STARTED";
        String dropReason = dropped ? "NO_ACTIVE_GUI_LISTENER" : "NONE";
        long gameTick = ChatClefDiagnostics.currentClientTickId();
        if (dropped) {
            uncorrelatedAggregate.droppedNoActiveListener(gameTick);
        } else {
            uncorrelatedAggregate.dispatchStarted(gameTick);
        }
        if (flow != null) {
            if (dropped) {
                flow.trace().screenDispatchDroppedNoActiveListener();
            } else {
                flow.trace().screenDispatchStarted();
            }
        }
        uncorrelatedEmitter.detail(
                "EXACT_GUI_SCREEN_TAIL_HUB_DECISION",
                "screen_event_bus_pre_dispatch",
                ContainerGuiSemanticFingerprint.of(
                        "EVENT_BUS_DISPATCH_BEGIN",
                        snapshot.eventPreOpen(),
                        snapshot.targetFamily(),
                        snapshot.screenTypeActual(),
                        snapshot.handlerTypeActual(),
                        snapshot.tickWindow().state(),
                        disposition,
                        dropReason
                ),
                gameTick,
                null,
                screenFields(
                        snapshot,
                        "TAIL_HUB_PRE_DISPATCH",
                        disposition,
                        dropReason
                ),
                new Object[]{
                        "guiContainerEventHubIdentity", "unavailable:legacy_event_bus_transport",
                        "registeredGuiListenerCount", observation.registeredListenerCount(),
                        "dispatchEligibleGuiListenerCount", observation.eligibleListenerCount(),
                        "inactiveAtSnapshotGuiListenerCount",
                        observation.registeredListenerCount() - observation.eligibleListenerCount(),
                        "screenTransportImplementation", "adris.altoclef.eventbus.EventBus"
                }
        );
        return dropped
                ? ContainerScreenDispatchProbe.noop()
                : ContainerScreenDispatchProbe.active(this, observation);
    }

    public void onListenerStarted(
            ContainerScreenDispatchObservation dispatch,
            String listenerClass,
            String listenerIdentity) {
        dispatch.listenerStarted();
        long gameTick = ChatClefDiagnostics.currentClientTickId();
        uncorrelatedAggregate.listenerStarted(gameTick);
        if (dispatch.correlatedFlow() != null) {
            dispatch.correlatedFlow().trace().screenListenerStarted();
        }
        emitListener(
                dispatch,
                "CONTAINER_SCREEN_EVENT_INTAKE",
                "screen_event_listener_callback_started",
                "TAIL_EVENT_BUS_LISTENER_INTAKE",
                "RECEIVED",
                "NONE",
                listenerClass,
                listenerIdentity
        );
    }

    public void onListenerCompleted(
            ContainerScreenDispatchObservation dispatch,
            String listenerClass,
            String listenerIdentity) {
        dispatch.listenerCompleted();
        long gameTick = ChatClefDiagnostics.currentClientTickId();
        uncorrelatedAggregate.listenerCompleted(gameTick);
        if (dispatch.correlatedFlow() != null) {
            dispatch.correlatedFlow().trace().screenListenerCompleted();
        }
        emitListener(
                dispatch,
                "CONTAINER_SCREEN_EVENT_LISTENER_DECISION",
                "screen_event_listener_callback_returned",
                "TAIL_EVENT_BUS_LISTENER_DECISION",
                "PROCESSED",
                "NONE",
                listenerClass,
                listenerIdentity
        );
    }

    public void onListenerSkippedInactive(
            ContainerScreenDispatchObservation dispatch,
            String listenerClass,
            String listenerIdentity) {
        dispatch.listenerSkippedInactive();
        long gameTick = ChatClefDiagnostics.currentClientTickId();
        uncorrelatedAggregate.listenerSkippedInactive(gameTick);
        if (dispatch.correlatedFlow() != null) {
            dispatch.correlatedFlow().trace().screenListenerSkipped();
        }
        emitListener(
                dispatch,
                "CONTAINER_SCREEN_EVENT_LISTENER_DECISION",
                "screen_event_listener_inactive",
                "TAIL_EVENT_BUS_LISTENER_DECISION",
                "DROPPED",
                "SUBSCRIPTION_MARKED_DELETED",
                listenerClass,
                listenerIdentity
        );
    }

    public void onListenerClassCastFailed(
            ContainerScreenDispatchObservation dispatch,
            String listenerClass,
            String listenerIdentity) {
        dispatch.listenerClassCastFailed();
        long gameTick = ChatClefDiagnostics.currentClientTickId();
        uncorrelatedAggregate.listenerClassCastFailed(gameTick);
        emitListener(
                dispatch,
                "CONTAINER_SCREEN_EVENT_LISTENER_DECISION",
                "screen_event_listener_class_cast_failed",
                "TAIL_EVENT_BUS_LISTENER_DECISION",
                "PROCESSED",
                "CLASS_CAST_EXCEPTION_CAUGHT_BY_EXISTING_EVENT_BUS",
                listenerClass,
                listenerIdentity
        );
    }

    public void onDispatchCompleted(ContainerScreenDispatchObservation dispatch) {
        ContainerScreenEventSnapshot snapshot = dispatch.screen();
        ContainerGuiActiveFlow flow = dispatch.correlatedFlow();
        long gameTick = ChatClefDiagnostics.currentClientTickId();
        uncorrelatedAggregate.dispatchCompleted(gameTick);
        if (flow != null) {
            flow.trace().screenDispatchCompleted();
        }
        uncorrelatedEmitter.detail(
                "EXACT_GUI_SCREEN_TAIL_HUB_DISPATCH_COMPLETED",
                "screen_event_bus_dispatch_returned_normally",
                ContainerGuiSemanticFingerprint.of(
                        "EVENT_BUS_DISPATCH_COMPLETE",
                        snapshot.eventPreOpen(),
                        snapshot.targetFamily(),
                        snapshot.screenTypeActual(),
                        snapshot.handlerTypeActual(),
                        snapshot.tickWindow().state(),
                        dispatch.registeredListenerCount(),
                        dispatch.startedListenerCount(),
                        dispatch.skippedInactiveListenerCount(),
                        dispatch.classCastFailureCount()
                ),
                gameTick,
                null,
                screenFields(
                        snapshot,
                        "TAIL_HUB_POST_DISPATCH",
                        "DISPATCH_COMPLETED",
                        "NONE"
                ),
                new Object[]{
                        "guiContainerEventHubIdentity", "unavailable:legacy_event_bus_transport",
                        "registeredGuiListenerCount", dispatch.registeredListenerCount(),
                        "dispatchEligibleGuiListenerCount", dispatch.eligibleListenerCount(),
                        "inactiveAtSnapshotGuiListenerCount",
                        dispatch.registeredListenerCount() - dispatch.eligibleListenerCount(),
                        "completedGuiListenerCallbackCount", dispatch.completedListenerCount(),
                        "skippedInactiveAfterSnapshotGuiListenerCount", dispatch.skippedInactiveListenerCount(),
                        "listenerClassCastFailureCount", dispatch.classCastFailureCount(),
                        "screenTransportImplementation", "adris.altoclef.eventbus.EventBus"
                }
        );
    }

    private void emitListener(
            ContainerScreenDispatchObservation dispatch,
            String eventName,
            String reason,
            String stage,
            String disposition,
        String dropReason,
        String listenerClass,
        String listenerIdentity) {
        ContainerScreenEventSnapshot snapshot = dispatch.screen();
        uncorrelatedEmitter.detail(
                eventName,
                reason,
                ContainerGuiSemanticFingerprint.of(
                        eventName,
                        snapshot.eventPreOpen(),
                        snapshot.targetFamily(),
                        snapshot.screenTypeActual(),
                        snapshot.handlerTypeActual(),
                        snapshot.tickWindow().state(),
                        disposition,
                        dropReason,
                        listenerClass
                ),
                ChatClefDiagnostics.currentClientTickId(),
                null,
                screenFields(snapshot, stage, disposition, dropReason),
                new Object[]{
                        "listenerClass", listenerClass,
                        "listenerIdentity", listenerIdentity,
                        "guiContainerEventHubIdentity", "unavailable:legacy_event_bus_transport",
                        "registeredGuiListenerCount", dispatch.registeredListenerCount(),
                        "dispatchEligibleGuiListenerCount", dispatch.eligibleListenerCount(),
                        "screenEventListenerStartedCount", dispatch.startedListenerCount(),
                        "completedGuiListenerCallbackCount", dispatch.completedListenerCount(),
                        "skippedInactiveAfterSnapshotGuiListenerCount", dispatch.skippedInactiveListenerCount(),
                        "screenTransportImplementation", "adris.altoclef.eventbus.EventBus"
                }
        );
    }

    private Object[] screenFields(
            ContainerScreenEventSnapshot snapshot,
            String stage,
            String disposition,
            String dropReason) {
        String budgetScope = "UNCORRELATED_SCREEN_ACTIVATION_DETAIL";
        if ("TAIL_SOURCE_PRE_PUBLISH".equals(stage)) {
            return ContainerGuiDiagnosticFields.screen(
                    snapshot,
                    snapshot.tickWindow(),
                    stage,
                    disposition,
                    dropReason,
                    budgetScope
            );
        }
        if ("TAIL_HUB_PRE_DISPATCH".equals(stage)
                || "TAIL_HUB_POST_DISPATCH".equals(stage)) {
            return ContainerGuiDiagnosticFields.screen(
                    snapshot,
                    snapshot.tickWindow(),
                    stage,
                    disposition,
                    dropReason,
                    budgetScope
            );
        }
        return ContainerGuiDiagnosticFields.eventBusTransport(
                snapshot,
                snapshot.tickWindow(),
                stage,
                disposition,
                dropReason,
                budgetScope
        );
    }

    private ContainerGuiActiveFlow matchingFlow(ContainerScreenEventSnapshot snapshot) {
        ContainerGuiActiveFlow current = activeFlow.get();
        if (current == null || snapshot == null
                || !current.trace().source().screenOpenEventIdentity()
                .equals(snapshot.screenOpenEventIdentity())) {
            return null;
        }
        return current;
    }
}
