package lavi.minecraft.integration.carryon.reflection;

import net.fabricmc.loader.api.FabricLoader;

//20260731_kpopmodder: Keep Fabric mod presence/version probing separate from reflective state reads.
public final class CarryOnModProbe {
    private static final String MOD_ID = "carryon";

    public CarryOnModStatus probe() {
        try {
            FabricLoader loader = FabricLoader.getInstance();
            boolean loaded = loader.isModLoaded(MOD_ID);
            String version = loaded
                    ? loader.getModContainer(MOD_ID)
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown")
                    : "absent";
            return CarryOnModStatus.available(loaded, version);
        } catch (LinkageError | RuntimeException e) {
            return CarryOnModStatus.failed(e);
        }
    }
}
