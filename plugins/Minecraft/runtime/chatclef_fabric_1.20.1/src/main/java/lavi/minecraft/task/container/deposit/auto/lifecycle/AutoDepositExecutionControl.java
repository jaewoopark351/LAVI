package lavi.minecraft.task.container.deposit.auto.lifecycle;

//20260914_kpopmodder: Keep explicit runner stop distinct from initial idle and native safety suspension.
public final class AutoDepositExecutionControl {
    private boolean stopped;

    //20260915_openai: The native runner is independent of the legacy LAVI(old) AI bridge toggle.
    public static boolean automationAvailable(boolean legacyBridgeEnabled, boolean runnerActive) {
        return legacyBridgeEnabled || runnerActive;
    }

    public void stop() { stopped = true; }

    /** A native command's runner.enable is the existing explicit resume boundary. */
    public boolean permitsExecution(boolean runnerActive) {
        if (stopped && runnerActive) stopped = false;
        return !stopped;
    }

    public boolean stopped() { return stopped; }
}
