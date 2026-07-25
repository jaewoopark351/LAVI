package adris.altoclef.player2api;

//20260725_kpopmodder: Added this toggle to keep the original ChatClef AI companion API disabled for LAVI bridge builds.

public class Player2ApiFeatureToggle {

    private static final String ENABLED_PROPERTY = "chatclef.player2api.enabled";

    public static boolean isEnabled() {
        return Boolean.parseBoolean(System.getProperty(ENABLED_PROPERTY, "false"));
    }
}
