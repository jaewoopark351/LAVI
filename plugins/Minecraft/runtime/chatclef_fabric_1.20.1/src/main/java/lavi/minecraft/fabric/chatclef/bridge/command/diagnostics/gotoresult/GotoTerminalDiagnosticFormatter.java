package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.gotoresult;

import java.util.Map;

//20260913_kpopmodder: Bound and sanitize diagnostic fields without rendering command dialogue.
public final class GotoTerminalDiagnosticFormatter {
    private static final int MAX_FIELDS = 32;
    private static final int MAX_KEY_LENGTH = 48;
    private static final int MAX_VALUE_LENGTH = 160;

    public String format(Map<String, Object> fields) {
        StringBuilder line = new StringBuilder("goto terminal ");
        int count = 0;
        for (Map.Entry<String, Object> field : fields.entrySet()) {
            if (++count > MAX_FIELDS) break;
            line.append(token(field.getKey(), MAX_KEY_LENGTH)).append('=')
                    .append(token(field.getValue(), MAX_VALUE_LENGTH)).append(' ');
        }
        return line.toString().stripTrailing();
    }

    private String token(Object value, int limit) {
        String text = value instanceof String string ? string
                : value instanceof Boolean || value instanceof Number ? value.toString() : "UNAVAILABLE";
        StringBuilder result = new StringBuilder(Math.min(text.length(), limit));
        for (int index = 0; index < Math.min(text.length(), limit); index++) {
            char ch = text.charAt(index);
            result.append(ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z'
                    || ch >= '0' && ch <= '9' || "._:-@/".indexOf(ch) >= 0 ? ch : '_');
        }
        return result.toString();
    }
}
