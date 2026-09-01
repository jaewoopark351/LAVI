package lavi.minecraft.diagnostics.crafting.acquisition.target.position;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//20260901_kpopmodder: Canonicalize only already-observed comma-separated block positions.
public final class CraftResourceTargetPosition {
    private static final Pattern COMMA_SEPARATED_BLOCK_POSITION = Pattern.compile(
            "^\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*$"
    );

    private CraftResourceTargetPosition() {
    }

    public static String canonicalize(String value) {
        if (value == null || value.isBlank()) {
            return "UNAVAILABLE";
        }
        String normalized = value.trim();
        Matcher matcher = COMMA_SEPARATED_BLOCK_POSITION.matcher(normalized);
        if (!matcher.matches()) {
            return normalized;
        }
        return matcher.group(1) + "," + matcher.group(2) + "," + matcher.group(3);
    }
}
