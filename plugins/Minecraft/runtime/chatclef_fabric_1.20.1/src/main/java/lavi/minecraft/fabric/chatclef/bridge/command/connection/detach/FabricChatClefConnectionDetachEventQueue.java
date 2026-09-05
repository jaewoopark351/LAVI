package lavi.minecraft.fabric.chatclef.bridge.command.connection.detach;

//20260905_kpopmodder: Own queued transport detach notifications for ordinary commands.

import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefConnectionDetachedEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public final class FabricChatClefConnectionDetachEventQueue {
    private final Deque<FabricChatClefConnectionDetachedEvent> events = new ArrayDeque<>();

    public synchronized void enqueue(long connectionGeneration, String reason) {
        events.offer(FabricChatClefConnectionDetachedEvent.of(connectionGeneration, reason));
    }

    public synchronized Optional<FabricChatClefConnectionDetachedEvent> poll() {
        return Optional.ofNullable(events.poll());
    }

    public synchronized void clear() {
        events.clear();
    }
}
