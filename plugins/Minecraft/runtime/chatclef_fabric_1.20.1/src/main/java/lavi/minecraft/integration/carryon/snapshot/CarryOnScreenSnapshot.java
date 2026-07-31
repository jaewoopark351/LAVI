package lavi.minecraft.integration.carryon.snapshot;

//20260731_kpopmodder: Group container/screen fields for Carry On diagnostics.
public record CarryOnScreenSnapshot(String screenName,
                                    String screenHandlerName,
                                    String screenHandlerSyncId,
                                    String cursorStack) {
}
