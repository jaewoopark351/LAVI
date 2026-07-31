package lavi.minecraft.integration.carryon.snapshot;

//20260731_kpopmodder: Group Baritone-related diagnostic fields without owning Baritone cleanup.
public record CarryOnBaritoneSnapshot(String baritonePathing,
                                      String customGoalOwner,
                                      String breakingBlockState) {
}
