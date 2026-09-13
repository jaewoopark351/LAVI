//#if MC == 12001
package lavi.minecraft.task.movement.gotopreflight.diagnostics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260913_kpopmodder: Verify independent sinks and frozen correlation without any Minecraft action.
class GotoPreparationDiagnosticsTest {
    @Test
    void failedLegacySinkCannotHidePhaseAndTerminalPhysicalObservations() {
        List<String> events = new ArrayList<>();
        List<Object[]> records = new ArrayList<>();
        GotoPreparationDiagnostics diagnostics = new GotoPreparationDiagnostics("22ebb1fc",
                message -> { throw new LinkageError("legacy unavailable"); },
                (event, fields) -> { events.add(event); records.add(fields); },
                () -> new Object[]{"commandRequestId", "original-request"});
        assertDoesNotThrow(() -> {
            diagnostics.log("PHASE from=NATIVE to=ARRIVAL_CLEANUP player=500,90,-928");
            diagnostics.log("ARRIVED target=500,90,-928");
        });
        assertEquals(List.of("GOTO_PREPARATION_PHASE", "GOTO_PREPARATION_ARRIVED"), events);
        assertEquals("22ebb1fc", field(records.get(1), "operationId"));
        assertEquals("original-request", field(records.get(1), "commandRequestId"));
    }

    @Test
    void physicalSinkFailureCannotStopLegacyOutputOrEscapeToTask() {
        List<String> legacy = new ArrayList<>();
        GotoPreparationDiagnostics diagnostics = new GotoPreparationDiagnostics("root-id", legacy::add,
                (event, fields) -> { throw new IllegalStateException("physical unavailable"); },
                () -> new Object[0]);
        assertDoesNotThrow(() -> {
            diagnostics.decision("AIR_COLUMN_UNAVAILABLE", "player=original");
            diagnostics.log("ARRIVED target=original");
        });
        assertEquals(2, legacy.size());
        assertEquals("[LAVI GOTO V3.1] ARRIVED target=original operation=root-id", legacy.get(1));
    }

    @Test
    void firstContextRemainsBoundEvenWhenGlobalProviderChangesBeforeTerminal() {
        AtomicInteger captures = new AtomicInteger();
        Object[] context = {"commandRequestId", "original", "commandSessionId", "session-1"};
        List<Object[]> records = new ArrayList<>();
        GotoPreparationDiagnostics diagnostics = new GotoPreparationDiagnostics("owner", message -> { },
                (event, fields) -> records.add(fields), () -> { captures.incrementAndGet(); return context; });
        diagnostics.log("NATIVE_FIRST start=original target=original");
        context[1] = "replacement";
        diagnostics.log("ARRIVED target=original");
        diagnostics.log("ARRIVED target=duplicate");
        assertEquals(1, captures.get());
        assertEquals(2, records.size());
        assertEquals("original", field(records.get(1), "commandRequestId"));
    }

    @Test
    void lateRequiredNativeReasonAndTerminalSurviveBothDetailCaps() {
        List<String> legacy = new ArrayList<>();
        List<String> events = new ArrayList<>();
        GotoPreparationDiagnostics diagnostics = new GotoPreparationDiagnostics("owner", legacy::add,
                (event, fields) -> events.add(event), () -> new Object[0]);
        for (int index = 0; index < 100; index++) diagnostics.decision("detail_" + index, "unchanged");
        diagnostics.decision("MATERIALS_SUFFICIENT_NATIVE_CONTINUES", "held=34");
        diagnostics.log("ARRIVED target=original");
        assertEquals(34, legacy.size()); // 32 old details + one old cap + unchanged arrival message.
        assertEquals(35, events.size()); // 32 details + separate summary + reserved native reason + terminal.
        assertTrue(events.get(33).equals("GOTO_PREPARATION_NATIVE_DECISION"));
        assertEquals("GOTO_PREPARATION_ARRIVED", events.get(34));
    }

    @Test
    void failedContextCaptureRecordsEvidenceGapWithoutSuppressingOwnerTerminal() {
        List<Object[]> records = new ArrayList<>();
        GotoPreparationDiagnostics diagnostics = new GotoPreparationDiagnostics("owner", message -> { },
                (event, fields) -> records.add(fields), () -> { throw new LinkageError("context unavailable"); });
        assertDoesNotThrow(() -> diagnostics.log("FAILED reason=ARRIVAL_LOST target=original"));
        assertEquals(false, field(records.get(0), "commandContextAvailable"));
        assertEquals("LinkageError", field(records.get(0), "commandContextError"));
    }

    private static Object field(Object[] fields, String name) {
        for (int index = 0; index < fields.length; index += 2) {
            if (name.equals(fields[index])) return fields[index + 1];
        }
        throw new AssertionError("Missing field: " + name);
    }
}
//#endif
