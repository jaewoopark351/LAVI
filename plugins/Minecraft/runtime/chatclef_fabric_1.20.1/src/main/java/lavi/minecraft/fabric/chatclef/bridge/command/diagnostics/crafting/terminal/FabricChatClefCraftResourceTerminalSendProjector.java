package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal;

import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceResultDeliveryStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceResultSendStatus;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;

import java.util.Locale;

//20260901_kpopmodder: Project only directly observable command-context send state.
final class FabricChatClefCraftResourceTerminalSendProjector {
    FabricChatClefCraftResourceTerminalSendObservation fromContext(
            FabricChatClefCommandContext context
    ) {
        if (context == null) {
            return unknown();
        }
        if (context.terminalSent()) {
            return new FabricChatClefCraftResourceTerminalSendObservation(
                    CraftResourceResultSendStatus.SENT,
                    CraftResourceResultDeliveryStatus.DELIVERED,
                    true,
                    false
            );
        }
        if (context.terminalSendInFlight()) {
            return new FabricChatClefCraftResourceTerminalSendObservation(
                    CraftResourceResultSendStatus.IN_FLIGHT,
                    CraftResourceResultDeliveryStatus.UNKNOWN,
                    false,
                    true
            );
        }
        String outcome = safe(context.lastTerminalSendOutcome()).toUpperCase(Locale.ROOT);
        if (outcome.startsWith("NO_SOCKET")) {
            return failed(CraftResourceResultSendStatus.NO_SOCKET);
        }
        if (outcome.startsWith("GENERATION_MISMATCH")
                || outcome.startsWith("ENCODE_FAILED")
                || outcome.startsWith("SEND_FAILED")
                || outcome.startsWith("ASYNC_SEND_FAILED")
                || context.terminalSendRetryExhausted()) {
            return failed(CraftResourceResultSendStatus.FAILED);
        }
        if (context.terminalSendAttemptCount() == 0) {
            return new FabricChatClefCraftResourceTerminalSendObservation(
                    CraftResourceResultSendStatus.NOT_ATTEMPTED,
                    CraftResourceResultDeliveryStatus.NOT_DELIVERED,
                    false,
                    false
            );
        }
        return unknown();
    }

    FabricChatClefCraftResourceTerminalSendObservation started() {
        return new FabricChatClefCraftResourceTerminalSendObservation(
                CraftResourceResultSendStatus.IN_FLIGHT,
                CraftResourceResultDeliveryStatus.UNKNOWN,
                false,
                true
        );
    }

    FabricChatClefCraftResourceTerminalSendObservation sent() {
        return new FabricChatClefCraftResourceTerminalSendObservation(
                CraftResourceResultSendStatus.SENT,
                CraftResourceResultDeliveryStatus.DELIVERED,
                true,
                false
        );
    }

    private static FabricChatClefCraftResourceTerminalSendObservation failed(
            CraftResourceResultSendStatus status
    ) {
        return new FabricChatClefCraftResourceTerminalSendObservation(
                status,
                CraftResourceResultDeliveryStatus.NOT_DELIVERED,
                false,
                false
        );
    }

    private static FabricChatClefCraftResourceTerminalSendObservation unknown() {
        return new FabricChatClefCraftResourceTerminalSendObservation(
                CraftResourceResultSendStatus.UNKNOWN,
                CraftResourceResultDeliveryStatus.UNKNOWN,
                false,
                false
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
