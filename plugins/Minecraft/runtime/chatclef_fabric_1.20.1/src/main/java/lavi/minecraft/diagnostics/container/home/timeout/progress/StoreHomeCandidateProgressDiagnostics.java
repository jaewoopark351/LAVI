package lavi.minecraft.diagnostics.container.home.timeout.progress;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.home.timeout.event.StoreHomeDiagnosticEmitter;
import lavi.minecraft.diagnostics.container.home.timeout.event.StoreHomeEventFields;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.home.execution.HomeStorageOperationContext;
import lavi.minecraft.task.container.home.execution.session.HomeStorageContainerSession;

//20260902_kpopmodder: Isolate STORE_HOME progress admission, suppression, and ordered emission.
public final class StoreHomeCandidateProgressDiagnostics {
    public boolean admitsSnapshotCapture(
            StoreHomeDiagnosticEmitter emitter,
            String candidateId) {
        return emitter.canObserveProgress(candidateId);
    }

    public boolean admitsSemanticEmission(
            StoreHomeCandidateProgressObservation observation) {
        return observation.shouldEmit();
    }

    public void recordSuppressed(
            StoreHomeDiagnosticEmitter emitter,
            String eventName) {
        emitter.recordProgressSuppressed(eventName);
    }

    public boolean emitProgress(
            StoreHomeDiagnosticEmitter emitter,
            String eventName,
            String reason,
            String candidateId,
            Task owner,
            Object[] operationFields,
            HomeStorageOperationContext context,
            AutoDepositTrustedDestinationCandidate candidate,
            StoreHomeCandidateProgressState active,
            int remainingCandidateCountIncludingCurrent,
            int candidateTicks,
            long clientTickId,
            StoreHomeProgressSnapshot snapshot,
            StoreHomeCandidateProgressObservation observation,
            int suppressedBeforeEmission,
            HomeStorageContainerSession session) {
        return emitter.emitProgress(
                eventName,
                reason,
                candidateId,
                owner,
                StoreHomeDiagnosticEmitter.merge(
                        operationFields,
                        StoreHomeEventFields.operationContext(context, candidate),
                        StoreHomeEventFields.candidate(
                                active,
                                remainingCandidateCountIncludingCurrent,
                                candidateTicks,
                                clientTickId
                        ),
                        StoreHomeEventFields.progress(
                                active,
                                snapshot,
                                observation,
                                clientTickId,
                                suppressedBeforeEmission
                        ),
                        StoreHomeEventFields.session(session)
                )
        );
    }
}
