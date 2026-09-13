package lavi.minecraft.diagnostics.observation.state.checkpoint;

import java.util.Set;

//20260913_kpopmodder: Leave coarse deposit checkpoints to evaluated decisions, not empty tick-entry transport events.
public final class ObservationCheckpointEventPolicy {
    private static final Set<String> ENTRY_EVENTS = Set.of("AUTO_DEPOSIT_RUNTIME_ENTRY", "AUTO_DEPOSIT_BINDING_RETURN",
            "AUTO_DEPOSIT_PRESSURE_RETURN", "AUTO_DEPOSIT_EVALUATION_ENTER",
            "AUTO_DEPOSIT_SCHEDULER_EVALUATED", "AUTO_DEPOSIT_SCHEDULER_SELECTED");

    private ObservationCheckpointEventPolicy() { }

    public static boolean allows(String event) { return !ENTRY_EVENTS.contains(event); }
}
