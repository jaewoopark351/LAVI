package lavi.minecraft.overlay.visibility;

import adris.altoclef.AltoClef;
import baritone.api.Settings;

//20260805_kpopmodder: Keep overlay render state changes separate from diagnostics and user-facing output.
final class OverlayRenderVisibilityFacade {
    void setVisible(AltoClef mod, boolean visible) {
        mod.setBuiltInHudVisible(visible);
        setBaritoneRenderVisible(mod, visible);
    }

    private void setBaritoneRenderVisible(AltoClef mod, boolean visible) {
        Settings baritoneSettings = mod.getClientBaritoneSettings();
        baritoneSettings.renderPath.value = visible;
        baritoneSettings.renderGoal.value = visible;
    }
}
