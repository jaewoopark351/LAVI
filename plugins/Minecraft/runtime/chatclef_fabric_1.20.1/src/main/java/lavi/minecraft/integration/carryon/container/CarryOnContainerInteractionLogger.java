package lavi.minecraft.integration.carryon.container;

import adris.altoclef.AltoClef;
import baritone.api.utils.input.Input;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import lavi.minecraft.diagnostics.interaction.BlockInteractionScreenSnapshot;
import lavi.minecraft.integration.carryon.CarryOnObservation;
import lavi.minecraft.integration.carryon.snapshot.CarryOnBaritoneSnapshot;
import lavi.minecraft.integration.carryon.snapshot.CarryOnBaritoneSnapshotCollector;
import lavi.minecraft.integration.carryon.snapshot.CarryOnInputSnapshot;
import lavi.minecraft.integration.carryon.snapshot.CarryOnInputSnapshotCollector;
import lavi.minecraft.integration.carryon.snapshot.CarryOnPlayerSnapshot;
import lavi.minecraft.integration.carryon.snapshot.CarryOnPlayerSnapshotCollector;
import lavi.minecraft.integration.carryon.snapshot.CarryOnTaskSnapshot;
import lavi.minecraft.integration.carryon.snapshot.CarryOnTaskSnapshotCollector;
import net.minecraft.client.MinecraftClient;
import lavi.minecraft.diagnostics.postplace.PostPlaceContainerOpenIntent;

//20260805_kpopmodder: Emit Carry On container-interaction diagnostics without owning recovery behavior.
final class CarryOnContainerInteractionLogger {
    void logCapabilityResolved(BlockInteractionContext context, CarryOnObservation observation) {
        ChatClefDiagnostics.logBoundary(
                "CARRY_ON_CAPABILITY_RESOLVED",
                "carry_on_container_interaction_capability_resolved",
                null,
                ChatClefDiagnostics.withCommandContextFields(fields(
                        contextFields(context),
                        runtimeFields(),
                        observationFields("current", observation)
                ))
        );
    }

    void logStateTransition(BlockInteractionContext context,
                            CarryOnObservation before,
                            CarryOnObservation after,
                            Object interactResult,
                            BlockInteractionScreenSnapshot screenAfter) {
        ChatClefDiagnostics.logBoundary(
                "CARRY_ON_STATE_TRANSITION_OBSERVED",
                "carry_on_container_interaction_state_transition",
                null,
                ChatClefDiagnostics.withCommandContextFields(fields(
                        contextFields(context),
                        runtimeFields(),
                        screenFields(screenAfter),
                        observationFields("before", before),
                        observationFields("after", after),
                        "interactResult", value(interactResult)
                ))
        );
    }

    void logPostcondition(BlockInteractionContext context,
                          CarryOnObservation before,
                          CarryOnObservation after,
                          Object interactResult,
                          BlockInteractionScreenSnapshot screenAfter) {
        ChatClefDiagnostics.logBoundary(
                "CONTAINER_OPEN_POSTCONDITION_OBSERVED",
                "carry_on_container_interaction_postcondition",
                null,
                ChatClefDiagnostics.withCommandContextFields(fields(
                        contextFields(context),
                        runtimeFields(),
                        screenFields(screenAfter),
                        observationFields("before", before),
                        observationFields("after", after),
                        "interactResult", value(interactResult),
                        "screenHandlerChangedImmediately", screenHandlerChanged(context, screenAfter)
                ))
        );
    }

    void logPickupAttribution(BlockInteractionContext context,
                              CarryOnObservation before,
                              CarryOnObservation after,
                              Object interactResult,
                              BlockInteractionScreenSnapshot screenAfter,
                              CarryOnContainerPickupEvidence evidence) {
        ChatClefDiagnostics.logWarningEvent(
                "CARRY_ON_CONTAINER_PICKUP_ATTRIBUTION",
                "container_interaction_changed_to_carrying",
                null,
                ChatClefDiagnostics.withCommandContextFields(fields(
                        contextFields(context),
                        runtimeFields(),
                        screenFields(screenAfter),
                        observationFields("before", before),
                        observationFields("after", after),
                        "evidenceLevel", evidence,
                        "interactResult", value(interactResult),
                        "rawRightClickState", ChatClefDiagnostics.rawInputHeldState(Input.CLICK_RIGHT),
                        "rawSneakState", ChatClefDiagnostics.rawInputHeldState(Input.SNEAK),
                        "rawLeftClickState", ChatClefDiagnostics.rawInputHeldState(Input.CLICK_LEFT),
                        "screenHandlerChangedImmediately", screenHandlerChanged(context, screenAfter)
                ))
        );
    }

