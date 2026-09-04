package lavi.minecraft.diagnostics.container.gui.slot;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.gui.emission.ContainerGuiDiagnosticFields;
import lavi.minecraft.diagnostics.container.gui.emission.ContainerGuiSemanticFingerprint;
import lavi.minecraft.diagnostics.container.gui.lifecycle.ContainerGuiActiveFlow;
import lavi.minecraft.diagnostics.container.gui.lifecycle.ContainerGuiFlowTrace;
import lavi.minecraft.diagnostics.container.gui.tick.ContainerClientTickWindowSnapshot;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.function.Supplier;

//20260904_kpopmodder: Observe slot request, local prediction, and later live state without issuing clicks.
public final class ContainerSlotFlowDiagnostics {
    private final ContainerSlotActionSnapshotCollector snapshots =
            new ContainerSlotActionSnapshotCollector();
    private final ContainerSlotPostTickVerifier postTickVerifier =
            new ContainerSlotPostTickVerifier(snapshots);
    private final ContainerServerReconciliationDiagnostics serverReconciliation;
    private final ThreadLocal<ActiveSlotAction> activeSlotAction = new ThreadLocal<>();
    private final Supplier<ContainerClientTickWindowSnapshot> tickWindow;

    public ContainerSlotFlowDiagnostics(Supplier<ContainerClientTickWindowSnapshot> tickWindow) {
        this.tickWindow = tickWindow;
        serverReconciliation = new ContainerServerReconciliationDiagnostics(
                snapshots,
                postTickVerifier,
                tickWindow
        );
    }

    public ContainerSlotActionProbe begin(
            ContainerGuiActiveFlow active,
            ScreenHandler handler,
            int syncId,
            int windowSlot,
            int button,
            SlotActionType actionType,
            ClientPlayerEntity player) {
        if (active == null) {
            return ContainerSlotActionProbe.noop();
        }
        ContainerGuiFlowTrace flow = active.trace();
        long gameTick = ChatClefDiagnostics.currentClientTickId();
        ContainerSlotActionObservation action = snapshots.capture(
                handler,
                syncId,
                windowSlot,
                button,
                actionType,
                player,
                gameTick
        );
        flow.slotActionRequested(action.before(), gameTick);
        active.emitter().detail(
                "SLOT_ACTION_REQUEST",
                "container_controller_click_slot_requested",
                ContainerGuiSemanticFingerprint.of(
                        "SLOT_REQUEST",
                        flow.source().targetFamily(),
                        action.handlerClass(),
                        action.windowSlot(),
                        action.slotButton(),
                        action.slotActionType(),
                        action.before().focusItemId()
                ),
                gameTick,
                ChatClefDiagnostics.currentTaskForDiagnostics(),
                ContainerGuiDiagnosticFields.downstream(
                        flow.source(),
                        tickWindow.get(),
                        "SLOT_ACTION_REQUEST",
                        "NONE"
                ),
                ContainerSlotDiagnosticFields.from(action, action.before())
        );
        activeSlotAction.set(new ActiveSlotAction(active, action, handler, player));
        return ContainerSlotActionProbe.active(this, action);
    }

    public void onLocalMutation(
            ScreenHandler handler,
            int windowSlot,
            ItemStack before,
            ItemStack after) {
        ActiveSlotAction active = activeSlotAction.get();
        if (active == null || active.handler != handler) {
            return;
        }
        active.localMutationCount++;
        ContainerItemCountSnapshot current = snapshots.after(handler, active.player, active.action);
        ContainerGuiFlowTrace flow = active.flow.trace();
        ContainerItemCountSnapshot baseline = flow.transferBaseline(current.focusItemId());
        int playerDelta = baseline == null ? 0 : baseline.playerItemCount() - current.playerItemCount();
        int containerDelta = baseline == null ? 0 : current.containerItemCount() - baseline.containerItemCount();
        boolean pairedDelta = baseline != null && playerDelta > 0 && playerDelta == containerDelta;
        long gameTick = ChatClefDiagnostics.currentClientTickId();
        flow.localDeltaObserved(gameTick);
        active.flow.emitter().detail(
                "CONTAINER_DELTA_OBSERVED",
                "client_predicted_screen_handler_slot_changed",
                ContainerGuiSemanticFingerprint.of(
                        "CLIENT_PREDICTED_DELTA",
                        flow.source().targetFamily(),
                        active.action.slotActionType(),
                        active.action.slotButton(),
                        active.action.windowSlot(),
                        ContainerSlotDiagnosticFields.stackSummary(before),
                        ContainerSlotDiagnosticFields.stackSummary(after),
                        pairedDelta
                ),
                gameTick,
                ChatClefDiagnostics.currentTaskForDiagnostics(),
                ContainerGuiDiagnosticFields.downstream(
                        flow.source(),
                        tickWindow.get(),
                        "CLIENT_PREDICTED_CONTAINER_DELTA_OBSERVED",
                        "CLIENT_PREDICTION_ONLY"
                ),
                ContainerGuiDiagnosticFields.merge(
                        ContainerSlotDiagnosticFields.from(active.action, current),
                        new Object[]{
                                "changedWindowSlot", windowSlot,
                                "changedSlotBefore", ContainerSlotDiagnosticFields.stackSummary(before),
                                "changedSlotAfter", ContainerSlotDiagnosticFields.stackSummary(after),
                                "playerItemDeltaFromTransferBaseline", playerDelta,
                                "containerItemDeltaFromTransferBaseline", containerDelta,
                                "pairedDeltaObserved", pairedDelta,
                                "deltaObservationSource", "CLIENT_PREDICTED_SCREEN_HANDLER",
                                "serverAcknowledgementObserved", false
                        }
                )
        );
    }

