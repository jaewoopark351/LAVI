package lavi.minecraft.diagnostics.observation.format;

import lavi.minecraft.diagnostics.observation.state.ObservationEmission;
import lavi.minecraft.diagnostics.observation.state.ObservationEvidenceMemory;
import lavi.minecraft.diagnostics.observation.state.ObservationOutputAccounting;

import java.util.Map;

//20260913_kpopmodder: Format already-frozen evidence and accounting into stable payload and summary fields.
public final class ObservationPayloadFormatter {
    private ObservationPayloadFormatter() { }

    public static String evidenceSnapshot(String event, String reason, long tick, Object[] fields) {
        return ObservationFields.boundUtf8("tick=" + tick + " " + event + ":" + reason + " "
                + ObservationFields.freeze(fields), 2_048);
    }

    public static ObservationEmission emission(String event, String reason, String tier, long tick, long nanos,
                                                Object[] fields, ObservationOutputAccounting output,
                                                ObservationEvidenceMemory evidence) {
        Object[] checkpointFields = "SUMMARY".equals(tier) || "TERMINAL".equals(tier)
                ? output.checkpointFields() : new Object[0];
        return new ObservationEmission(event, reason, tier, tick, nanos,
                ObservationFields.concat(new Object[]{"observedEvent", event, "observedReason", reason,
                        "observedTick", tick, "localTier", tier, "capturedCount", output.captured(),
                        "attemptedCount", output.attempted(), "priorAdmittedCount", output.admitted(),
                        "priorEmissionCallsReturned", output.returned(), "priorEmissionFailures", output.failures(),
                        "suppressedCount", output.suppressed(), "firstSignatureOverflow", evidence.firstOverflow(),
                        "priorDispatchOutcome", output.lastOutcome(), "filePersistence", "NOT_VERIFIED"},
                        ObservationFields.concat(checkpointFields, fields)),
                optionalFields(evidence));
    }

    private static Object[] optionalFields(ObservationEvidenceMemory evidence) {
        Object[] result = new Object[]{"firstSignatureCount", evidence.firstCount(),
                "recentTransitionCount", evidence.recentCount()};
        int index = 0;
        for (Map.Entry<String, String> pin : evidence.pins().entrySet()) {
            result = ObservationFields.concat(result, "firstPin" + index + "Name", pin.getKey());
            result = ObservationFields.concat(result, ObservationFields.chunks("firstPin" + index++, pin.getValue()));
        }
        return ObservationFields.concat(result,
                ObservationFields.chunks("recentTransitions", evidence.recentTransitions().toString()));
    }

    public static Object[] summary(ObservationOutputAccounting output, ObservationEvidenceMemory evidence) {
        return ObservationFields.concat(new Object[]{"captured", output.captured(), "attempted", output.attempted(),
                "admitted", output.admitted(), "emissionCallsReturned", output.returned(),
                "dispatchFailures", output.failures(), "suppressed", output.suppressed(),
                "firstOverflow", evidence.firstOverflow(), "lastOutcome", output.lastOutcome(),
                "firstPins", evidence.pins().toString(), "recentSize", evidence.recentCount(),
                "firstSize", evidence.firstCount()}, output.checkpointFields());
    }
}
