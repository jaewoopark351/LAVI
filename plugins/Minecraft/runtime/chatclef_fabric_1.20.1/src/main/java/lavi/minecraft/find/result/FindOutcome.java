//#if MC == 12001
//$$ package lavi.minecraft.find.result;

//$$ import lavi.minecraft.find.model.FindCandidate;
//$$ import lavi.minecraft.find.model.FindRequest;

//$$ //20260914_kpopmodder: A single immutable operation result distinguishes query completion from satisfaction.
//$$ public record FindOutcome(String operationId, FindRequest request, String findResult, boolean satisfied,
//$$                           String dimension, FindCandidate candidate, int visited, int matched,
//$$                           boolean scopeComplete, String reason, FindTerminalPhaseEvidence terminalEvidence) {
//$$     public FindOutcome(String operationId, FindRequest request, String findResult, boolean satisfied,
//$$                        String dimension, FindCandidate candidate, int visited, int matched,
//$$                        boolean scopeComplete, String reason) {
//$$         this(operationId, request, findResult, satisfied, dimension, candidate, visited, matched, scopeComplete, reason, null);
//$$     }
//$$     public FindOutcome {
//$$         if (operationId == null || request == null || findResult == null || dimension == null || reason == null) {
//$$             throw new IllegalArgumentException("null_find_outcome_field");
//$$         }
//$$         if (satisfied && (candidate == null || !scopeComplete)) {
//$$             throw new IllegalArgumentException("found_requires_complete_revalidated_candidate");
//$$         }
//$$         if (!satisfied && candidate != null) throw new IllegalArgumentException("non_success_has_candidate");
//$$     }
//$$ }

//#endif
