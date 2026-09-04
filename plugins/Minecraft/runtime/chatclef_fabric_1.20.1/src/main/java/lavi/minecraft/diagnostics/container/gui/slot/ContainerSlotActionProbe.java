package lavi.minecraft.diagnostics.container.gui.slot;

//20260904_kpopmodder: Close one synchronous slot observation without changing controller exceptions.
public final class ContainerSlotActionProbe {
    private static final ContainerSlotActionProbe NOOP = new ContainerSlotActionProbe(null, null);

    private final ContainerSlotFlowDiagnostics diagnostics;
    private final ContainerSlotActionObservation observation;

    private ContainerSlotActionProbe(
            ContainerSlotFlowDiagnostics diagnostics,
            ContainerSlotActionObservation observation) {
        this.diagnostics = diagnostics;
        this.observation = observation;
    }

    public static ContainerSlotActionProbe active(
            ContainerSlotFlowDiagnostics diagnostics,
            ContainerSlotActionObservation observation) {
        return diagnostics == null || observation == null
                ? NOOP
                : new ContainerSlotActionProbe(diagnostics, observation);
    }

    public static ContainerSlotActionProbe noop() {
        return NOOP;
    }

    public void returned() {
        safely(() -> diagnostics.onReturned(observation));
    }

    public void failed(Throwable failure) {
        safely(() -> diagnostics.onFailed(observation, failure));
    }

    private void safely(Runnable action) {
        if (diagnostics == null || action == null) {
            return;
        }
        try {
            action.run();
        } catch (RuntimeException | LinkageError ignored) {
        }
    }
}
