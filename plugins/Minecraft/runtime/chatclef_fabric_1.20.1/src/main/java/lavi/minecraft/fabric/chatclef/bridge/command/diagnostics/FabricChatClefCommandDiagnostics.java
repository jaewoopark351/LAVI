package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.FabricChatClefCraftResourceLifecycleDiagnosticsRouter;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationReader;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.FabricChatClefCraftResourceTerminalLifecycleObserver;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.payload.FabricChatClefCommandDiagnosticDetailsSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.payload.FabricChatClefCommandDiagnosticLogPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

import java.util.Map;

//20260803_kpopmodder: Added diagnostic logging to prove Fabric ChatClef command lifecycle boundaries.
public final class FabricChatClefCommandDiagnostics {
    private final FabricChatClefBridgeDiagnostics diagnostics;

    public FabricChatClefCommandDiagnostics(FabricChatClefBridgeDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    public void info(String event, FabricChatClefCommandExecution execution) {
        diagnostics.info(
                "command lifecycle "
                        + FabricChatClefCommandDiagnosticLogPayload.execution(event, execution)
        );
        FabricChatClefCraftResourceLifecycleDiagnosticsRouter.observe(event, execution, null);
    }

    public void warn(String event, FabricChatClefCommandExecution execution) {
        diagnostics.warn(
                "command lifecycle "
                        + FabricChatClefCommandDiagnosticLogPayload.execution(event, execution)
        );
        FabricChatClefCraftResourceLifecycleDiagnosticsRouter.observe(event, execution, null);
    }

    public void info(
            String event,
            FabricChatClefCommandExecution execution,
            FabricChatClefCommandDiagnosticDetailsPayload details
    ) {
        FabricChatClefCommandDiagnosticDetailsSnapshot detailsSnapshot =
                FabricChatClefCommandDiagnosticDetailsSnapshot.capture(details);
        diagnostics.info(
                "command lifecycle "
                        + FabricChatClefCommandDiagnosticLogPayload.execution(
                                event,
                                execution,
                                detailsSnapshot
                        )
        );
        FabricChatClefCraftResourceLifecycleDiagnosticsRouter.observe(
                event,
                execution,
                detailsSnapshot.toMap()
        );
    }

    public void warn(
            String event,
            FabricChatClefCommandExecution execution,
            FabricChatClefCommandDiagnosticDetailsPayload details
    ) {
        FabricChatClefCommandDiagnosticDetailsSnapshot detailsSnapshot =
                FabricChatClefCommandDiagnosticDetailsSnapshot.capture(details);
        diagnostics.warn(
                "command lifecycle "
                        + FabricChatClefCommandDiagnosticLogPayload.execution(
                                event,
                                execution,
                                detailsSnapshot
                        )
        );
        FabricChatClefCraftResourceLifecycleDiagnosticsRouter.observe(
                event,
                execution,
                detailsSnapshot.toMap()
        );
    }

    public void contextInfo(
            String event,
            FabricChatClefCommandContext context,
            FabricChatClefCommandDiagnosticDetailsPayload details
    ) {
        FabricChatClefCommandDiagnosticDetailsSnapshot detailsSnapshot =
                FabricChatClefCommandDiagnosticDetailsSnapshot.capture(details);
        diagnostics.info(
                "command lifecycle "
                        + FabricChatClefCommandDiagnosticLogPayload.context(
                                event,
                                context,
                                detailsSnapshot
                        )
        );
        observeContextSafely(event, context, detailsSnapshot.toMap());
    }

    public void contextWarn(
            String event,
            FabricChatClefCommandContext context,
            FabricChatClefCommandDiagnosticDetailsPayload details
    ) {
        FabricChatClefCommandDiagnosticDetailsSnapshot detailsSnapshot =
                FabricChatClefCommandDiagnosticDetailsSnapshot.capture(details);
        diagnostics.warn(
                "command lifecycle "
                        + FabricChatClefCommandDiagnosticLogPayload.context(
                                event,
                                context,
                                detailsSnapshot
                        )
        );
        observeContextSafely(event, context, detailsSnapshot.toMap());
    }

    private static void observeContextSafely(
            String event,
            FabricChatClefCommandContext context,
            Map<String, Object> details
    ) {
        try {
            FabricChatClefCraftResourceTerminalLifecycleObserver.observeContext(
                    event,
                    context,
                    details
            );
        } catch (RuntimeException | LinkageError error) {
            FabricChatClefCraftResourceAssociationReader.observeProjectionFailure(
                    "COMMAND_CONTEXT_PROJECTION_" + String.valueOf(event),
                    error
            );
        }
    }
}
