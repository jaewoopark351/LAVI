//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.diagnostics;

import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.capture.UserRootSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.payload.RootRetirementPayload;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.lang.ref.WeakReference;

//20260913_kpopmodder: First finite semantic boundaries use the bridge sink, outside exhausted engine detail budgets.
public final class RootTerminationTrace {
    private WeakReference<FabricChatClefCommandExecution> current = new WeakReference<>(null);
    private final Set<String> emitted = new HashSet<>();
    private final Set<String> required = new HashSet<>();
    private void bind(FabricChatClefCommandExecution execution) {
        if (current.get() != execution) {
            current = new WeakReference<>(execution);
            emitted.clear();
            required.clear();
        }
    }
    public void observe(FabricChatClefCommandDiagnostics diagnostics, FabricChatClefCommandExecution execution,
                        UserRootSnapshot snapshot) {
        bind(execution);
        String key = snapshot.status() + ":" + execution.rootTermination().reason()
                + ":" + execution.rootTermination().pending() + ":" + execution.rootTermination().ready();
        boolean firstRetirement = execution.rootTermination().retirementSnapshot() != null
                && required.add("retirement_observed");
        boolean firstReady = execution.rootTermination().ready() && required.add("retirement_ready");
        if (!firstRetirement && !firstReady && (emitted.size() >= 32 || !emitted.add(key))) return;
        try {
            var fields = new LinkedHashMap<String, Object>(RootRetirementPayload.fields(execution.rootTermination()));
            fields.put("read_status", snapshot.status().name());
            fields.put("read_error", snapshot.error());
            diagnostics.info("user_root_lifetime_boundary", execution, () -> fields);
        } catch (RuntimeException | LinkageError ignored) {
            // The diagnostic outcome never selects, defers or changes the command result.
        }
    }
    public void rejectedEvent(FabricChatClefCommandDiagnostics diagnostics, FabricChatClefCommandExecution execution,
                              String reason, Object task, Object lifetime) {
        bind(execution);
        if (!required.add("event_rejected:" + reason)) return;
        try {
            var fields = new LinkedHashMap<String, Object>(RootRetirementPayload.fields(execution.rootTermination()));
            fields.put("association_rejection", reason);
            fields.put("event_root_identity", task == null ? "none" : Integer.toHexString(System.identityHashCode(task)));
            fields.put("event_lifetime_identity", lifetime == null ? "none" : Integer.toHexString(System.identityHashCode(lifetime)));
            diagnostics.info("user_root_event_rejected", execution, () -> fields);
        } catch (RuntimeException | LinkageError ignored) {
            // Rejected-event diagnostics cannot attach the event or change the terminal result.
        }
    }
}
//#endif
