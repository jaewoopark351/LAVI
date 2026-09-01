package lavi.minecraft.diagnostics.formatting;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//20260828_kpopmodder: Encode one diagnostic event under an explicit UTF-8 byte cap.
public final class DiagnosticBoundedEventFormatter {
    private static final int INITIAL_VALUE_LIMIT_BYTES = 256;
    private static final int[] REQUIRED_VALUE_LIMITS_BYTES = {
            128, 96, 64, 32, 16, 8, 4, 1
    };
    private static final int KEY_LIMIT_BYTES = 96;
    private static final String CAPTURE_STATUS_KEY = "diagnosticCaptureStatus";
    private static final Set<String> EXACT_REQUIRED_VALUE_KEYS = Set.of(
            "commandRequestId",
            "commandCorrelationId",
            "commandSessionId",
            "rootAssignmentId",
            "boundRootTaskInstanceId"
    );

    private DiagnosticBoundedEventFormatter() {
    }

    public static DiagnosticBoundedEventText format(
            String prefix,
            Object[] baseFields,
            Object[] requiredFields,
            Object[] optionalFields,
            int maxUtf8Bytes) {
        if (maxUtf8Bytes <= 0) {
            throw new IllegalArgumentException("maxUtf8Bytes must be positive");
        }

        NormalizedFields base = normalize(baseFields, INITIAL_VALUE_LIMIT_BYTES, false);
        NormalizedFields required = normalize(requiredFields, INITIAL_VALUE_LIMIT_BYTES, true);
        NormalizedFields optional = normalize(optionalFields, INITIAL_VALUE_LIMIT_BYTES, false);
        boolean partial = base.partial() || required.partial() || optional.partial();
        List<Field> retainedOptional = new ArrayList<>(optional.fields());

        String rendered = render(
                prefix,
                withCaptureStatus(base.fields(), required.fields(), partial),
                retainedOptional
        );
        while (utf8Length(rendered) > maxUtf8Bytes && !retainedOptional.isEmpty()) {
            retainedOptional.remove(retainedOptional.size() - 1);
            partial = true;
            rendered = render(
                    prefix,
                    withCaptureStatus(base.fields(), required.fields(), true),
                    retainedOptional
            );
        }

        if (utf8Length(rendered) > maxUtf8Bytes) {
            for (int valueLimit : REQUIRED_VALUE_LIMITS_BYTES) {
                base = normalize(baseFields, valueLimit, false);
                required = normalize(requiredFields, valueLimit, true);
                rendered = render(
                        prefix,
                        withCaptureStatus(base.fields(), required.fields(), true),
                        List.of()
                );
                partial = true;
                if (utf8Length(rendered) <= maxUtf8Bytes) {
                    break;
                }
            }
        }

        if (utf8Length(rendered) > maxUtf8Bytes) {
            rendered = truncateUtf8(
                    prefix + "diagnosticCaptureStatus=partial boundedPayloadUnavailable=true",
                    maxUtf8Bytes
            );
            partial = true;
        }

        return new DiagnosticBoundedEventText(
                rendered,
                partial,
                Math.max(0, optional.fields().size() - retainedOptional.size())
        );
    }

    public static int utf8Length(String value) {
        return value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length;
    }

    private static NormalizedFields normalize(
            Object[] fields,
            int valueLimitBytes,
            boolean preserveExactRequiredValues) {
        List<Field> normalized = new ArrayList<>();
        boolean partial = false;
        if (fields == null) {
            return new NormalizedFields(List.of(), false);
        }
        for (int index = 0; index < fields.length; index += 2) {
            String rawKey = DiagnosticValueFormatter.value(fields[index]);
            String rawValue = DiagnosticFieldValueEncoder.encode(
                    index + 1 < fields.length ? fields[index + 1] : "missing"
            );
            String key = truncateUtf8(rawKey, KEY_LIMIT_BYTES);
            boolean preserveExactValue = preserveExactRequiredValues
                    && EXACT_REQUIRED_VALUE_KEYS.contains(rawKey);
            String value = preserveExactValue
                    ? rawValue
                    : truncateUtf8(rawValue, valueLimitBytes);
            partial |= !key.equals(rawKey) || !value.equals(rawValue);
            normalized.add(new Field(key, value));
        }
        return new NormalizedFields(List.copyOf(normalized), partial);
    }

    private static List<Field> withCaptureStatus(
            List<Field> base,
            List<Field> required,
            boolean partial) {
        List<Field> result = new ArrayList<>(base.size() + required.size() + 1);
        result.addAll(base);
        boolean statusFound = false;
        for (Field field : required) {
            if (CAPTURE_STATUS_KEY.equals(field.key())) {
                result.add(new Field(field.key(), partial ? "partial" : field.value()));
                statusFound = true;
            } else {
                result.add(field);
            }
        }
        if (!statusFound) {
            result.add(new Field(CAPTURE_STATUS_KEY, partial ? "partial" : "complete"));
        }
        return result;
    }

    private static String render(String prefix, List<Field> required, List<Field> optional) {
        StringBuilder result = new StringBuilder(prefix == null ? "" : prefix);
        boolean needsSpace = result.length() > 0 && !Character.isWhitespace(
                result.charAt(result.length() - 1)
        );
        for (Field field : required) {
            needsSpace = append(result, field, needsSpace);
        }
        for (Field field : optional) {
            needsSpace = append(result, field, needsSpace);
        }
        return result.toString();
    }

    private static boolean append(StringBuilder target, Field field, boolean needsSpace) {
        if (needsSpace) {
            target.append(' ');
        }
        target.append(field.key()).append('=').append(field.value());
        return true;
    }

    private static String truncateUtf8(String value, int maxBytes) {
        if (value == null || value.isEmpty() || utf8Length(value) <= maxBytes) {
            return value == null ? "null" : value;
        }
        String suffix = "...";
        if (maxBytes <= utf8Length(suffix)) {
            return ".".repeat(Math.max(0, maxBytes));
        }
        int contentLimit = Math.max(0, maxBytes - utf8Length(suffix));
        StringBuilder result = new StringBuilder();
        int used = 0;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            int bytes = utf8Length(character);
            if (used + bytes > contentLimit) {
                break;
            }
            result.append(character);
            used += bytes;
            offset += Character.charCount(codePoint);
        }
        return result.append(suffix).toString();
    }

    private record Field(String key, String value) {
    }

    private record NormalizedFields(List<Field> fields, boolean partial) {
    }
}
