package lavi.minecraft.fabric.chatclef.bridge.command;

import lavi.minecraft.command.result.instant.InstantCommandResult;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FabricChatClefInstantDiagnosticsTest {
    @Test void productionFormatterAndSinkPreserveDecisionValuesWithoutPrivateListFields() {
        PrintStream previous = System.out;
        var bytes = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));
            new FabricChatClefOrdinaryCommandDispatchDiagnostics(new FabricChatClefBridgeDiagnostics()).instantResultObserved(context(),
                    new InstantCommandResult("overlay off", "overlay", "completed", "SETTING_APPLIED",
                            Map.of("enabled", false, "private", "not-for-logs")));
        } finally { System.setOut(previous); }
        String line = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(line.contains("[LAVI Fabric ChatClef Bridge]"));
        assertTrue(line.contains("request=req-instant-log"));
        assertTrue(line.contains("command=overlay outcome=completed reason=SETTING_APPLIED values=enabled=false"));
        assertFalse(line.contains("not-for-logs"));
        assertEquals(1, line.lines().count());
    }

    @Test void productionSinkFailureCannotEscapeIntoCommandExecution() {
        PrintStream previous = System.out;
        try {
            System.setOut(new PrintStream(new ByteArrayOutputStream()) {
                @Override public void println(String line) { throw new IllegalStateException("sink-unavailable"); }
            });
            var diagnostics = new FabricChatClefOrdinaryCommandDispatchDiagnostics(new FabricChatClefBridgeDiagnostics());
            assertDoesNotThrow(() -> diagnostics.instantResultObserved(context(),
                    new InstantCommandResult("overlay off", "overlay", "completed", "SETTING_APPLIED", Map.of("enabled", false))));
        } finally { System.setOut(previous); }
    }

    private FabricChatClefCommandContext context() {
        var request = new FabricChatClefCommandRequest();
        request.requestId = "req-instant-log";
        request.command = "overlay off";
        request.source = "test";
        return new FabricChatClefCommandContext(request, "corr-instant-log", "session-instant-log", 1L);
    }
}
