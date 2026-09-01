package lavi.minecraft.diagnostics.crafting.acquisition.association;

import java.nio.charset.StandardCharsets;

//20260901_kpopmodder: Bound retained association coverage-gap text by UTF-8 bytes.
final class CraftResourceAssociationTextBound {
    private static final int RETAINED_TEXT_UTF8_LIMIT = 256;

    private CraftResourceAssociationTextBound() {
    }

    static String unavailableIfBlank(String value) {
        if (value == null || value.isBlank()) {
            return "UNAVAILABLE";
        }
        if (value.getBytes(StandardCharsets.UTF_8).length <= RETAINED_TEXT_UTF8_LIMIT) {
            return value;
        }
        StringBuilder bounded = new StringBuilder();
        int usedBytes = 0;
        int offset = 0;
        while (offset < value.length()) {
            int codePoint = value.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            int characterBytes = character.getBytes(StandardCharsets.UTF_8).length;
            if (usedBytes + characterBytes > RETAINED_TEXT_UTF8_LIMIT) {
                break;
            }
            bounded.appendCodePoint(codePoint);
            usedBytes += characterBytes;
            offset += Character.charCount(codePoint);
        }
        return bounded.toString();
    }
}
