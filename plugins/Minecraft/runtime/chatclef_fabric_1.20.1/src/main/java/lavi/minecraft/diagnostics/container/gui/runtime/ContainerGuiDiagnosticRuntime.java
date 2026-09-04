package lavi.minecraft.diagnostics.container.gui.runtime;

import adris.altoclef.eventbus.events.ScreenOpenEvent;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.gui.budget.ContainerGuiDiagnosticAggregateSnapshot;
import lavi.minecraft.diagnostics.container.gui.budget.ContainerGuiDiagnosticLimiter;
import lavi.minecraft.diagnostics.container.gui.correlation.ContainerOpenCorrelationRegistry;
import lavi.minecraft.diagnostics.container.gui.correlation.ContainerOpenInteractionObservation;
import lavi.minecraft.diagnostics.container.gui.correlation.ContainerOpenInteractionObserver;
import lavi.minecraft.diagnostics.container.gui.correlation.ContainerOpenInteractionSnapshotCollector;
import lavi.minecraft.diagnostics.container.gui.dispatch.ContainerScreenDispatchProbe;
import lavi.minecraft.diagnostics.container.gui.emission.ContainerGuiDiagnosticEmitter;
import lavi.minecraft.diagnostics.container.gui.emission.ContainerGuiDiagnosticFields;
import lavi.minecraft.diagnostics.container.gui.lifecycle.ContainerGuiActiveFlow;
import lavi.minecraft.diagnostics.container.gui.lifecycle.ContainerGuiFlowReporter;
import lavi.minecraft.diagnostics.container.gui.lifecycle.ContainerGuiFlowTrace;
import lavi.minecraft.diagnostics.container.gui.lifecycle.ContainerGuiRootTerminalDecision;
import lavi.minecraft.diagnostics.container.gui.screen.ContainerScreenEventSnapshot;
import lavi.minecraft.diagnostics.container.gui.screen.ContainerScreenEventSnapshotCollector;
import lavi.minecraft.diagnostics.container.gui.screen.ContainerScreenTransportAggregate;
import lavi.minecraft.diagnostics.container.gui.screen.ContainerScreenTransportDiagnostics;
import lavi.minecraft.diagnostics.container.gui.slot.ContainerSlotActionProbe;
import lavi.minecraft.diagnostics.container.gui.slot.ContainerSlotFlowDiagnostics;
import lavi.minecraft.diagnostics.container.gui.task.ContainerTaskFlowDiagnostics;
import lavi.minecraft.diagnostics.container.gui.tick.ContainerClientTickWindow;
import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import lavi.minecraft.diagnostics.interaction.BlockInteractionObserver;
import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleObserver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
//20260904_kpopmodder: Coordinate observation-only container flow diagnostics without owning gameplay state.
public final class ContainerGuiDiagnosticRuntime implements DiagnosticSessionLifecycleObserver {
    private final ContainerClientTickWindow tickWindow = new ContainerClientTickWindow();
    private final ContainerOpenCorrelationRegistry correlations = new ContainerOpenCorrelationRegistry();
    private final ContainerOpenInteractionSnapshotCollector interactionCollector =
            new ContainerOpenInteractionSnapshotCollector();
    private final ContainerScreenEventSnapshotCollector screenCollector =
            new ContainerScreenEventSnapshotCollector();
    private final ContainerGuiDiagnosticLimiter uncorrelatedLimiter =
            new ContainerGuiDiagnosticLimiter();
    private final ContainerGuiDiagnosticEmitter uncorrelatedEmitter =
            new ContainerGuiDiagnosticEmitter(uncorrelatedLimiter);
    private final ContainerScreenTransportAggregate uncorrelatedTransport =
            new ContainerScreenTransportAggregate();
    private final ContainerTaskFlowDiagnostics taskDiagnostics =
            new ContainerTaskFlowDiagnostics();
    private final ContainerGuiFlowReporter flowReporter =
            new ContainerGuiFlowReporter();
    private final BlockInteractionObserver interactionObserver =
            new ContainerOpenInteractionObserver(this);
    private final ContainerScreenTransportDiagnostics screenTransport;
    private final ContainerSlotFlowDiagnostics slotDiagnostics;

    private ContainerScreenEventSnapshot lastTailSource;
    private ContainerGuiActiveFlow activeFlow;

    public ContainerGuiDiagnosticRuntime() {
        screenTransport = new ContainerScreenTransportDiagnostics(
                uncorrelatedEmitter,
                uncorrelatedTransport,
                () -> activeFlow
        );
        slotDiagnostics = new ContainerSlotFlowDiagnostics(tickWindow::snapshot);
    }

    public BlockInteractionObserver interactionObserver() {
        return interactionObserver;
    }

