package lavi.minecraft.diagnostics.crafting.acquisition.terminal;

//20260901_kpopmodder: Return an immutable terminal observation decision to the emitter.
public record CraftResourceTerminalDecision(
        boolean finalized,
        boolean summaryRequested,
        String reason,
        CraftResourceTerminalSnapshot snapshot
) {
}
