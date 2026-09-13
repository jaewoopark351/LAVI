package lavi.minecraft.diagnostics.observation.format;

import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;

/** Frozen bounded text only; never retain or invoke a gameplay object's toString. */
public final class ObservationFields {
    private ObservationFields() { }

    public static String text(Object value) {
        String result;
        if (value == null) result = "UNKNOWN";
        else if (value instanceof String string) result = string;
        else if (value instanceof Number || value instanceof Boolean || value instanceof Character)
            result = String.valueOf(value);
        else if (value instanceof Enum<?> enumeration) result = enumeration.name();
        else result = value.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(value));
        result = result.replace('\n', ' ').replace('\r', ' ');
        return boundUtf8(result, 240);
    }

    public static String boundUtf8(String value, int bytes) {
        if (value.getBytes(StandardCharsets.UTF_8).length <= bytes) return value;
        int end = 0, used = 0;
        while (end < value.length()) {
            int point = value.codePointAt(end);
            int width = point <= 0x7f ? 1 : point <= 0x7ff ? 2 : point <= 0xffff ? 3 : 4;
            if (used + width > bytes - 11) break;
            used += width;
            end += Character.charCount(point);
        }
        return value.substring(0, end) + "[TRUNCATED]";
    }

    public static Object[] copy(Object... fields) {
        List<Object> result = new ArrayList<>();
        int bytes = 0;
        if (fields != null) for (int n = 0; n < fields.length; n += 2) {
            String key = text(fields[n]);
            String value = text(n + 1 < fields.length ? fields[n + 1] : "MISSING_VALUE");
            bytes += key.getBytes(StandardCharsets.UTF_8).length + value.getBytes(StandardCharsets.UTF_8).length;
            if (n >= 160 || bytes > 6_144) {
                result.add("observationFieldsTruncated"); result.add(true); break;
            }
            result.add(key); result.add(value);
        }
        return result.toArray();
    }

    public static Object[] chunks(String prefix, String frozen) {
        List<Object> result = new ArrayList<>();
        int offset = 0, part = 0;
        while (offset < frozen.length() && part < 12) {
            int end = Math.min(frozen.length(), offset + 64);
            if (end < frozen.length() && Character.isHighSurrogate(frozen.charAt(end - 1))) end--;
            result.add(prefix + "Part" + part++); result.add(frozen.substring(offset, end));
            offset = end;
        }
        result.add(prefix + "Truncated"); result.add(offset < frozen.length());
        return result.toArray();
    }

    public static String freeze(Object... fields) {
        StringBuilder result = new StringBuilder();
        if (fields != null) {
            for (int index = 0; index < fields.length; index += 2) {
                String pair = text(fields[index]) + "="
                        + (index + 1 < fields.length ? text(fields[index + 1]) : "MISSING_VALUE");
                if (result.length() + pair.length() > 1_800) {
                    result.append(" fieldsTruncated=true");
                    break;
                }
                if (!result.isEmpty()) result.append(' ');
                result.append(pair);
            }
        }
        return boundUtf8(result.toString(), 2_048);
    }

    public static Object[] concat(Object[] first, Object... second) {
        List<Object> values = new ArrayList<>(first.length + second.length);
        java.util.Collections.addAll(values, first);
        java.util.Collections.addAll(values, second);
        return values.toArray();
    }
}