    public void onClientTickHead(long serial) {
        tickWindow.onHead(serial);
        correlations.expire(serial);
        ContainerGuiActiveFlow current = activeFlow;
        if (current == null) {
            return;
        }
        current.trace().onClientTickHead(serial);
        if (!bindingStillLive(current.trace())) {
            finishActiveFlow("LIVE_GUI_BINDING_CHANGED_BEFORE_CLIENT_TICK", null);
            return;
        }
        slotDiagnostics.onClientTick(current, serial);
        flowReporter.emitHeartbeat(current, tickWindow.snapshot(), serial);
    }

    public void onClientTickBoundaryPublished(long serial) {
        tickWindow.onBoundaryPublished(serial);
    }

    public void onClientTickReturn(long serial) {
        tickWindow.onReturn(serial);
    }

    public void onBlockInteractionStarted(BlockInteractionContext context) {
        correlations.begin(interactionCollector.capture(context));
    }

    public void onBlockInteractionReturned(BlockInteractionContext context, Object result) {
        if (context != null) {
            correlations.complete(context.interactionId(), result);
        }
    }

    public void onScreenTailSource(ScreenOpenEvent event) {
        long gameTick = ChatClefDiagnostics.currentClientTickId();
        ContainerOpenInteractionObservation interaction = event == null
                || event.preOpen
                || event.screen == null
                ? null
                : correlations.match(event.screen, gameTick);
        ContainerScreenEventSnapshot snapshot = screenCollector.capture(
                event,
                interaction,
                tickWindow.snapshot()
        );
        lastTailSource = snapshot;
        String boundaryActivationId = uncorrelatedEmitter.boundaryActivationId();

        boolean acceptedFlow = event != null
                && event.screen != null
                && interaction != null
                && snapshot.diagnosticCorrelationCandidateAccepted();
        if (acceptedFlow) {
            if (activeFlow != null) {
                finishActiveFlow("NEXT_CORRELATED_CONTAINER_SCREEN_OPENED", null);
            }
            activeFlow = new ContainerGuiActiveFlow(
                    snapshot,
                    gameTick,
                    boundaryActivationId
            );
        }

        screenTransport.emitTailSource(snapshot, gameTick);

        if (event == null || event.screen == null) {
            finishActiveFlow("SCREEN_CLOSED_AT_TAIL", null);
            return;
        }
        if (!acceptedFlow && activeFlow != null
                && !activeFlow.trace().source().screenObjectIdentity()
                .equals(snapshot.screenObjectIdentity())) {
            finishActiveFlow("SCREEN_REPLACED_AT_TAIL", null);
        }
    }

    public ContainerScreenDispatchProbe beginScreenDispatch(
            ScreenOpenEvent event,
            int registeredListenerCount,
            int eligibleListenerCount) {
        ContainerScreenEventSnapshot snapshot = screenCollector.capture(
                event,
                matchingSourceInteraction(event),
                tickWindow.snapshot()
        );
        return screenTransport.beginDispatch(
                snapshot,
                registeredListenerCount,
                eligibleListenerCount
        );
    }

    public void onTaskEvaluationStarted(Task task) {
        ContainerGuiActiveFlow current = activeFlow;
        if (current == null || !bindingStillLive(current.trace())) {
            return;
        }
        taskDiagnostics.onEvaluationStarted(current, tickWindow.snapshot(), task);
    }

    public void onTaskReconciliation(
            Task parent,
            Task activeChildBefore,
            Task candidateChild,
            boolean isEqualResult,
            boolean canInterruptEvaluated,
            boolean canInterruptPreviousChild,
            boolean replacementApplied,
            boolean previousChildStopCalled,
            Task activeChildAfter,
            boolean candidateDiscardedBecauseEqual,
            boolean childCleared) {
        ContainerGuiActiveFlow current = activeFlow;
        if (current == null || !bindingStillLive(current.trace())) {
            return;
        }
        taskDiagnostics.onReconciliation(
                current,
                tickWindow.snapshot(),
                parent,
                activeChildBefore,
                candidateChild,
                isEqualResult,
                canInterruptEvaluated,
                canInterruptPreviousChild,
                replacementApplied,
                previousChildStopCalled,
                activeChildAfter,
                candidateDiscardedBecauseEqual,
                childCleared
        );
    }

    public ContainerSlotActionProbe beginSlotAction(
            ScreenHandler handler,
            int syncId,
            int windowSlot,
            int button,
            SlotActionType actionType,
            ClientPlayerEntity player) {
        ContainerGuiActiveFlow current = activeFlowForHandler(handler, syncId);
        if (current == null) {
            return ContainerSlotActionProbe.noop();
        }
        return slotDiagnostics.begin(
                current,
                handler,
                syncId,
                windowSlot,
                button,
                actionType,
                player
        );
    }

    public void onLocalSlotMutation(
            ScreenHandler handler,
            int windowSlot,
            ItemStack before,
            ItemStack after) {
        slotDiagnostics.onLocalMutation(handler, windowSlot, before, after);
    }

    public void onServerSlotUpdateApplied(
            ScreenHandler handler,
            int syncId,
            int revision,
            int windowSlot,
            ItemStack packetStack) {
        ContainerGuiActiveFlow current = activeFlowForHandler(handler, syncId);
        if (current == null) {
            return;
        }
        slotDiagnostics.onServerSlotUpdateApplied(
                current,
                handler,
                syncId,
                revision,
                windowSlot,
                packetStack
        );
    }

