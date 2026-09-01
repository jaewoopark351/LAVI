package lavi.minecraft.diagnostics.session.runtime;

import adris.altoclef.tasksystem.Task;

//20260831_kpopmodder: Expose only pre-admitted bounded record emission inside one atomic group lease.
@FunctionalInterface
public interface DiagnosticBoundedGroupEmitter {
    void emit(
            String eventName,
            String reason,
            Task task,
            int maxUtf8Bytes,
            Object[] requiredFields,
            Object[] optionalFields);
}
