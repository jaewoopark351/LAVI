package lavi.minecraft.diagnostics.mining.gold.root;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.observation.ObservationScope;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

//20260913_kpopmodder: Bound one UserTaskChain's diagnostic root-to-scope associations independently of emission.
final class GoldUserRootBindings {
    private final List<GoldUserRootBinding> bindings = new ArrayList<>();

    boolean add(Task root, String assignment, ObservationScope scope, Supplier<Object[]> terminalFields) {
        bindings.removeIf(entry -> entry.root().get() == null || !entry.scope().isCurrent());
        for (int index = 0; index < bindings.size(); index++) {
            GoldUserRootBinding entry = bindings.get(index);
            if (entry.scope() == scope) {
                // runTask can publish a new diagnostic assignment while retaining the same actual root.
                if (!entry.matches(root, assignment)) bindings.set(index,
                        new GoldUserRootBinding(new WeakReference<>(root), assignment, scope, terminalFields));
                return true;
            }
        }
        if (bindings.size() >= 4) return false;
        bindings.add(new GoldUserRootBinding(new WeakReference<>(root), assignment, scope, terminalFields));
        return true;
    }

    List<GoldUserRootBinding> remove(Task root, String assignment) {
        List<GoldUserRootBinding> removed = bindings.stream()
                .filter(entry -> entry.matches(root, assignment)).toList();
        bindings.removeAll(removed);
        return removed;
    }

    void reassign(Task root, String previousAssignment, String nextAssignment) {
        for (int index = 0; index < bindings.size(); index++) {
            GoldUserRootBinding entry = bindings.get(index);
            if (entry.matches(root, previousAssignment)) bindings.set(index,
                    new GoldUserRootBinding(entry.root(), nextAssignment, entry.scope(), entry.terminalFields()));
        }
    }

    void clear() { bindings.clear(); }
}
