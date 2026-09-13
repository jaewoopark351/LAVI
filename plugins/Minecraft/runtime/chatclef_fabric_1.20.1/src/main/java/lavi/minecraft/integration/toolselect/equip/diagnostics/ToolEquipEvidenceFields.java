//#if MC == 12001
package lavi.minecraft.integration.toolselect.equip.diagnostics;

import lavi.minecraft.integration.toolselect.equip.execution.ToolEquipAttempt;
import lavi.minecraft.integration.toolselect.equip.model.ToolStackValue;

//20260913_kpopmodder: Format immutable request/observation values without serializing NBT or reading the game again.
public final class ToolEquipEvidenceFields {
    private ToolEquipEvidenceFields() { }
    public static Object[] capture(ToolEquipAttempt attempt) {
        var request = attempt.request();
        var expected = request.expected();
        var observed = attempt.lastObservedFrame();
        int selectedTarget = request.sourceInventory() < 9 ? request.sourceInventory() : request.destinationHotbar();
        return new Object[]{
                "purpose", request.purpose(), "sourceInventory", request.sourceInventory(),
                "sourceWindow", expected.sourceWindow(), "destinationHotbar", request.destinationHotbar(),
                "destinationWindow", expected.destinationWindow(), "status", attempt.status(),
                "swapCount", attempt.swapCount(), "confirmationEvaluations", attempt.confirmationEvaluations(),
                "maxConfirmationEvaluations", 20,
                "expectedSource", summary(expected.source()), "expectedDestination", summary(expected.destination()),
                "observedFrameAvailable", observed != null,
                "observedSource", observed == null ? "UNAVAILABLE" : summary(observed.source()),
                "observedDestination", observed == null ? "UNAVAILABLE" : summary(observed.destination()),
                "observedSourceWindow", observed == null ? -1 : observed.sourceWindow(),
                "observedDestinationWindow", observed == null ? -1 : observed.destinationWindow(),
                "observedSelectedHotbar", observed == null ? -1 : observed.selectedHotbar(),
                "observedBindingMatches", observed != null && expected.binding().matches(observed.binding()),
                "selectedSourceAtExpectedLocation", observed != null && expected.source().equals(
                        request.sourceInventory() < 9 ? observed.source() : observed.destination()),
                "displacedDestinationAtSource", observed != null && (request.sourceInventory() < 9
                        || expected.destination().equals(observed.source())),
                "selectedHandSlotMatches", observed != null && observed.selectedHotbar() == selectedTarget,
                "comparisonIncludesItemCountDamageAndNbt", true,
                "observationAuthority", "EXISTING_LOCAL_FRAME_READ_NOT_SERVER_ACK",
                "requestValueIsPersistentIdentity", false};
    }
    private static String summary(ToolStackValue value) {
        String item = String.valueOf(value.item());
        if (item.length() > 96) item = item.substring(0, 96);
        return "item=" + item + ",count=" + value.count() + ",damage=" + value.damage() + ",nbtPresent=" + (value.tag() != null);
    }
}
//#endif
