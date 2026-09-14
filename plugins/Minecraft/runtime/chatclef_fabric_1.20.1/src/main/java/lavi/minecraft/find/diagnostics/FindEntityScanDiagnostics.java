//#if MC == 12001
//$$ package lavi.minecraft.find.diagnostics;
//$$
//$$ import java.util.ArrayList;
//$$ import java.util.List;
//$$ import lavi.minecraft.find.model.FindCandidate;
//$$
//$$ //20260914_kpopmodder: Diagnostic counters and samples never participate in query selection or completion.
//$$ public final class FindEntityScanDiagnostics {
//$$     private final List<String> samples = new ArrayList<>(3);
//$$     private int self, dead, removed, kind, targetId, radius, rawCompared, rawMatches, rawUnavailable, rawNotEvaluated;
//$$     private boolean unavailable;
//$$     public void capture(Object localId, String rawId, boolean compared, boolean rawMatch, String rejection, Double distance) {
//$$         try {
//$$             if (compared) rawCompared++;
//$$             if (rawMatch) rawMatches++;
//$$             if (rawId.equals("UNAVAILABLE")) rawUnavailable++;
//$$             if (rawId.equals("NOT_EVALUATED")) rawNotEvaluated++;
//$$             switch (rejection) {
//$$                 case "self" -> self++;
//$$                 case "dead" -> dead++;
//$$                 case "removed" -> removed++;
//$$                 case "wrong_kind" -> kind++;
//$$                 case "wrong_target_id" -> targetId++;
//$$                 case "outside_radius" -> radius++;
//$$                 default -> { }
//$$             }
//$$             if (samples.size() < 3) {
//$$                 String sample = "id=" + localId + ";type=" + rawId + ";first=" + rejection
//$$                         + ";distanceSq=" + (distance == null ? "NOT_EVALUATED" : distance);
//$$                 samples.add(sample.length() <= 80 ? sample : sample.substring(0, 69) + "[TRUNCATED]");
//$$             }
//$$         } catch (RuntimeException | LinkageError | AssertionError ignored) { unavailable = true; }
//$$     }
//$$     public void emit(FindLog log, int visited, int matched, boolean complete, String reason, Object sourceWorldTime, FindCandidate selected,
//$$                      String failureStage, String failureType) {
//$$         emit(log, visited, matched, complete, reason, sourceWorldTime, selected, failureStage, failureType, "INITIAL_OBSERVATION");
//$$     }
//$$     public void emit(FindLog log, int visited, int matched, boolean complete, String reason, Object sourceWorldTime, FindCandidate selected,
//$$                      String failureStage, String failureType, String phaseRole) {
//$$         if (log == null) return;
//$$         try {
//$$             if (reason.equals("entity_read_failed")) unavailable = true;
//$$             log.eventInPhase(phaseRole, "ENTITY_QUERY_COMPLETED", "reason", reason,
//$$                     "failureStage", failureStage, "failureType", failureType,
//$$                     "source", "TRACKER_LIVE_CLIENT_MEMBERSHIP", "sourceWorldTime", sourceWorldTime,
//$$                     "distancePolicy", "CLIENT_OBSERVABLE_NO_RADIUS", "radiusCheck", "NOT_EVALUATED",
//$$                     "visited", visited, "matched", matched, "scopeComplete", complete,
//$$                     "scopeResult", complete ? matched > 0 ? "MATCH" : "MISS" : "INCOMPLETE",
//$$                     "excludedSelf", count(self), "excludedDead", count(dead), "excludedRemoved", count(removed),
//$$                     "excludedKind", count(kind), "excludedTargetId", count(targetId), "excludedRadius", count(radius),
//$$                     "rawIdCompared", count(rawCompared), "rawIdMatches", count(rawMatches),
//$$                     "rawIdUnavailable", count(rawUnavailable), "rawIdNotEvaluated", count(rawNotEvaluated),
//$$                     "selectedLocalId", selected == null ? "NOT_OBSERVED" : selected.entityId(),
//$$                     "selectedX", selected == null ? "NOT_EVALUATED" : selected.x(),
//$$                     "selectedY", selected == null ? "NOT_EVALUATED" : selected.y(),
//$$                     "selectedZ", selected == null ? "NOT_EVALUATED" : selected.z(),
//$$                     "selectedDistanceSq", selected == null ? "NOT_EVALUATED" : selected.distanceSquared(),
//$$                     "diagnosticValues", unavailable ? "UNAVAILABLE" : "CAPTURED",
//$$                     "sample0", sample(0),
//$$                     "sample1", reason.equals("entity_read_failed") ? "OMITTED_READ_FAILURE" : sample(1),
//$$                     "sample2", reason.equals("entity_read_failed") ? "OMITTED_READ_FAILURE" : sample(2));
//$$         } catch (RuntimeException | LinkageError | AssertionError ignored) { unavailable = true; }
//$$     }
//$$     private Object count(int value) { return unavailable ? "UNAVAILABLE" : value; }
//$$     private String sample(int index) { return index < samples.size() ? samples.get(index) : "NOT_OBSERVED"; }
//$$ }
//#endif
