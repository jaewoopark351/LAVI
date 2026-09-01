package lavi.minecraft.diagnostics.crafting.acquisition.terminal;

import java.nio.charset.StandardCharsets;

//20260901_kpopmodder: Bound retained terminal diagnostics without altering opaque IDs.
public final class CraftResourceTerminalTextBound {
    public static final int RETAINED_TEXT_UTF8_LIMIT = 512;

    private CraftResourceTerminalTextBound() {
    }

    public static String safe(String value) {
        return truncate(value == null ? "" : value, RETAINED_TEXT_UTF8_LIMIT);
    }

    public static String unavailableIfBlank(String value) {
        return value == null || value.isBlank() ? "UNAVAILABLE" : safe(value);
    }

    static String truncate(String value, int maximumBytes) {
        if (value == null || maximumBytes <= 0) {
            return "";
        }
        if (value.getBytes(StandardCharsets.UTF_8).length <= maximumBytes) {
            return value;
        }
        StringBuilder bounded = new StringBuilder();
        int usedBytes = 0;
        int offset = 0;
        while (offset < value.length()) {
            int codePoint = value.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            int characterBytes = character.getBytes(StandardCharsets.UTF_8).length;
            if (usedBytes + characterBytes > maximumBytes) {
                break;
            }
            bounded.appendCodePoint(codePoint);
            usedBytes += characterBytes;
            offset += Character.charCount(codePoint);
        }
        return bounded.toString();
    }
}
