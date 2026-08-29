package lavi.minecraft.task.container.home.execution.task.composition;

import lavi.minecraft.diagnostics.container.home.StoreHomeManifestStaleDiagnostics;
import lavi.minecraft.diagnostics.container.home.timeout.StoreHomeTimeoutDiagnostics;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositWorldKeyReader;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.execution.HomeStorageDestinationSelector;
import lavi.minecraft.task.container.home.execution.HomeStorageScreenSlotResolver;
import lavi.minecraft.task.container.home.execution.HomeStorageTransferExecutor;
import lavi.minecraft.task.container.home.execution.candidate.StoreHomeCandidateValidator;
import lavi.minecraft.task.container.home.execution.candidate.navigation.StoreHomeCandidateNavigationStep;
import lavi.minecraft.task.container.home.execution.candidate.rejection.StoreHomeCandidateRejector;
import lavi.minecraft.task.container.home.execution.candidate.selection.StoreHomeCandidateAttemptStarter;
import lavi.minecraft.task.container.home.execution.candidate.view.StoreHomeCandidateRuntimeView;
import lavi.minecraft.task.container.home.execution.context.HomeStorageCursorStateReader;
import lavi.minecraft.task.container.home.execution.initialization.StoreHomeRequestInitializer;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeOperationProgress;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeTerminalClassifier;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeTerminalReporter;
import lavi.minecraft.task.container.home.execution.operation.pending.StoreHomePendingOwnershipGuard;
import lavi.minecraft.task.container.home.execution.operation.reporting.StoreHomeReportingPlanCapture;
import lavi.minecraft.task.container.home.execution.operation.terminal.StoreHomeOperationTerminator;
import lavi.minecraft.task.container.home.execution.session.HomeStorageConfirmedTransferCommitter;
import lavi.minecraft.task.container.home.execution.session.HomeStorageContainerActivationGate;
import lavi.minecraft.task.container.home.execution.session.HomeStorageManifestValidator;
import lavi.minecraft.task.container.home.execution.session.activation.StoreHomeSessionActivator;
import lavi.minecraft.task.container.home.execution.session.flow.StoreHomeSessionStep;
import lavi.minecraft.task.container.home.execution.session.transfer.StoreHomeTransferResultHandler;
import lavi.minecraft.task.container.home.execution.state.StoreHomeExecutionState;
import lavi.minecraft.task.container.home.execution.task.lifecycle.StoreHomeTaskLifecycleController;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutLifecycle;
import lavi.minecraft.task.container.home.execution.timeout.candidate.StoreHomeCandidateTimeoutObserver;
import lavi.minecraft.task.container.home.execution.timeout.decision.StoreHomeTimeoutDecisionApplier;
import lavi.minecraft.task.container.home.planning.HomeLoadoutPlanner;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySnapshotReader;

import java.util.Objects;

//20260829_kpopmodder: Assemble the StoreHomeTask object graph without owning Task behavior.
public final class StoreHomeTaskAssembly {
    private StoreHomeTaskAssembly() {
    }

    public static StoreHomeTaskDependencies defaultDependencies(
            AutoDepositTrustedDestinationRepository repository,
            AutoDepositExactOpenContainerBinding exactOpenContainerBinding,
            AutoDepositWorldKeyReader worldKeyReader,
            HomeStorageInventorySnapshotReader snapshotReader,
            HomeLoadoutPlanner planner,
            HomeStorageDestinationSelector destinationSelector,
            HomeStorageScreenSlotResolver slotResolver,
            HomeStorageTransferExecutor transferExecutor,
            StoreHomeManifestStaleDiagnostics manifestStaleDiagnostics) {
        return new StoreHomeTaskDependencies(
                exactOpenContainerBinding,
                worldKeyReader,
                snapshotReader,
                planner,
                destinationSelector,
                slotResolver,
                transferExecutor,
                manifestStaleDiagnostics,
                new StoreHomeCandidateValidator(repository),
                new HomeStorageContainerActivationGate(
                        repository, exactOpenContainerBinding, worldKeyReader
                ),
                new HomeStorageManifestValidator(),
                new HomeStorageConfirmedTransferCommitter(),
                new StoreHomeTerminalClassifier(),
                new StoreHomeTerminalReporter(),
                StoreHomeOperationProgress.start(
                        manifestStaleDiagnostics.operationId()
                )
        );
    }

