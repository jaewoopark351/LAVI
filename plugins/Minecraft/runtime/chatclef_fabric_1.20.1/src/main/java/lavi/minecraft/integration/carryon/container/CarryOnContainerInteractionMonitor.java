package lavi.minecraft.integration.carryon.container;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import lavi.minecraft.diagnostics.interaction.BlockInteractionObserver;
import lavi.minecraft.diagnostics.interaction.BlockInteractionScreenSnapshot;
import lavi.minecraft.integration.carryon.CarryOnCarryState;
import lavi.minecraft.integration.carryon.CarryOnDiagnostics;
import lavi.minecraft.integration.carryon.CarryOnObservation;
import lavi.minecraft.integration.carryon.CarryOnObservationClassifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;

import java.util.HashMap;
import java.util.Map;

//20260805_kpopmodder: Observe Carry On effects around container-open interactions without changing behavior.
public final class CarryOnContainerInteractionMonitor implements BlockInteractionObserver {
    private final Map<Long, CarryOnContainerInteractionState> pendingInteractions = new HashMap<>();
    private final CarryOnContainerInteractionLogger logger = new CarryOnContainerInteractionLogger();
    private final CarryOnContainerPostconditionWindow postconditionWindow = new CarryOnContainerPostconditionWindow(logger);
    private final CarryOnContainerCarryingAttemptTracker carryingAttemptTracker = new CarryOnContainerCarryingAttemptTracker(logger);
    private String lastCapabilityKey = "";

    @Override
    public void beforeBlockInteraction(BlockInteractionContext context,
                                       ClientPlayerEntity player,
                                       Object hand,
                                       BlockHitResult hitResult) {
        if (!canObserve(context)) {
            return;
        }
        CarryOnObservation before = CarryOnDiagnostics.observe();
        logCapabilityIfChanged(context, before);
        carryingAttemptTracker.observe(context, before);
        pendingInteractions.put(context.interactionId(), new CarryOnContainerInteractionState(context, before));
    }

    @Override
    public void afterBlockInteraction(BlockInteractionContext context,
                                      ClientPlayerEntity player,
                                      Object hand,
                                      BlockHitResult hitResult,
                                      Object result) {
        if (!canObserve(context)) {
            return;
        }
        CarryOnContainerInteractionState pending = pendingInteractions.remove(context.interactionId());
        if (pending == null) {
            return;
        }

        CarryOnObservation before = pending.stateBefore();
        CarryOnObservation after = CarryOnDiagnostics.observe();
        logCapabilityIfChanged(context, after);

        BlockInteractionScreenSnapshot screenAfter = BlockInteractionScreenSnapshot.current(MinecraftClient.getInstance(), player);
        postconditionWindow.start(context, before, result, screenAfter, after);
        if (!CarryOnObservationClassifier.sameObservation(before, after)) {
            logger.logStateTransition(context, before, after, result, screenAfter);
        }
        if (unexpectedPickup(before, after)) {
            CarryOnContainerPickupEvidence evidence = CarryOnContainerPickupEvidence.classify(context, after);
            logger.logPickupAttribution(context, before, after, result, screenAfter, evidence);
            logger.logPostcondition(context, before, after, result, screenAfter);
        }
    }

    public void onEndClientTick(MinecraftClient client) {
        postconditionWindow.onEndClientTick(client);
    }

    private boolean canObserve(BlockInteractionContext context) {
        return ChatClefDiagnostics.isBoundaryEnabled()
                && context != null
                && context.screenOpeningTarget();
    }

    private boolean unexpectedPickup(CarryOnObservation before, CarryOnObservation after) {
        return before != null
                && after != null
                && before.state() == CarryOnCarryState.AVAILABLE_NOT_CARRYING
                && after.state() == CarryOnCarryState.AVAILABLE_CARRYING;
    }

    private void logCapabilityIfChanged(BlockInteractionContext context, CarryOnObservation observation) {
        String key = capabilityKey(observation);
        if (key.equals(lastCapabilityKey)) {
            return;
        }
        lastCapabilityKey = key;
        logger.logCapabilityResolved(context, observation);
    }

    private String capabilityKey(CarryOnObservation observation) {
        if (observation == null) {
            return "unavailable";
        }
        return observation.loaded()
                + "|" + observation.version()
                + "|" + observation.exceptionType();
    }
}
