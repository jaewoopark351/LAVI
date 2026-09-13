//#if MC == 12001
package lavi.minecraft.integration.toolselect.equip.confirmation;
import lavi.minecraft.integration.toolselect.equip.model.*;
//20260913_kpopmodder: Confirm placement independently of a later engine or defense hand selection.
public final class ToolEquipPostcondition {
    private ToolEquipPostcondition() { }
    public static boolean placementReflected(ToolEquipRequest request, ToolEquipFrame actual) {
        if (!actual.mappingValid()) return false;
        if (request.sourceInventory() < 9) return actual.source().equals(request.expected().source());
        return actual.destination().equals(request.expected().source())
                && actual.source().equals(request.expected().destination());
    }
}
//#endif
