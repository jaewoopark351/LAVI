package lavi.minecraft.integration.carryon.container;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import lavi.minecraft.diagnostics.interaction.BlockInteractionScreenSnapshot;
import lavi.minecraft.integration.carryon.CarryOnCarryState;
import lavi.minecraft.integration.carryon.CarryOnObservation;
import net.minecraft.client.MinecraftClient;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//20260815_kpopmodder: Observe a bounded click-to-GUI window without retrying, delaying, or cancelling tasks.
final class CarryOnContainerPostconditionWindow {
    private static final int[] OBSERVATION_OFFSETS = {1, 2, 5};
    private static final int MAX_WINDOW_AGE_TICKS = 20;

    private final Map<Long, CarryOnContainerPostconditionWindowEntry> windows = new HashMap<>();
    private final CarryOnContainerInteractionLogger logger;

    CarryOnContainerPostconditionWindow(CarryOnContainerInteractionLogger logger) {
        this.logger = logger;
    }

    void start(BlockInteractionContext context,
               CarryOnObservation stateBefore,
               Object interactResult,
               BlockInteractionScreenSnapshot returnScreen,
               CarryOnObservation returnObservation) {
        if (context == null || returnObservation == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        long returnTick = ChatClefDiagnostics.currentClientTickId();
        CarryOnContainerPostconditionSnapshot returnSnapshot = CarryOnContainerPostconditionSnapshot.current(
                client,
                context,
                0,
                returnObservation,
                returnScreen,
                "",
                0
        );
        CarryOnContainerPostconditionOutcome outcome = classify(context, stateBefore, returnSnapshot, true);
        boolean terminal = outcome != CarryOnContainerPostconditionOutcome.OBSERVATION_PENDING;
        logger.logOutcomeWindow(context, interactResult, returnSnapshot, returnSnapshot,
                "RETURN_SNAPSHOT", outcome, terminal, false);
        if (terminal) {
            return;
        }
        windows.put(context.interactionId(), new CarryOnContainerPostconditionWindowEntry(
                context,
                stateBefore,
                interactResult,
                returnTick,
                returnSnapshot
        ));
    }

    void onEndClientTick(MinecraftClient client) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            windows.clear();
            return;
        }
        if (client == null || client.player == null || client.world == null) {
            windows.clear();
            return;
        }