    public static StoreHomeTaskLifecycleController assembleLifecycle(
            StoreHomeTaskDependencies dependencies,
            StoreHomeExecutionState state,
            StoreHomeTimeoutLifecycle timeoutLifecycle) {
        StoreHomeTaskDependencies checkedDependencies = Objects.requireNonNull(
                dependencies, "dependencies"
        );
        StoreHomeExecutionState checkedState = Objects.requireNonNull(
                state, "state"
        );
        StoreHomeTimeoutLifecycle checkedTimeoutLifecycle = Objects.requireNonNull(
                timeoutLifecycle, "timeoutLifecycle"
        );

        StoreHomeTimeoutDiagnostics timeoutDiagnostics =
                new StoreHomeTimeoutDiagnostics(
                        checkedState.operation().current().operationId(),
                        checkedTimeoutLifecycle.policy(),
                        checkedDependencies.exactOpenContainerBinding(),
                        checkedDependencies.transferExecutor()
                );
        HomeStorageCursorStateReader cursorStateReader =
                new HomeStorageCursorStateReader();
        StoreHomeCandidateRuntimeView candidateView =
                new StoreHomeCandidateRuntimeView(checkedState);
        StoreHomePendingOwnershipGuard pendingGuard =
                new StoreHomePendingOwnershipGuard(
                        checkedState,
                        checkedDependencies.transferExecutor()
                );
        StoreHomeReportingPlanCapture reportingPlanCapture =
                new StoreHomeReportingPlanCapture(
                        checkedState,
                        checkedDependencies.transferExecutor(),
                        checkedDependencies.worldKeyReader(),
                        cursorStateReader,
                        checkedDependencies.snapshotReader(),
                        checkedDependencies.planner()
                );
        StoreHomeOperationTerminator terminator =
                new StoreHomeOperationTerminator(
                        checkedState,
                        checkedDependencies.terminalClassifier(),
                        checkedDependencies.terminalReporter(),
                        timeoutDiagnostics,
                        checkedTimeoutLifecycle,
                        candidateView,
                        reportingPlanCapture
                );
        StoreHomeRequestInitializer initializer =
                new StoreHomeRequestInitializer(
                        checkedState,
                        cursorStateReader,
                        checkedDependencies.worldKeyReader(),
                        checkedDependencies.snapshotReader(),
                        checkedDependencies.planner(),
                        checkedDependencies.destinationSelector(),
                        timeoutDiagnostics,
                        terminator
                );
        StoreHomeCandidateAttemptStarter candidateStarter =
                new StoreHomeCandidateAttemptStarter(
                        checkedState,
                        checkedDependencies.candidateValidator(),
                        checkedTimeoutLifecycle,
                        timeoutDiagnostics,
                        candidateView
                );
        StoreHomeCandidateRejector candidateRejector =
                new StoreHomeCandidateRejector(
                        checkedState,
                        pendingGuard,
                        terminator,
                        checkedTimeoutLifecycle,
                        timeoutDiagnostics,
                        candidateView
                );
        StoreHomeSessionActivator sessionActivator =
                new StoreHomeSessionActivator(
                        checkedState,
                        checkedDependencies.snapshotReader(),
                        checkedDependencies.planner(),
                        checkedDependencies.manifestStaleDiagnostics(),
                        timeoutDiagnostics,
                        checkedTimeoutLifecycle,
                        candidateView,
                        terminator
                );
        StoreHomeCandidateNavigationStep navigationStep =
                new StoreHomeCandidateNavigationStep(
                        checkedState,
                        checkedDependencies.exactOpenContainerBinding(),
                        checkedDependencies.candidateValidator(),
                        checkedDependencies.activationGate(),
                        checkedDependencies.transferExecutor(),
                        checkedTimeoutLifecycle,
                        candidateRejector,
                        sessionActivator,
                        terminator
                );
        StoreHomeTransferResultHandler transferResultHandler =
                new StoreHomeTransferResultHandler(
                        checkedState,
                        checkedDependencies.transferExecutor(),
                        checkedDependencies.transferCommitter(),
                        checkedTimeoutLifecycle,
                        checkedDependencies.manifestStaleDiagnostics(),
                        checkedDependencies.exactOpenContainerBinding(),
                        pendingGuard,
                        candidateRejector,
                        terminator
                );
        StoreHomeSessionStep sessionStep = new StoreHomeSessionStep(
                checkedState,
                checkedDependencies.activationGate(),
                checkedDependencies.snapshotReader(),
                checkedDependencies.manifestValidator(),
                checkedDependencies.manifestStaleDiagnostics(),
                checkedDependencies.slotResolver(),
                checkedDependencies.transferExecutor(),
                checkedDependencies.exactOpenContainerBinding(),
                candidateRejector,
                terminator,
                transferResultHandler
        );
        StoreHomeCandidateTimeoutObserver candidateTimeoutObserver =
                new StoreHomeCandidateTimeoutObserver(
                        checkedState,
                        checkedDependencies.exactOpenContainerBinding(),
                        checkedTimeoutLifecycle
                );
        StoreHomeTimeoutDecisionApplier timeoutDecisionApplier =
                new StoreHomeTimeoutDecisionApplier(
                        checkedState,
                        pendingGuard,
                        terminator,
                        checkedTimeoutLifecycle,
                        timeoutDiagnostics,
                        candidateView,
                        candidateRejector
                );

        return new StoreHomeTaskLifecycleController(
                checkedDependencies.worldKeyReader(),
                checkedTimeoutLifecycle,
                timeoutDiagnostics,
                checkedState,
                cursorStateReader,
                pendingGuard,
                terminator,
                initializer,
                candidateStarter,
                candidateTimeoutObserver,
                navigationStep,
                sessionStep,
                timeoutDecisionApplier,
                candidateView
        );
    }
}
