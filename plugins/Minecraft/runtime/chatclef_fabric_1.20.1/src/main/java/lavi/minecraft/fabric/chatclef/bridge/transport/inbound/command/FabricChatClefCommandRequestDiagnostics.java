package lavi.minecraft.fabric.chatclef.bridge.transport.inbound.command;

//20260905_kpopmodder: Render bounded ordinary command_request queue diagnostics.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

public final class FabricChatClefCommandRequestDiagnostics {
    private final FabricChatClefBridgeDiagnostics diagnostics;

    public FabricChatClefCommandRequestDiagnostics(FabricChatClefBridgeDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    public void rejectedByQueue(
            FabricChatClefCommandRequest request,
            String activeRequest,
            long generation
    ) {
        diagnostics.warn(
                "rejected command_request rejected_by=java_command_queue request="
                        + request.requestId
                        + " source="
                        + request.source
                        + " active_request="
                        + activeRequest
                        + " generation="
                        + generation
        );
    }

    public void queued(FabricChatClefCommandRequest request, long generation) {
        diagnostics.info(
                "queued command request="
                        + request.requestId
                        + " source="
                        + request.source
                        + " command="
                        + request.command
                        + " generation="
                        + generation
        );
    }
}
