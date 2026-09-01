package lavi.minecraft.diagnostics.crafting.acquisition.terminal;

//20260901_kpopmodder: Freeze the opaque identity used by one two-phase terminal ledger.
public record CraftResourceTerminalKey(
        String commandSessionId,
        long commandConnectionGeneration,
        String commandRequestId,
        String commandCorrelationId,
        String rootAssignmentId
) {
}
