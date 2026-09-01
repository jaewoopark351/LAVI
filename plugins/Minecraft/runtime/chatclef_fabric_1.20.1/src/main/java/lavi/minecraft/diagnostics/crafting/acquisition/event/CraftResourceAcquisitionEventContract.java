package lavi.minecraft.diagnostics.crafting.acquisition.event;

import lavi.minecraft.diagnostics.formatting.DiagnosticBoundedEventFormatter;
import lavi.minecraft.diagnostics.formatting.DiagnosticBoundedEventText;
import lavi.minecraft.diagnostics.session.admission.DiagnosticAdmissionRequest;
import lavi.minecraft.diagnostics.session.admission.DiagnosticEventFamily;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticEventFamilyClassifier;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

//20260901_kpopmodder: Reuse shared family admission and bounded formatting for acquisition events.
public final class CraftResourceAcquisitionEventContract {
    private static final int PHYSICAL_BYTE_LIMIT = 8_192;
    public static final String UNAVAILABLE_SOURCE_EVENT_SEQUENCE =
            "UNAVAILABLE_SOURCE_EMITTER_DOES_NOT_EXPOSE_SEQUENCE";
    private static final Set<String> SEMANTIC_FINGERPRINT_FIELDS = Set.of(
            "event",
            "eventName",
            "rootTaskClass",
            "parentTaskClass",
            "childTaskClass",
            "resourceStage",
            "targetRole",
            "targetPosition",
            "expectedBlockIds",
            "observedBlockId",
            "requestedItem",
            "requestedCount",
            "currentItemCount",
            "targetItemCount",
            "activeRequirementItem",
            "activeRequirementCount",
            "selectedBranch",
            "associationStatus",
            "reason"
    );

    public DiagnosticEventFamily familyFor(String eventName) {
        return DiagnosticEventFamilyClassifier.classify(eventName);
    }

    public DiagnosticAdmissionRequest admissionRequest(String eventName, boolean modeEligible) {
        DiagnosticEventFamily family = familyFor(eventName);
        return modeEligible
                ? DiagnosticAdmissionRequest.eligible(family)
                : DiagnosticAdmissionRequest.modeIneligible(family);
    }

    public Map<String, Object> sourceReference(String sourceEventName, Object sourceEventSequence) {
        String exactSourceEventName = CraftResourceSourceEventName
                .requireExact(sourceEventName)
                .name();
        return Map.of(
                "sourceEventName", exactSourceEventName,
                "sourceEventSequence", normalizedSourceEventSequence(sourceEventSequence)
        );
    }

    public String semanticFingerprint(Map<String, Object> fields) {
        Map<String, Object> selected = new LinkedHashMap<>();
        if (fields != null) {
            fields.keySet().stream()
                    .filter(SEMANTIC_FINGERPRINT_FIELDS::contains)
                    .sorted()
                    .forEach(key -> selected.put(key, normalizeSemanticValue(fields.get(key))));
        }
        return sha256Hex(renderSemanticFields(selected));
    }

    public DiagnosticBoundedEventText format(
            String eventName,
            Map<String, Object> requiredFields,
            Map<String, Object> optionalFields
    ) {
        if (eventName == null || eventName.isBlank()) {
            throw new IllegalArgumentException("eventName must be nonblank");
        }
        return DiagnosticBoundedEventFormatter.format(
                "[LAVI ChatClefBoundary]",
                new Object[]{
                        "event", eventName
                },
                requiredFieldArray(requiredFields),
                optionalFieldArray(optionalFields),
                PHYSICAL_BYTE_LIMIT
        );
    }

    public int physicalByteLimit() {
        return PHYSICAL_BYTE_LIMIT;
    }

    Object[] requiredFieldArray(Map<String, Object> requiredFields) {
        if (requiredFields == null) {
            throw new IllegalArgumentException("requiredFields must include source provenance");
        }
        Object rawSourceEventName = requiredFields.get("sourceEventName");
        if (!(rawSourceEventName instanceof String sourceEventName)) {
            throw new IllegalArgumentException("sourceEventName must be an exact String value");
        }
        String exactSourceEventName = CraftResourceSourceEventName
                .requireExact(sourceEventName)
                .name();

        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("behavior_effect", "none");
        normalized.put("sourceEventName", exactSourceEventName);
        normalized.put(
                "sourceEventSequence",
                normalizedSourceEventSequence(requiredFields.get("sourceEventSequence"))
        );
        requiredFields.forEach((key, value) -> {
            if (!isReservedProvenanceField(key)) {
                normalized.put(key, value);
            }
        });
        return toFieldArray(normalized);
    }

    Object[] optionalFieldArray(Map<String, Object> optionalFields) {
        if (optionalFields == null || optionalFields.isEmpty()) {
            return new Object[0];
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        optionalFields.forEach((key, value) -> {
            if (!isReservedProvenanceField(key)) {
                normalized.put(key, value);
            }
        });
        return toFieldArray(normalized);
    }

    private static boolean isReservedProvenanceField(String key) {
        return "behavior_effect".equals(key)
                || "sourceEventName".equals(key)
                || "sourceEventSequence".equals(key);
    }

    private static Object normalizedSourceEventSequence(Object sourceEventSequence) {
        if (sourceEventSequence instanceof Number) {
            return sourceEventSequence;
        }
        if (sourceEventSequence instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        return UNAVAILABLE_SOURCE_EVENT_SEQUENCE;
    }

    private static Object normalizeSemanticValue(Object value) {
        if (value instanceof Iterable<?> iterable) {
            List<String> normalized = new ArrayList<>();
            for (Object element : iterable) {
                if (element instanceof String stringValue) {
                    normalized.add(stringValue);
                }
            }
            Collections.sort(normalized);
            return List.copyOf(normalized);
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean
                || value instanceof Enum<?>) {
            return value;
        }
        return "UNAVAILABLE_UNSAFE_SEMANTIC_VALUE";
    }

    private static String renderSemanticFields(Map<String, Object> fields) {
        StringBuilder rendered = new StringBuilder();
        fields.forEach((key, value) -> rendered
                .append(key.length()).append(':').append(key)
                .append('=')
                .append(renderSemanticValue(value))
                .append(';'));
        return rendered.toString();
    }

    private static String renderSemanticValue(Object value) {
        if (value instanceof List<?> list) {
            StringBuilder rendered = new StringBuilder("[");
            for (Object element : list) {
                String string = element instanceof String ? (String) element : "UNAVAILABLE";
                rendered.append(string.length()).append(':').append(string).append(',');
            }
            return rendered.append(']').toString();
        }
        if (value instanceof String string) {
            return string.length() + ":" + string;
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Enum<?>) {
            return value.toString();
        }
        return "UNAVAILABLE";
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte part : digest) {
                result.append(String.format("%02x", part & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 unavailable", error);
        }
    }

    private static Object[] toFieldArray(Map<String, Object> fields) {
        if (fields == null || fields.isEmpty()) {
            return new Object[0];
        }
        Object[] result = new Object[fields.size() * 2];
        int index = 0;
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            result[index++] = entry.getKey();
            result[index++] = entry.getValue();
        }
        return result;
    }
}
