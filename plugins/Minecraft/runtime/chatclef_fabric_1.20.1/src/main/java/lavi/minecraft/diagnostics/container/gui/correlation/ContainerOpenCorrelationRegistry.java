package lavi.minecraft.diagnostics.container.gui.correlation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

//20260904_kpopmodder: Bound diagnostic-only interaction correlation independently of gameplay binding.
public final class ContainerOpenCorrelationRegistry {
    private static final int MAX_PENDING = 8;
    private static final long MAX_AGE_TICKS = 20L;

    private final Deque<ContainerOpenInteractionObservation> pending = new ArrayDeque<>();

    public synchronized void begin(ContainerOpenInteractionObservation observation) {
        if (observation == null) {
            return;
        }
        remove(observation.interactionId());
        while (pending.size() >= MAX_PENDING) {
            pending.removeFirst();
        }
        pending.addLast(observation);
    }

    public synchronized void complete(long interactionId, Object result) {
        for (Iterator<ContainerOpenInteractionObservation> iterator = pending.iterator(); iterator.hasNext(); ) {
            ContainerOpenInteractionObservation current = iterator.next();
            if (current.interactionId() == interactionId) {
                iterator.remove();
                pending.addLast(current.withInteractResult(result));
                return;
            }
        }
    }

    public synchronized ContainerOpenInteractionObservation match(Screen screen, long currentTick) {
        expire(currentTick);
        MinecraftClient client = MinecraftClient.getInstance();
        ScreenHandler eventHandler = screen instanceof HandledScreen<?> handled
                ? handled.getScreenHandler()
                : null;
        Iterator<ContainerOpenInteractionObservation> iterator = pending.descendingIterator();
        ContainerOpenInteractionObservation newestCandidate = null;
        while (iterator.hasNext()) {
            ContainerOpenInteractionObservation candidate = iterator.next();
            if (newestCandidate == null) {
                newestCandidate = candidate;
            }
            if (matches(candidate, screen, eventHandler, client)) {
                iterator.remove();
                return candidate;
            }
        }
        return newestCandidate;
    }

    public synchronized void expire(long currentTick) {
        pending.removeIf(candidate -> currentTick >= candidate.interactionStartClientTick()
                && currentTick - candidate.interactionStartClientTick() > MAX_AGE_TICKS);
    }

    public synchronized void clear() {
        pending.clear();
    }

    synchronized int sizeForTests() {
        return pending.size();
    }

    private static boolean matches(ContainerOpenInteractionObservation candidate,
                                   Screen screen,
                                   ScreenHandler eventHandler,
                                   MinecraftClient client) {
        if (candidate == null || client == null || client.world == null
                || client.player == null || client.currentScreen != screen
                || client.player.currentScreenHandler != eventHandler
                || !candidate.targetFamily().matches(screen, eventHandler)
                || !candidate.worldIdentity().equals(identity(client.world))) {
            return false;
        }
        try {
            String dimension = client.world.getRegistryKey().getValue().toString();
            String liveBlockId = Registries.BLOCK.getId(
                    client.world.getBlockState(candidate.target()).getBlock()
            ).toString();
            return candidate.dimension().equals(dimension)
                    && candidate.targetBlockId().equals(liveBlockId);
        } catch (RuntimeException | LinkageError error) {
            return false;
        }
    }

    private void remove(long interactionId) {
        pending.removeIf(candidate -> candidate.interactionId() == interactionId);
    }

    private static String identity(Object value) {
        return value == null
                ? "unavailable"
                : value.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(value));
    }
}
