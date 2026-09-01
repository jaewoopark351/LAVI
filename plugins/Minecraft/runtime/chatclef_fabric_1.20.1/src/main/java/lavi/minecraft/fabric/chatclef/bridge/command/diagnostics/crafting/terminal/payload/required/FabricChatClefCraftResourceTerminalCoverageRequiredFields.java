package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.required;

import java.util.Map;

//20260901_kpopmodder: Keep explicit exception and observation-gap coverage in the envelope.
public final class FabricChatClefCraftResourceTerminalCoverageRequiredFields {
    private FabricChatClefCraftResourceTerminalCoverageRequiredFields() {
    }

    public static void append(Map<String, Object> fields) {
        fields.put(
                "blockOptionalMetaExceptionCount",
                "UNAVAILABLE_SOURCE_NOT_PRESENT_IN_CHECKOUT"
        );
        fields.put(
                "blockOptionalMetaCoverageGapCount",
                "UNAVAILABLE_NO_PROVEN_BOUNDARY_ENTRY"
        );
        fields.put("lastSuccessfulBoundary", "UNAVAILABLE_NOT_CAPTURED_SEPARATELY");
        fields.put("firstExplicitFailureBoundary", "UNAVAILABLE_NOT_CAPTURED_SEPARATELY");
        fields.put("firstUnobservedBoundaryAfter", "UNAVAILABLE_NOT_CAPTURED_SEPARATELY");
    }
}
