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
    @Test void offFailureAndNoOutcomeRetirementReuseCapturedCoreValuesWithoutFreshContextGettersUnder2048Bytes() throws Exception {
        var registryField = ChatClefDiagnostics.class.getDeclaredField("COMMAND_CONTEXTS"); registryField.setAccessible(true);
        var registry = (DiagnosticCommandContextRegistry) registryField.get(null);
        var providerField = DiagnosticCommandContextRegistry.class.getDeclaredField("provider"); providerField.setAccessible(true);
        var originalProvider = (DiagnosticCommandContextProvider) providerField.get(registry);
        var contextReads = new java.util.concurrent.atomic.AtomicInteger();
        try (var client = HeadlessMinecraftClientSession.outOfGame()) {
            ChatClefDiagnostics.setBoundaryEnabled(false); ChatClefDiagnostics.resetDiagnosticSessionForTests();
            ChatClefDiagnostics.registerCommandContextProvider(() -> {
                contextReads.incrementAndGet();
                return DiagnosticCommandContextSnapshot.active("r".repeat(128), "c".repeat(128), "s".repeat(128), 1,
                        "private Steve 00000000-0000-0000-0000-000000000001", "microphone_final");
            });
            PrintStream previous = System.out; ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capture);
                for (boolean outcome : List.of(true, false)) {
                    bytes.reset(); FindLog log = new FindLog("o".repeat(128));
                    log.event(outcome ? "PARENT_OUTCOME" : "PARENT_RETIRED",
                            "targetId", "minecraft:villager", "kind", "entity", "mode", "report",
                            "findResult", outcome ? "TIMEOUT" : "UNKNOWN", "satisfied", false,
                            "profileAssigned", outcome, "reason", outcome ? "discovery_deadline_exhausted" : "owned_cleanup_failed",
                            "elapsedParentNanos", 180_000_000_001L, "elapsedApproachNanos", 0L,
                            "parentLimitNanos", 300_000_000_000L, "discoveryLimitNanos", 180_000_000_000L,
                            "observationLimitNanos", 5_000_000_000L, "approachLimitNanos", 120_000_000_000L,
                            "visited", 4096, "matched", 1, "scanStarts", 3, "routeStarts", 2,
                            "localVisitLimit", 4096, "cumulativeVisitLimit", 262144, "scanStartLimit", 181,
                            "routeStartLimit", 8, "sampledTravel", 144.5, "displacement", 90.0,
                            "travelLimit", 1024.0, "displacementLimit", 512.0, "noProgressNanos", 25_000_000_000L,
                            "noProgressLimitNanos", 30_000_000_000L, "scopeComplete", false,
                            "explorationQuiet", outcome, "inputQuiet", true, "discoveryComplete", false,
                            "readFailureType", "NONE");
                    if (outcome) log.terminalFailure("TIMEOUT:discovery_deadline_exhausted", 4096, 1, false);
                    log.close("test_retired");
                    List<String> lines = bytes.toString(StandardCharsets.UTF_8).lines().filter(line -> line.contains("LAVI FIND")).toList();
                    assertEquals(outcome ? 2 : 1, lines.size());
                    for (String line : lines) {
                        assertTrue(line.getBytes(StandardCharsets.UTF_8).length <= 2048, line);
                        assertTrue(line.contains("diagnosticCaptureStatus=complete"), line);
                        for (String field : List.of("operationId=" + "o".repeat(128), "targetId=minecraft:villager",
                                "elapsedParentNanos=180000000001", "elapsedApproachNanos=0", "parentLimitNanos=300000000000",
                                "discoveryLimitNanos=180000000000", "scanStarts=3", "routeStarts=2", "visited=4096", "matched=1",
                                "sampledTravel=144.5", "displacement=90.0", "noProgressNanos=25000000000", "readFailureType=NONE",
                                "profileAssigned=" + outcome, "findResult=" + (outcome ? "TIMEOUT" : "UNKNOWN"))) assertTrue(line.contains(field), line);
                        assertFalse(line.contains("Steve"), line); assertFalse(line.contains("00000000-0000"), line);
                    }
                }
                assertEquals(0, contextReads.get());
            } finally { System.setOut(previous); }
        } finally { ChatClefDiagnostics.registerCommandContextProvider(originalProvider); }
    }
    @Test void bothMovementRolesAndFixedDiscoveryBoundariesSurviveOrdinarySaturationWithinOne128SlotTrace() throws Exception {
        try (var client = HeadlessMinecraftClientSession.outOfGame()) {
            ChatClefDiagnostics.setBoundaryEnabled(false); ChatClefDiagnostics.resetDiagnosticSessionForTests();
            ChatClefDiagnostics.setBoundaryEnabled(true);
            PrintStream previous = System.out; ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capture);
                for (int index = 0; index <= lavi.minecraft.diagnostics.session.admission.DiagnosticSessionLimits.ORDINARY_CEILING; index++)
                    ChatClefDiagnostics.logBoundedBoundaryWithDispatchResult("FIND_PRESSURE_DETAIL", "pressure", null, 2048,
                            new Object[]{"detailIndex", index}, new Object[0]);
                bytes.reset(); FindLog log = new FindLog("full-phase-operation");
                log.event("ROOT_LIFETIME_BINDING_RESULT", "reason", "accepted_first_binding");
                for (String event : List.of("PARENT_STARTED", "CANDIDATE_LATCHED", "EXPLORATION_STARTED", "EXPLORATION_SUSPENDED",
                        "EXPLORATION_RESUMED", "EXPLORATION_STOP_REQUESTED", "EXPLORATION_CLEANUP_RESULT", "HANDOFF_VALIDATED",
                        "EXPLORATION_ROUTE_STARTED", "EXPLORATION_WAYPOINT_SELECTED", "EXPLORATION_PLAN_STARTED",
                        "EXPLORATION_STEP_COMPLETED", "EXPLORATION_WAYPOINT_REACHED", "EXPLORATION_MOVEMENT_SUSPENDED",
                        "APPROACH_STARTED", "APPROACH_PLAN_STARTED", "APPROACH_STEP_COMPLETED", "APPROACH_MOVEMENT_RESULT")) log.event(event);
                for (String role : List.of("INITIAL_OBSERVATION", "DISCOVERY_LOOP_OBSERVATION")) {
                    log.eventInPhase(role, "DISCOVERY_SCAN_STARTED"); log.eventInPhase(role, "ENTITY_QUERY_STARTED");
                    for (String result : List.of("MISS", "MATCH")) {
                        log.eventInPhase(role, "DISCOVERY_SCAN_COMPLETED", "reason", "complete_loaded_scope", "scopeResult", result);
                        log.eventInPhase(role, "ENTITY_QUERY_COMPLETED", "reason", "complete_loaded_scope", "scopeResult", result);
                        log.eventInPhase(role, "ENTITY_QUERY_COMPLETED", "reason", "complete_loaded_scope", "scopeResult", result,
                                "scanIndex", 99, "visited", 4096); // Numeric metadata cannot consume another slot.
                    }
                }
                for (String role : List.of("INITIAL_CANDIDATE_REVALIDATION", "HANDOFF_REVALIDATION", "ARRIVAL_REVALIDATION"))
                    log.eventInPhase(role, "ENTITY_REVALIDATION_RESULT", "reason", "selected_entity_revalidated");
                for (String role : List.of("EXPLORATION_MOVEMENT", "APPROACH_MOVEMENT")) {
                    FindLog movement = log.inPhase(role);
                    for (String event : List.of("APPROACH_CAPTURE_COMPLETE", "APPROACH_PLAN_RESULT", "APPROACH_PLAN_ADMITTED"))
                        if (event.equals("APPROACH_PLAN_RESULT")) movement.event(event, "nativeResult", "SUCCESS_TO_GOAL"); else movement.event(event);
                    for (String key : List.of("MOVE_FORWARD", "MOVE_BACK", "MOVE_LEFT", "MOVE_RIGHT", "SPRINT")) {
                        movement.event("INPUT_CLAIM", "input", key, "claimed", true);
                        movement.event("INPUT_RELEASE", "input", key, "released", true, "leaseStillOwned", false, "supersededWriterPreserved", false);
                    }
                    for (String kind : List.of("MovementTraverse", "MovementDiagonal"))
                        movement.event(role.equals("EXPLORATION_MOVEMENT") ? "EXPLORATION_STEP_STARTED" : "APPROACH_STEP_STARTED", "nativeKind", kind);
                }
                log.inPhase("EXPLORATION_MOVEMENT").event("APPROACH_PLAN_RESULT", "nativeResult", "SUCCESS_SEGMENT");
                log.inPhase("EXPLORATION_MOVEMENT").event("INPUT_RELEASE", "input", "MOVE_LEFT", "released", false,
                        "leaseStillOwned", true, "supersededWriterPreserved", false);
                log.inPhase("EXPLORATION_MOVEMENT").event("INPUT_RELEASE", "input", "MOVE_LEFT", "released", false,
                        "leaseStillOwned", false, "supersededWriterPreserved", true);
                log.event("PHASE_CHANGED", "phaseBefore", "INITIAL_OBSERVATION", "phaseAfter", "EXPLORING");
                log.event("PHASE_CHANGED", "phaseBefore", "EXPLORING", "phaseAfter", "STOPPING_EXPLORATION");
                log.event("PHASE_CHANGED", "phaseBefore", "STOPPING_EXPLORATION", "phaseAfter", "APPROACHING");
                log.event("PHASE_CHANGED", "phaseBefore", "APPROACHING", "phaseAfter", "TERMINAL");
                for (String reason : List.of("stationary", "sampledmotion")) log.event("DISCOVERY_MOVEMENT_SAMPLED", "reason", reason);
                for (String reason : List.of("active_exploration", "inactive")) log.event("DISCOVERY_PROGRESS_CLOCK_CHANGED", "reason", reason);
                log.event("DISCOVERY_WAYPOINT_PROGRESS", "reason", "full_waypoint_arrival");
                log.event("PARENT_OUTCOME", "findResult", "FOUND_AND_IN_SAFE_RANGE", "reason", "same_target_safe_range_and_owned_cleanup");
                log.close("matching_task_finished");
                List<String> lines = bytes.toString(StandardCharsets.UTF_8).lines().filter(line -> line.contains("event=FIND_")).toList();
                assertEquals(79, lines.size()); assertTrue(lines.size() <= FindTraceReservation.MAX_EVENTS);
                for (String line : lines) {
                    assertTrue(line.contains("diagnosticCaptureStatus=complete"), line);
                    assertTrue(line.contains("findTraceReservation=RESERVED_OUTPUT_UNVERIFIED"), line);
                    assertTrue(line.getBytes(StandardCharsets.UTF_8).length <= 2048, line);
                }
                assertEquals(22, lines.stream().filter(line -> line.contains("event=FIND_INPUT_CLAIM ") || line.contains("event=FIND_INPUT_RELEASE ")).count());
                assertEquals(3, lines.stream().filter(line -> line.contains("event=FIND_APPROACH_PLAN_RESULT ")).count());
                assertTrue(lines.stream().anyMatch(line -> line.contains("nativeResult=SUCCESS_SEGMENT") && line.contains("phaseRole=EXPLORATION_MOVEMENT")));
                assertTrue(lines.stream().anyMatch(line -> line.contains("released=false") && line.contains("leaseStillOwned=true") && line.contains("supersededWriterPreserved=false")));
                assertTrue(lines.stream().anyMatch(line -> line.contains("released=false") && line.contains("leaseStillOwned=false") && line.contains("supersededWriterPreserved=true")));
                assertEquals(1, lines.stream().filter(line -> line.contains("event=FIND_PARENT_OUTCOME ")).count());
                assertEquals(1, lines.stream().filter(line -> line.contains("event=FIND_TRACE_TERMINAL ")).count());
                assertTrue(lines.stream().anyMatch(line -> line.contains("requiredBoundarySignature=FindExplorationTask|ROOT_LIFETIME_BINDING_RESULT||accepted_first_binding")));
                assertEquals(128, ChatClefDiagnostics.diagnosticSessionSnapshot().family(
                        lavi.minecraft.diagnostics.session.admission.DiagnosticEventFamily.FIND_RESERVED_BOUNDARY).admittedSlots());
            } finally { System.setOut(previous); ChatClefDiagnostics.setBoundaryEnabled(false); }
        }
    }
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
    @Test void secondUnreservedTraceReportsExclusionThroughOrdinarySharedGateWithoutRefund() throws Exception {
        try (var client = HeadlessMinecraftClientSession.outOfGame()) {
            ChatClefDiagnostics.setBoundaryEnabled(false); ChatClefDiagnostics.resetDiagnosticSessionForTests();
            ChatClefDiagnostics.setBoundaryEnabled(true);
            PrintStream previous = System.out; ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capture); List<FindLog> logs = new ArrayList<>();
                for (int index = 0; index < 1; index++) logs.add(new FindLog("reserved-" + index));
                logs.get(0).close("test_retired");
                FindLog fifth = new FindLog("second-operation"); fifth.event("STARTED"); fifth.event("RESUMED");
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
