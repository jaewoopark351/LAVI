package lavi.minecraft.task.container.home.execution.transfer.click;

//20260829_kpopmodder: Carry one immutable exact quick-move issue outcome.
public record HomeStorageQuickMoveOutcome(
        boolean issued,
        String reason,
        String exceptionClass) {
    public static HomeStorageQuickMoveOutcome success() {
        return new HomeStorageQuickMoveOutcome(
                true,
                "quick_move_requested",
                "none"
        );
    }

    public static HomeStorageQuickMoveOutcome failure(String reason) {
        return failure(reason, "RuntimeException");
    }

    public static HomeStorageQuickMoveOutcome failure(
            String reason,
            String exceptionClass) {
        return new HomeStorageQuickMoveOutcome(false, reason, exceptionClass);
    }
}
