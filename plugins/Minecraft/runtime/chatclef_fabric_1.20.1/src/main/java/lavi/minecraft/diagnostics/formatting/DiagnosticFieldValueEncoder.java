package lavi.minecraft.diagnostics.formatting;

import java.nio.charset.StandardCharsets;

//20260831_kpopmodder: Keep whitespace-delimited diagnostic key/value records unambiguous.
public final class DiagnosticFieldValueEncoder {
    public static final String EMPTY_TOKEN = "~EMPTY~";
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private DiagnosticFieldValueEncoder() {
    }

    public static String encode(Object rawValue) {
        String value = DiagnosticValueFormatter.value(rawValue);
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        StringBuilder encoded = new StringBuilder(bytes.length);
        for (byte rawByte : bytes) {
            int unsigned = rawByte & 0xFF;
            if (isTokenSafeAscii(unsigned)) {
                encoded.append((char) unsigned);
            } else {
                encoded.append('%')
                        .append(HEX[(unsigned >>> 4) & 0x0F])
                        .append(HEX[unsigned & 0x0F]);
            }
        }
        return encoded.length() == 0 ? EMPTY_TOKEN : encoded.toString();
    }

    private static boolean isTokenSafeAscii(int value) {
        return value >= 0x21
                && value <= 0x7E
                && value != '='
                && value != '%'
                && value != '~';
    }
}
