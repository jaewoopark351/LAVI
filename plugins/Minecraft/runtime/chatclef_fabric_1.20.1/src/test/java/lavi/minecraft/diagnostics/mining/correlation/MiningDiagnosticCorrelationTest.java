package lavi.minecraft.diagnostics.mining.correlation;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260830_kpopmodder: Keep task-null Baritone correlation stable without querying Store ownership.
class MiningDiagnosticCorrelationTest {
    @Test
    void taskNullUsesCallerBucketFallbackAndExplicitUnavailableStoreOperation() {
        MiningDiagnosticCorrelation correlation = MiningDiagnosticCorrelation.capture(
                null,
                "baritone_calculation|17",
                "BARITONE_BUCKET_OR_GENERATION_FALLBACK"
        );

        assertEquals("baritone_calculation|17", correlation.key());
        assertEquals("BARITONE_BUCKET_OR_GENERATION_FALLBACK", correlation.source());
        assertEquals("UNAVAILABLE", fields(correlation.commandContextFields()).get("storeOperationId"));
    }

    private static Map<String, Object> fields(Object[] values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index + 1 < values.length; index += 2) {
            result.put(String.valueOf(values[index]), values[index + 1]);
        }
        return result;
    }
}
