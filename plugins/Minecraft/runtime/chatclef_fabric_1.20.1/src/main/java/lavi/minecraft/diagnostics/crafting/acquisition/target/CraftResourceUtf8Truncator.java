package lavi.minecraft.diagnostics.crafting.acquisition.target;

import java.nio.charset.StandardCharsets;

//20260901_kpopmodder: Bound diagnostic text without splitting a UTF-8 code point.
final class CraftResourceUtf8Truncator {
    private CraftResourceUtf8Truncator() {
    }

    static String truncate(String value, int maximumBytes) {
        if (value == null || maximumBytes <= 0) {
            return "";
        }
        if (utf8Length(value) <= maximumBytes) {
            return value;
        }
        StringBuilder bounded = new StringBuilder();
        int usedBytes = 0;
        int offset = 0;
        while (offset < value.length()) {
            int codePoint = value.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            int characterBytes = utf8Length(character);
            if (usedBytes + characterBytes > maximumBytes) {
                break;
            }
            bounded.appendCodePoint(codePoint);
            usedBytes += characterBytes;
            offset += Character.charCount(codePoint);
        }
        return bounded.toString();
    }

    static int utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }
}
