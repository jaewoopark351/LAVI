package lavi.minecraft.task.container.home.execution.operation.reporting;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositWorldKeyReader;
import lavi.minecraft.task.container.home.execution.HomeStorageTransferExecutor;
import lavi.minecraft.task.container.home.execution.context.HomeStorageCursorStateReader;
import lavi.minecraft.task.container.home.execution.state.StoreHomeExecutionState;
import lavi.minecraft.task.container.home.planning.HomeLoadoutPlanner;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySnapshotReader;
import lavi.minecraft.task.container.home.planning.HomeStoragePlan;

import java.util.Objects;

//20260829_kpopmodder: Added this type file to capture only the terminal reporting plan.
public final class StoreHomeReportingPlanCapture {
    private final StoreHomeExecutionState state;
    private final HomeStorageTransferExecutor transferExecutor;
    private final AutoDepositWorldKeyReader worldKeyReader;
    private final HomeStorageCursorStateReader cursorStateReader;
    private final HomeStorageInventorySnapshotReader snapshotReader;
    private final HomeLoadoutPlanner planner;

    public StoreHomeReportingPlanCapture(
            StoreHomeExecutionState state,
            HomeStorageTransferExecutor transferExecutor,
            AutoDepositWorldKeyReader worldKeyReader,
            HomeStorageCursorStateReader cursorStateReader,
            HomeStorageInventorySnapshotReader snapshotReader,
            HomeLoadoutPlanner planner) {
        this.state = Objects.requireNonNull(state, "state");
        this.transferExecutor = Objects.requireNonNull(
                transferExecutor, "transferExecutor"
        );
        this.worldKeyReader = Objects.requireNonNull(
                worldKeyReader, "worldKeyReader"
        );
        this.cursorStateReader = Objects.requireNonNull(
                cursorStateReader, "cursorStateReader"
        );
        this.snapshotReader = Objects.requireNonNull(snapshotReader, "snapshotReader");
        this.planner = Objects.requireNonNull(planner, "planner");
    }

    public void capture(AltoClef mod) {
        if (state.context().current() == null
                || transferExecutor.hasPending()
                || !state.context().current().matches(mod, worldKeyReader)
                || !cursorStateReader.isEmpty(mod)) {
            return;
        }
        snapshotReader.capture(mod).ifPresent(snapshot -> {
            HomeStoragePlan reportingOnly = planner.plan(snapshot);
            state.publishedPlan().publish(reportingOnly);
            state.operation().replace(
                    state.operation().current().withLatestRemainingStacks(
                            reportingOnly.manifest().steps().size()
                    )
            );
        });
    }
}
