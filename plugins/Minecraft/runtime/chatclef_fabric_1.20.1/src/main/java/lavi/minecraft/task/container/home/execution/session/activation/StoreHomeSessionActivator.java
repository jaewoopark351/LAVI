package lavi.minecraft.task.container.home.execution.session.activation;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.home.StoreHomeManifestStaleDiagnostics;
import lavi.minecraft.diagnostics.container.home.timeout.StoreHomeTimeoutDiagnostics;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import lavi.minecraft.task.container.home.execution.StoreHomeResult;
import lavi.minecraft.task.container.home.execution.candidate.view.StoreHomeCandidateRuntimeView;
import lavi.minecraft.task.container.home.execution.operation.terminal.StoreHomeOperationTerminator;
import lavi.minecraft.task.container.home.execution.session.HomeStorageActivationBaseline;
import lavi.minecraft.task.container.home.execution.session.HomeStorageContainerActivation;
import lavi.minecraft.task.container.home.execution.session.HomeStorageContainerSession;
import lavi.minecraft.task.container.home.execution.state.StoreHomeExecutionState;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutLifecycle;
import lavi.minecraft.task.container.home.planning.HomeLoadoutPlanner;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySnapshot;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySnapshotReader;
import lavi.minecraft.task.container.home.planning.HomeStoragePlan;

import java.util.Objects;
import java.util.Optional;

//20260829_kpopmodder: Own creation of one exact trusted container session.
public final class StoreHomeSessionActivator {
    private final StoreHomeExecutionState state;
    private final HomeStorageInventorySnapshotReader snapshotReader;
    private final HomeLoadoutPlanner planner;
    private final StoreHomeManifestStaleDiagnostics manifestStaleDiagnostics;
    private final StoreHomeTimeoutDiagnostics timeoutDiagnostics;
    private final StoreHomeTimeoutLifecycle timeoutLifecycle;
    private final StoreHomeCandidateRuntimeView candidateView;
    private final StoreHomeOperationTerminator terminator;

    public StoreHomeSessionActivator(
            StoreHomeExecutionState state,
            HomeStorageInventorySnapshotReader snapshotReader,
            HomeLoadoutPlanner planner,
            StoreHomeManifestStaleDiagnostics manifestStaleDiagnostics,
            StoreHomeTimeoutDiagnostics timeoutDiagnostics,
            StoreHomeTimeoutLifecycle timeoutLifecycle,
            StoreHomeCandidateRuntimeView candidateView,
            StoreHomeOperationTerminator terminator) {
        this.state = Objects.requireNonNull(state, "state");
        this.snapshotReader = Objects.requireNonNull(snapshotReader, "snapshotReader");
        this.planner = Objects.requireNonNull(planner, "planner");
        this.manifestStaleDiagnostics = Objects.requireNonNull(
                manifestStaleDiagnostics, "manifestStaleDiagnostics"
        );
        this.timeoutDiagnostics = Objects.requireNonNull(
                timeoutDiagnostics, "timeoutDiagnostics"
        );
        this.timeoutLifecycle = Objects.requireNonNull(
                timeoutLifecycle, "timeoutLifecycle"
        );
        this.candidateView = Objects.requireNonNull(candidateView, "candidateView");
        this.terminator = Objects.requireNonNull(terminator, "terminator");
    }

    public void activate(
            Task diagnosticOwner,
            AltoClef mod,
            AutoDepositTrustedDestinationCandidate candidate,
            HomeStorageContainerActivation activation) {
        state.lifecycle().transitionTo(StoreHomePhase.PLAN_LOADOUT);
        Optional<HomeStorageInventorySnapshot> captured = snapshotReader.capture(mod);
        if (captured.isEmpty()) {
            terminator.finish(
                    diagnosticOwner,
                    mod,
                    StoreHomeResult.MANIFEST_STALE,
                    "activation_inventory_snapshot_unavailable"
            );
            return;
        }
        HomeStorageInventorySnapshot inventory = captured.orElseThrow();
        HomeStoragePlan plan = planner.plan(inventory);
        HomeStorageActivationBaseline baseline = new HomeStorageActivationBaseline(
                inventory, plan
        );
        int ordinal = state.operation().current().nextContainerSessionOrdinal();
        state.session().activate(HomeStorageContainerSession.activate(
                candidate, ordinal, baseline, activation
        ));
        timeoutLifecycle.markCandidateActivated();
        state.operation().replace(state.operation().current().activatedSession(
                state.session().current().progress().remainingStackCount()
        ));
        state.publishedPlan().publish(plan);
        manifestStaleDiagnostics.captureSessionBaseline(
                mod, ordinal, plan, state.context().current()
        );
        timeoutDiagnostics.recordCandidateActivated(
                diagnosticOwner,
                mod,
                state.lifecycle().phase(),
                state.operation().current(),
                timeoutLifecycle.observation(),
                state.context().current(),
                state.candidateAttempt().current(),
                state.session().current(),
                candidateView.remainingCandidateCount()
        );
        if (state.session().current().progress().complete()) {
            terminator.finish(
                    diagnosticOwner,
                    mod,
                    StoreHomeResult.COMPLETED,
                    "nothing_to_store_at_trusted_container_activation"
            );
        }
    }
}
