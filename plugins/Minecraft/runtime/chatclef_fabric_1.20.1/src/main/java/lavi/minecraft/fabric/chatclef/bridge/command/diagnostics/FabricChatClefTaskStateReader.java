package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;

import java.util.HashMap;
import java.util.Map;

//20260803_kpopmodder: Observe current ChatClef user task state without changing engine behavior.
public final class FabricChatClefTaskStateReader {
    public Task currentTaskOrNull() {
        try {
            return currentTaskOrThrow();
        } catch (Throwable ignored) {
            return null;
        }
    }

    public FabricChatClefTaskSnapshot captureCurrentTaskSnapshot() {
        try {
            return FabricChatClefTaskSnapshot.capture(currentTaskOrThrow());
        } catch (Throwable error) {
            return FabricChatClefTaskSnapshot.unavailable(error);
        }
    }

    public Map<String, Object> runtimeData() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("thread_name", Thread.currentThread().getName());
        payload.put("observed_at_ms", System.currentTimeMillis());
        payload.put("client_tick_id", ChatClefDiagnostics.currentClientTickId());
        payload.put("current_task", captureCurrentTaskSnapshot().toMap());
        return payload;
    }

    private Task currentTaskOrThrow() {
        AltoClef mod = AltoClef.getInstance();
        if (mod == null || mod.getUserTaskChain() == null) {
            return null;
        }
        return mod.getUserTaskChain().getCurrentTask();
    }
}