    public void onReturned(ContainerSlotActionObservation action) {
        ActiveSlotAction active = activeSlotAction.get();
        if (active == null || active.action != action) {
            return;
        }
        try {
            ContainerItemCountSnapshot current = snapshots.after(
                    active.handler,
                    active.player,
                    active.action
            );
            postTickVerifier.track(
                    active.flow.trace(),
                    active.action,
                    active.handler,
                    active.player,
                    current
            );
            active.flow.emitter().detail(
                    "CONTAINER_SLOT_ACTION_RETURN",
                    active.localMutationCount == 0
                            ? "controller_click_returned_without_local_slot_delta"
                            : "controller_click_returned_after_local_slot_delta",
                    ContainerGuiSemanticFingerprint.of(
                            "SLOT_RETURN",
                            active.flow.trace().source().targetFamily(),
                            action.slotActionType(),
                            action.slotButton(),
                            action.windowSlot(),
                            active.localMutationCount == 0
                    ),
                    ChatClefDiagnostics.currentClientTickId(),
                    ChatClefDiagnostics.currentTaskForDiagnostics(),
                    ContainerGuiDiagnosticFields.downstream(
                            active.flow.trace().source(),
                            tickWindow.get(),
                            "SLOT_ACTION_RETURN",
                            active.localMutationCount == 0
                                    ? "NO_CLIENT_PREDICTED_SLOT_DELTA"
                                    : "CLIENT_PREDICTED_SLOT_DELTA"
                    ),
                    ContainerGuiDiagnosticFields.merge(
                            ContainerSlotDiagnosticFields.from(action, current),
                            new Object[]{
                                    "localSlotMutationCount", active.localMutationCount,
                                    "deltaObservationSource", "CLIENT_PREDICTED_SCREEN_HANDLER",
                                    "serverAcknowledgementObserved", false
                            }
                    )
            );
        } finally {
            activeSlotAction.remove();
        }
    }

    public void onFailed(ContainerSlotActionObservation action, Throwable failure) {
        ActiveSlotAction active = activeSlotAction.get();
        if (active == null || active.action != action) {
            return;
        }
        try {
            long gameTick = ChatClefDiagnostics.currentClientTickId();
            active.flow.trace().slotActionFailed(gameTick);
            active.flow.emitter().detail(
                    "CONTAINER_SLOT_ACTION_EXCEPTION",
                    "controller_click_slot_existing_exception_path",
                    ContainerGuiSemanticFingerprint.of(
                            "SLOT_EXCEPTION",
                            active.flow.trace().source().targetFamily(),
                            action.slotActionType(),
                            action.slotButton(),
                            action.windowSlot(),
                            failure == null ? "unavailable" : failure.getClass().getName()
                    ),
                    gameTick,
                    ChatClefDiagnostics.currentTaskForDiagnostics(),
                    ContainerGuiDiagnosticFields.downstream(
                            active.flow.trace().source(),
                            tickWindow.get(),
                            "SLOT_ACTION_EXCEPTION",
                            failure == null ? "UNKNOWN_EXCEPTION" : failure.getClass().getName()
                    ),
                    ContainerGuiDiagnosticFields.merge(
                            ContainerSlotDiagnosticFields.from(action, action.before()),
                            new Object[]{
                                    "exceptionType", failure == null
                                            ? "unavailable"
                                            : failure.getClass().getName(),
                                    "exceptionMessage", failure == null
                                            ? "unavailable"
                                            : String.valueOf(failure.getMessage()),
                                    "existingExceptionBehavior", "PRESERVED"
                            }
                    )
            );
        } finally {
            activeSlotAction.remove();
        }
    }

    public void onClientTick(ContainerGuiActiveFlow active, long gameTick) {
        if (active == null) {
            return;
        }
        for (ContainerSlotPostTickObservation observation
                : postTickVerifier.observe(active.trace(), gameTick)) {
            emitPostTickObservation(active, observation);
        }
    }

