//#if MC == 12001
package lavi.minecraft.integration.toolselect.equip.diagnostics;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.integration.toolselect.equip.execution.ToolEquipAttempt;
import java.util.HashSet;
import java.util.Set;
//20260913_kpopmodder: Keep bounded passive output separate from action/counter ownership.
public final class ToolEquipAttemptDiagnostics {
    private final Set<String> emitted = new HashSet<>();
    private boolean terminalEmitted;
    public void result(Task owner, ToolEquipAttempt attempt, String boundary) {
        try {
            String key = boundary + ":" + attempt.status().name();
            if (emitted.size() >= 32 || !emitted.add(key)) return;
            ChatClefDiagnostics.logBoundedBoundaryWithDispatchResult("MINING_OPERATION_TOOL_EXACT_RESULT", key, owner, 4096,
                    ToolEquipEvidenceFields.capture(attempt), new Object[0]);
        } catch (RuntimeException | LinkageError ignored) { }
    }
    public void failure(Task owner, String reason, int evaluations) {
        try {
            if (terminalEmitted) return;
            terminalEmitted = true;
            ChatClefDiagnostics.logBoundedBoundaryWithDispatchResult("MINING_OPERATION_TOOL_PLACEMENT_FAILURE", reason, owner, 4096,
                    new Object[]{"failureReason", reason, "activeEvaluations", evaluations, "maxActiveEvaluations", 100}, new Object[0]);
        } catch (RuntimeException | LinkageError ignored) { }
    }
}
//#endif