    void logOutcomeWindow(BlockInteractionContext context,
                          Object interactResult,
                          CarryOnContainerPostconditionSnapshot returnSnapshot,
                          CarryOnContainerPostconditionSnapshot observedSnapshot,
                          String windowEvent,
                          CarryOnContainerPostconditionOutcome outcome,
                          boolean terminal,
                          boolean summary) {
        CarryOnContainerPickupEvidence evidence = CarryOnContainerPickupEvidence.classify(
                context,
                observedSnapshot == null ? null : observedSnapshot.carryObservation()
        );
        ChatClefDiagnostics.logBoundary(
                "CONTAINER_OPEN_INTERACTION_OUTCOME_WINDOW",
                "carry_on_container_open_interaction_outcome_window",
                null,
                ChatClefDiagnostics.withCommandContextFields(fields(
                        contextFields(context),
                        runtimeFields(),
                        postconditionSnapshotFields("return", returnSnapshot, context),
                        postconditionSnapshotFields("observed", observedSnapshot, context),
                        "windowEvent", windowEvent,
                        "observationOffsetTicks", observedSnapshot == null ? "unavailable" : observedSnapshot.observationOffsetTicks(),
                        "expectedScreenHandler", CarryOnContainerExpectedGui.expectedScreenHandler(context),
                        "expectedGuiOpened", CarryOnContainerExpectedGui.expectedGuiOpened(
                                context,
                                observedSnapshot == null ? null : observedSnapshot.screenSnapshot()
                        ),
                        "screenHandlerChangedObserved", CarryOnContainerExpectedGui.screenHandlerChanged(
                                context,
                                observedSnapshot == null ? null : observedSnapshot.screenSnapshot()
                        ),
                        "pickupEvidenceLevel", evidence,
                        "targetIdentityMatchesCarriedBlock", evidence == CarryOnContainerPickupEvidence.CONFIRMED_TARGET_IDENTITY,
                        "interactResult", value(interactResult),
                        "outcome", outcome,
                        "terminal", terminal,
                        "summary", summary,
                        "behavior_effect", "none"
                ))
        );
    }

    void logAttemptWhileCarrying(BlockInteractionContext context,
                                 CarryOnObservation observation,
                                 String trigger,
                                 boolean summary,
                                 int suppressedRepeatCount) {
        ChatClefDiagnostics.logWarningEvent(
                "CONTAINER_OPEN_ATTEMPT_WHILE_CARRYING",
                "container_open_attempt_while_carrying",
                null,
                ChatClefDiagnostics.withCommandContextFields(fields(
                        contextFields(context),
                        runtimeFields(),
                        observationFields("current", observation),
                        "trigger", trigger,
                        "summary", summary,
                        "suppressedRepeatCount", suppressedRepeatCount,
                        "expectedScreenHandler", CarryOnContainerExpectedGui.expectedScreenHandler(context),
                        "rawRightClickState", ChatClefDiagnostics.rawInputHeldState(Input.CLICK_RIGHT),
                        "rawSneakKeyPressed", ChatClefDiagnostics.rawInputHeldState(Input.SNEAK),
                        "inputSneakHeld", ChatClefDiagnostics.inputHeldState(Input.SNEAK),
                        "behavior_effect", "none"
                ))
        );
    }

    void logAttemptWhileCarryingCap(int cap) {
        ChatClefDiagnostics.logBoundary(
                "CONTAINER_OPEN_ATTEMPT_WHILE_CARRYING_CAP_REACHED",
                "container_open_attempt_while_carrying_cap_reached",
                null,
                ChatClefDiagnostics.withCommandContextFields(
                        "diagnosticScope", "carry_on_container_open_attempt_while_carrying",
                        "owner", "carry_on_container_interaction_monitor",
                        "mode", "BOUNDARY",
                        "trigger", "session_cap",
                        "cap", cap,
                        "behavior_effect", "none"
                )
        );
    }

