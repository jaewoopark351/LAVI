package lavi.minecraft.fabric.chatclef.bridge.diagnostics;

//20260801_kpopmodder: Keep Fabric ChatClef bridge diagnostics out of shared Minecraft code.
public final class FabricChatClefBridgeDiagnostics {
    private static final String PREFIX = "[LAVI Fabric ChatClef Bridge] ";

    public void info(String message) {
        System.out.println(PREFIX + message);
    }

    public void warn(String message) {
        System.out.println(PREFIX + "[WARN] " + message);
    }

    public void error(String message) {
        System.err.println(PREFIX + "[ERROR] " + message);
    }
}