    public void onServerInventoryUpdateApplied(
            ScreenHandler handler,
            int syncId,
            int revision,
            int packetSlotCount,
            ItemStack packetCursorStack) {
        ContainerGuiActiveFlow current = activeFlowForHandler(handler, syncId);
        if (current == null) {
            return;
        }
        slotDiagnostics.onServerInventoryUpdateApplied(
                current,
                handler,
                syncId,
                revision,
                packetSlotCount,
                packetCursorStack
        );
    }

    public void onUserTaskTerminal(
            Task task,
            boolean actuallyDone,
            String frozenRootAssignmentId,
            String currentRootAssignmentId,
            String finishTriggerHint) {
        if (activeFlow == null
                || !activeFlow.rootAssignmentMatches(frozenRootAssignmentId)) {
            return;
        }
        String terminalReason = ContainerGuiRootTerminalDecision.reason(
                actuallyDone,
                frozenRootAssignmentId,
                currentRootAssignmentId,
                finishTriggerHint
        );
        if (terminalReason == null) {
            return;
        }
        finishActiveFlow(terminalReason, task);
    }

    @Override
    public void beforeModeOff() {
        try {
            finishActiveFlow("DIAGNOSTICS_MODE_OFF", null);
            ContainerGuiDiagnosticAggregateSnapshot aggregate =
                    uncorrelatedEmitter.aggregateSnapshotIfActive();
            if (aggregate != null) {
                uncorrelatedEmitter.terminal(
                        "EXACT_GUI_SCREEN_SESSION_FINAL_CHECKPOINT",
                        "container_gui_screen_session_mode_off",
                        ChatClefDiagnostics.currentClientTickId(),
                        null,
                        uncorrelatedTransport.fields(
                                aggregate,
                                "MODE_OFF",
                                "LOCAL_DETAIL_EXEMPT_AGGREGATE_CHECKPOINT",
                                true
                        ),
                        new Object[0]
                );
            }
        } finally {
            clearState();
        }
    }

    @Override
    public Object[] finalSnapshotFields() {
        ContainerGuiDiagnosticAggregateSnapshot aggregate =
                uncorrelatedEmitter.aggregateSnapshotIfActive();
        if (aggregate == null) {
            return new Object[0];
        }
        return ContainerGuiDiagnosticFields.merge(new Object[]{
                    "diagnosticBoundaryActivationId",
                    aggregate.diagnosticBoundaryActivationId()
                },
                uncorrelatedTransport.fields(
                        aggregate,
                        "CLEAN_TEARDOWN_FINAL_SNAPSHOT",
                        "LOCAL_DETAIL_EXEMPT_FINAL_SNAPSHOT_PROJECTION",
                        false
                ));
    }

    @Override
    public void afterCleanTeardownSnapshotAttempt(boolean emissionCallsReturned) {
        clearState();
    }

    private ContainerOpenInteractionObservation matchingSourceInteraction(ScreenOpenEvent event) {
        if (event == null || lastTailSource == null
                || !lastTailSource.screenOpenEventIdentity().equals(identity(event))) {
            return null;
        }
        return lastTailSource.interaction();
    }

    private boolean bindingStillLive(ContainerGuiFlowTrace flow) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.currentScreen == null || client.player == null
                || client.player.currentScreenHandler == null) {
            return false;
        }
        return flow.source().screenObjectIdentity().equals(identity(client.currentScreen))
                && flow.source().playerHandlerIdentity().equals(identity(client.player.currentScreenHandler))
                && flow.source().liveSyncId() == client.player.currentScreenHandler.syncId;
    }

    private ContainerGuiActiveFlow activeFlowForHandler(
            ScreenHandler handler,
            int syncId) {
        ContainerGuiActiveFlow current = activeFlow;
        if (current == null
                || !bindingStillLive(current.trace())
                || !current.trace().source().handledScreenHandlerIdentity().equals(identity(handler))
                || current.trace().source().capturedSyncId() != syncId) {
            return null;
        }
        return current;
    }

    private void finishActiveFlow(String terminalReason, Task task) {
        ContainerGuiActiveFlow current = activeFlow;
        if (current == null) {
            return;
        }
        try {
            flowReporter.emitTerminal(current, terminalReason, task);
        } finally {
            if (activeFlow == current) {
                activeFlow = null;
            }
            slotDiagnostics.clear();
        }
    }

    private void clearState() {
        activeFlow = null;
        lastTailSource = null;
        slotDiagnostics.clear();
        correlations.clear();
        tickWindow.clear();
        uncorrelatedTransport.clear();
        uncorrelatedLimiter.clearForModeTransition();
    }

    private static String identity(Object value) {
        return value == null
                ? "unavailable"
                : value.getClass().getName() + "@"
                + Integer.toHexString(System.identityHashCode(value));
    }
}
