package lavi.minecraft.overlay.visibility;

//20260805_kpopmodder: Keep overlay command output formatting in one narrow logging helper.
final class OverlayVisibilityLogger {
    void logVisible(boolean visible) {
        System.out.println("ALTO CLEF: LAVI overlay " + (visible ? "ON" : "OFF")
                + "; diagnostics=" + (visible ? "BOUNDARY" : "OFF"));
    }
}