    private Object[] contextFields(BlockInteractionContext context) {
        return new Object[]{
                "interactionId", context == null ? "unavailable" : context.interactionId(),
                "postPlaceOperationId", postPlaceOperationId(context),
                "interactionStartClientTick", context == null ? "unavailable" : context.startClientTickId(),
                "matchedHead", context != null && context.matchedHead(),
                "targetKind", context == null ? "unavailable" : context.targetKind(),
                "targetBlockId", context == null ? "unavailable" : context.targetBlockId(),
                "targetBlockDescription", context == null ? "unavailable" : context.targetBlockDescription(),
                "targetBlockState", context == null ? "unavailable" : context.targetBlockState(),
                "targetPosition", context == null ? "unavailable" : ChatClefDiagnostics.blockPos(context.targetPosition()),
                "hand", context == null ? "unavailable" : context.hand(),
                "hitSide", context == null ? "unavailable" : context.hitSide(),
                "hitType", context == null ? "unavailable" : context.hitType(),
                "screenNameBefore", screenBefore(context).screenName(),
                "screenHandlerBefore", screenBefore(context).screenHandlerName(),
                "screenHandlerSyncIdBefore", screenBefore(context).screenHandlerSyncId()
        };
    }

    private Object[] postconditionSnapshotFields(String prefix,
                                                 CarryOnContainerPostconditionSnapshot snapshot,
                                                 BlockInteractionContext context) {
        if (snapshot == null) {
            return new Object[]{
                    prefix + "CarryOnLoaded", "unavailable",
                    prefix + "CarryOnVersion", "unavailable",
                    prefix + "CarryState", "unavailable",
                    prefix + "CarriedBlockId", "unavailable",
                    prefix + "CarriedBlockDescription", "unavailable",
                    prefix + "CarriedBlockState", "unavailable",
                    prefix + "ScreenName", "unavailable",
                    prefix + "ScreenHandler", "unavailable",
                    prefix + "ScreenHandlerSyncId", "unavailable",
                    prefix + "TargetBlockId", "unavailable",
                    prefix + "TargetBlockDescription", "unavailable",
                    prefix + "TargetBlockState", "unavailable",
                    prefix + "TargetRemoved", "unavailable",
                    prefix + "TargetStillMatchesClicked", "unavailable",
                    prefix + "InputSneakHeld", "unavailable",
                    prefix + "RawSneakKeyPressed", "unavailable",
                    prefix + "PlayerSneaking", "unavailable",
                    prefix + "SneakStateStableTicks", "unavailable"
            };
        }
        CarryOnObservation observation = snapshot.carryObservation();
        BlockInteractionScreenSnapshot screen = snapshot.screenSnapshot();
        CarryOnContainerTargetSnapshot target = snapshot.targetSnapshot();
        return fields(
                observationFields(prefix, observation),
                "observationOffsetTicks" + capitalized(prefix), snapshot.observationOffsetTicks(),
                prefix + "ScreenName", screen == null ? "unavailable" : screen.screenName(),
                prefix + "ScreenHandler", screen == null ? "unavailable" : screen.screenHandlerName(),
                prefix + "ScreenHandlerSyncId", screen == null ? "unavailable" : screen.screenHandlerSyncId(),
                prefix + "TargetBlockId", target == null ? "unavailable" : target.blockId(),
                prefix + "TargetBlockDescription", target == null ? "unavailable" : target.blockDescription(),
                prefix + "TargetBlockState", target == null ? "unavailable" : target.blockState(),
                prefix + "TargetRemoved", target != null && target.removed(),
                prefix + "TargetStillMatchesClicked", target != null && target.stillMatches(context),
                prefix + "InputSneakHeld", snapshot.inputSneakHeld(),
                prefix + "RawSneakKeyPressed", snapshot.rawSneakKeyPressed(),
                prefix + "PlayerSneaking", snapshot.playerSneaking(),
                prefix + "SneakStateStableTicks", snapshot.sneakStateStableTicks()
        );
    }

    private Object[] observationFields(String prefix, CarryOnObservation observation) {
        return new Object[]{
                prefix + "CarryOnLoaded", observation == null ? "unavailable" : observation.loaded(),
                prefix + "CarryOnVersion", observation == null ? "unavailable" : observation.version(),
                prefix + "CarryState", observation == null ? "unavailable" : observation.state(),
                prefix + "CarriedBlockId", observation == null ? "unavailable" : observation.carriedBlockId(),
                prefix + "CarriedBlockDescription", observation == null ? "unavailable" : observation.carriedBlockDescription(),
                prefix + "CarriedBlockState", observation == null ? "unavailable" : observation.carriedBlockState(),
                prefix + "CarriedBlockExceptionType", observation == null ? "unavailable" : observation.carriedBlockExceptionType(),
                prefix + "ObservationExceptionType", observation == null ? "unavailable" : observation.exceptionType()
        };
    }