        long tick = ChatClefDiagnostics.currentClientTickId();
        Iterator<CarryOnContainerPostconditionWindowEntry> iterator = windows.values().iterator();
        while (iterator.hasNext()) {
            CarryOnContainerPostconditionWindowEntry entry = iterator.next();
            long ageTicks = tick - entry.returnClientTickId();
            if (ageTicks > MAX_WINDOW_AGE_TICKS) {
                iterator.remove();
                continue;
            }
            while (entry.nextOffsetIndex() < OBSERVATION_OFFSETS.length
                    && ageTicks >= OBSERVATION_OFFSETS[entry.nextOffsetIndex()]) {
                int offset = OBSERVATION_OFFSETS[entry.nextOffsetIndex()];
                entry.advanceOffsetIndex();
                CarryOnContainerPostconditionSnapshot snapshot = CarryOnContainerPostconditionSnapshot.current(
                        client,
                        entry.context(),
                        offset,
                        previousSneakKey(entry),
                        previousSneakStableTicks(entry)
                );
                CarryOnContainerPostconditionOutcome outcome = classify(entry.context(), entry.stateBefore(), snapshot, isTerminalOffset(offset));
                boolean terminal = outcome != CarryOnContainerPostconditionOutcome.OBSERVATION_PENDING
                        || isTerminalOffset(offset);
                if (terminal) {
                    logger.logOutcomeWindow(entry.context(), entry.interactResult(), entry.returnSnapshot(), snapshot,
                            "TERMINAL_OUTCOME", terminalOutcome(entry.context(), snapshot, outcome), true, false);
                    iterator.remove();
                    break;
                }
                if (!entry.stateChangeLogged() && stateChanged(entry.returnSnapshot(), snapshot)) {
                    entry.markStateChangeLogged();
                    logger.logOutcomeWindow(entry.context(), entry.interactResult(), entry.returnSnapshot(), snapshot,
                            "STATE_CHANGE_SNAPSHOT", outcome, false, false);
                }
            }
        }
    }

    private CarryOnContainerPostconditionOutcome classify(BlockInteractionContext context,
                                                          CarryOnObservation stateBefore,
                                                          CarryOnContainerPostconditionSnapshot snapshot,
                                                          boolean terminalOffset) {
        if (snapshot == null) {
            return terminalOffset
                    ? CarryOnContainerPostconditionOutcome.OBSERVATION_WINDOW_EXPIRED
                    : CarryOnContainerPostconditionOutcome.OBSERVATION_PENDING;
        }
        if (CarryOnContainerExpectedGui.expectedGuiOpened(context, snapshot.screenSnapshot())) {
            return snapshot.observationOffsetTicks() == 0
                    ? CarryOnContainerPostconditionOutcome.GUI_OPENED
                    : CarryOnContainerPostconditionOutcome.GUI_OPEN_DELAYED;
        }
        CarryOnContainerPickupEvidence evidence = CarryOnContainerPickupEvidence.classify(context, snapshot.carryObservation());
        if (observedPickup(stateBefore, snapshot.carryObservation()) && evidence == CarryOnContainerPickupEvidence.CONFIRMED_TARGET_IDENTITY) {
            return CarryOnContainerPostconditionOutcome.CARRY_ON_PICKUP_CONFIRMED;
        }
        if (observedPickup(stateBefore, snapshot.carryObservation()) && evidence == CarryOnContainerPickupEvidence.STRONG_TEMPORAL_ATTRIBUTION) {
            return CarryOnContainerPostconditionOutcome.CARRY_ON_PICKUP_STRONGLY_ATTRIBUTED;
        }
        if (terminalOffset && targetRemoved(snapshot)) {
            return CarryOnContainerPostconditionOutcome.TARGET_REMOVED_WITHOUT_GUI;
        }
        if (terminalOffset && targetStillMatches(context, snapshot)) {
            return CarryOnContainerPostconditionOutcome.NO_GUI_TARGET_STILL_PRESENT;
        }
        if (terminalOffset) {
            return CarryOnContainerPostconditionOutcome.OBSERVATION_WINDOW_EXPIRED;
        }
        return CarryOnContainerPostconditionOutcome.OBSERVATION_PENDING;
    }

    private CarryOnContainerPostconditionOutcome terminalOutcome(BlockInteractionContext context,
                                                                 CarryOnContainerPostconditionSnapshot snapshot,
                                                                 CarryOnContainerPostconditionOutcome outcome) {
        if (outcome != CarryOnContainerPostconditionOutcome.OBSERVATION_PENDING) {
            return outcome;
        }
        if (targetRemoved(snapshot)) {
            return CarryOnContainerPostconditionOutcome.TARGET_REMOVED_WITHOUT_GUI;
        }
        if (targetStillMatches(context, snapshot)) {
            return CarryOnContainerPostconditionOutcome.NO_GUI_TARGET_STILL_PRESENT;
        }
        return CarryOnContainerPostconditionOutcome.OBSERVATION_WINDOW_EXPIRED;
    }

    private boolean observedPickup(CarryOnObservation stateBefore, CarryOnObservation observation) {
        return stateBefore != null
                && observation != null
                && stateBefore.state() == CarryOnCarryState.AVAILABLE_NOT_CARRYING
                && observation.state() == CarryOnCarryState.AVAILABLE_CARRYING;
    }

    private boolean stateChanged(CarryOnContainerPostconditionSnapshot baseline,
                                 CarryOnContainerPostconditionSnapshot observed) {
        return baseline != null
                && observed != null
                && !baseline.fingerprint().equals(observed.fingerprint());
    }

    private boolean isTerminalOffset(int offset) {
        return offset == OBSERVATION_OFFSETS[OBSERVATION_OFFSETS.length - 1];
    }

    private boolean targetRemoved(CarryOnContainerPostconditionSnapshot snapshot) {
        return snapshot != null
                && snapshot.targetSnapshot() != null
                && snapshot.targetSnapshot().removed();
    }

    private boolean targetStillMatches(BlockInteractionContext context, CarryOnContainerPostconditionSnapshot snapshot) {
        return snapshot != null
                && snapshot.targetSnapshot() != null
                && snapshot.targetSnapshot().stillMatches(context);
    }

    private String previousSneakKey(CarryOnContainerPostconditionWindowEntry entry) {
        return entry == null || entry.returnSnapshot() == null ? "" : entry.returnSnapshot().sneakKey();
    }

    private int previousSneakStableTicks(CarryOnContainerPostconditionWindowEntry entry) {
        return entry == null || entry.returnSnapshot() == null ? 0 : entry.returnSnapshot().sneakStateStableTicks();
    }
}
