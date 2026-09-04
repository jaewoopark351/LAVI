package lavi.minecraft.diagnostics.container.gui.slot;

import lavi.minecraft.diagnostics.container.gui.lifecycle.ContainerGuiFlowTrace;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.ScreenHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

//20260904_kpopmodder: Bound later-tick live-handler verification independently of slot issuance.
public final class ContainerSlotPostTickVerifier {
    private static final int MAX_PENDING = 32;
    private static final int MAX_OBSERVATION_TICKS = 20;

    private final ContainerSlotActionSnapshotCollector snapshots;
    private final Deque<PendingVerification> pending = new ArrayDeque<>();

    public ContainerSlotPostTickVerifier(ContainerSlotActionSnapshotCollector snapshots) {
        this.snapshots = snapshots;
    }

    public synchronized void track(
            ContainerGuiFlowTrace flow,
            ContainerSlotActionObservation action,
            ScreenHandler handler,
            ClientPlayerEntity player,
            ContainerItemCountSnapshot returnedState) {
        if (flow == null || action == null || handler == null) {
            return;
        }
        while (pending.size() >= MAX_PENDING) {
            pending.removeFirst();
        }
        pending.addLast(new PendingVerification(
                flow,
                action,
                handler,
                player,
                returnedState == null ? action.before() : returnedState
        ));
    }

    public synchronized List<ContainerSlotPostTickObservation> observe(
            ContainerGuiFlowTrace activeFlow,
            long gameTick) {
        List<ContainerSlotPostTickObservation> observations = new ArrayList<>();
        for (Iterator<PendingVerification> iterator = pending.iterator(); iterator.hasNext(); ) {
            PendingVerification verification = iterator.next();
            if (verification.flow != activeFlow) {
                iterator.remove();
                continue;
            }
            long elapsed = gameTick - verification.action.requestGameTick();
            if (elapsed <= 0L) {
                continue;
            }
            int ticksAfterRequest = (int) Math.min(Integer.MAX_VALUE, elapsed);
            ContainerItemCountSnapshot current = snapshots.after(
                    verification.handler,
                    verification.player,
                    verification.action
            );
            verification.checkOrdinal++;
            boolean changedFromRequest = !sameCounts(verification.action.before(), current);
            boolean changedSincePrevious = !sameCounts(verification.previous, current);
            boolean closed = ticksAfterRequest >= MAX_OBSERVATION_TICKS;
            observations.add(new ContainerSlotPostTickObservation(
                    verification.flow,
                    verification.action,
                    verification.previous,
                    current,
                    gameTick,
                    ticksAfterRequest,
                    verification.checkOrdinal,
                    changedFromRequest,
                    changedSincePrevious,
                    closed
            ));
            verification.previous = current;
            if (closed) {
                iterator.remove();
            }
        }
        return List.copyOf(observations);
    }

    public synchronized ContainerSlotActionObservation latestMatchingAction(
            ContainerGuiFlowTrace activeFlow,
            ScreenHandler handler,
            int syncId,
            long gameTick) {
        Iterator<PendingVerification> iterator = pending.descendingIterator();
        while (iterator.hasNext()) {
            PendingVerification verification = iterator.next();
            long elapsed = gameTick - verification.action.requestGameTick();
            if (verification.flow == activeFlow
                    && verification.handler == handler
                    && verification.action.syncId() == syncId
                    && elapsed >= 0L
                    && elapsed <= MAX_OBSERVATION_TICKS) {
                return verification.action;
            }
        }
        return null;
    }

    public synchronized void clear() {
        pending.clear();
    }

    private static boolean sameCounts(
            ContainerItemCountSnapshot first,
            ContainerItemCountSnapshot second) {
        if (first == second) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }
        return first.playerItemCount() == second.playerItemCount()
                && first.containerItemCount() == second.containerItemCount()
                && first.cursorItemCount() == second.cursorItemCount()
                && first.cursorStack().equals(second.cursorStack())
                && first.clickedSlotStack().equals(second.clickedSlotStack())
                && first.furnaceMaterialSlot().equals(second.furnaceMaterialSlot())
                && first.furnaceFuelSlot().equals(second.furnaceFuelSlot())
                && first.furnaceOutputSlot().equals(second.furnaceOutputSlot());
    }

    private static final class PendingVerification {
        private final ContainerGuiFlowTrace flow;
        private final ContainerSlotActionObservation action;
        private final ScreenHandler handler;
        private final ClientPlayerEntity player;
        private ContainerItemCountSnapshot previous;
        private int checkOrdinal;

        private PendingVerification(
                ContainerGuiFlowTrace flow,
                ContainerSlotActionObservation action,
                ScreenHandler handler,
                ClientPlayerEntity player,
                ContainerItemCountSnapshot previous) {
            this.flow = flow;
            this.action = action;
            this.handler = handler;
            this.player = player;
            this.previous = previous;
        }
    }
}
