package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics;

import lavi.minecraft.diagnostics.command.DiagnosticCommandContextProvider;
import lavi.minecraft.diagnostics.command.DiagnosticCommandContextSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;

import java.util.Optional;

//20260805_kpopmodder: Expose active Fabric command context to diagnostics as read-only log context.
public final class FabricChatClefDiagnosticCommandContextProvider implements DiagnosticCommandContextProvider {
    private final FabricChatClefCommandQueue commandQueue;

    public FabricChatClefDiagnosticCommandContextProvider(FabricChatClefCommandQueue commandQueue) {
        this.commandQueue = commandQueue;
    }

    @Override
    public DiagnosticCommandContextSnapshot snapshot() {
        if (commandQueue == null) {
            return DiagnosticCommandContextSnapshot.unavailable("queue_unavailable");
        }
        Optional<FabricChatClefCommandContext> activeContext = commandQueue.activeContext();
        if (activeContext.isEmpty()) {
            return DiagnosticCommandContextSnapshot.unavailable("no_active_command");
        }
        FabricChatClefCommandContext context = activeContext.get();
        FabricChatClefCommandRequest request = context.request();
        return DiagnosticCommandContextSnapshot.active(
                context.requestId(),
                context.correlationId(),
                context.sessionId(),
                context.connectionGeneration(),
                request == null ? "" : request.command,
                request == null ? "" : request.source
        );
    }
}
