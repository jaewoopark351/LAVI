package lavi.minecraft.task.container.deposit.auto.maintenance.working;

import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceTask;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunReason;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunResult;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;
import lavi.minecraft.task.container.deposit.auto.recovery.AutoDepositDestinationManifest;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;

import java.util.Objects;
import java.util.Optional;

//20260914_kpopmodder: Retain only original recovery debt/provenance and completion proof, never an old slot child.
public final class AutoDepositRecoveryResumeState {
    private final WorkingSetSnapshot snapshot;
    private final AutoDepositDestinationManifest manifest;
    private final boolean childrenComplete;
    private final AutoDepositRunReason refusal;

    private AutoDepositRecoveryResumeState(WorkingSetSnapshot snapshot, AutoDepositDestinationManifest manifest,
            boolean childrenComplete, AutoDepositRunReason refusal) {
        this.snapshot = snapshot;
        this.manifest = manifest;
        this.childrenComplete = childrenComplete;
        this.refusal = refusal;
    }

    public static AutoDepositRecoveryResumeState from(AutoDepositPlan current, AutoDepositMaintenanceTask previous) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(previous, "previous");
        return capture(current, previous.snapshot(), previous.manifest(), previous.result());
    }

    static AutoDepositRecoveryResumeState capture(AutoDepositPlan current, WorkingSetSnapshot snapshot,
            AutoDepositDestinationManifest manifest, Optional<AutoDepositRunResult> settled) {
        AutoDepositRunReason refusal = null;
        if (settled.isEmpty() || !settled.get().cleanupComplete()) {
            refusal = AutoDepositRunReason.CLEANUP_FAILED;
        } else if (snapshot == null || manifest == null) {
            refusal = AutoDepositRunReason.WORKING_SET_UNAVAILABLE;
        } else if (snapshot.worldIdentity() != current.context().worldIdentity()
                || snapshot.dimension() != current.context().dimension()
                || snapshot.userTaskRoot() != current.context().userTaskRoot()
                || manifest.worldIdentity() != snapshot.worldIdentity()
                || manifest.dimension() != snapshot.dimension()) {
            refusal = AutoDepositRunReason.CONTEXT_CHANGED;
        }
        return new AutoDepositRecoveryResumeState(snapshot, manifest,
                settled.map(AutoDepositRunResult::childrenComplete).orElse(false), refusal);
    }

    public WorkingSetSnapshot snapshot() { return snapshot; }
    public AutoDepositDestinationManifest manifest() { return manifest; }
    public boolean childrenComplete() { return childrenComplete; }
    public Optional<AutoDepositRunReason> refusal() { return Optional.ofNullable(refusal); }
}