    public void onServerSlotUpdateApplied(
            ContainerGuiActiveFlow active,
            ScreenHandler handler,
            int syncId,
            int revision,
            int windowSlot,
            ItemStack packetStack) {
        serverReconciliation.slotUpdateApplied(
                active,
                handler,
                syncId,
                revision,
                windowSlot,
                packetStack
        );
    }

    public void onServerInventoryUpdateApplied(
            ContainerGuiActiveFlow active,
            ScreenHandler handler,
            int syncId,
            int revision,
            int packetSlotCount,
            ItemStack packetCursorStack) {
        serverReconciliation.fullInventoryUpdateApplied(
                active,
                handler,
                syncId,
                revision,
                packetSlotCount,
                packetCursorStack
        );
    }

    public void clear() {
        activeSlotAction.remove();
        postTickVerifier.clear();
    }

    private void emitPostTickObservation(
            ContainerGuiActiveFlow active,
            ContainerSlotPostTickObservation observation) {
        ContainerGuiFlowTrace flow = observation.flow();
        ContainerItemCountSnapshot current = observation.current();
        ContainerItemCountSnapshot baseline = flow.transferBaseline(current.focusItemId());
        int playerDecrease = baseline == null
                ? 0
                : baseline.playerItemCount() - current.playerItemCount();
        int containerIncrease = baseline == null
                ? 0
                : current.containerItemCount() - baseline.containerItemCount();
        boolean pairedDelta = baseline != null
                && playerDecrease > 0
                && playerDecrease == containerIncrease;
        boolean newLiveDeltaEvidence = observation.changedFromRequest()
                && (observation.checkOrdinal() == 1 || observation.changedSincePreviousCheck());
        flow.postTickSlotStateChecked(newLiveDeltaEvidence, observation.observedGameTick());

        String state = observation.changedFromRequest()
                ? observation.changedSincePreviousCheck()
                ? "LIVE_DELTA_CHANGED_SINCE_PREVIOUS_CHECK"
                : "LIVE_DELTA_PRESENT_STABLE"
                : "NO_LIVE_DELTA_FROM_REQUEST_BASELINE";
        String reason = observation.verificationWindowClosed()
                ? observation.changedFromRequest()
                ? "post_request_live_handler_window_closed_with_delta"
                : "post_request_live_handler_window_closed_without_delta"
                : observation.checkOrdinal() == 1
                ? "first_post_request_client_tick_live_handler_check"
                : "post_request_live_handler_state_check";
        active.emitter().detail(
                observation.changedFromRequest()
                        ? "CONTAINER_DELTA_OBSERVED"
                        : "CONTAINER_DELTA_CHECK",
                reason,
                ContainerGuiSemanticFingerprint.of(
                        "POST_TICK_SLOT_STATE",
                        flow.source().targetFamily(),
                        observation.action().slotActionType(),
                        observation.action().slotButton(),
                        observation.action().windowSlot(),
                        state,
                        observation.verificationWindowClosed()
                ),
                observation.observedGameTick(),
                ChatClefDiagnostics.currentTaskForDiagnostics(),
                ContainerGuiDiagnosticFields.downstream(
                        flow.source(),
                        tickWindow.get(),
                        "CONTAINER_DELTA_POST_TICK_CHECK",
                        "SERVER_ACKNOWLEDGEMENT_UNOBSERVED"
                ),
                ContainerGuiDiagnosticFields.merge(
                        ContainerSlotDiagnosticFields.from(observation.action(), current),
                        new Object[]{
                                "slotActionRequestGameTick", observation.action().requestGameTick(),
                                "ticksAfterSlotActionRequest", observation.ticksAfterRequest(),
                                "postRequestCheckOrdinal", observation.checkOrdinal(),
                                "postRequestLiveHandlerState", state,
                                "liveHandlerStateChangedFromRequest", observation.changedFromRequest(),
                                "liveHandlerStateChangedSincePreviousCheck",
                                observation.changedSincePreviousCheck(),
                                "verificationWindowClosed", observation.verificationWindowClosed(),
                                "playerItemDecreaseFromTransferBaseline", playerDecrease,
                                "containerItemIncreaseFromTransferBaseline", containerIncrease,
                                "pairedDeltaObserved", pairedDelta,
                                "deltaObservationSource", "POST_REQUEST_CLIENT_TICK_LIVE_HANDLER",
                                "serverAcknowledgementObserved", false,
                                "serverAcknowledgementEvidence", "UNOBSERVED_WITHOUT_PACKET_BOUNDARY"
                        }
                )
        );
    }

    private static final class ActiveSlotAction {
        private final ContainerGuiActiveFlow flow;
        private final ContainerSlotActionObservation action;
        private final ScreenHandler handler;
        private final ClientPlayerEntity player;
        private int localMutationCount;

        private ActiveSlotAction(
                ContainerGuiActiveFlow flow,
                ContainerSlotActionObservation action,
                ScreenHandler handler,
                ClientPlayerEntity player) {
            this.flow = flow;
            this.action = action;
            this.handler = handler;
            this.player = player;
        }
    }
}
