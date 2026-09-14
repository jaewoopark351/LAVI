//#if MC == 12001
//$$ package lavi.minecraft.find.diagnostics;

//$$ import java.util.HashSet;
//$$ import java.util.List;
//$$ import java.util.Set;
//$$ import lavi.minecraft.diagnostics.ChatClefDiagnostics;
//$$ import lavi.minecraft.diagnostics.session.admission.DiagnosticAdmissionToken;
//$$ import lavi.minecraft.diagnostics.session.admission.DiagnosticEventFamily;

//$$ //20260914_kpopmodder: One 128-slot trace retains the existing family quota and process-lifetime shared cap.
//$$ final class FindTraceReservation {
//$$     static final int MAX_EVENTS = 128;
//$$     private static final int ORDINARY_SIGNATURE_LIMIT = 32;
//$$     private static final Set<String> REQUIRED_EVENTS = Set.of("STARTED", "RESUMED", "SUSPENDED", "OBSERVATION_COMPLETED",
//$$             "ENTITY_QUERY_STARTED", "ENTITY_QUERY_COMPLETED", "ENTITY_REVALIDATION_RESULT",
//$$             "PARENT_STARTED", "PHASE_CHANGED", "DISCOVERY_SCAN_STARTED", "DISCOVERY_SCAN_COMPLETED",
//$$             "ROOT_LIFETIME_BINDING_RESULT",
//$$             "APPROACH_MOVEMENT_RESULT",
//$$             "DISCOVERY_MOVEMENT_SAMPLED", "DISCOVERY_PROGRESS_CLOCK_CHANGED", "DISCOVERY_WAYPOINT_PROGRESS",
//$$             "CANDIDATE_LATCHED", "BOUNDS_EXHAUSTED", "DEADLINE_EXHAUSTED", "HANDOFF_VALIDATED", "PARENT_RETIRED",
//$$             "EXPLORATION_STARTED", "EXPLORATION_SUSPENDED",
//$$             "EXPLORATION_RESUMED", "EXPLORATION_STOP_REQUESTED", "EXPLORATION_CLEANUP_RESULT",
//$$             "EXPLORATION_ROUTE_RESULT", "EXPLORATION_NO_PROGRESS", "EXPLORATION_ROUTE_STARTED",
//$$             "EXPLORATION_WAYPOINT_SELECTED", "EXPLORATION_WAYPOINT_REJECTED", "EXPLORATION_PLAN_STARTED",
//$$             "EXPLORATION_STEP_STARTED", "EXPLORATION_STEP_COMPLETED", "EXPLORATION_WAYPOINT_REACHED",
//$$             "EXPLORATION_MOVEMENT_REJECTED", "EXPLORATION_MOVEMENT_SUSPENDED",
//$$             "EXPLORATION_MOVEMENT_READ_FAILED",
//$$             "REPORT_TERMINAL", "OBSERVATION_TERMINAL", "APPROACH_RESUMED", "APPROACH_SUSPENDED", "APPROACH_TERMINAL",
//$$             "APPROACH_CAPTURE_COMPLETE", "APPROACH_PLAN_RESULT", "APPROACH_PLAN_ADMITTED", "APPROACH_PLAN_REJECTED",
//$$             "APPROACH_STARTED", "APPROACH_PLAN_STARTED", "APPROACH_STEP_STARTED", "APPROACH_STEP_COMPLETED",
//$$             "INPUT_LEASE_LOST", "INPUT_CLAIM", "INPUT_WRITE_REJECTED", "INPUT_RELEASE", "RETIRED");
//$$     record Claim(boolean emit, DiagnosticAdmissionToken token, String status) { }
//$$     private final boolean injected;
//$$     private final List<DiagnosticAdmissionToken> tokens;
//$$     private final boolean boundaryEligibleAtAdmission;
//$$     private final String exclusionReason;
//$$     private final Set<String> signatures = new HashSet<>();
//$$     private final Set<String> ordinarySignatures = new HashSet<>();
//$$     private String terminalSignature;
//$$     private String outcomeSignature;
//$$     private int nextBoundary;
//$$     private boolean terminalSpent, outcomeSpent, closed, outputFailed;
//$$     private boolean exclusionReported, activationReported;

