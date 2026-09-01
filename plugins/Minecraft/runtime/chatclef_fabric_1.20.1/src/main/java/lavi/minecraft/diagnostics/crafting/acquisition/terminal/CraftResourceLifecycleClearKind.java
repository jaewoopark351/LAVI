package lavi.minecraft.diagnostics.crafting.acquisition.terminal;

//20260901_kpopmodder: Identify the existing lifecycle barrier without clearing lifecycle state.
public enum CraftResourceLifecycleClearKind {
    NORMAL_TERMINAL_RESULT_SENT,
    CONNECTION_DETACHED,
    OTHER,
    UNAVAILABLE
}
