package lavi.minecraft.diagnostics.crafting.acquisition.target;

//20260901_kpopmodder: Keep resource progress independent from the target block's role.
public enum CraftResourceStage {
    RECIPE_PLANNING,
    IRON_INPUT_ACQUISITION,
    RAW_IRON_SMELTING,
    FINAL_CRAFTING,
    PLACEMENT_SUPPORT,
    UNKNOWN
}
