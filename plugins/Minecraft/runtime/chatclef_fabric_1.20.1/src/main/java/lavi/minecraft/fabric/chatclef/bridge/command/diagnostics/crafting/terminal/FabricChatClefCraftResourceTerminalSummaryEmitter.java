package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceTerminalDecision;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticDispatchResult;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.FabricChatClefCraftResourceTerminalPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.FabricChatClefCraftResourceTerminalPayloadAssembler;

//20260901_kpopmodder: Emit one preassembled bounded summary without owning payload policy.
final class FabricChatClefCraftResourceTerminalSummaryEmitter {
    private static final int PHYSICAL_UTF8_BYTE_LIMIT = 8_192;
    private static final String EVENT_NAME =
            "CRAFT_RESOURCE_ACQUISITION_TERMINAL_SUMMARY";
    private static final FabricChatClefCraftResourceTerminalPayloadAssembler PAYLOADS =
            new FabricChatClefCraftResourceTerminalPayloadAssembler();

    DiagnosticDispatchResult emit(
            CraftResourceTerminalDecision decision,
            FabricChatClefCraftResourceTerminalScopeBinding terminalBinding,
            FabricChatClefCraftResourceTerminalEvidence evidence,
            String sourceEventName) {
        FabricChatClefCraftResourceTerminalPayload payload = PAYLOADS.assemble(
                decision,
                terminalBinding,
                evidence,
                sourceEventName
        );
        return ChatClefDiagnostics.logBoundedBoundaryWithDispatchResult(
                EVENT_NAME,
                "craft_resource_acquisition_terminal_summary",
                null,
                PHYSICAL_UTF8_BYTE_LIMIT,
                payload.requiredFieldArray(),
                payload.optionalFieldArray()
        );
    }
}
