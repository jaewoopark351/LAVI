//#if MC == 12001
//$$ package lavi.minecraft.find.result;

//$$ import lavi.minecraft.find.model.FindRequest;

//$$ //20260914_kpopmodder: Authoritative immutable phase facts remain internal; no wire or logger can create discovery proof.
//$$ public record FindTerminalPhaseEvidence(String operationId, FindRequest request, Object userRoot, Phase phase,
//$$                                         boolean discoveryComplete, boolean explorationQuiet, boolean finalRevalidated,
//$$                                         boolean approachHandedOff, boolean approachArrived, boolean inputQuiet) {
//$$     public enum Phase { INITIAL_OBSERVATION, EXPLORING, STOPPING_EXPLORATION, APPROACHING, TERMINAL }
//$$     public FindTerminalPhaseEvidence {
//$$         if (operationId == null || request == null || userRoot == null || phase == null)
//$$             throw new IllegalArgumentException("null_find_phase_evidence");
//$$     }
//$$     // discoveryComplete means a completed matching local scan and live identity check latched a target, not that every area was searched.
//$$     public boolean boundTo(FindOutcome outcome, Object root) {
//$$         return request == outcome.request() && operationId.equals(outcome.operationId()) && userRoot == root;
//$$     }
//$$     @Override public String toString() { return "FindTerminalPhaseEvidence[phase=" + phase + "]"; }
//$$ }
//#endif
