package lavi.minecraft.diagnostics.crafting.acquisition.source.container.reconciliation;

import adris.altoclef.tasksystem.Task;

//20260901_kpopmodder: Keep optional Fabric projection failures outside Task scheduling.
public final class CraftResourceContainerReconciliationSourceEventObserver {
    private static final CraftResourceContainerReconciliationSourceEventListener NO_OP =
            (parent, activeChildBefore, candidateChild, isEqualResult,
                    canInterruptEvaluated, canInterruptPreviousChild,
                    replacementApplied, previousChildStopCalled, activeChildAfter,
                    candidateDiscardedBecauseEqual, childCleared,
                    sourceEmissionCompleted) -> {
            };

    private static volatile CraftResourceContainerReconciliationSourceEventListener listener =
            NO_OP;

    private CraftResourceContainerReconciliationSourceEventObserver() {
    }

    public static void install(
            CraftResourceContainerReconciliationSourceEventListener installed) {
        listener = installed == null ? NO_OP : installed;
    }

    public static void observe(
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
            boolean sourceEmissionCompleted) {
        try {
            listener.onChildReconciliation(
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
                    childCleared,
                    sourceEmissionCompleted
            );
        } catch (RuntimeException | LinkageError ignoredDiagnosticFailure) {
            // An optional observer cannot change Task scheduling or lifecycle.
        }
    }
}
