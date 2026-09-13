package lavi.minecraft.diagnostics.observation;

import lavi.minecraft.diagnostics.formatting.DiagnosticBoundedEventFormatter;
import lavi.minecraft.diagnostics.observation.format.ObservationFields;
import lavi.minecraft.diagnostics.observation.state.ObservationLedger;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class ObservationPayloadTest {
    @Test void physicalFormatterRetainsLaterFieldsInsteadOfTruncatingOneCombinedSnapshot() {
        Object[] fields = ObservationFields.copy("longDetail", "a".repeat(500),
                "targetSlot", 19, "actualStep", "MOVE_ACCESS_PICKAXE_TO_HOTBAR");
        var emission = new ObservationLedger().capture("PREPARE", "HOTBAR_MISSING", "missing", false, 1, 1, fields);
        var formatted = DiagnosticBoundedEventFormatter.format("test ", new Object[0],
                ObservationFields.concat(new Object[]{"commandRequestId", "original-request"}, emission.required()),
                emission.optional(), 8192);
        String rendered = formatted.toString();
        assertTrue(rendered.contains("original-request"));
        assertTrue(rendered.contains("targetSlot"));
        assertTrue(rendered.contains("MOVE_ACCESS_PICKAXE_TO_HOTBAR"));
        assertTrue(rendered.contains("TRUNCATED"));
    }

    @Test void utf8CapsRetainWholeCodePointsAndSignalLoss() {
        String bounded = ObservationFields.text("가😀".repeat(200));
        assertTrue(bounded.getBytes(StandardCharsets.UTF_8).length <= 240);
        assertTrue(bounded.endsWith("[TRUNCATED]"));
        assertFalse(bounded.contains("\ufffd"));
        String snapshot = ObservationFields.freeze("value", "가😀".repeat(2000));
        assertTrue(snapshot.getBytes(StandardCharsets.UTF_8).length <= 2048);
        Object[] chunks = ObservationFields.chunks("pin", "가😀".repeat(500));
        assertTrue(Arrays.asList(chunks).contains(Boolean.TRUE));
    }

    @Test void requestContextCannotBeChangedThroughCallerOrReturnedArrays() {
        Object[] original = {"commandRequestId", "old-request"};
        ObservationScope scope = new ObservationScope(null, "mining", "old", original);
        original[1] = "new-request";
        Object[] copy = scope.context();
        copy[1] = "newer-request";
        assertEquals("old-request", scope.context()[1]);
    }
}
