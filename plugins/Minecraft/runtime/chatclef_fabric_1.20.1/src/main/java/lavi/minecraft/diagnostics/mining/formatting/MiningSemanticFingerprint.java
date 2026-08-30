package lavi.minecraft.diagnostics.mining.formatting;

//20260830_kpopmodder: Own stable semantic-fingerprint joining without runtime observation.
public final class MiningSemanticFingerprint {
    private MiningSemanticFingerprint() {
    }

    public static String join(String... values) {
        return String.join("|", values);
    }
}
