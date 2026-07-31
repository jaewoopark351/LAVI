package lavi.minecraft.overlay.visibility;

import adris.altoclef.AltoClef;
import baritone.api.Settings;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260731_kpopmodder: Keep overlay visibility effects separate from command parsing.
public final class OverlayVisibilityController {
    public void setVisible(AltoClef mod, boolean visible) {
        mod.setBuiltInHudVisible(visible);
        setBaritoneRenderVisible(mod, visible);
        ChatClefDiagnostics.setBoundaryEnabled(visible);
        System.out.println("ALTO CLEF: LAVI overlay " + (visible ? "ON" : "OFF")
                + "; diagnostics=" + (visible ? "BOUNDARY" : "OFF"));
    }

    private void setBaritoneRenderVisible(AltoClef mod, boolean visible) {
        Settings baritoneSettings = mod.getClientBaritoneSettings();
        baritoneSettings.renderPath.value = visible;
        baritoneSettings.renderGoal.value = visible;
    }
}
