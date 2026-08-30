package lavi.minecraft.diagnostics.container.store.deposit.budget;

enum StoreDepositDetailFamily {
    LIFECYCLE(16),
    CANDIDATE(44),
    CHILD_RECONCILIATION(44),
    INTERACTION(44),
    CRAFT_ROUTE(44),
    CONTAINER_EFFECT(44),
    MOVEMENT(16),
    TRANSFER(44),
    SLOT_ACTION(16),
    SLOT_MUTATION(64),
    NOT_STORED(44),
    POLICY(84),
    AUTOMATIC_TERMINAL(64),
    BARITONE(44),
    CHECKPOINT(64),
    OTHER(16);

    private final int cap;

    StoreDepositDetailFamily(int cap) {
        this.cap = cap;
    }

    int cap() {
        return cap;
    }

    static StoreDepositDetailFamily forEvent(String eventName) {
        if (eventName == null) {
            return OTHER;
        }
        return switch (eventName) {
            case "STORE_TASK_LIFECYCLE_BOUNDARY" -> LIFECYCLE;
            case "STORE_CONTAINER_PARENT_CANDIDATE_DECISION",
                 "STORE_CONTAINER_FILTERED_SEARCH_RESULT",
                 "STORE_CONTAINER_PURSUIT_DECISION",
                 "STORE_CONTAINER_TARGET_CALLBACK_DECISION" -> CANDIDATE;
            case "STORE_TASK_CHILD_RECONCILIATION" -> CHILD_RECONCILIATION;
            case "CONTAINER_OPEN_ATTEMPT_OBSERVED",
                 "CONTAINER_OPEN_RETURN_OBSERVED",
                 "CONTAINER_OPEN_INTERACTION_OUTCOME_WINDOW" -> INTERACTION;
            case "STORE_CRAFT_ROUTE_EVALUATION_ENTERED" -> CRAFT_ROUTE;
            case "STORE_CONTAINER_EFFECT_OBSERVATION" -> CONTAINER_EFFECT;
            case "STORE_DEPOSIT_MOVEMENT_RESULT" -> MOVEMENT;
            case "STORE_CONTAINER_TRANSFER_DECISION",
                 "STORE_DEPOSIT_TRANSFER_BEGIN",
                 "STORE_DEPOSIT_TRANSFER_TERMINAL" -> TRANSFER;
            case "STORE_DEPOSIT_SLOT_ACTION" -> SLOT_ACTION;
            case "STORE_DEPOSIT_SLOT_MUTATION" -> SLOT_MUTATION;
            case "STORE_DEPOSIT_NOT_STORED_PROJECTION" -> NOT_STORED;
            case "AUTO_DEPOSIT_POLICY_SNAPSHOT",
                 "AUTO_DEPOSIT_POLICY_ITEM_DECISION",
                 "AUTO_DEPOSIT_POLICY_STACK_FACT" -> POLICY;
            case "STORE_DEPOSIT_AUTOMATIC_TERMINAL" -> AUTOMATIC_TERMINAL;
            case "STORE_DEPOSIT_CHECKPOINT_SUMMARY" -> CHECKPOINT;
            default -> eventName.startsWith("STORE_BARITONE_") ? BARITONE : OTHER;
        };
    }
}
