package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.Subscription;
import adris.altoclef.eventbus.events.TaskFinishedEvent;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

//20260803_kpopmodder: Queue user task finish observations outside the WebSocket transport path.
public final class FabricChatClefUserTaskFinishedObserver {
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefTaskStateReader taskStateReader;
    private final ConcurrentLinkedQueue<FabricChatClefCommandTerminationObservation> observations = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private Subscription<TaskFinishedEvent> subscription;

    public FabricChatClefUserTaskFinishedObserver(
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefTaskStateReader taskStateReader
    ) {
        this.diagnostics = diagnostics;
        this.taskStateReader = taskStateReader;
    }

    public void register() {
        if (!registered.compareAndSet(false, true)) {
            return;
        }
        subscription = EventBus.subscribe(TaskFinishedEvent.class, this::onTaskFinished);
        diagnostics.info("registered ChatClef TaskFinishedEvent observer");
    }

    public FabricChatClefCommandTerminationObservation poll() {
        FabricChatClefCommandTerminationObservation observation = observations.poll();
        if (observation == null) {
            return null;
        }
        return observation.withDequeueMetadata(
                System.currentTimeMillis(),
                ChatClefDiagnostics.currentClientTickId(),
                observations.size(),
                taskStateReader.ownershipSnapshot()
        );
    }

    private void onTaskFinished(TaskFinishedEvent event) {
        try {
            int queueDepthBefore = observations.size();
            FabricChatClefCommandTerminationObservation observation =
                    FabricChatClefCommandTerminationObservation.fromTaskFinishedEvent(
                            event,
                            ChatClefDiagnostics.nextEventSequence(),
                            ChatClefDiagnostics.currentClientTickId(),
                            queueDepthBefore,
                            queueDepthBefore + 1,
                            taskStateReader.ownershipSnapshot()
                    );
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
