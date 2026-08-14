package lavi.minecraft.integration.carryon.container;

import baritone.api.utils.input.Input;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import lavi.minecraft.diagnostics.interaction.BlockInteractionScreenSnapshot;
import lavi.minecraft.integration.carryon.CarryOnDiagnostics;
import lavi.minecraft.integration.carryon.CarryOnObservation;
import net.minecraft.client.MinecraftClient;

//20260815_kpopmodder: Capture one bounded post-click observation point without owning retry or recovery.
final class CarryOnContainerPostconditionSnapshot {
    private final int observationOffsetTicks;
    private final CarryOnObservation carryObservation;
    private final BlockInteractionScreenSnapshot screenSnapshot;
    private final CarryOnContainerTargetSnapshot targetSnapshot;
    private final String inputSneakHeld;
    private final String rawSneakKeyPressed;
    private final String playerSneaking;
    private final int sneakStateStableTicks;

    private CarryOnContainerPostconditionSnapshot(int observationOffsetTicks,
                                                  CarryOnObservation carryObservation,
                                                  BlockInteractionScreenSnapshot screenSnapshot,
                                                  CarryOnContainerTargetSnapshot targetSnapshot,
                                                  String inputSneakHeld,
                                                  String rawSneakKeyPressed,
                                                  String playerSneaking,
                                                  int sneakStateStableTicks) {
        this.observationOffsetTicks = observationOffsetTicks;
        this.carryObservation = carryObservation;
        this.screenSnapshot = screenSnapshot;
        this.targetSnapshot = targetSnapshot;
        this.inputSneakHeld = inputSneakHeld;
        this.rawSneakKeyPressed = rawSneakKeyPressed;
        this.playerSneaking = playerSneaking;
        this.sneakStateStableTicks = sneakStateStableTicks;
    }

    static CarryOnContainerPostconditionSnapshot current(MinecraftClient client,
                                                         BlockInteractionContext context,
                                                         int observationOffsetTicks,
                                                         String previousSneakKey,
                                                         int previousSneakStateStableTicks) {
        return current(
                client,
                context,
                observationOffsetTicks,
                CarryOnDiagnostics.observe(),
                null,
                previousSneakKey,
                previousSneakStateStableTicks
        );
    }

    static CarryOnContainerPostconditionSnapshot current(MinecraftClient client,
                                                         BlockInteractionContext context,
                                                         int observationOffsetTicks,
                                                         CarryOnObservation carryObservation,
                                                         BlockInteractionScreenSnapshot providedScreenSnapshot,
                                                         String previousSneakKey,
                                                         int previousSneakStateStableTicks) {
        BlockInteractionScreenSnapshot screenSnapshot = providedScreenSnapshot == null
                ? BlockInteractionScreenSnapshot.current(client, client == null ? null : client.player)
                : providedScreenSnapshot;
        CarryOnObservation observedCarryState = carryObservation == null ? CarryOnDiagnostics.observe() : carryObservation;
        CarryOnContainerTargetSnapshot targetSnapshot = CarryOnContainerTargetSnapshot.current(
                client,
                context == null ? null : context.targetPosition()
        );
        String inputSneakHeld = ChatClefDiagnostics.inputHeldState(Input.SNEAK);
        String rawSneakKeyPressed = ChatClefDiagnostics.rawInputHeldState(Input.SNEAK);
        String playerSneaking = ChatClefDiagnostics.safeValue(() -> client == null || client.player == null ? null : client.player.isSneaking());
        String currentSneakKey = sneakKey(inputSneakHeld, rawSneakKeyPressed, playerSneaking);
        int stableTicks = currentSneakKey.equals(previousSneakKey)
                ? Math.max(1, previousSneakStateStableTicks + 1)
                : 1;
        return new CarryOnContainerPostconditionSnapshot(
                observationOffsetTicks,
                observedCarryState,
                screenSnapshot,
                targetSnapshot,
                inputSneakHeld,
                rawSneakKeyPressed,
                playerSneaking,
                stableTicks
        );
    }

    int observationOffsetTicks() {
        return observationOffsetTicks;
    }

    CarryOnObservation carryObservation() {
        return carryObservation;
    }

    BlockInteractionScreenSnapshot screenSnapshot() {
        return screenSnapshot;
    }

    CarryOnContainerTargetSnapshot targetSnapshot() {
        return targetSnapshot;
    }

    String inputSneakHeld() {
        return inputSneakHeld;
    }

    String rawSneakKeyPressed() {
        return rawSneakKeyPressed;
    }

    String playerSneaking() {
        return playerSneaking;
    }

    int sneakStateStableTicks() {
        return sneakStateStableTicks;
    }

    String sneakKey() {
        return sneakKey(inputSneakHeld, rawSneakKeyPressed, playerSneaking);
    }

    String fingerprint() {
        return screenFingerprint()
                + "|" + carryFingerprint()
                + "|" + (targetSnapshot == null ? "unavailable" : targetSnapshot.fingerprint())
                + "|" + sneakKey();
    }

    private String screenFingerprint() {
        if (screenSnapshot == null) {
            return "unavailable|unavailable|unavailable";
        }
        return screenSnapshot.screenName()
                + "|" + screenSnapshot.screenHandlerName()
                + "|" + screenSnapshot.screenHandlerSyncId();
    }

    private String carryFingerprint() {
        if (carryObservation == null) {
            return "unavailable|unavailable|unavailable|unavailable|unavailable|unavailable|unavailable|unavailable";
        }
        return carryObservation.loaded()
                + "|" + carryObservation.version()
                + "|" + carryObservation.state()
                + "|" + carryObservation.carriedBlockId()
                + "|" + carryObservation.carriedBlockDescription()
                + "|" + carryObservation.carriedBlockState()
                + "|" + carryObservation.exceptionType()
                + "|" + carryObservation.carriedBlockExceptionType();
    }

    private static String sneakKey(String inputSneakHeld, String rawSneakKeyPressed, String playerSneaking) {
        return inputSneakHeld + "|" + rawSneakKeyPressed + "|" + playerSneaking;
    }
}
