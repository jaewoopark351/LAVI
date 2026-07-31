package lavi.minecraft.integration.carryon.snapshot;

//20260731_kpopmodder: Group input fields for Carry On diagnostics.
public record CarryOnInputSnapshot(String rightClickState,
                                   String sneakState,
                                   String leftClickState,
                                   String movementInputState) {
}
