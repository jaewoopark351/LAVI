package lavi.minecraft.diagnostics.crafting.acquisition.source.container;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import net.minecraft.block.Block;

//20260901_kpopmodder: Keep generic container diagnostics independent of Fabric projections.
public final class CraftResourceContainerSourceEventObserver {
    private static final CraftResourceContainerSourceEventListener NO_OP =
            (task, target, blocks, reason, stateKey, fields) -> {
            };
    private static volatile CraftResourceContainerSourceEventListener listener = NO_OP;

    private CraftResourceContainerSourceEventObserver() {
    }

    public static void install(CraftResourceContainerSourceEventListener installed) {
        listener = installed == null ? NO_OP : installed;
    }

    public static void observeTargetDecision(
            Task task,
            ItemTarget containerTarget,
            Block[] containerBlocks,
            String reason,
            String stateKey,
            Object[] branchFields) {
        try {
            listener.onTargetDecision(
                    task,
                    containerTarget,
                    containerBlocks,
                    reason,
                    stateKey,
                    branchFields
            );
        } catch (RuntimeException | LinkageError ignoredDiagnosticFailure) {
            // The optional projection cannot change the authoritative log path.
        }
    }

    public static void observeChildSelection(
            Task task,
            Task candidateChild,
            ItemTarget containerTarget,
            Block[] containerBlocks,
            String reason,
            String stateKey,
            Object[] branchFields,
            boolean sourceEmissionCompleted) {
        try {
            listener.onChildSelection(
                    task,
                    candidateChild,
                    containerTarget,
                    containerBlocks,
                    reason,
                    stateKey,
                    branchFields,
                    sourceEmissionCompleted
            );
        } catch (RuntimeException | LinkageError ignoredDiagnosticFailure) {
            // The optional projection cannot change the authoritative log path.
        }
    }
}
