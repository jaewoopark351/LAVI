package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.Subscription;
import adris.altoclef.eventbus.events.TaskFinishedEvent;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

//20260803_kpopmodder: Queue user task finish observations outside the WebSocket transport path.
public final class FabricChatClefUserTaskFinishedObserver {
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final ConcurrentLinkedQueue<FabricChatClefCommandTerminationObservation> observations = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private Subscription<TaskFinishedEvent> subscription;

    public FabricChatClefUserTaskFinishedObserver(FabricChatClefBridgeDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    public void register() {
        if (!registered.compareAndSet(false, true)) {
            return;
        }
        subscription = EventBus.subscribe(TaskFinishedEvent.class, this::onTaskFinished);
        diagnostics.info("registered ChatClef TaskFinishedEvent observer");
    }

    public FabricChatClefCommandTerminationObservation poll() {
        return observations.poll();
    }

    private void onTaskFinished(TaskFinishedEvent event) {
        try {
            FabricChatClefCommandTerminationObservation observation =
                    FabricChatClefCommandTerminationObservation.fromTaskFinishedEvent(event);
            observations.offer(observation);
            diagnostics.info("queued TaskFinishedEvent observation data=" + observation.toMap());
        } catch (Throwable error) {
            diagnostics.warn(
                    "TaskFinishedEvent observation failed "
                            + error.getClass().getSimpleName()
                            + ": "
                            + nullSafeMessage(error)
            );
        }
    }

    @SuppressWarnings("unused")
    private void unregister() {
        if (!registered.compareAndSet(true, false)) {
            return;
        }
        EventBus.unsubscribe(subscription);
        subscription = null;
    }

    private static String nullSafeMessage(Throwable error) {
        return error.getMessage() == null ? "" : error.getMessage();
    }
}
