package lavi.minecraft.diagnostics.crafting.acquisition.terminal;

//20260901_kpopmodder: Preserve result-send mechanics independently from command termination.
public enum CraftResourceResultSendStatus {
    NOT_ATTEMPTED,
    IN_FLIGHT,
    SENT,
    NO_SOCKET,
    FAILED,
    UNKNOWN
}
