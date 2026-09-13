package lavi.minecraft.diagnostics.mining.gold.root;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.observation.ObservationDiagnostics;
import lavi.minecraft.diagnostics.observation.ObservationScope;
import lavi.minecraft.diagnostics.observation.format.ObservationFields;

import java.util.function.Supplier;

//20260913_kpopmodder: Close only registered gold scopes after an actual user-root removal; yielding is not terminal.
public final class GoldUserRootObservation {
    private final GoldUserRootBindings bindings = new GoldUserRootBindings();

    public void bind(Task root, String assignment, ObservationScope scope, Supplier<Object[]> terminalFields) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) { bindings.clear(); return; }
        try {
            if (root != null && scope.isCurrent() && !bindings.add(root, assignment, scope, terminalFields))
                ObservationDiagnostics.captureFailed("mining", "USER_ROOT_BINDING_CAP");
        } catch (RuntimeException | LinkageError failure) {
            ObservationDiagnostics.captureFailed("mining", "USER_ROOT_BINDING_FAILED");
        }
    }

    public void removed(Task previousRoot, Task currentRoot, String assignment, String reason) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) { bindings.clear(); return; }
        if (previousRoot == null || previousRoot == currentRoot) return;
        try {
            for (GoldUserRootBinding binding : bindings.remove(previousRoot, assignment)) {
                if (!binding.scope().isCurrent()) {
                    ObservationDiagnostics.captureFailed("mining", "STALE_USER_ROOT_TERMINAL");
                    continue;
                }
                binding.scope().close(reason, ObservationFields.concat(new Object[]{
                        "terminalOwner", "UserTaskChain", "rootAssignmentId", assignment,
                        "removedRootClass", previousRoot.getClass().getSimpleName(),
                        "removedRootIdentity", Integer.toHexString(System.identityHashCode(previousRoot)),
                        "rootActuallyRemoved", true, "commandSuccess", "NOT_INFERRED_FROM_ROOT_REMOVAL"
                }, binding.terminalFields().get()));
            }
        } catch (RuntimeException | LinkageError failure) {
            ObservationDiagnostics.captureFailed("mining", "USER_ROOT_TERMINAL_CAPTURE_FAILED");
        }
    }

    public void assigned(Task previousRoot, Task currentRoot, String previousAssignment, String nextAssignment) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) { bindings.clear(); return; }
        if (previousRoot != currentRoot) {
            removed(previousRoot, currentRoot, previousAssignment, "USER_ROOT_REPLACED");
            return;
        }
        try { bindings.reassign(previousRoot, previousAssignment, nextAssignment); }
        catch (RuntimeException | LinkageError failure) {
            ObservationDiagnostics.captureFailed("mining", "USER_ROOT_ASSIGNMENT_CAPTURE_FAILED");
        }
    }
}