//$$     FindTraceReservation(boolean injected) {
//$$         this.injected = injected;
//$$         boolean eligible = false;
//$$         try { eligible = !injected && ChatClefDiagnostics.isBoundaryEnabled(); }
//$$         catch (RuntimeException | LinkageError | AssertionError ignored) { }
//$$         boundaryEligibleAtAdmission = eligible;
//$$         List<DiagnosticAdmissionToken> reserved = List.of();
//$$         if (!injected) {
//$$             try { reserved = ChatClefDiagnostics.reserveDiagnosticTrace(DiagnosticEventFamily.FIND_RESERVED_BOUNDARY, MAX_EVENTS); }
//$$             catch (RuntimeException | LinkageError | AssertionError ignored) { }
//$$         }
//$$         tokens = reserved;
//$$         exclusionReason = injected || tokens.size() == MAX_EVENTS ? "NONE"
//$$                 : eligible ? "RESERVATION_NOT_GRANTED_REASON_UNAVAILABLE" : "BOUNDARY_INELIGIBLE_AT_ADMISSION";
//$$     }
//$$     String observeTraceExclusion() {
//$$         if (injected || closed || tokens.size() == MAX_EVENTS) return null;
//$$         boolean eligible;
//$$         try { eligible = ChatClefDiagnostics.isBoundaryEnabled(); }
//$$         catch (RuntimeException | LinkageError | AssertionError ignored) { return null; }
//$$         if (!eligible) return null;
//$$         if (!boundaryEligibleAtAdmission && !activationReported) {
//$$             activationReported = true;
//$$             return "BOUNDARY_BECAME_ELIGIBLE_AFTER_ADMISSION_TRACE_REMAINS_PARTIAL";
//$$         }
//$$         if (!exclusionReported && boundaryEligibleAtAdmission) {
//$$             exclusionReported = true;
//$$             return exclusionReason;
//$$         }
//$$         return null;
//$$     }
//$$     String exclusionReason() { return exclusionReason; }
//$$     Claim claim(String event, String signature) {
//$$         if (closed) return new Claim(false, null, status());
//$$         if (event.equals("TRACE_TERMINAL")) {
//$$             if (terminalSignature != null) return new Claim(false, null, status());
//$$             terminalSignature = signature;
//$$             if (injected) return new Claim(true, null, status());
//$$             if (tokens.size() != MAX_EVENTS) return new Claim(false, null, status());
//$$             terminalSpent = true;
//$$             return new Claim(true, tokens.get(MAX_EVENTS - 1), status());
//$$         }
//$$         if (event.equals("REPORT_TERMINAL") || event.equals("APPROACH_TERMINAL") || event.equals("PARENT_OUTCOME")) {
//$$             if (outcomeSignature != null) return new Claim(false, null, status());
//$$             outcomeSignature = signature;
//$$             if (injected) return new Claim(true, null, status());
//$$             if (tokens.size() != MAX_EVENTS) return new Claim(false, null, status());
//$$             outcomeSpent = true;
//$$             return new Claim(true, tokens.get(MAX_EVENTS - 2), status());
//$$         }
//$$         if (!REQUIRED_EVENTS.contains(event)) {
//$$             if (ordinarySignatures.contains(signature) || ordinarySignatures.size() == ORDINARY_SIGNATURE_LIMIT) return new Claim(false, null, status());
//$$             ordinarySignatures.add(signature);
//$$             return new Claim(true, null, "ORDINARY_DETAIL_" + status());
//$$         }
//$$         if (signatures.contains(signature)) return new Claim(false, null, status());
//$$         if (signatures.size() == MAX_EVENTS - 2) { outputFailed = true; return new Claim(false, null, status()); }
//$$         signatures.add(signature);
//$$         if (injected) return new Claim(true, null, status());
//$$         if (tokens.size() != MAX_EVENTS) return new Claim(false, null, status());
//$$         if (nextBoundary >= MAX_EVENTS - 2) { outputFailed = true; return new Claim(false, null, status()); }
//$$         return new Claim(true, tokens.get(nextBoundary++), status());
//$$     }
//$$     String status() {
//$$         if (injected) return "INJECTED_OUTPUT_CAPTURE";
//$$         if (tokens.size() != MAX_EVENTS) return "UNRESERVED_OR_DISABLED_PARTIAL";
//$$         return outputFailed ? "RESERVED_OUTPUT_INCOMPLETE" : "RESERVED_OUTPUT_UNVERIFIED";
//$$     }
//$$     void markOutputFailure() { outputFailed = true; }
//$$     boolean closed() { return closed; }
//$$     void close() {
//$$         if (closed) return;
//$$         closed = true;
//$$         for (int index = nextBoundary; index < tokens.size(); index++) {
//$$             if (index == MAX_EVENTS - 1 && terminalSpent) continue;
//$$             if (index == MAX_EVENTS - 2 && outcomeSpent) continue;
//$$             try { ChatClefDiagnostics.abandonReservedBoundary(tokens.get(index)); }
//$$             catch (RuntimeException | LinkageError | AssertionError ignored) { outputFailed = true; }
//$$         }
//$$     }
//$$ }

//#endif
