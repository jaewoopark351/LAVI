package lavi.minecraft.diagnostics.container.gui.slot;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.gui.emission.ContainerGuiDiagnosticFields;
import lavi.minecraft.diagnostics.container.gui.emission.ContainerGuiSemanticFingerprint;
import lavi.minecraft.diagnostics.container.gui.lifecycle.ContainerGuiActiveFlow;
import lavi.minecraft.diagnostics.container.gui.lifecycle.ContainerGuiFlowTrace;
import lavi.minecraft.diagnostics.container.gui.tick.ContainerClientTickWindowSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

import java.util.function.Supplier;

//20260904_kpopmodder: Report applied S2C reconciliation without inventing a per-click acknowledgement.
public final class ContainerServerReconciliationDiagnostics {
    private final ContainerSlotActionSnapshotCollector snapshots;
    private final ContainerSlotPostTickVerifier pending;
    private final Supplier<ContainerClientTickWindowSnapshot> tickWindow;

    public ContainerServerReconciliationDiagnostics(
            ContainerSlotActionSnapshotCollector snapshots,
            ContainerSlotPostTickVerifier pending,
            Supplier<ContainerClientTickWindowSnapshot> tickWindow) {
        this.snapshots = snapshots;
        this.pending = pending;
        this.tickWindow = tickWindow;
    }

    public void slotUpdateApplied(
            ContainerGuiActiveFlow active,
            ScreenHandler handler,
            int syncId,
            int revision,
            int windowSlot,
            ItemStack packetStack) {
        emit(
                active,
                handler,
                syncId,
                revision,
                "SERVER_SLOT_UPDATE_APPLIED",
                windowSlot,
                -1,
                packetStack,
                null
        );
    }

    public void fullInventoryUpdateApplied(
            ContainerGuiActiveFlow active,
            ScreenHandler handler,
            int syncId,
            int revision,
            int packetSlotCount,
            ItemStack packetCursorStack) {
        emit(
                active,
                handler,
                syncId,
                revision,
                "SERVER_FULL_INVENTORY_UPDATE_APPLIED",
                -1,
                packetSlotCount,
                null,
                packetCursorStack
        );
    }

    private void emit(
            ContainerGuiActiveFlow active,
            ScreenHandler handler,
            int syncId,
            int revision,
            String updateKind,
            int packetWindowSlot,
            int packetSlotCount,
            ItemStack packetStack,
            ItemStack packetCursorStack) {
        if (active == null || handler == null) {
            return;
        }
        long gameTick = ChatClefDiagnostics.currentClientTickId();
        ContainerGuiFlowTrace flow = active.trace();
        ContainerSlotActionObservation action = pending.latestMatchingAction(
                flow,
                handler,
                syncId,
                gameTick
        );
        if (action == null) {
            return;
        }
        ClientPlayerEntity player = currentPlayer();
        ContainerItemCountSnapshot current = snapshots.after(handler, player, action);
        ContainerItemCountSnapshot baseline = flow.transferBaseline(current.focusItemId());
        int playerDecrease = baseline == null
                ? 0
                : baseline.playerItemCount() - current.playerItemCount();
        int containerIncrease = baseline == null
                ? 0
                : current.containerItemCount() - baseline.containerItemCount();
        boolean stateChangedFromRequest = action.before() != null
                && !action.before().equals(current);
        boolean pairedDelta = baseline != null
                && playerDecrease > 0
                && playerDecrease == containerIncrease;
        flow.serverReconciliationObserved(stateChangedFromRequest, gameTick);
        active.emitter().detail(
                updateKind,
                "server_s2c_container_reconciliation_applied",
                ContainerGuiSemanticFingerprint.of(
                        "SERVER_RECONCILIATION",
                        flow.source().targetFamily(),
                        updateKind,
                        action.slotActionType(),
                        action.slotButton(),
                        packetWindowSlot,
                        pairedDelta
                ),
                gameTick,
                ChatClefDiagnostics.currentTaskForDiagnostics(),
                ContainerGuiDiagnosticFields.downstream(
                        flow.source(),
                        tickWindow.get(),
                        updateKind,
                        "SERVER_RECONCILIATION_OBSERVED"
                ),
                ContainerGuiDiagnosticFields.merge(
                        ContainerSlotDiagnosticFields.from(action, current),
                        new Object[]{
                                "packetSyncId", syncId,
                                "packetRevision", revision,
                                "appliedHandlerRevision", handler.getRevision(),
                                "packetWindowSlot", packetWindowSlot,
                                "packetSlotCount", packetSlotCount,
                                "packetStack", packetStack == null
                                        ? "not_applicable"
                                        : ContainerSlotDiagnosticFields.stackSummary(packetStack),
                                "packetCursorStack",
                                packetCursorStack == null
                                        ? "not_applicable"
                                        : ContainerSlotDiagnosticFields.stackSummary(packetCursorStack),
                                "slotActionRequestGameTick", action.requestGameTick(),
                                "ticksAfterSlotActionRequest", gameTick - action.requestGameTick(),
                                "pendingSlotActionMatched", true,
                                "serverReconciliationStateChangedFromRequest",
                                stateChangedFromRequest,
                                "playerItemDecreaseFromTransferBaseline", playerDecrease,
                                "containerItemIncreaseFromTransferBaseline", containerIncrease,
                                "pairedDeltaObserved", pairedDelta,
                                "deltaObservationSource", "SERVER_S2C_RECONCILIATION_APPLIED",
                                "serverReconciliationObserved", true,
                                "serverAcknowledgementObserved", false,
                                "serverAcknowledgementEvidence", "PROTOCOL_HAS_NO_PER_CLICK_ACK"
                        }
                )
        );
    }

    private static ClientPlayerEntity currentPlayer() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client == null ? null : client.player;
    }
}
