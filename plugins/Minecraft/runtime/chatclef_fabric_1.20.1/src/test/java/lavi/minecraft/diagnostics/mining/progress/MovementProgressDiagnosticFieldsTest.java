package lavi.minecraft.diagnostics.mining.progress;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260830_kpopmodder: Preserve checker result and reset fields in the admitted detail projection.
class MovementProgressDiagnosticFieldsTest {
    @Test
    void fieldsExposeExistingResetProvenanceAndCheckResult() {
        BlockPos target = new BlockPos(2, 70, 3);
        Map<String, Object> fields = fields(MovementProgressDiagnosticFields.capture(
                null, target, "DESTROY_MOVE", 1, true, true, true, "BARITONE_PATHING", "PASS",
                true, true, false, "not_evaluated", false, "not_evaluated"));

        assertEquals(true, fields.get("resetObservedBeforeCheck"));
        assertEquals("BARITONE_PATHING", fields.get("resetReason"));
        assertEquals(true, fields.get("checkResult"));
    }

    private static Map<String, Object> fields(Object[] values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index + 1 < values.length; index += 2) {
            result.put(String.valueOf(values[index]), values[index + 1]);
        }
        return result;
    }
}
