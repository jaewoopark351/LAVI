package lavi.minecraft.diagnostics.crafting.acquisition.source.container.reconciliation;

import adris.altoclef.tasksystem.Task;

//20260901_kpopmodder: Expose exact container child reconciliation identities after scheduling.
public interface CraftResourceContainerReconciliationSourceEventListener {
    void onChildReconciliation(
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
            boolean childCleared,
            boolean sourceEmissionCompleted
    );
}
