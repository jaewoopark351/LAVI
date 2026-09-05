package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.terminal;

//20260905_kpopmodder: Evaluate one active execution's terminal decision without owning callbacks or ticks.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandOutcomeClassifier;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminalDecision;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefWaitingDecisionDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.FabricChatClefNonterminalLifecycleEvidencePublisher;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.FabricChatClefPreexistingIdleRootStabilityObserver;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.execution.FabricChatClefActiveExecutionStore;

public final class FabricChatClefCommandTerminalEvaluator {
    private final FabricChatClefActiveExecutionStore executionStore;
    private final FabricChatClefCommandOutcomeClassifier outcomeClassifier;
    private final FabricChatClefPreexistingIdleRootStabilityObserver stabilityObserver;
    private final FabricChatClefWaitingDecisionDiagnostics waitingDiagnostics;
    private final FabricChatClefNonterminalLifecycleEvidencePublisher nonterminalEvidencePublisher;
    private final FabricChatClefCommandTerminalResultDispatcher terminalResultDispatcher;

    public FabricChatClefCommandTerminalEvaluator(
            FabricChatClefActiveExecutionStore executionStore,
            FabricChatClefCommandOutcomeClassifier outcomeClassifier,
            FabricChatClefPreexistingIdleRootStabilityObserver stabilityObserver,
            FabricChatClefWaitingDecisionDiagnostics waitingDiagnostics,
            FabricChatClefNonterminalLifecycleEvidencePublisher nonterminalEvidencePublisher,
            FabricChatClefCommandTerminalResultDispatcher terminalResultDispatcher
    ) {
        this.executionStore = executionStore;
        this.outcomeClassifier = outcomeClassifier;
        this.stabilityObserver = stabilityObserver;
        this.waitingDiagnostics = waitingDiagnostics;
        this.nonterminalEvidencePublisher = nonterminalEvidencePublisher;
        this.terminalResultDispatcher = terminalResultDispatcher;
    }

    public void evaluate(
            FabricChatClefCommandExecution execution,
            boolean publishNonterminalEvidence,
            FabricChatClefCommandContext activeContext
    ) {
        if (executionStore.current() != execution) {
            return;
        }
        if (publishNonterminalEvidence) {
            stabilityObserver.update(execution, activeContext);
        }
        FabricChatClefCommandTerminalDecision decision = outcomeClassifier.classify(execution);
        if (!decision.terminal()) {
            waitingDiagnostics.log(execution, decision.reason());
            if (publishNonterminalEvidence) {
                nonterminalEvidencePublisher.publishIfEligible(execution, decision.reason(), activeContext);
            }
            return;
        }
        terminalResultDispatcher.dispatchDecision(execution, decision);
    }
}
