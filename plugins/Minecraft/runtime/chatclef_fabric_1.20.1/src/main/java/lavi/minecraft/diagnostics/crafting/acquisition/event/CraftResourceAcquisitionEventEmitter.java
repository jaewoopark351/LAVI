package lavi.minecraft.diagnostics.crafting.acquisition.event;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticDispatchResult;
import lavi.minecraft.diagnostics.session.admission.DiagnosticAdmissionDecision;
import lavi.minecraft.diagnostics.session.admission.DiagnosticCapTrigger;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.lifecycle.FabricChatClefCraftResourceDiagnosticLifecycle;

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

        //20260913_kpopmodder: Late projections cannot emit after their cleanup owner is unavailable.
        DiagnosticDispatchResult unavailable = new DiagnosticDispatchResult(new DiagnosticAdmissionDecision(
                false, null, null, DiagnosticAdmissionDecision.RejectionReason.MODE_INELIGIBLE,
                DiagnosticCapTrigger.NONE, ChatClefDiagnostics.diagnosticSessionSnapshot()), null, null);
        return FabricChatClefCraftResourceDiagnosticLifecycle.callIfAvailable(
                () -> ChatClefDiagnostics.logBoundedBoundaryWithDispatchResult(
                eventName,
                reason,
                null,
                contract.physicalByteLimit(),
                contract.requiredFieldArray(requiredFields),
                contract.optionalFieldArray(optionalFields)
        ), unavailable);
    }
}
