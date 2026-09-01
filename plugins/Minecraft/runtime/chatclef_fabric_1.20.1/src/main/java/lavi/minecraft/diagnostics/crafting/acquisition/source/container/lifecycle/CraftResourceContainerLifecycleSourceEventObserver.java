package lavi.minecraft.diagnostics.crafting.acquisition.source.container.lifecycle;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetObservationKind;

//20260902_kpopmodder: Isolate optional owner-stop projections from container cleanup behavior.
public final class CraftResourceContainerLifecycleSourceEventObserver {
    private static final CraftResourceContainerLifecycleSourceEventListener NO_OP =
            (owner, interruptTask, terminationKind, sourceEmissionCompleted) -> {
            };
    private static volatile CraftResourceContainerLifecycleSourceEventListener listener =
            NO_OP;

    private CraftResourceContainerLifecycleSourceEventObserver() {
    }

    public static void install(
            CraftResourceContainerLifecycleSourceEventListener installed) {
        listener = installed == null ? NO_OP : installed;
    }

    public static boolean isOwnerTracked(Task owner) {
        try {
            return listener.isOwnerTracked(owner);
        } catch (RuntimeException | LinkageError ignoredDiagnosticFailure) {
            return false;
        }
    }

    public static void observe(
            Task owner,
            Task interruptTask,
            CraftResourceTargetObservationKind terminationKind,
            boolean sourceEmissionCompleted) {
        try {
            listener.onOwnerStop(
                    owner,
                    interruptTask,
                    terminationKind,
                    sourceEmissionCompleted
            );
        } catch (RuntimeException | LinkageError ignoredDiagnosticFailure) {
            // An optional observer cannot change owner cleanup or Task lifecycle.
        }
    }
}
