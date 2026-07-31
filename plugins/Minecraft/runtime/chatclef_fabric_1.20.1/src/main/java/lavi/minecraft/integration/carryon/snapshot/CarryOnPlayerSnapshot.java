package lavi.minecraft.integration.carryon.snapshot;

//20260731_kpopmodder: Group player pose and held-item fields for Carry On diagnostics.
public record CarryOnPlayerSnapshot(String playerPosition,
                                    String playerVelocity,
                                    String lookRotation,
                                    String playerPoseState,
                                    String selectedHotbarSlot,
                                    String mainHandItem,
                                    String offHandItem) {
}
