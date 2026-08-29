package lavi.minecraft.task.container.home.execution.operation.terminal;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.home.timeout.StoreHomeTimeoutDiagnostics;
import lavi.minecraft.task.container.home.execution.StoreHomeResult;
import lavi.minecraft.task.container.home.execution.candidate.view.StoreHomeCandidateRuntimeView;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeTerminalClassifier;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeTerminalReporter;
import lavi.minecraft.task.container.home.execution.operation.reporting.StoreHomeReportingPlanCapture;
import lavi.minecraft.task.container.home.execution.state.StoreHomeExecutionState;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutLifecycle;

import java.util.Objects;

//20260829_kpopmodder: Added this type file to own only STORE_HOME terminal effects.
public final class StoreHomeOperationTerminator {
    private final StoreHomeExecutionState state;
    private final StoreHomeTerminalClassifier terminalClassifier;
    private final StoreHomeTerminalReporter terminalReporter;
    private final StoreHomeTimeoutDiagnostics timeoutDiagnostics;
    private final StoreHomeTimeoutLifecycle timeoutLifecycle;
    private final StoreHomeCandidateRuntimeView candidateView;
    private final StoreHomeReportingPlanCapture reportingPlanCapture;

    public StoreHomeOperationTerminator(
            StoreHomeExecutionState state,
            StoreHomeTerminalClassifier terminalClassifier,
            StoreHomeTerminalReporter terminalReporter,
            StoreHomeTimeoutDiagnostics timeoutDiagnostics,
            StoreHomeTimeoutLifecycle timeoutLifecycle,
            StoreHomeCandidateRuntimeView candidateView,
            StoreHomeReportingPlanCapture reportingPlanCapture) {
        this.state = Objects.requireNonNull(state, "state");
        this.terminalClassifier = Objects.requireNonNull(
                terminalClassifier, "terminalClassifier"
        );
        this.terminalReporter = Objects.requireNonNull(
                terminalReporter, "terminalReporter"
        );
        this.timeoutDiagnostics = Objects.requireNonNull(
                timeoutDiagnostics, "timeoutDiagnostics"
        );
        this.timeoutLifecycle = Objects.requireNonNull(
                timeoutLifecycle, "timeoutLifecycle"
        );
        this.candidateView = Objects.requireNonNull(candidateView, "candidateView");
        this.reportingPlanCapture = Objects.requireNonNull(
                reportingPlanCapture, "reportingPlanCapture"
        );
    }

    public void finishExhausted(
            Task diagnosticOwner,
            AltoClef mod,
            String reason) {
        reportingPlanCapture.capture(mod);
        finish(
                diagnosticOwner,
                mod,
                terminalClassifier.classifyExhausted(state.operation().current()),
                reason
        );
    }

    public void finish(
            Task diagnosticOwner,
            AltoClef mod,
            StoreHomeResult terminal,
            String reason) {
        if (!state.lifecycle().pending()) {
            return;
        }
        state.lifecycle().finish(terminal, reason);
        terminalReporter.report(
                mod,
                state.lifecycle().result(),
                state.operation().current(),
                state.lifecycle().terminalReason()
        );
        timeoutDiagnostics.recordTerminal(
                diagnosticOwner,
                mod,
                state.lifecycle().phase(),
                state.operation().current(),
                timeoutLifecycle.observation(),
                state.context().current(),
                state.candidateAttempt().current(),
                state.session().current(),
                candidateView.remainingCandidateCount(),
                state.lifecycle().result(),
                state.lifecycle().terminalReason()
        );
    }
}
