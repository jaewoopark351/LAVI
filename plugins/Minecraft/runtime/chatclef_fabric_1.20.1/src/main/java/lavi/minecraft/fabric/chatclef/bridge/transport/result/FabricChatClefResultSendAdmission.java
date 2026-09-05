package lavi.minecraft.fabric.chatclef.bridge.transport.result;

//20260905_kpopmodder: Own socket and generation admission for command-result sends.

import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendStatus;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;

import java.net.http.WebSocket;

public final class FabricChatClefResultSendAdmission {
    private final FabricChatClefResultSendDiagnostics diagnostics;

    public FabricChatClefResultSendAdmission(FabricChatClefResultSendDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    public FabricChatClefResultSendAdmissionDecision evaluate(
            WebSocket socket,
            long generation,
            long activeGeneration
    ) {
        if (socket != null && generation == activeGeneration) {
            return FabricChatClefResultSendAdmissionDecision.admitted();
        }
        diagnostics.inactiveGeneration(generation, activeGeneration);
        if (socket == null) {
            return rejected(
                    FabricChatClefCommandResultSendStatus.NO_SOCKET,
                    "socket is not connected"
            );
        }
        return rejected(
                FabricChatClefCommandResultSendStatus.GENERATION_MISMATCH,
                "generation=" + generation + " active_generation=" + activeGeneration
        );
    }

    private static FabricChatClefResultSendAdmissionDecision rejected(
            FabricChatClefCommandResultSendStatus status,
            String message
    ) {
        return FabricChatClefResultSendAdmissionDecision.rejected(
                FabricChatClefCommandResultSendSubmission.failed(
                        FabricChatClefCommandResultSendOutcome.failed(status, message)
                )
        );
    }
}
