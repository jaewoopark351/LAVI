//#if MC == 12001
package lavi.minecraft.integration.toolselect.equip.execution;
import lavi.minecraft.integration.toolselect.equip.model.*;
import lavi.minecraft.integration.toolselect.equip.validation.ToolEquipValidation;
import lavi.minecraft.integration.toolselect.equip.confirmation.ToolEquipPostcondition;
//20260913_kpopmodder: Emit one swap at most and confirm only during this owner's active evaluations.
public final class ToolEquipAttempt {
    public static final int MAX_CONFIRMATION_EVALUATIONS = 20;
    private final ToolEquipRequest request;
    private ToolEquipStatus status = ToolEquipStatus.NEW;
    private boolean submitted, selectionSubmitted;
    private int swaps, confirmations;
    //20260913_kpopmodder: Retain only a frame already read by the action owner; logging never re-reads game slots.
    private ToolEquipFrame lastObservedFrame;
    public ToolEquipAttempt(ToolEquipRequest request) { this.request = java.util.Objects.requireNonNull(request); }
    public ToolEquipStatus advance(ToolEquipPort port) {
        if (status.terminal()) return status;
        ToolEquipFrame frame = lastObservedFrame = port.read(request.sourceInventory(), request.destinationHotbar());
        if (frame == null || !request.expected().binding().matches(frame.binding())) return status = ToolEquipStatus.INVALID_BINDING;
        if (!frame.inputAvailable()) return status = ToolEquipStatus.PREEMPTED;
        if (!submitted) {
            ToolEquipStatus validation = ToolEquipValidation.beforeAction(request, frame);
            if (validation != ToolEquipStatus.NEW) return status = validation;
            submitted = true;
            if (request.sourceInventory() >= 9) {
                swaps++;
                port.swap(frame.sourceWindow(), request.destinationHotbar());
                frame = lastObservedFrame = port.read(request.sourceInventory(), request.destinationHotbar());
            }
        }
        if (frame == null || !request.expected().binding().matches(frame.binding())) return status = ToolEquipStatus.INVALID_BINDING;
        if (!frame.inputAvailable()) return status = ToolEquipStatus.PREEMPTED;
        if (ToolEquipPostcondition.placementReflected(request, frame)) {
            if (request.purpose() == ToolEquipPurpose.HOTBAR_PLACEMENT) return status = ToolEquipStatus.PLACED;
            int target = request.sourceInventory() < 9 ? request.sourceInventory() : request.destinationHotbar();
            if (frame.selectedHotbar() == target) return status = ToolEquipStatus.SELECTED;
            if (!selectionSubmitted) {
                selectionSubmitted = true;
                port.selectHotbar(target);
                frame = lastObservedFrame = port.read(request.sourceInventory(), request.destinationHotbar());
                if (frame != null && request.expected().binding().matches(frame.binding())
                        && frame.inputAvailable() && frame.selectedHotbar() == target
                        && ToolEquipPostcondition.placementReflected(request, frame)) return status = ToolEquipStatus.SELECTED;
            }
        }
        confirmations++;
        return status = confirmations >= MAX_CONFIRMATION_EVALUATIONS
                ? ToolEquipStatus.CONFIRMATION_TIMEOUT : ToolEquipStatus.WAITING_CONFIRMATION;
    }
    public ToolEquipRequest request() { return request; }
    public ToolEquipStatus status() { return status; }
    public int swapCount() { return swaps; }
    public int confirmationEvaluations() { return confirmations; }
    public ToolEquipFrame lastObservedFrame() { return lastObservedFrame; }
}
//#endif
