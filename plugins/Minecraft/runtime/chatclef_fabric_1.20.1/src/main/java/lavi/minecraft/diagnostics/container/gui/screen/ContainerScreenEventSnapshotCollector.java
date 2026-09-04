package lavi.minecraft.diagnostics.container.gui.screen;

import adris.altoclef.eventbus.events.ScreenOpenEvent;
import lavi.minecraft.diagnostics.container.gui.correlation.ContainerOpenInteractionObservation;
import lavi.minecraft.diagnostics.container.gui.correlation.ContainerTargetFamily;
import lavi.minecraft.diagnostics.container.gui.tick.ContainerClientTickWindowSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;

//20260904_kpopmodder: Read raw event and live client state at one boundary without mutating either.
public final class ContainerScreenEventSnapshotCollector {
    public ContainerScreenEventSnapshot capture(
            ScreenOpenEvent event,
            ContainerOpenInteractionObservation interaction,
            ContainerClientTickWindowSnapshot tickWindow) {
        MinecraftClient client = MinecraftClient.getInstance();
        Screen eventScreen = event == null ? null : event.screen;
        ScreenHandler eventHandler = eventScreen instanceof HandledScreen<?> handled
                ? handled.getScreenHandler()
                : null;
        ScreenHandler playerHandler = client == null || client.player == null
                ? null
                : client.player.currentScreenHandler;
        ContainerTargetFamily family = interaction == null
                ? inferredFamily(eventScreen, eventHandler)
                : interaction.targetFamily();
        String liveBlockId = liveBlockId(client, interaction);
        boolean screenMatched = interaction != null
                && family.matches(eventScreen, eventHandler);
        boolean handlerMatched = interaction != null
                && handlerMatches(family, eventHandler);
        boolean screenCurrent = client != null && client.currentScreen == eventScreen;
        boolean handlerCurrent = eventHandler != null && eventHandler == playerHandler;
        boolean worldMatched = interaction != null
                && interaction.worldIdentity().equals(identity(client == null ? null : client.world));
        boolean dimensionMatched = interaction != null
                && interaction.dimension().equals(dimension(client));
        boolean targetMatched = interaction != null
                && interaction.targetBlockId().equals(liveBlockId);
        boolean correlationCandidateAccepted = interaction != null
                && screenMatched
                && handlerMatched
                && screenCurrent
                && handlerCurrent
                && worldMatched
                && dimensionMatched
                && targetMatched;
        return new ContainerScreenEventSnapshot(
                identity(event),
                event != null && event.preOpen,
                identity(eventScreen),
                simpleName(eventScreen),
                className(eventScreen),
                eventScreen instanceof HandledScreen<?>,
                identity(eventHandler),
                identity(playerHandler),
                syncId(eventHandler),
                syncId(playerHandler),
                className(eventHandler),
                screenCurrent,
                handlerCurrent,
                family,
                interaction == null || interaction.target() == null
                        ? "unavailable"
                        : interaction.target().getX() + "," + interaction.target().getY() + "," + interaction.target().getZ(),
                interaction == null ? "unavailable" : interaction.targetBlockId(),
                liveBlockId,
                identity(client == null ? null : client.world),
                dimension(client),
                interaction == null ? "unavailable" : family.expectedScreenType(),
                interaction == null ? "unavailable" : family.expectedHandlerType(),
                screenMatched,
                handlerMatched,
                correlationCandidateAccepted,
                correlationReason(
                        event,
                        interaction,
                        screenMatched,
                        handlerMatched,
                        screenCurrent,
                        handlerCurrent,
                        worldMatched,
                        dimensionMatched,
                        targetMatched
                ),
                interaction,
                tickWindow
        );
    }

    private static ContainerTargetFamily inferredFamily(Screen screen, ScreenHandler handler) {
        if (ContainerTargetFamily.FURNACE.matches(screen, handler)) {
            return ContainerTargetFamily.FURNACE;
        }
        if (ContainerTargetFamily.CHEST.matches(screen, handler)) {
            return ContainerTargetFamily.CHEST;
        }
        return ContainerTargetFamily.UNSUPPORTED;
    }

    private static boolean handlerMatches(ContainerTargetFamily family, ScreenHandler handler) {
        if (family == ContainerTargetFamily.FURNACE) {
            return handler instanceof net.minecraft.screen.FurnaceScreenHandler;
        }
        if (family == ContainerTargetFamily.CHEST) {
            return handler instanceof net.minecraft.screen.GenericContainerScreenHandler;
        }
        return false;
    }

    private static String correlationReason(
            ScreenOpenEvent event,
            ContainerOpenInteractionObservation interaction,
            boolean screenMatched,
            boolean handlerMatched,
            boolean screenCurrent,
            boolean handlerCurrent,
            boolean worldMatched,
            boolean dimensionMatched,
            boolean targetMatched) {
        if (event == null) return "NULL_EVENT_UNOBSERVABLE";
        if (event.preOpen) return "PRE_OPEN_EVENT_NOT_TAIL";
        if (interaction == null) return "NO_MATCHING_BLOCK_INTERACT_OBSERVATION";
        if (!worldMatched) return "WORLD_IDENTITY_MISMATCH";
        if (!dimensionMatched) return "DIMENSION_MISMATCH";
        if (!targetMatched) return "TARGET_BLOCK_MISMATCH";
        if (!screenCurrent) return "EVENT_SCREEN_NOT_CURRENT_SCREEN";
        if (!screenMatched) return "SCREEN_TYPE_MISMATCH";
        if (!handlerMatched) return "HANDLER_TYPE_MISMATCH";
        if (!handlerCurrent) return "EVENT_HANDLER_NOT_PLAYER_HANDLER";
        return "HEURISTIC_RECENT_EXACT_CONTAINER_INTERACTION";
    }

    private static String liveBlockId(
            MinecraftClient client,
            ContainerOpenInteractionObservation interaction) {
        if (client == null || client.world == null || interaction == null || interaction.target() == null) {
            return "unavailable";
        }
        try {
            return Registries.BLOCK.getId(
                    client.world.getBlockState(interaction.target()).getBlock()
            ).toString();
        } catch (RuntimeException | LinkageError error) {
            return "unavailable:" + error.getClass().getSimpleName();
        }
    }

    private static String dimension(MinecraftClient client) {
        try {
            return client == null || client.world == null
                    ? "unavailable"
                    : client.world.getRegistryKey().getValue().toString();
        } catch (RuntimeException | LinkageError error) {
            return "unavailable:" + error.getClass().getSimpleName();
        }
    }

    private static int syncId(ScreenHandler handler) {
        return handler == null ? -1 : handler.syncId;
    }

    private static String simpleName(Object value) {
        return value == null ? "none" : value.getClass().getSimpleName();
    }

    private static String className(Object value) {
        return value == null ? "none" : value.getClass().getName();
    }

    private static String identity(Object value) {
        return value == null
                ? "unavailable"
                : value.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(value));
    }
}
