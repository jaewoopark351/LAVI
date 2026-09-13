package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.gotoresult;

import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

//20260913_kpopmodder: Isolate observation failures from the existing one-time terminal factory.
public final class GotoTerminalDiagnostics {
    private final GotoTerminalDiagnosticProjector projector = new GotoTerminalDiagnosticProjector();
    private final GotoTerminalDiagnosticFormatter formatter = new GotoTerminalDiagnosticFormatter();
    private final Consumer<String> sink;

    public GotoTerminalDiagnostics() {
        this(new FabricChatClefBridgeDiagnostics()::info);
    }

    public GotoTerminalDiagnostics(Consumer<String> sink) {
        this.sink = Objects.requireNonNull(sink, "diagnostic sink");
    }

    public FabricChatClefCommandResultPayload observeResult(
            FabricChatClefCommandExecution execution, FabricChatClefCommandResultPayload result
    ) {
        try {
            Map<String, Object> fields = projector.project(execution, result);
            if (!fields.isEmpty()) sink.accept(formatter.format(fields));
        } catch (RuntimeException | LinkageError ignored) {
            // This is a diagnostic-only boundary. Never replace, retry or fail the result.
        }
        return result;
    }
}
