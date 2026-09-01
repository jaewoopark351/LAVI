package lavi.minecraft.diagnostics.crafting.acquisition.terminal;

//20260901_kpopmodder: Distinguish an observed cleanup barrier from diagnostic retention expiry.
public enum CraftResourceFinalizationMode {
    PENDING,
    OBSERVED_BARRIER,
    DIAGNOSTIC_FALLBACK_INCOMPLETE
}
