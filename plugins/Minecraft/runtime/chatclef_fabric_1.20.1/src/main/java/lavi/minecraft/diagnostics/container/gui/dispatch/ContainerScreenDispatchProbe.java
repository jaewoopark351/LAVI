package lavi.minecraft.diagnostics.container.gui.dispatch;

import lavi.minecraft.diagnostics.container.gui.screen.ContainerScreenTransportDiagnostics;

//20260904_kpopmodder: Keep EventBus observer calls nonthrowing and method-local.
public final class ContainerScreenDispatchProbe {
    private static final ContainerScreenDispatchProbe NOOP =
            new ContainerScreenDispatchProbe(null, null);

    private final ContainerScreenTransportDiagnostics diagnostics;
    private final ContainerScreenDispatchObservation observation;

    private ContainerScreenDispatchProbe(
            ContainerScreenTransportDiagnostics diagnostics,
            ContainerScreenDispatchObservation observation) {
        this.diagnostics = diagnostics;
        this.observation = observation;
    }

    public static ContainerScreenDispatchProbe active(
            ContainerScreenTransportDiagnostics diagnostics,
            ContainerScreenDispatchObservation observation) {
        if (diagnostics == null || observation == null) {
            return NOOP;
        }
        return new ContainerScreenDispatchProbe(diagnostics, observation);
    }

    public static ContainerScreenDispatchProbe noop() {
        return NOOP;
    }

    public boolean isActive() {
        return diagnostics != null;
    }

    public void listenerStarted(String listenerClass, String listenerIdentity) {
        safely(() -> diagnostics.onListenerStarted(observation, listenerClass, listenerIdentity));
    }

    public void listenerCompleted(String listenerClass, String listenerIdentity) {
        safely(() -> diagnostics.onListenerCompleted(observation, listenerClass, listenerIdentity));
    }

    public void listenerSkippedInactive(String listenerClass, String listenerIdentity) {
        safely(() -> diagnostics.onListenerSkippedInactive(observation, listenerClass, listenerIdentity));
    }

    public void listenerClassCastFailed(String listenerClass, String listenerIdentity) {
        safely(() -> diagnostics.onListenerClassCastFailed(observation, listenerClass, listenerIdentity));
    }

    public void dispatchCompleted() {
        safely(() -> diagnostics.onDispatchCompleted(observation));
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
