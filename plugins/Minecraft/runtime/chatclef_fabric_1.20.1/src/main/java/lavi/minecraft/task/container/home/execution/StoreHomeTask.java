package lavi.minecraft.task.container.home.execution;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.home.StoreHomeManifestStaleDiagnostics;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositWorldKeyReader;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.execution.candidate.StoreHomeCandidateValidator;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeOperationProgress;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeTerminalClassifier;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeTerminalReporter;
import lavi.minecraft.task.container.home.execution.session.HomeStorageConfirmedTransferCommitter;
import lavi.minecraft.task.container.home.execution.session.HomeStorageContainerActivationGate;
import lavi.minecraft.task.container.home.execution.session.HomeStorageManifestValidator;
import lavi.minecraft.task.container.home.execution.state.StoreHomeExecutionState;
import lavi.minecraft.task.container.home.execution.task.composition.StoreHomeTaskAssembly;
import lavi.minecraft.task.container.home.execution.task.composition.StoreHomeTaskDependencies;
import lavi.minecraft.task.container.home.execution.task.lifecycle.StoreHomeTaskLifecycleController;
import lavi.minecraft.task.container.home.execution.task.view.StoreHomeTaskView;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutLifecycle;
import lavi.minecraft.task.container.home.planning.HomeLoadoutPlanner;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySnapshotReader;
import lavi.minecraft.task.container.home.planning.HomeStoragePlan;
import lavi.minecraft.task.container.home.result.StoreHomeOutcome;

import java.util.Objects;
import java.util.Optional;

//20260827_kpopmodder: Orchestrate one explicit trusted-only home-storage root Task.
//20260829_kpopmodder: Reduced this type to the stable Task facade after folderizing its collaborators.
public final class StoreHomeTask extends Task {
    private final StoreHomeTimeoutLifecycle timeoutLifecycle =
            StoreHomeTimeoutLifecycle.standard();
    private final StoreHomeExecutionState state;
    private final StoreHomeTaskLifecycleController lifecycleController;
    private final StoreHomeTaskView view;

    public StoreHomeTask(
            AutoDepositTrustedDestinationRepository repository,
            AutoDepositExactOpenContainerBinding exactOpenContainerBinding,
            AutoDepositWorldKeyReader worldKeyReader,
            HomeStorageInventorySnapshotReader snapshotReader,
            HomeLoadoutPlanner planner,
            HomeStorageDestinationSelector destinationSelector,
            HomeStorageScreenSlotResolver slotResolver,
            HomeStorageTransferExecutor transferExecutor,
            StoreHomeManifestStaleDiagnostics manifestStaleDiagnostics) {
        this(StoreHomeTaskAssembly.defaultDependencies(
                repository,
                exactOpenContainerBinding,
                worldKeyReader,
                snapshotReader,
                planner,
                destinationSelector,
                slotResolver,
                transferExecutor,
                manifestStaleDiagnostics
        ));
    }

    public StoreHomeTask(
            AutoDepositExactOpenContainerBinding exactOpenContainerBinding,
            AutoDepositWorldKeyReader worldKeyReader,
            HomeStorageInventorySnapshotReader snapshotReader,
            HomeLoadoutPlanner planner,
            HomeStorageDestinationSelector destinationSelector,
            HomeStorageScreenSlotResolver slotResolver,
            HomeStorageTransferExecutor transferExecutor,
            StoreHomeManifestStaleDiagnostics manifestStaleDiagnostics,
            StoreHomeCandidateValidator candidateValidator,
            HomeStorageContainerActivationGate activationGate,
            HomeStorageManifestValidator manifestValidator,
            HomeStorageConfirmedTransferCommitter transferCommitter,
            StoreHomeTerminalClassifier terminalClassifier,
            StoreHomeTerminalReporter terminalReporter,
            StoreHomeOperationProgress operation) {
        this(new StoreHomeTaskDependencies(
                exactOpenContainerBinding,
                worldKeyReader,
                snapshotReader,
                planner,
                destinationSelector,
                slotResolver,
                transferExecutor,
                manifestStaleDiagnostics,
                candidateValidator,
                activationGate,
                manifestValidator,
                transferCommitter,
                terminalClassifier,
                terminalReporter,
                operation
        ));
    }

    private StoreHomeTask(StoreHomeTaskDependencies dependencies) {
        StoreHomeTaskDependencies checkedDependencies = Objects.requireNonNull(
                dependencies, "dependencies"
        );
        state = new StoreHomeExecutionState(checkedDependencies.operation());
        lifecycleController = StoreHomeTaskAssembly.assembleLifecycle(
                checkedDependencies, state, timeoutLifecycle
        );
        view = new StoreHomeTaskView(state);
    }

    @Override
    protected void onStart() {
        lifecycleController.onStart(this);
    }

    @Override
    protected Task onTick() {
        return lifecycleController.onTick(this);
    }

    @Override
    protected void onStop(Task interruptTask) {
        lifecycleController.onStop(this, interruptTask);
    }

    @Override
    public boolean isFinished() {
        return view.finished();
    }

    @Override
    protected boolean isEqual(Task other) {
        return this == other;
    }

    @Override
    protected String toDebugString() {
        return view.debugString();
    }

    public StoreHomeResult result() {
        return view.result();
    }

    public StoreHomePhase phase() {
        return view.phase();
    }

    public HomeStoragePlan plan() {
        return view.plan();
    }

    //20260827_kpopmodder: Preserve the immutable terminal snapshot bridge API.
    public Optional<StoreHomeOutcome> outcome() {
        return view.outcome();
    }
}
