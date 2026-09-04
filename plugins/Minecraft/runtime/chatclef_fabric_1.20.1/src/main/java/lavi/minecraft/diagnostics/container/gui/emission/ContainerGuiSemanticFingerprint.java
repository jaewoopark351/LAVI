package lavi.minecraft.diagnostics.container.gui.emission;

import java.util.StringJoiner;

//20260904_kpopmodder: Exclude ticks, serials, sync IDs, correlations, and object identities from dedupe keys.
public final class ContainerGuiSemanticFingerprint {
    private ContainerGuiSemanticFingerprint() {
    }

    public static String of(Object... stableSemanticValues) {
        StringJoiner joiner = new StringJoiner("|");
        if (stableSemanticValues != null) {
            for (Object value : stableSemanticValues) {
                joiner.add(value == null ? "UNAVAILABLE" : String.valueOf(value));
            }
        }
        return joiner.toString();
    }
}
