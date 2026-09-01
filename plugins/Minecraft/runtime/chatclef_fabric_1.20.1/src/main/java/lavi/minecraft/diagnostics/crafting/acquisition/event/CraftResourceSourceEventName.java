package lavi.minecraft.diagnostics.crafting.acquisition.event;

import java.util.Objects;

/**
 * Existing authoritative events that an acquisition projection may reference.
 */
public enum CraftResourceSourceEventName {
    VISIBLE_TASK_RETURN,
    TASK_CHILD_RECONCILIATION,
    SMELT_CHILD_SELECTION_STATE,
    SMELT_MATERIAL_PROGRESS_SNAPSHOT,
    FURNACE_OPERATION_GATE_TRANSITION,
    FURNACE_CONTAINER_ROUTE_TRANSITION,
    MINE_TARGET_SELECTION_TRANSITION,
    MINE_TARGET_GOAL_REQUEST,
    MINE_TARGET_ABANDONED,
    CONTAINER_TASK_TARGET_DECISION,
    CONTAINER_TASK_BRANCH,
    CONTAINER_TASK_CHILD_RECONCILIATION,
    CONTAINER_TASK_OWNER_STOP,
    CRAFTING_TABLE_ROUTE_RETRY_SUMMARY,
    CONTAINER_OPEN_ATTEMPT_OBSERVED,
    CONTAINER_OPEN_RETURN_OBSERVED;

    public static CraftResourceSourceEventName requireExact(String sourceEventName) {
        Objects.requireNonNull(sourceEventName, "sourceEventName");
        try {
            return valueOf(sourceEventName);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException(
                    "Unsupported acquisition source event: " + sourceEventName,
                    error
            );
        }
    }
}
