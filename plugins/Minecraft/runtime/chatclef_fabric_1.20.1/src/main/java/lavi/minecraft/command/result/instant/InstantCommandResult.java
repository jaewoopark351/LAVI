package lavi.minecraft.command.result.instant;

import java.util.Map;

//20260915_kpopmodder: Frozen observation of one synchronous native command decision.
public record InstantCommandResult(String command, String commandName, String outcome, String reason, Map<String, Object> values) {
    public InstantCommandResult { values = Map.copyOf(values); }
    public Map<String, Object> toMap() {
        return Map.of("schema_version", 1, "command", command, "command_name", commandName,
                "outcome", outcome, "reason", reason, "values", values);
    }
}
