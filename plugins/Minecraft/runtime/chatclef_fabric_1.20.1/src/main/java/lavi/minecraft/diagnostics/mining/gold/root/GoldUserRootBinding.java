package lavi.minecraft.diagnostics.mining.gold.root;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.observation.ObservationScope;

import java.lang.ref.WeakReference;
import java.util.function.Supplier;

//20260913_kpopmodder: Keep exact root-assignment provenance without retaining a live Task tree.
record GoldUserRootBinding(WeakReference<Task> root, String assignment,
                          ObservationScope scope, Supplier<Object[]> terminalFields) {
    boolean matches(Task task, String rootAssignment) {
        return root.get() == task && assignment.equals(rootAssignment);
    }
}
