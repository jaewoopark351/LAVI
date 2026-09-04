package lavi.minecraft.diagnostics.container.gui.emission;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.gui.budget.ContainerGuiDiagnosticAggregateSnapshot;
import lavi.minecraft.diagnostics.container.gui.budget.ContainerGuiDiagnosticLimiter;
import lavi.minecraft.diagnostics.container.gui.budget.ContainerGuiDiagnosticLimits;
import lavi.minecraft.diagnostics.container.gui.budget.ContainerGuiEmissionDecision;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionSnapshot;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticDispatchResult;

//20260904_kpopmodder: Route bounded GUI-flow records through the existing shared session authority.
public final class ContainerGuiDiagnosticEmitter {
    private final ContainerGuiDiagnosticLimiter limiter;
    private final String suppressionEventName;

    public ContainerGuiDiagnosticEmitter(ContainerGuiDiagnosticLimiter limiter) {
        this(limiter, "EXACT_GUI_SCREEN_DISPATCH_SUPPRESSION_SUMMARY");
    }

    public ContainerGuiDiagnosticEmitter(
            ContainerGuiDiagnosticLimiter limiter,
            String suppressionEventName) {
        this.limiter = limiter;
        this.suppressionEventName = suppressionEventName;
    }

    public boolean detail(
            String eventName,
            String reason,
            String fingerprint,
            long gameTick,
            Task task,
            Object[] requiredFields,
            Object[] optionalFields) {
        ContainerGuiEmissionDecision local = limiter.evaluate(fingerprint, gameTick);
        if (local.outcome() == ContainerGuiEmissionDecision.Outcome.REPEAT_SUMMARY) {
            emitRepeatSummary(fingerprint, local.suppressedRepeatCount(), gameTick);
            return false;
        }
        if (local.outcome() != ContainerGuiEmissionDecision.Outcome.DETAIL) {
            return false;
        }
        Object[] required = ContainerGuiDiagnosticFields.merge(
                sessionFields(local.diagnosticBoundaryActivationId(), gameTick),
                requiredFields
        );
        DiagnosticDispatchResult result = ChatClefDiagnostics.logBoundedBoundaryWithDispatchResult(
                eventName,
                reason,
                task,
                ContainerGuiDiagnosticLimits.MAX_EVENT_UTF8_BYTES,
                required,
                optionalFields
        );
        limiter.recordSharedOutcome(result.admitted(), result.emissionCompleted());
        return result.emissionCompleted();
    }

    public boolean terminal(
            String eventName,
            String reason,
            long gameTick,
            Task task,
            Object[] requiredFields,
            Object[] optionalFields) {
        Object[] required = ContainerGuiDiagnosticFields.merge(
                sessionFields(limiter.activationId(), gameTick),
                requiredFields
        );
        DiagnosticDispatchResult result = ChatClefDiagnostics.logBoundedBoundaryWithDispatchResult(
                eventName,
                reason,
                task,
                ContainerGuiDiagnosticLimits.MAX_EVENT_UTF8_BYTES,
                required,
                optionalFields
        );
        return result.emissionCompleted();
    }

    public ContainerGuiDiagnosticAggregateSnapshot aggregateSnapshot() {
        return limiter.snapshot();
    }

    public ContainerGuiDiagnosticAggregateSnapshot aggregateSnapshotIfActive() {
        return limiter.snapshotIfActive();
    }

    public boolean hasActivation() {
        return limiter.hasActivation();
    }

    public String activeActivationIdOrUnavailable() {
        return limiter.activeActivationIdOrUnavailable();
    }

    public String boundaryActivationId() {
        return limiter.activationId();
    }

    private void emitRepeatSummary(String fingerprint, int repeats, long gameTick) {
        ChatClefDiagnostics.logBoundedBoundaryWithDispatchResult(
                suppressionEventName,
                "unchanged_container_gui_observations_suppressed",
                null,
                ContainerGuiDiagnosticLimits.MAX_EVENT_UTF8_BYTES,
                sessionFields(limiter.activationId(), gameTick),
                ContainerGuiDiagnosticFields.merge(new Object[]{
                        "diagnosticBudgetScope", "LOCAL_DETAIL_EXEMPT_SUPPRESSION_SUMMARY",
                        "semanticFingerprint", fingerprint,
                        "suppressedRepeatCount", repeats
                }, ContainerGuiDiagnosticFields.aggregate(limiter.snapshot()))
        );
    }

    private static Object[] sessionFields(String activationId, long gameTick) {
        DiagnosticSessionSnapshot session = ChatClefDiagnostics.diagnosticSessionSnapshot();
        return new Object[]{
                "diagnosticSessionId", session.diagnosticSessionId(),
                "diagnosticBoundaryActivationId", activationId,
                "gameTick", gameTick,
                "behavior_effect", "none"
        };
    }
}
