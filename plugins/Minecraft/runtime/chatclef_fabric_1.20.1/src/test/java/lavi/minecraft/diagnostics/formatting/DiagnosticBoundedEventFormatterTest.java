package lavi.minecraft.diagnostics.formatting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticBoundedEventFormatterTest {
    @Test
    void preservesRequiredFieldsAndDropsOptionalFieldsUnderTheUtf8Cap() {
        DiagnosticBoundedEventText result = DiagnosticBoundedEventFormatter.format(
                "ALTO CLEF: [test] ",
                new Object[]{"event", "STORE_HOME_MANIFEST_STALE"},
                new Object[]{
                        "operationId", "store-home-42",
                        "diagnosticCaptureStatus", "complete"
                },
                new Object[]{"optional", "x".repeat(4000)},
                256
        );

        assertTrue(DiagnosticBoundedEventFormatter.utf8Length(result.text()) <= 256);
        assertTrue(result.text().contains("operationId=store-home-42"));
        assertTrue(result.text().contains("diagnosticCaptureStatus=partial"));
        assertTrue(result.omittedOptionalFieldCount() > 0);
        assertTrue(result.partial());
    }

    @Test
    void leavesSmallCompleteEventsUnchanged() {
        DiagnosticBoundedEventText result = DiagnosticBoundedEventFormatter.format(
                "prefix ",
                new Object[]{"event", "event-name"},
                new Object[]{"diagnosticCaptureStatus", "complete"},
                new Object[]{"slot", 8},
                8192
        );

        assertFalse(result.partial());
        assertEquals(0, result.omittedOptionalFieldCount());
        assertTrue(result.text().contains("diagnosticCaptureStatus=complete"));
        assertTrue(result.text().contains("slot=8"));
    }

    @Test
    void keepsAllRequiredKeysWhenLargeValuesNeedFurtherBounding() {
        Object[] required = new Object[122];
        for (int index = 0; index < 60; index++) {
            required[index * 2] = "requiredField" + index;
            required[index * 2 + 1] = "v".repeat(4000);
        }
        required[120] = "diagnosticCaptureStatus";
        required[121] = "complete";

        DiagnosticBoundedEventText result = DiagnosticBoundedEventFormatter.format(
                "prefix ",
                new Object[]{"event", "STORE_HOME_MANIFEST_STALE"},
                required,
                new Object[0],
                8192
        );

        assertTrue(DiagnosticBoundedEventFormatter.utf8Length(result.text()) <= 8192);
        assertTrue(result.text().contains("requiredField0="));
        assertTrue(result.text().contains("requiredField59="));
        assertTrue(result.text().contains("diagnosticCaptureStatus=partial"));
        assertFalse(result.text().contains("boundedPayloadUnavailable=true"));
    }
}
