package lavi.minecraft.diagnostics.container.gui.lifecycle;

//20260904_kpopmodder: Classify an observed root ending without controlling Task completion.
public final class ContainerGuiRootTerminalDecision {
    private ContainerGuiRootTerminalDecision() {
    }

    public static String reason(
            boolean actuallyDone,
            String frozenRootAssignmentId,
            String currentRootAssignmentId,
            String finishTriggerHint) {
        if (!actuallyDone && same(frozenRootAssignmentId, currentRootAssignmentId)) {
            return null;
        }
        if (!actuallyDone) {
            return "USER_TASK_ROOT_REPLACED_AFTER_ON_FINISH_CALLBACK";
        }
        return "cancel_after_chain_stop".equals(finishTriggerHint)
                ? "USER_TASK_CANCELLED"
                : "USER_TASK_FINISHED";
    }

    private static boolean same(String first, String second) {
        return first != null && first.equals(second);
    }
}
