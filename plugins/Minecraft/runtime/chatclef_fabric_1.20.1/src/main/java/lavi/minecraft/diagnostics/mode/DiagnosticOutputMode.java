package lavi.minecraft.diagnostics.mode;

//20260731_kpopmodder: Keep diagnostic output mode parsing outside the main ChatClef diagnostic facade.
public enum DiagnosticOutputMode {
    OFF,
    BOUNDARY,
    VERBOSE;

    public static DiagnosticOutputMode fromEnvironment() {
        String configured = System.getProperty("lavi.chatclef.diagnostics");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("LAVI_CHATCLEF_DIAGNOSTICS");
        }
        if (configured != null && !configured.isBlank()) {
            return switch (configured.trim().toLowerCase()) {
                case "off" -> OFF;
                case "boundary" -> BOUNDARY;
                case "verbose" -> VERBOSE;
                default -> OFF;
            };
        }
        return OFF;
    }
}
