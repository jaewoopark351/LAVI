package lavi.minecraft.task.container.home.execution.operation;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.container.home.execution.StoreHomeResult;

import java.util.Objects;

//20260828_kpopmodder: Render one terminal operation summary without owning terminal policy.
public final class StoreHomeTerminalReporter {
    public void report(
            AltoClef mod,
            StoreHomeResult result,
            StoreHomeOperationProgress progress,
            String reason) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(reason, "reason");
        if (mod == null) {
            return;
        }
        String message = "Store home: result=" + result
                + ", storedItems=" + progress.storedItems()
                + ", touchedStacks=" + progress.touchedStackCount()
                + ", remainingStacks=" + progress.latestRemainingStacks()
                + ", reason=" + reason;
        if (result == StoreHomeResult.COMPLETED) {
            mod.log(message);
        } else {
            mod.logWarning(message);
        }
    }
}
