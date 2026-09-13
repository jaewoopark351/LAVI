//#if MC == 12001
package lavi.minecraft.integration.toolselect.equip.validation;
import lavi.minecraft.integration.toolselect.equip.execution.ToolEquipPort;
import lavi.minecraft.integration.toolselect.equip.model.*;
//20260913_kpopmodder: Preserve the caller's selected value/window rather than silently accepting a newer source.
public final class ToolEquipRequestFactory {
    private ToolEquipRequestFactory() { }
    public static ToolEquipRequest capture(ToolEquipPort port, ToolEquipPurpose purpose,
            int source, int sourceWindow, ToolStackValue selected, int destination) {
        ToolEquipFrame actual = port.read(source, destination);
        ToolEquipFrame expected = new ToolEquipFrame(actual.binding(), selected, actual.destination(),
                sourceWindow, actual.destinationWindow(), actual.selectedHotbar(), actual.mappingValid(),
                actual.cursorEmpty(), actual.exchangeAllowed(), actual.inputAvailable(), actual.sourceUsable(), actual.destinationAvailable());
        return new ToolEquipRequest(purpose, source, destination, expected);
    }
}
//#endif
