//#if MC == 12001
package lavi.minecraft.find.diagnostics;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.command.DiagnosticCommandContextProvider;
import lavi.minecraft.diagnostics.command.DiagnosticCommandContextRegistry;
import lavi.minecraft.diagnostics.command.DiagnosticCommandContextSnapshot;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionLimits;
import lavi.minecraft.find.model.FindCandidate;
import lavi.minecraft.find.model.FindRequest;
import lavi.minecraft.find.observation.FindObservationOperation;
import lavi.minecraft.find.observation.FindObservationPort;
import lavi.minecraft.find.result.FindOutcome;
import lavi.minecraft.testsupport.HeadlessMinecraftClientSession;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Verify real diagnostic admission/formatting/stdout with deterministic owner snapshots.
// The fixture below verifies logging and operation non-interference, not production Tracker filtering or game delivery.
class FindEntityObservationLogTest {
    private static final String OPERATION = "entity-report-log-operation";
    private static final FindCandidate CANDIDATE = new FindCandidate(41, "digest", "private-stable-key", 8, 65, 2, 68);

    @Test void realSidecarSuccessAndReadFailurePreserveRequiredOutputWithMaxContextAndLongSamples() throws Exception {
        var registryField = ChatClefDiagnostics.class.getDeclaredField("COMMAND_CONTEXTS"); registryField.setAccessible(true);
        var registry = (DiagnosticCommandContextRegistry) registryField.get(null);
        var providerField = DiagnosticCommandContextRegistry.class.getDeclaredField("provider"); providerField.setAccessible(true);
        var originalProvider = (DiagnosticCommandContextProvider) providerField.get(registry);
        String requestId = "r".repeat(128), correlationId = "c".repeat(128), sessionId = "s".repeat(128);
        try (var client = HeadlessMinecraftClientSession.outOfGame()) {
            ChatClefDiagnostics.setBoundaryEnabled(false); ChatClefDiagnostics.resetDiagnosticSessionForTests();
            ChatClefDiagnostics.registerCommandContextProvider(() -> DiagnosticCommandContextSnapshot.active(
                    requestId, correlationId, sessionId, Long.MAX_VALUE, "private command text", "microphone_final"));
            ChatClefDiagnostics.setBoundaryEnabled(true);
            PrintStream previous = System.out; ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capture);
                for (boolean failedRead : List.of(false, true)) {
                    // A closed trace does not refund the session's one 128-slot FIND reservation.
                    startBoundaryTestSession();
                    bytes.reset();
                    FindEntityScanDiagnostics diagnostics = new FindEntityScanDiagnostics();
                    String longType = "examplemod:" + "x".repeat(117);
                    diagnostics.capture(41, longType, true, true, "matched", 6400.0);
                    diagnostics.capture(42, longType, true, false, "wrong_target_id", null);
                    diagnostics.capture(43, longType, true, false, "wrong_kind", null);
                    FindLog log = new FindLog("00000000-0000-0000-0000-000000000001");
                    try {
                        diagnostics.emit(log, failedRead ? 4 : 3, 1, !failedRead,
                                failedRead ? "entity_read_failed" : "complete_loaded_scope_candidate_selected", Long.MAX_VALUE,
                                CANDIDATE, failedRead ? "entity_liveness" : "NONE",
                                failedRead ? "java.lang.NullPointerException" : "NONE");
                    } finally {
                        log.close("matching_task_finished");
                        assertAllDiagnosticEmissionsSettled();
                    }
                    String output = bytes.toString(StandardCharsets.UTF_8);
                    String query = eventLine(output, "ENTITY_QUERY_COMPLETED");
                    assertTrue(query.getBytes(StandardCharsets.UTF_8).length <= 2048, query);
                    assertTrue(query.contains("diagnosticCaptureStatus=complete"), query);
                    for (String required : List.of("commandRequestId=" + requestId, "commandCorrelationId=" + correlationId,
                            "commandSessionId=" + sessionId, "commandConnectionGeneration=" + Long.MAX_VALUE,
                            "source=TRACKER_LIVE_CLIENT_MEMBERSHIP", "sourceWorldTime=" + Long.MAX_VALUE,
                            "distancePolicy=CLIENT_OBSERVABLE_NO_RADIUS", "radiusCheck=NOT_EVALUATED",
                            "visited=" + (failedRead ? 4 : 3), "matched=1", "scopeComplete=" + !failedRead,
                            "scopeResult=" + (failedRead ? "INCOMPLETE" : "MATCH"), "phaseRole=INITIAL_OBSERVATION",
                            "selectedLocalId=41", "selectedX=8", "selectedY=65", "selectedZ=2", "selectedDistanceSq=68.0",
                            "diagnosticValues=" + (failedRead ? "UNAVAILABLE" : "CAPTURED"),
                            "failureStage=" + (failedRead ? "entity_liveness" : "NONE"),
                            "failureType=" + (failedRead ? "java.lang.NullPointerException" : "NONE"))) assertTrue(query.contains(required), query);
                    for (String counter : List.of("excludedSelf", "excludedDead", "excludedRemoved", "excludedKind",
                            "excludedTargetId", "excludedRadius", "rawIdCompared", "rawIdMatches", "rawIdUnavailable", "rawIdNotEvaluated")) {
                        int expected = counter.equals("rawIdCompared") ? 3
                                : List.of("excludedKind", "excludedTargetId", "rawIdMatches").contains(counter) ? 1 : 0;
                        assertTrue(query.contains(counter + "=" + (failedRead ? "UNAVAILABLE" : expected)), query);
                    }
                    assertTrue(query.contains("sample0="), query); assertTrue(query.contains("[TRUNCATED]"), query);
                    for (String sample : List.of("sample1", "sample2")) {
                        assertTrue(query.contains(sample + "=" + (failedRead ? "OMITTED_READ_FAILURE" : "id%3D")), query);
                    }
                    String reason = failedRead ? "entity_read_failed" : "complete_loaded_scope_candidate_selected";
                    assertTrue(query.contains("requiredBoundarySignature=MinecraftFindEntityObservation|ENTITY_QUERY_COMPLETED||" + reason + "|||||I|" + (failedRead ? "X" : "F") + " "), query);
                    assertFalse(output.contains("private command text"), output);
                }
            } finally { System.setOut(previous); ChatClefDiagnostics.setBoundaryEnabled(false); }
        } finally { ChatClefDiagnostics.registerCommandContextProvider(originalProvider); }
    }

    @Test void queryAndRevalidationBoundariesSurviveSharedOrdinaryCeilingInRealStdout() throws Exception {
        try (var client = HeadlessMinecraftClientSession.outOfGame()) {
            ChatClefDiagnostics.setBoundaryEnabled(false); ChatClefDiagnostics.resetDiagnosticSessionForTests();
            ChatClefDiagnostics.setBoundaryEnabled(true);
            PrintStream previous = System.out; ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capture); FindLog log = new FindLog(OPERATION);
                for (int detail = 0; detail <= DiagnosticSessionLimits.ORDINARY_CEILING; detail++) {
                    ChatClefDiagnostics.logBoundedBoundaryWithDispatchResult("FIND_TEST_DETAIL", "pressure", null,
                            2048, new Object[]{"detailIndex", detail}, new Object[0]);
                }
                assertEquals(DiagnosticSessionLimits.ORDINARY_CEILING,
                        ChatClefDiagnostics.diagnosticSessionSnapshot().ordinarySlotsUsed());
                bytes.reset();
                FindOutcome outcome = runFixture(log, true);
                log.close("matching_task_finished");
                assertTrue(outcome.satisfied());
                String output = bytes.toString(StandardCharsets.UTF_8);
                List<String> required = List.of("STARTED", "ENTITY_QUERY_STARTED", "ENTITY_QUERY_COMPLETED",
                        "OBSERVATION_COMPLETED", "ENTITY_REVALIDATION_RESULT", "REPORT_TERMINAL", "TRACE_TERMINAL");
                int prior = -1;
                for (String event : required) {
                    List<String> lines = output.lines().filter(line -> line.contains("event=FIND_" + event + " ")).toList();
                    assertEquals(1, lines.size(), event + "\n" + output);
                    String line = lines.get(0);
                    assertTrue(line.contains("operationId=" + OPERATION), line);
                    assertTrue(line.contains("diagnosticCaptureStatus=complete"), line);
                    assertTrue(line.getBytes(StandardCharsets.UTF_8).length <= 2048, line);
                    int current = output.indexOf(line); assertTrue(current > prior, output); prior = current;
                }
                String query = eventLine(output, "ENTITY_QUERY_COMPLETED");
                for (String value : List.of("visited=6", "matched=1", "excludedSelf=1", "excludedDead=1",
                        "excludedRemoved=1", "excludedKind=1", "excludedTargetId=1", "rawIdMatches=2",
                        "rawIdUnavailable=0", "rawIdNotEvaluated=0", "scopeComplete=true", "diagnosticValues=CAPTURED",
                        "sourceWorldTime=17")) assertTrue(query.contains(value), query);
                assertTrue(query.contains("requiredBoundarySignature=MinecraftFindEntityObservation|ENTITY_QUERY_COMPLETED"), query);
                assertTrue(eventLine(output, "ENTITY_QUERY_STARTED").contains("distancePolicy=CLIENT_OBSERVABLE_NO_RADIUS"), output);
                assertTrue(eventLine(output, "ENTITY_REVALIDATION_RESULT").contains("revalidated=true"), output);
                assertFalse(output.contains("private-stable-key"), output);
                assertFalse(output.contains("commandText="), output);
            } finally { System.setOut(previous); ChatClefDiagnostics.setBoundaryEnabled(false); }
        }
    }

    @Test void numericChangesDoNotCreateNewSignaturesButMeaningfulQueryResultsDo() {
        List<Object[]> output = new ArrayList<>();
        FindLog log = new FindLog(OPERATION, (event, fields) -> output.add(fields));
        log.event("ENTITY_QUERY_COMPLETED", "reason", "complete_loaded_scope_no_match", "visited", 1, "matched", 0);
        log.event("ENTITY_QUERY_COMPLETED", "reason", "complete_loaded_scope_no_match", "visited", 500, "matched", 0);
        log.event("ENTITY_QUERY_COMPLETED", "reason", "complete_loaded_scope_candidate_selected", "visited", 500, "matched", 1);
        log.event("ENTITY_QUERY_COMPLETED", "reason", "entity_visit_limit_exhausted", "visited", 4096, "matched", 1);
        assertEquals(3, output.size());
        for (Object[] fields : output) {
            String signature = String.valueOf(value(fields, "requiredBoundarySignature"));
            assertTrue(signature.startsWith("MinecraftFindEntityObservation|ENTITY_QUERY_COMPLETED|"), signature);
            assertFalse(signature.contains("4096"), signature); assertFalse(signature.contains("500"), signature);
        }
    }

    @Test void offBoundaryAndActualSinkFailureKeepIdenticalFoundAndMissDecisions() throws Exception {
        try (var client = HeadlessMinecraftClientSession.outOfGame()) {
            PrintStream previous = System.out; ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capture);
                for (boolean found : List.of(true, false)) {
                    ChatClefDiagnostics.setBoundaryEnabled(false); ChatClefDiagnostics.resetDiagnosticSessionForTests();
                    FindLog off = new FindLog(OPERATION); FindOutcome expected = runClosedFixture(off, found);
                    startBoundaryTestSession();
                    FindLog boundary = new FindLog(OPERATION);
                    assertEquals(expected, runClosedFixture(boundary, found));
                    for (Throwable failure : List.of(new IllegalStateException("sink_failed"),
                            new AssertionError("sink_assertion"), new LinkageError("sink_linkage"))) {
                        startBoundaryTestSession();
                        try (PrintStream failed = failingStream(failure)) {
                            System.setOut(failed); FindLog sinkFailure = new FindLog(OPERATION);
                            try {
                                assertEquals(128, ChatClefDiagnostics.diagnosticSessionSnapshot().family(
                                        lavi.minecraft.diagnostics.session.admission.DiagnosticEventFamily.FIND_RESERVED_BOUNDARY).admittedSlots());
                                assertEquals(expected, runFixture(sinkFailure, found));
                            } finally {
                                sinkFailure.close("matching_task_finished");
                                assertAllDiagnosticEmissionsSettled();
                            }
                        } finally { System.setOut(capture); }
                    }
                }
                String output = bytes.toString(StandardCharsets.UTF_8);
                assertTrue(output.contains("terminalReason=NOT_OBSERVED_IN_LOADED_SCOPE:complete_loaded_scope_no_match"), output);
                assertTrue(output.contains("reason=complete_loaded_scope_no_match"), output);
                assertTrue(output.contains("detailedTrace=UNRESERVED_OR_DISABLED_PARTIAL"), output);
            } finally { System.setOut(previous); ChatClefDiagnostics.setBoundaryEnabled(false); }
        }
    }

    @Test void injectedAssertionAndLinkageFailureDoNotChangeObservationOutcome() {
        for (boolean found : List.of(true, false)) {
            FindOutcome expected = runFixture(new FindLog(OPERATION, (event, fields) -> { }), found);
            for (Error failure : List.of(new AssertionError("sink_assertion"), new LinkageError("sink_linkage"))) {
                FindLog failed = new FindLog(OPERATION, (event, fields) -> { throw failure; });
                assertEquals(expected, runFixture(failed, found));
                assertDoesNotThrow(() -> failed.close("matching_task_finished"));
            }
        }
    }

    @Test void fatalVmErrorIsNotContainedAsAnOrdinaryDiagnosticFailure() {
        OutOfMemoryError failure = new OutOfMemoryError("synthetic_vm_error");
        boolean[] armed = {true};
        FindLog log = new FindLog(OPERATION, (event, fields) -> { if (armed[0]) throw failure; });
        try {
            assertSame(failure, assertThrows(OutOfMemoryError.class,
                    () -> log.event("ENTITY_QUERY_STARTED", "reason", "live_membership_query_requested")));
        } finally { armed[0] = false; log.close("synthetic_vm_fixture_retired"); }
    }

    private static FindOutcome runFixture(FindLog log, boolean found) {
        FindObservationPort port = new FindObservationPort() {
            final Binding binding = new Binding(new Object(), new Object(), "minecraft:overworld", 0, 65, 0, -64, 320);
            @Override public Binding binding() { return binding; }
            @Override public boolean matches(Binding original) { return original == binding; }
            @Override public EntityScan scanEntities(FindRequest request, Binding original, int limit) {
                log.event("ENTITY_QUERY_STARTED", "reason", "live_membership_query_requested", "source", "TRACKER_LIVE_CLIENT_MEMBERSHIP",
                        "distancePolicy", "CLIENT_OBSERVABLE_NO_RADIUS", "originX", 0, "originY", 65, "originZ", 0);
                log.event("ENTITY_QUERY_COMPLETED", "reason", found ? "complete_loaded_scope_candidate_selected" : "complete_loaded_scope_no_match",
                        "visited", 6, "matched", found ? 1 : 0, "excludedSelf", 1, "excludedDead", 1,
                        "excludedRemoved", 1, "excludedKind", 1, "excludedTargetId", found ? 1 : 2,
                        "excludedRadius", 0, "rawIdCompared", 6, "rawIdMatches", found ? 2 : 1,
                        "rawIdUnavailable", 0, "rawIdNotEvaluated", 0, "scopeComplete", true,
                        "diagnosticValues", "CAPTURED", "sourceWorldTime", 17,
                        "sample0", "local:41,id:minecraft:villager,alive:true", "sample1", "local:42,alive:false",
                        "sample2", "local:43,removed:true");
                return new EntityScan(6, found ? 1 : 0, true, found ? CANDIDATE : null);
            }
            @Override public FindCandidate readBlock(FindRequest request, Binding original, int x, int y, int z) {
                fail("Entity report must not invoke block reads"); return null;
            }
            @Override public FindCandidate revalidate(FindRequest request, Binding original, FindCandidate candidate) {
                log.event("ENTITY_REVALIDATION_RESULT", "reason", "selected_candidate_revalidated", "localEntityId", candidate.entityId(),
                        "selectedX", candidate.x(), "selectedY", candidate.y(), "selectedZ", candidate.z(),
                        "distanceSquared", candidate.distanceSquared(), "revalidated", true);
                return candidate;
            }
        };
        FindObservationOperation operation = new FindObservationOperation(new FindRequest("entity", "minecraft:villager", "report", "catalog", 1),
                OPERATION, port, () -> 0, log);
        operation.tick(); assertNotNull(operation.outcome()); return operation.outcome();
    }
    private static String eventLine(String output, String event) {
        return output.lines().filter(line -> line.contains("event=FIND_" + event + " ")).findFirst().orElseThrow();
    }
    private static void startBoundaryTestSession() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
        ChatClefDiagnostics.setBoundaryEnabled(true);
    }
    private static FindOutcome runClosedFixture(FindLog log, boolean found) {
        try { return runFixture(log, found); }
        finally {
            log.close("matching_task_finished");
            assertAllDiagnosticEmissionsSettled();
        }
    }
    private static void assertAllDiagnosticEmissionsSettled() {
        assertEquals(0, ChatClefDiagnostics.diagnosticSessionSnapshot().emissionPending());
        assertEquals(0, ChatClefDiagnostics.diagnosticSessionSnapshot().emissionInProgress());
    }
    private static PrintStream failingStream(Throwable failure) {
        return new PrintStream(OutputStream.nullOutputStream()) {
            @Override public void println(String text) {
                if (failure instanceof RuntimeException runtime) throw runtime;
                throw (Error) failure;
            }
        };
    }
    private static Object value(Object[] fields, String key) {
        for (int index = 0; index + 1 < fields.length; index += 2) if (key.equals(fields[index])) return fields[index + 1];
        return null;
    }
}
//#endif
