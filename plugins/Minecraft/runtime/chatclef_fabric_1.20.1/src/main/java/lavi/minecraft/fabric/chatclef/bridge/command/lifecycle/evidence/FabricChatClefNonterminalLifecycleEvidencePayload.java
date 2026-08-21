package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.payload.FabricChatClefNonterminalLifecycleEvidencePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;

import java.util.Map;

//20260820_kpopmodder: Add nonterminal lifecycle evidence fields at the existing command_result data edge.
public final class FabricChatClefNonterminalLifecycleEvidencePayload implements FabricChatClefCommandResultDataPayload {
    private final FabricChatClefCommandResultDataPayload basePayload;
    private final String stage;
    private final int evidenceSequence;
    private final String waitingReason;
    private final FabricChatClefTaskSnapshot currentRootTask;
    private final FabricChatClefStableRequestQuiescenceObservation quiescence;

    private FabricChatClefNonterminalLifecycleEvidencePayload(
            FabricChatClefCommandResultDataPayload basePayload,
            String stage,
            int evidenceSequence,
            String waitingReason,
            FabricChatClefTaskSnapshot currentRootTask,
            FabricChatClefStableRequestQuiescenceObservation quiescence
    ) {
        this.basePayload = basePayload;
        this.stage = nullToEmpty(stage);
        this.evidenceSequence = evidenceSequence;
        this.waitingReason = nullToEmpty(waitingReason);
        this.currentRootTask = currentRootTask;
        this.quiescence = quiescence;
    }

    public static FabricChatClefNonterminalLifecycleEvidencePayload of(
            FabricChatClefCommandResultDataPayload basePayload,
            String stage,
            int evidenceSequence,
            String waitingReason,
            FabricChatClefTaskSnapshot currentRootTask,
            FabricChatClefStableRequestQuiescenceObservation quiescence
    ) {
        return new FabricChatClefNonterminalLifecycleEvidencePayload(
                basePayload,
                stage,
                evidenceSequence,
                waitingReason,
                currentRootTask,
                quiescence
        );
    }

    @Override
    public Map<String, Object> toMap() {
        return FabricChatClefNonterminalLifecycleEvidencePayloadMap.toMap(
                basePayload,
                stage,
                evidenceSequence,
                waitingReason,
                currentRootTask,
                quiescence
        );
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
