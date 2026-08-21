package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.FabricChatClefStableRequestQuiescenceObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;

import java.util.HashMap;
import java.util.Map;

//20260820_kpopmodder: Serialize additive running lifecycle evidence without changing v1 result keys.
public final class FabricChatClefNonterminalLifecycleEvidencePayloadMap {
    private FabricChatClefNonterminalLifecycleEvidencePayloadMap() {
    }

    public static Map<String, Object> toMap(
            FabricChatClefCommandResultDataPayload basePayload,
            String stage,
            int evidenceSequence,
            String waitingReason,
            FabricChatClefTaskSnapshot currentRootTask,
            FabricChatClefStableRequestQuiescenceObservation quiescence
    ) {
        Map<String, Object> payload = new HashMap<>();
        if (basePayload != null) {
            payload.putAll(basePayload.toMap());
        }
        payload.put("classification", "nonterminal_diagnostic");
        payload.put("lifecycle_evidence_version", "2026-08-20.java-nonterminal-evidence.v1");
        payload.put("lifecycle_evidence_stage", stage);
        payload.put("evidence_sequence", evidenceSequence);
        payload.put("waiting_reason", waitingReason);
        payload.put("terminal_status", "NONE");
        payload.put("task_finished_event_absence_evidence", true);
        payload.put("gameplay_effect", "UNVERIFIED");
        payload.put("current_root_task", currentRootTask == null ? Map.of() : currentRootTask.toMap());
        payload.put("stable_request_quiescence", quiescence == null ? Map.of() : quiescence.toMap());
        payload.put(
                "lifecycle_evidence",
                lifecycleEvidence(stage, evidenceSequence)
        );
        return payload;
    }

    private static Map<String, Object> lifecycleEvidence(String stage, int evidenceSequence) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("version", "2026-08-20.java-nonterminal-evidence.v1");
        payload.put("stage", stage);
        payload.put("evidence_sequence", evidenceSequence);
        payload.put("status", "running");
        payload.put("terminal_status", "NONE");
        payload.put("task_finished_event_received", false);
        payload.put("task_finished_event_absence_evidence", true);
        payload.put("gameplay_effect", "UNVERIFIED");
        return payload;
    }
}
