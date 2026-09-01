package lavi.minecraft.diagnostics.crafting.acquisition.terminal;

//20260901_kpopmodder: Keep the first authoritative command termination cause immutable.
public enum CraftResourcePrimaryTerminationCause {
    NATURAL_TASK_FINISH,
    EXISTING_COMMAND_DEADLINE,
    EXPLICIT_CANCEL,
    CONNECTION_DETACH_CANCEL,
    COMMAND_EXCEPTION,
    DISPATCH_EXCEPTION,
    UNKNOWN
}
