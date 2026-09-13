//#if MC == 12001
package lavi.minecraft.integration.toolselect.equip.validation;
import lavi.minecraft.integration.toolselect.equip.model.*;
//20260913_kpopmodder: Validate both ends of the proposed swap before selection or click side effects.
public final class ToolEquipValidation {
    private ToolEquipValidation() { }
    public static ToolEquipStatus beforeAction(ToolEquipRequest request, ToolEquipFrame actual) {
        if (actual == null || !request.expected().binding().matches(actual.binding())) return ToolEquipStatus.INVALID_BINDING;
        if (!actual.inputAvailable()) return ToolEquipStatus.PREEMPTED;
        if (request.sourceInventory() < 0 || request.sourceInventory() >= 36
                || request.destinationHotbar() < 0 || request.destinationHotbar() >= 9
                || !actual.mappingValid() || actual.sourceWindow() != request.expected().sourceWindow()
                || actual.destinationWindow() != request.expected().destinationWindow()) return ToolEquipStatus.INVALID_MAPPING;
        if (!actual.cursorEmpty()) return ToolEquipStatus.CURSOR_OCCUPIED;
        if (actual.source().empty() || !actual.source().equals(request.expected().source())) return ToolEquipStatus.SOURCE_CHANGED;
        if (!actual.destination().equals(request.expected().destination())) return ToolEquipStatus.DESTINATION_CHANGED;
        if (!actual.sourceUsable()) return ToolEquipStatus.SOURCE_UNUSABLE;
        if (!actual.destinationAvailable()) return ToolEquipStatus.DESTINATION_UNAVAILABLE;
        if (request.sourceInventory() >= 9 && !actual.exchangeAllowed()) return ToolEquipStatus.SWAP_NOT_ALLOWED;
        return ToolEquipStatus.NEW;
    }
}
//#endif
