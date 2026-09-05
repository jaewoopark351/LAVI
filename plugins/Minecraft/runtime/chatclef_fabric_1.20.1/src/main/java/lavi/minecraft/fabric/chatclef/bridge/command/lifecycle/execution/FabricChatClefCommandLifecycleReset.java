package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.execution;

//20260905_kpopmodder: Reset request-scoped ordinary lifecycle observers at ownership boundaries.

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefWaitingDecisionDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.FabricChatClefNonterminalLifecycleEvidencePublisher;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.FabricChatClefPreexistingIdleRootStabilityObserver;

public final class FabricChatClefCommandLifecycleReset {
    private final FabricChatClefWaitingDecisionDiagnostics waitingDiagnostics;
    private final FabricChatClefNonterminalLifecycleEvidencePublisher nonterminalEvidencePublisher;
    private final FabricChatClefPreexistingIdleRootStabilityObserver stabilityObserver;

    public FabricChatClefCommandLifecycleReset(
            FabricChatClefWaitingDecisionDiagnostics waitingDiagnostics,
            FabricChatClefNonterminalLifecycleEvidencePublisher nonterminalEvidencePublisher,
            FabricChatClefPreexistingIdleRootStabilityObserver stabilityObserver
    ) {
        this.waitingDiagnostics = waitingDiagnostics;
        this.nonterminalEvidencePublisher = nonterminalEvidencePublisher;
        this.stabilityObserver = stabilityObserver;
    }

    public void reset() {
        waitingDiagnostics.reset();
        nonterminalEvidencePublisher.reset();
        stabilityObserver.reset();
    }
}
