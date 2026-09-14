package lavi.minecraft.task.container.deposit.auto.recovery;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.store.deposit.pressure.AutoDepositObservationOwner;
import lavi.minecraft.diagnostics.container.store.deposit.pressure.AutoDepositOperationObservation;
import lavi.minecraft.diagnostics.observation.ObservationScope;
import adris.altoclef.util.helpers.WorldHelper;
import lavi.minecraft.task.container.deposit.auto.DepositAllAutoDiagnostics;
import lavi.minecraft.task.container.deposit.auto.working.PlayerInventorySnapshotReader;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;
import lavi.minecraft.task.container.deposit.auto.recovery.progress.ConfirmedRecoveredItemCount;
import net.minecraft.item.Item;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

public final class RecoverReservedItemsTask extends Task implements AutoDepositObservationOwner {
    private static final int CANDIDATE_TIMEOUT_TICKS = 600;

    private final WorkingSetSnapshot snapshot;
    private final AutoDepositDestinationManifest manifest;
    private final PlayerInventorySnapshotReader inventoryReader = new PlayerInventorySnapshotReader();
    private final AutoDepositRecoveryCandidateSelector selector = new AutoDepositRecoveryCandidateSelector();

    private final Queue<AutoDepositRecoveryCandidate> candidates = new ArrayDeque<>();
    private AutoDepositRecoveryCandidate currentCandidate;
    private ExactPickupFromContainerTask currentPickup;
    //20260914_kpopmodder: Finished candidates are frozen; a live candidate is added only by the read-only getter.
    private final ConfirmedRecoveredItemCount completedRecoveredItems = new ConfirmedRecoveredItemCount();
    private Terminal terminal = Terminal.RUNNING;
    private int candidateTicks;
    private boolean queueInitialized;
    //20260913_kpopmodder: Preserve original diagnostic provenance through delayed recovery termination.
    private final AutoDepositOperationObservation observation;

    public RecoverReservedItemsTask(WorkingSetSnapshot snapshot,
                                    AutoDepositDestinationManifest manifest) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.manifest = Objects.requireNonNull(manifest, "manifest");
        observation = new AutoDepositOperationObservation(snapshot.userTaskRoot());
    }

    @Override
    public ObservationScope diagnosticObservationScope() {
        return observation == null ? ObservationScope.NOOP : observation.scope();
    }

    @Override
    protected void onStart() {
        AltoClef mod = AltoClef.getInstance();
        if (!queueInitialized && terminal == Terminal.RUNNING) {
            Map<Item, Integer> deficits = currentDeficits(mod);
            List<AutoDepositRecoveryCandidate> selected = selector.select(mod, snapshot, deficits, manifest);
            candidates.addAll(selected);
            queueInitialized = true;
            DepositAllAutoDiagnostics.logRecoveryQueue(this, snapshot.epoch(), deficits.size(), selected.size());
        }
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        if (!contextMatches(mod)) {
            terminal = Terminal.CANCELLED_CONTEXT_CHANGED;
            return null;
        }
        Map<Item, Integer> deficits = currentDeficits(mod);
        if (deficits.isEmpty()) {
            terminal = Terminal.SATISFIED;
            return null;
        }

        if (currentPickup != null) {
            candidateTicks++;
            if (currentPickup.isFinished()) {
                DepositAllAutoDiagnostics.logRecoveryCandidateTerminal(this,
                        snapshot.epoch(),
                        currentCandidate,
                        currentPickup.result().name(),
                        currentDeficits(mod).size()
                );
                completedRecoveredItems.add(currentPickup.confirmedRecoveredCount());
                currentPickup = null;
                currentCandidate = null;
                candidateTicks = 0;
                return null;
            }
            if (candidateTicks >= CANDIDATE_TIMEOUT_TICKS) {
                DepositAllAutoDiagnostics.logRecoveryCandidateTerminal(this,
                        snapshot.epoch(),
                        currentCandidate,
                        "TIMEOUT",
                        deficits.size()
                );
                completedRecoveredItems.add(currentPickup.confirmedRecoveredCount());
                currentPickup = null;
                currentCandidate = null;
                candidateTicks = 0;
                return null;
            }
            return currentPickup;
        }

        currentCandidate = candidates.poll();
        if (currentCandidate == null) {
            terminal = Terminal.EXHAUSTED;
            return null;
        }
        Map<Item, Integer> current = inventoryReader.readMainAndCursor(mod);
        AutoDepositRecoveryTransferPlan transferPlan = AutoDepositRecoveryTransferPlan.create(
                current,
                deficits,
                currentCandidate
        );
        if (transferPlan.isEmpty()) {
            currentCandidate = null;
            return null;
        }
        currentPickup = new ExactPickupFromContainerTask(
                currentCandidate.position(),
                transferPlan.targetInventoryCounts(),
                transferPlan.withdrawalLimits()
        );
        candidateTicks = 0;
        DepositAllAutoDiagnostics.logRecoveryCandidateSelected(this,
                snapshot.epoch(), currentCandidate, deficits.size()
        );
        return currentPickup;
    }

    private Map<Item, Integer> currentDeficits(AltoClef mod) {
        return snapshot.deficits(inventoryReader.readMainAndCursor(mod));
    }

    private boolean contextMatches(AltoClef mod) {
        return mod.getWorld() == snapshot.worldIdentity()
                && WorldHelper.getCurrentDimension() == snapshot.dimension()
                && mod.getUserTaskChain() != null
                && mod.getUserTaskChain().getCurrentTask() == snapshot.userTaskRoot();
    }

    public Terminal terminal() {
        return terminal;
    }

    public int confirmedRecoveredCount() {
        return completedRecoveredItems.including(currentPickup == null ? 0 : currentPickup.confirmedRecoveredCount());
    }

    @Override
    protected void onStop(Task interruptTask) {
    }

    @Override
    public boolean isFinished() {
        return terminal != Terminal.RUNNING;
    }

    @Override
    protected boolean isEqual(Task other) {
        return this == other;
    }

    @Override
    protected String toDebugString() {
        return "Recovering automatic deposit working set";
    }

    public enum Terminal {
        RUNNING,
        SATISFIED,
        EXHAUSTED,
        CANCELLED_CONTEXT_CHANGED
    }
}
