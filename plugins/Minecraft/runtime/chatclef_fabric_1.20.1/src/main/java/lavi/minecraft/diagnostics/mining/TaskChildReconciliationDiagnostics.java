package lavi.minecraft.diagnostics.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.resources.MineAndCollectTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.reconciliation.ReconciliationDiagnosticFields;
import lavi.minecraft.diagnostics.mining.reconciliation.ReconciliationOutcome;
import lavi.minecraft.diagnostics.mining.reconciliation.ReconciliationSemanticFingerprint;
import lavi.minecraft.diagnostics.mining.reconciliation.ReconciliationTaskIdentity;
import lavi.minecraft.diagnostics.mining.reconciliation.ReconciliationTaskSnapshot;

//20260806_kpopmodder: Observe child candidate reconciliation without altering Task scheduling.
final class TaskChildReconciliationDiagnostics {
    private TaskChildReconciliationDiagnostics() {
    }

    static void log(Task parent,
                    Task activeChildBefore,
                    Task candidateChild,
                    boolean isEqualResult,
                    boolean canInterruptEvaluated,
                    boolean canInterruptPreviousChild,
                    boolean replacementApplied,
                    boolean previousChildStopCalled,
                    Task activeChildAfter,
                    boolean candidateDiscardedBecauseEqual) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || !(parent instanceof MineAndCollectTask.MineOrCollectTask)) {
            return;
        }
        if (!(activeChildBefore instanceof DestroyBlockTask)
                && !(candidateChild instanceof DestroyBlockTask)
                && !(activeChildAfter instanceof DestroyBlockTask)) {
            return;
        }
        MineAndCollectTask.MineOrCollectTask mineParent = (MineAndCollectTask.MineOrCollectTask) parent;
        ReconciliationTaskIdentity activeBeforeIdentity = ReconciliationTaskIdentity.capture(activeChildBefore);
        ReconciliationTaskIdentity candidateIdentity = ReconciliationTaskIdentity.capture(candidateChild);
        ReconciliationTaskIdentity activeAfterIdentity = ReconciliationTaskIdentity.capture(activeChildAfter);
        ReconciliationOutcome outcome = ReconciliationOutcome.classify(
                activeBeforeIdentity,
                candidateIdentity,
                activeAfterIdentity,
                isEqualResult,
                canInterruptEvaluated,
                canInterruptPreviousChild,
                replacementApplied,
                candidateDiscardedBecauseEqual
        );
        String fingerprint = ReconciliationSemanticFingerprint.create(
                parent, activeBeforeIdentity, candidateIdentity, activeAfterIdentity, outcome,
                isEqualResult, replacementApplied, candidateDiscardedBecauseEqual);
        MiningDiagnosticEmitter.emitLazy("TASK_CHILD_RECONCILIATION", "task_child_reconciliation", parent,
                "task_child_reconciliation|" + System.identityHashCode(parent),
                fingerprint,
                () -> {
                    AltoClef mod = AltoClef.getInstance();
                    ReconciliationTaskSnapshot activeBefore = ReconciliationTaskSnapshot.capture(
                            mod, mineParent, activeChildBefore);
                    ReconciliationTaskSnapshot candidate = ReconciliationTaskSnapshot.capture(
                            mod, mineParent, candidateChild);
                    ReconciliationTaskSnapshot activeAfter = ReconciliationTaskSnapshot.capture(
                            mod, mineParent, activeChildAfter);
                    return ReconciliationDiagnosticFields.capture(mod, mineParent, activeBefore, candidate,
                            activeAfter, outcome, isEqualResult, canInterruptEvaluated,
                            canInterruptPreviousChild, replacementApplied, previousChildStopCalled,
                            candidateDiscardedBecauseEqual);
                });
    }
}
