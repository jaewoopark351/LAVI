package lavi.minecraft.integration.carryon.snapshot;

//20260731_kpopmodder: Group target/crosshair fields for Carry On diagnostics.
public record CarryOnTargetSnapshot(String targetType,
                                    String targetId,
                                    String targetPosition,
                                    String dimension,
                                    String crosshairType,
                                    String crosshairTarget,
                                    String crosshairBlockId) {
}
