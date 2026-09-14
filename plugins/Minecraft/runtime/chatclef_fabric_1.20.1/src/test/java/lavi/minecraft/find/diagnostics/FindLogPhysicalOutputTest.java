//#if MC == 12001
package lavi.minecraft.find.diagnostics;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.command.DiagnosticCommandContextProvider;
import lavi.minecraft.diagnostics.command.DiagnosticCommandContextRegistry;
import lavi.minecraft.diagnostics.command.DiagnosticCommandContextSnapshot;
import lavi.minecraft.testsupport.HeadlessMinecraftClientSession;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Capture real shared-mode admission, formatter and stdout; do not claim file or game delivery.
class FindLogPhysicalOutputTest {
    @Test void registeredPlayerCommandContextRetainsCorrelationWithoutRawNameInRealFindOutput() throws Exception {
        var registryField = ChatClefDiagnostics.class.getDeclaredField("COMMAND_CONTEXTS"); registryField.setAccessible(true);
        var registry = (DiagnosticCommandContextRegistry) registryField.get(null);
        var providerField = DiagnosticCommandContextRegistry.class.getDeclaredField("provider"); providerField.setAccessible(true);
        var originalProvider = (DiagnosticCommandContextProvider) providerField.get(registry);
        try (var client = HeadlessMinecraftClientSession.outOfGame()) {
            ChatClefDiagnostics.setBoundaryEnabled(false); ChatClefDiagnostics.resetDiagnosticSessionForTests();
            ChatClefDiagnostics.registerCommandContextProvider(() -> DiagnosticCommandContextSnapshot.active(
                    "find-private-request", "find-private-correlation", "find-private-session", 17,
                    "find player Steve report", "microphone_final"));
            ChatClefDiagnostics.setBoundaryEnabled(true);
            PrintStream previous = System.out; ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capture); FindLog log = new FindLog("private-context-operation");
                log.event("STARTED", "targetKind", "player", "mode", "report");
                log.event("REPORT_TERMINAL", "findResult", "NOT_OBSERVED_IN_LOADED_SCOPE", "satisfied", false,
                        "visited", 2, "matched", 0, "scopeComplete", true);
                log.close("matching_task_finished");
                String output = bytes.toString(StandardCharsets.UTF_8);
                assertTrue(output.contains("event=FIND_STARTED"), output);
                assertTrue(output.contains("event=FIND_REPORT_TERMINAL"), output);
                assertTrue(output.contains("event=FIND_TRACE_TERMINAL"), output);
                assertTrue(output.contains("commandRequestId=find-private-request"), output);
                assertTrue(output.contains("commandCorrelationId=find-private-correlation"), output);
                assertTrue(output.contains("commandSessionId=find-private-session"), output);
                assertTrue(output.contains("commandConnectionGeneration=17"), output);
                assertTrue(output.contains("commandSource=microphone_final"), output);
                assertFalse(output.contains("Steve"), output); assertFalse(output.contains("commandText"), output);
                assertFalse(output.contains("find player"), output);
            } finally { System.setOut(previous); ChatClefDiagnostics.setBoundaryEnabled(false); }
        } finally { ChatClefDiagnostics.registerCommandContextProvider(originalProvider); }
        assertSame(originalProvider, providerField.get(registry));
    }
    @Test void ordinaryDetailCannotConsumeOutcomeAndAuthoritativeTraceTerminal() throws Exception {
        try (var client = HeadlessMinecraftClientSession.outOfGame()) {
            ChatClefDiagnostics.setBoundaryEnabled(false); ChatClefDiagnostics.resetDiagnosticSessionForTests();
            ChatClefDiagnostics.setBoundaryEnabled(true);
            PrintStream previous = System.out; ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capture);
                FindLog log = new FindLog("physical-find-operation");
                for (int detail = 0; detail < 100; detail++) log.event("DETAIL", "reason", "detail_" + detail);
                log.event("STARTED", "targetKind", "entity", "mode", "report");
                log.event("REPORT_TERMINAL", "findResult", "FOUND_AND_REPORTED", "satisfied", true, "visited", 3, "matched", 1);
                log.close("matching_task_finished"); log.close("duplicate_should_not_emit");
                String output = bytes.toString(StandardCharsets.UTF_8);
                assertTrue(output.contains("FIND_REPORT_TERMINAL"), output);
                assertTrue(output.contains("FIND_TRACE_TERMINAL"), output);
                assertTrue(output.contains("physical-find-operation"), output);
                assertFalse(output.contains("duplicate_should_not_emit"), output);
            } finally { System.setOut(previous); ChatClefDiagnostics.setBoundaryEnabled(false); }
        }
    }
    @Test void independentLeaseKeysAndBothNativeKindsAreDistinctWithinDeclaredProtectedScope() {
        List<Object[]> output = new ArrayList<>();
        FindLog log = new FindLog("signature-operation", (event, fields) -> output.add(fields));
        for (String event : List.of("STARTED", "RESUMED", "SUSPENDED", "OBSERVATION_COMPLETED", "OBSERVATION_TERMINAL",
                "APPROACH_RESUMED", "APPROACH_SUSPENDED", "APPROACH_CAPTURE_COMPLETE", "APPROACH_PLAN_RESULT",
                "APPROACH_PLAN_ADMITTED", "APPROACH_STARTED", "APPROACH_PLAN_STARTED", "APPROACH_STEP_COMPLETED")) log.event(event);
        for (String key : List.of("MOVE_FORWARD", "MOVE_BACK", "MOVE_LEFT", "MOVE_RIGHT", "SPRINT")) {
            log.event("INPUT_CLAIM", "input", key, "claimed", true);
            log.event("INPUT_RELEASE", "input", key, "released", true);
        }
        log.event("INPUT_LEASE_LOST", "input", "MOVE_LEFT");
        log.event("INPUT_RELEASE", "input", "MOVE_LEFT", "released", false);
        log.event("APPROACH_STEP_STARTED", "nativeKind", "MovementTraverse");
        log.event("APPROACH_STEP_STARTED", "nativeKind", "MovementDiagonal");
        log.event("APPROACH_TERMINAL", "findResult", "UNREACHABLE", "reason", "selected_path_invalid");
        log.close("matching_task_finished");
        assertEquals(30, output.size()); assertTrue(output.size() <= FindTraceReservation.MAX_EVENTS);
        var signatures = output.stream().map(fields -> {
            for (int index = 0; index < fields.length; index += 2) if (fields[index].equals("requiredBoundarySignature")) return fields[index + 1];
            return "missing";
        }).toList();
        assertEquals(signatures.size(), new java.util.HashSet<>(signatures).size());
    }
    @Test void offStillProducesBoundedNormalFailureOutput() throws Exception {
        try (var client = HeadlessMinecraftClientSession.outOfGame()) {
            ChatClefDiagnostics.setBoundaryEnabled(false);
            PrintStream previous = System.out; ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capture); FindLog log = new FindLog("off-find-operation");
                log.terminalFailure("OBSERVATION_BOUNDS_EXHAUSTED", 4096, 1, false); log.close("matching_task_finished");
                String output = bytes.toString(StandardCharsets.UTF_8);
                assertTrue(output.contains("OBSERVATION_BOUNDS_EXHAUSTED")); assertTrue(output.contains("visited=4096"));
                assertTrue(output.contains("off-find-operation")); assertTrue(output.contains("UNRESERVED_OR_DISABLED_PARTIAL"));
            } finally { System.setOut(previous); }
        }
    }
    @Test void fifthUnreservedTraceReportsExclusionThroughOrdinarySharedGate() throws Exception {
        try (var client = HeadlessMinecraftClientSession.outOfGame()) {
            ChatClefDiagnostics.setBoundaryEnabled(false); ChatClefDiagnostics.resetDiagnosticSessionForTests();
            ChatClefDiagnostics.setBoundaryEnabled(true);
            PrintStream previous = System.out; ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capture); List<FindLog> logs = new ArrayList<>();
                for (int index = 0; index < 4; index++) logs.add(new FindLog("reserved-" + index));
                FindLog fifth = new FindLog("fifth-operation"); fifth.event("STARTED"); fifth.event("RESUMED");
                String output = bytes.toString(StandardCharsets.UTF_8);
                assertTrue(output.contains("FIND_TRACE_EXCLUDED"), output);
                assertTrue(output.contains("RESERVATION_NOT_GRANTED_REASON_UNAVAILABLE"), output);
                assertEquals(1, output.lines().filter(line -> line.contains("event=FIND_TRACE_EXCLUDED")).count());
                assertEquals(128, ChatClefDiagnostics.diagnosticSessionSnapshot().family(
                        lavi.minecraft.diagnostics.session.admission.DiagnosticEventFamily.FIND_RESERVED_BOUNDARY).admittedSlots());
                fifth.close("test_retired"); logs.forEach(log -> log.close("test_retired"));
            } finally { System.setOut(previous); ChatClefDiagnostics.setBoundaryEnabled(false); }
        }
    }
    @Test void enablingDiagnosticsMidOperationDoesNotRetroactivelyReserveCompleteTrace() throws Exception {
        try (var client = HeadlessMinecraftClientSession.outOfGame()) {
            ChatClefDiagnostics.setBoundaryEnabled(false); ChatClefDiagnostics.resetDiagnosticSessionForTests();
            FindLog log = new FindLog("mid-activation-operation"); log.event("STARTED");
            PrintStream previous = System.out; ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capture); ChatClefDiagnostics.setBoundaryEnabled(true);
                log.event("RESUMED"); log.event("REPORT_TERMINAL", "findResult", "FOUND_AND_REPORTED"); log.close("test_retired");
                String output = bytes.toString(StandardCharsets.UTF_8);
                assertTrue(output.contains("BOUNDARY_BECAME_ELIGIBLE_AFTER_ADMISSION_TRACE_REMAINS_PARTIAL"), output);
                assertFalse(output.contains("event=FIND_REPORT_TERMINAL"), output);
                assertEquals(0, ChatClefDiagnostics.diagnosticSessionSnapshot().family(
                        lavi.minecraft.diagnostics.session.admission.DiagnosticEventFamily.FIND_RESERVED_BOUNDARY).admittedSlots());
            } finally { System.setOut(previous); ChatClefDiagnostics.setBoundaryEnabled(false); }
        }
    }
}
//#endif