    private Object[] runtimeFields() {
        AltoClef mod = mod();
        MinecraftClient client = client();
        CarryOnTaskSnapshot task = CarryOnTaskSnapshotCollector.collect(mod, "container_interaction_monitor");
        CarryOnInputSnapshot input = CarryOnInputSnapshotCollector.collect(mod);
        CarryOnBaritoneSnapshot baritone = CarryOnBaritoneSnapshotCollector.collect(mod);
        CarryOnPlayerSnapshot player = CarryOnPlayerSnapshotCollector.collect(client == null ? null : client.player);
        return new Object[]{
                "topLevelTask", task.topLevelTask(),
                "childTask", task.childTask(),
                "currentChain", task.currentChain(),
                "taskChain", task.taskChain(),
                "taskRunnerActive", task.taskRunnerActive(),
                "userTaskChainActive", task.userTaskChainActive(),
                "paused", task.paused(),
                "chatClefEnabled", task.chatClefEnabled(),
                "playerMode", task.playerMode(),
                "rightClickState", input.rightClickState(),
                "sneakState", input.sneakState(),
                "leftClickState", input.leftClickState(),
                "movementInputState", input.movementInputState(),
                "baritonePathing", baritone.baritonePathing(),
                "customGoalOwner", baritone.customGoalOwner(),
                "breakingBlockState", baritone.breakingBlockState(),
                "playerPosition", player.playerPosition(),
                "playerVelocity", player.playerVelocity(),
                "lookRotation", player.lookRotation(),
                "playerPoseState", player.playerPoseState(),
                "selectedHotbarSlot", player.selectedHotbarSlot(),
                "mainHandItem", player.mainHandItem(),
                "offHandItem", player.offHandItem()
        };
    }

    private AltoClef mod() {
        try {
            return AltoClef.getInstance();
        } catch (RuntimeException | LinkageError error) {
            return null;
        }
    }

    private MinecraftClient client() {
        try {
            return MinecraftClient.getInstance();
        } catch (RuntimeException | LinkageError error) {
            return null;
        }
    }

    private Object[] screenFields(BlockInteractionScreenSnapshot screenAfter) {
        return new Object[]{
                "screenNameAfter", screenAfter == null ? "unavailable" : screenAfter.screenName(),
                "screenHandlerAfter", screenAfter == null ? "unavailable" : screenAfter.screenHandlerName(),
                "screenHandlerSyncIdAfter", screenAfter == null ? "unavailable" : screenAfter.screenHandlerSyncId()
        };
    }

    private boolean screenHandlerChanged(BlockInteractionContext context, BlockInteractionScreenSnapshot screenAfter) {
        BlockInteractionScreenSnapshot before = screenBefore(context);
        return screenAfter != null
                && (!before.screenHandlerName().equals(screenAfter.screenHandlerName())
                || !before.screenHandlerSyncId().equals(screenAfter.screenHandlerSyncId()));
    }

    private BlockInteractionScreenSnapshot screenBefore(BlockInteractionContext context) {
        BlockInteractionScreenSnapshot screenBefore = context == null ? null : context.screenBefore();
        return screenBefore == null
                ? BlockInteractionScreenSnapshot.current(null, null)
                : screenBefore;
    }

    private Object postPlaceOperationId(BlockInteractionContext context) {
        PostPlaceContainerOpenIntent intent = context == null
                ? null
                : ChatClefDiagnostics.activePostPlaceContainerOpenIntent(context.targetPosition());
        return intent == null ? "unavailable" : intent.operationId();
    }

    private String capitalized(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String value(Object rawValue) {
        return rawValue == null ? "unavailable" : String.valueOf(rawValue);
    }

    private Object[] fields(Object... parts) {
        int totalLength = 0;
        for (Object part : parts) {
            totalLength += part instanceof Object[] array ? array.length : 1;
        }
        Object[] merged = new Object[totalLength];
        int index = 0;
        for (Object part : parts) {
            if (part instanceof Object[] array) {
                System.arraycopy(array, 0, merged, index, array.length);
                index += array.length;
            } else {
                merged[index++] = part;
            }
        }
        return merged;
    }
}
