package lavi.minecraft.diagnostics.crafting.acquisition.event;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticDispatchResult;

import java.util.Map;
import java.util.Objects;

/**
 * Sole physical logging route for resource-acquisition projections.
 */
public final class CraftResourceAcquisitionEventEmitter {
    private final CraftResourceAcquisitionEventContract contract;

    public CraftResourceAcquisitionEventEmitter() {
        this(new CraftResourceAcquisitionEventContract());
    }

    CraftResourceAcquisitionEventEmitter(CraftResourceAcquisitionEventContract contract) {
        this.contract = Objects.requireNonNull(contract, "contract");
    }

    public boolean emit(
            String eventName,
            String reason,
            Map<String, Object> requiredFields,
            Map<String, Object> optionalFields) {
        return emitWithDispatchResult(
                eventName,
                reason,
                requiredFields,
                optionalFields
        ).emissionCompleted();
    }

    public DiagnosticDispatchResult emitWithDispatchResult(
            String eventName,
            String reason,
            Map<String, Object> requiredFields,
            Map<String, Object> optionalFields) {
        Objects.requireNonNull(eventName, "eventName");
        Objects.requireNonNull(reason, "reason");

        return ChatClefDiagnostics.logBoundedBoundaryWithDispatchResult(
                eventName,
                reason,
                null,
                contract.physicalByteLimit(),
                contract.requiredFieldArray(requiredFields),
                contract.optionalFieldArray(optionalFields)
        );
    }
}
