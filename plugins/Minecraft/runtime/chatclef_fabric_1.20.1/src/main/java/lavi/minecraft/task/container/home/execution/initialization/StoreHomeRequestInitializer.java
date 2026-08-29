package lavi.minecraft.task.container.home.execution.initialization;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.home.timeout.StoreHomeTimeoutDiagnostics;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositWorldKeyReader;
import lavi.minecraft.task.container.deposit.auto.trusted.execution.AutoDepositTrustedCandidateQueue;
import lavi.minecraft.task.container.home.execution.HomeStorageDestinationSelector;
import lavi.minecraft.task.container.home.execution.HomeStorageOperationContext;
import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import lavi.minecraft.task.container.home.execution.StoreHomeResult;
import lavi.minecraft.task.container.home.execution.context.HomeStorageCursorStateReader;
import lavi.minecraft.task.container.home.execution.operation.terminal.StoreHomeOperationTerminator;
import lavi.minecraft.task.container.home.execution.state.StoreHomeExecutionState;
import lavi.minecraft.task.container.home.planning.HomeLoadoutPlanner;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySnapshot;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySnapshotReader;
import lavi.minecraft.task.container.home.planning.HomeStoragePlan;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

//20260829_kpopmodder: Own request acceptance, preflight, and candidate catalog initialization.
public final class StoreHomeRequestInitializer {
    private final StoreHomeExecutionState state;
    private final HomeStorageCursorStateReader cursorStateReader;
    private final AutoDepositWorldKeyReader worldKeyReader;
    private final HomeStorageInventorySnapshotReader snapshotReader;
    private final HomeLoadoutPlanner planner;
    private final HomeStorageDestinationSelector destinationSelector;
    private final StoreHomeTimeoutDiagnostics timeoutDiagnostics;
    private final StoreHomeOperationTerminator terminator;

    public StoreHomeRequestInitializer(
            StoreHomeExecutionState state,
            HomeStorageCursorStateReader cursorStateReader,
            AutoDepositWorldKeyReader worldKeyReader,
            HomeStorageInventorySnapshotReader snapshotReader,
            HomeLoadoutPlanner planner,
            HomeStorageDestinationSelector destinationSelector,
            StoreHomeTimeoutDiagnostics timeoutDiagnostics,
            StoreHomeOperationTerminator terminator) {
        this.state = Objects.requireNonNull(state, "state");
        this.cursorStateReader = Objects.requireNonNull(
                cursorStateReader, "cursorStateReader"
        );
        this.worldKeyReader = Objects.requireNonNull(
                worldKeyReader, "worldKeyReader"
        );
        this.snapshotReader = Objects.requireNonNull(snapshotReader, "snapshotReader");
        this.planner = Objects.requireNonNull(planner, "planner");
        this.destinationSelector = Objects.requireNonNull(
                destinationSelector, "destinationSelector"
        );
        this.timeoutDiagnostics = Objects.requireNonNull(
                timeoutDiagnostics, "timeoutDiagnostics"
        );
        this.terminator = Objects.requireNonNull(terminator, "terminator");
    }

    public void initialize(Task diagnosticOwner, AltoClef mod) {
        state.lifecycle().transitionTo(StoreHomePhase.ACCEPT_REQUEST);
        if (!cursorStateReader.isEmpty(mod)) {
            terminator.finish(
                    diagnosticOwner,
                    mod,
                    StoreHomeResult.CURSOR_NOT_EMPTY,
                    "cursor_not_empty_at_acceptance"
            );
            return;
        }
        state.lifecycle().transitionTo(StoreHomePhase.SNAPSHOT_CONTEXT);
        Optional<HomeStorageOperationContext> captured =
                HomeStorageOperationContext.capture(mod, worldKeyReader);
        if (captured.isEmpty()) {
            terminator.finish(
                    diagnosticOwner,
                    mod,
                    StoreHomeResult.CONTEXT_CHANGED,
                    "world_context_unavailable"
            );
            return;
        }
        state.context().capture(captured.orElseThrow());

        state.lifecycle().transitionTo(StoreHomePhase.PRECHECK_CURRENT_SURPLUS);
        Optional<HomeStorageInventorySnapshot> preflightSnapshot =
                snapshotReader.capture(mod);
        if (preflightSnapshot.isEmpty()) {
            terminator.finish(
                    diagnosticOwner,
                    mod,
                    StoreHomeResult.CONTEXT_CHANGED,
                    "inventory_snapshot_unavailable_at_acceptance"
            );
            return;
        }
        HomeStoragePlan advisory = planner.plan(preflightSnapshot.orElseThrow());
        state.operation().replace(state.operation().current().withLatestRemainingStacks(
                advisory.manifest().steps().size()
        ));
        if (advisory.manifest().steps().isEmpty()) {
            state.publishedPlan().publish(advisory);
            state.lifecycle().markInitialized();
            terminator.finish(
                    diagnosticOwner,
                    mod,
                    StoreHomeResult.COMPLETED,
                    "nothing_to_store"
            );
            return;
        }

        state.lifecycle().transitionTo(StoreHomePhase.BUILD_DESTINATION_QUEUE);
        List<AutoDepositTrustedDestinationCandidate> candidates =
                destinationSelector.snapshot(mod, state.context().current());
        state.candidateQueue().install(
                new AutoDepositTrustedCandidateQueue(candidates)
        );
        timeoutDiagnostics.recordCandidateCatalog(candidates.size());
        state.lifecycle().markInitialized();
    }
}
