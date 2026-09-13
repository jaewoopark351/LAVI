package lavi.minecraft.diagnostics.observation.emission;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.observation.ObservationScope;
import lavi.minecraft.diagnostics.observation.format.ObservationFields;
import lavi.minecraft.diagnostics.observation.state.ObservationEmission;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticDispatchResult;
import java.util.Locale;

/** Uses the existing physical logger and session admission authority; no alternate output channel. */
public final class ObservationBoundaryEmitter {
    private ObservationBoundaryEmitter() { }
    public static void emit(ObservationScope scope, ObservationEmission emission) {
        try {
            emitEligible(scope, emission);
        } catch (RuntimeException | LinkageError failure) {
            scope.ledger().settle(false, false, "DISPATCH_THREW_ADMISSION_UNKNOWN:" + failure.getClass().getSimpleName());
            lavi.minecraft.diagnostics.observation.ObservationDiagnostics.captureFailed(
                    scope.domain(), failure.getClass().getSimpleName());
        }
    }

    private static void emitEligible(ObservationScope scope, ObservationEmission emission) {
        String wrapper = switch (emission.tier()) {
            case "FIRST" -> "RESOURCE_OBSERVATION_" + scope.domain().toUpperCase(Locale.ROOT) + "_FIRST";
            case "SUMMARY" -> "RESOURCE_OBSERVATION_" + scope.domain().toUpperCase(Locale.ROOT) + "_SUMMARY";
            case "TERMINAL" -> "RESOURCE_OBSERVATION_TERMINAL";
            default -> "RESOURCE_OBSERVATION_DETAIL";
        };
        Object[] required = ObservationFields.concat(new Object[]{
                "observationDomain", scope.domain(), "observationOperationKey", scope.operationKey(),
                "observationActivation", scope.activation().generation(), "observationEpoch", scope.activation().epoch()},
                ObservationFields.concat(scope.context(), emission.required()));
        DiagnosticDispatchResult result = ChatClefDiagnostics.logBoundedBoundaryWithDispatchResult(
                wrapper, emission.reason(), null, 8_192, required, emission.optional());
        String outcome = !result.admitted() ? "ADMISSION_REJECTED:" + result.admission().rejectionReason()
                : result.emissionCompleted() ? "EMISSION_CALLS_RETURNED" : "EMISSION_FAILED_AFTER_ADMISSION";
        scope.ledger().settle(emission, result.admitted(), result.emissionCompleted(), outcome,
                result.admission().rejectionReason());
    }
}
