package lavi.minecraft.overlay.visibility;

import adris.altoclef.AltoClef;

//20260731_kpopmodder: Keep overlay visibility effects separate from command parsing.
public final class OverlayVisibilityController {
    private final OverlayRenderVisibilityFacade renderVisibility = new OverlayRenderVisibilityFacade();
    private final OverlayDiagnosticsVisibilityFacade diagnosticsVisibility = new OverlayDiagnosticsVisibilityFacade();
    private final OverlayVisibilityLogger logger = new OverlayVisibilityLogger();

    public void setVisible(AltoClef mod, boolean visible) {
        renderVisibility.setVisible(mod, visible);
        diagnosticsVisibility.setBoundaryEnabled(visible);
        logger.logVisible(visible);
    }
}
