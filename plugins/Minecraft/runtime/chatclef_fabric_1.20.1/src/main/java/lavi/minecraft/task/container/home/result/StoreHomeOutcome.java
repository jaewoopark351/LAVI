package lavi.minecraft.task.container.home.result;

import lavi.minecraft.task.container.home.execution.StoreHomeResult;

import java.util.Objects;

//20260827_kpopmodder: Snapshot one typed STORE_HOME terminal outcome independently of logs.
public final class StoreHomeOutcome {
    private final StoreHomeResult result;
    private final int storedItems;
    private final int remainingStacks;
    private final String reason;

    private StoreHomeOutcome(
            StoreHomeResult result,
            int storedItems,
            int remainingStacks,
            String reason
    ) {
        this.result = Objects.requireNonNull(result, "result");
        if (result == StoreHomeResult.PENDING) {
            throw new IllegalArgumentException("terminal result must not be PENDING");
        }
        if (storedItems < 0 || remainingStacks < 0) {
            throw new IllegalArgumentException("STORE_HOME counts must be non-negative");
        }
        if (result == StoreHomeResult.COMPLETED && remainingStacks != 0) {
            throw new IllegalArgumentException(
                    "COMPLETED requires zero remaining stacks"
            );
        }
        if (result.name().startsWith("PARTIAL_") && storedItems == 0) {
            throw new IllegalArgumentException(
                    "partial results require at least one confirmed stored item"
            );
        }
        this.storedItems = storedItems;
        this.remainingStacks = remainingStacks;
        this.reason = Objects.requireNonNull(reason, "reason");
        if (reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
    }

    public static StoreHomeOutcome terminal(
            StoreHomeResult result,
            int storedItems,
            int remainingStacks,
            String reason
    ) {
        return new StoreHomeOutcome(result, storedItems, remainingStacks, reason);
    }

    public static StoreHomeOutcome cursorNotEmptyAtCommandGate() {
        return terminal(
                StoreHomeResult.CURSOR_NOT_EMPTY,
                0,
                0,
                "cursor_not_empty_at_command_gate"
        );
    }

    public StoreHomeResult result() {
        return result;
    }

    public int storedItems() {
        return storedItems;
    }

    public int remainingStacks() {
        return remainingStacks;
    }

    public String reason() {
        return reason;
    }

    public boolean goalSatisfied() {
        return result == StoreHomeResult.COMPLETED && remainingStacks == 0;
    }
}
