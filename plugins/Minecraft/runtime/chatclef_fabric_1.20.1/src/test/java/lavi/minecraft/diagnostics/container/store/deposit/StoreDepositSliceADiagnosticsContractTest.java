package lavi.minecraft.diagnostics.container.store.deposit;

import adris.altoclef.tasks.container.ContainerStoredTracker;
import adris.altoclef.tasks.slot.MoveItemToSlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry.TrackerBinding;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositBudgetConstants;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerRouteState;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerRouteState.RouteCandidateReconciliation;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerRouteState.RouteMovementObservation;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationContext;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.effect.StoreDepositEffectDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.interaction.StoreDepositInteractionContext;
import lavi.minecraft.diagnostics.container.store.deposit.interaction.StoreDepositInteractionBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.interaction.StoreDepositInteractionDiagnosticFields;
import lavi.minecraft.diagnostics.container.store.deposit.route.StoreDepositMovementDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleLedger;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticTerminalDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositNotStoredDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositSlotActionDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositTransferAttemptRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositTransferSelectionSnapshot;
import lavi.minecraft.diagnostics.formatting.DiagnosticBoundedEventFormatter;
import lavi.minecraft.diagnostics.formatting.DiagnosticBoundedEventText;
import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import lavi.minecraft.diagnostics.interaction.BlockInteractionTargetInfo;
import lavi.minecraft.diagnostics.mode.DiagnosticModeController;
import lavi.minecraft.diagnostics.mode.DiagnosticOutputMode;
import lavi.minecraft.integration.carryon.CarryOnCarryState;
import lavi.minecraft.integration.carryon.CarryOnObservation;
import lavi.minecraft.task.container.deposit.auto.policy.diagnostics.AutoDepositPolicyItemSnapshot;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260830_kpopmodder: Lock all twenty-four Slice A diagnostics assertions into sixteen grouped scenarios.
class StoreDepositSliceADiagnosticsContractTest {

    @BeforeEach
    void startWithFreshDiagnosticSession() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    @DisplayName("scenario 1 [assertion 1]: aggregate x10 stays distinct from physical [10,64] selection")
    void aggregateTargetAndSelectedPhysicalSourceHaveSeparateIdentityAndCount() {
        ItemTarget aggregate = target(10);
        List<Integer> physicalCandidates = List.of(10, 64);
        StoreDepositTransferSelectionSnapshot snapshot = selection(aggregate, physicalCandidates.get(1));
        aggregate.infinite();
        Map<String, Object> fields = fields(snapshot.fields());

        assertEquals(List.of(10, 64), physicalCandidates);
        assertEquals(10, snapshot.aggregateTarget().getTargetCount());
        assertEquals(10, fields.get("aggregateTargetCount"));
        assertTrue(fields.containsKey("aggregateTargetItemId"));
        assertEquals(64, fields.get("selectedSourceCandidateCount"));
        assertEquals("fixture:physical-stack-64", fields.get("selectedSourceCandidateItemId"));
        assertNotEquals(fields.get("aggregateTargetItemId"), fields.get("selectedSourceCandidateItemId"));
        assertEquals(2, fields.get("potentialSourceSlotCount"));
        assertNotEquals(fields.get("aggregateTargetCount"), fields.get("selectedSourceCandidateCount"));

        AutoDepositPolicyItemSnapshot policyItem = AutoDepositPolicyItemSnapshot.pending(
                "policy-77-item-0",
                "minecraft:cobblestone",
                74,
                "0",
                "0",
                "NO_ACTIVE_WORKING_SET",
                "0",
                "NO_CATEGORY_RESERVE_ALLOCATED",
                "74",
                physicalCandidates,
                "DEPOSITABLE_SURPLUS",
                "GENERAL_SURPLUS",
                "GENERAL_CONTAINER"
        ).finalizeSelection(List.of(10), List.of(), false);
        assertEquals(List.of(10, 64), policyItem.eligibleWholeStackCounts());
        assertEquals(List.of(10), policyItem.selectedGeneralTargetCounts());
        assertEquals(10, policyItem.selectedAggregateTargetCount());
        assertEquals("GENERAL_CONTAINER", policyItem.destinationClass());
        assertEquals("SELECTED", policyItem.decision());
        assertEquals("PARTIAL_SELECTION_RELIEF_LIMIT", policyItem.decisionReason());
        assertTrue(policyItem.summary().contains("workingReason=NO_ACTIVE_WORKING_SET"));
        assertTrue(policyItem.summary().contains("reserveReason=NO_CATEGORY_RESERVE_ALLOCATED"));
    }

    @Test
    @DisplayName("scenario 2 [assertion 2]: physical/cursor x64 satisfies target x10 and leaves cursor x54")
    void partialPhysicalTransferKeepsLocalArithmeticSeparateFromDurability() throws IOException {
        StoreDepositTransferSelectionSnapshot snapshot = selection(target(10), 64);
        int terminalCursorCount = snapshot.selectedSourceCandidateCount()
                - snapshot.aggregateTarget().getTargetCount();

        assertEquals(54, terminalCursorCount);
        assertFalse(snapshot.acceptPartial());
        assertEquals("EMPTY_SLOT", snapshot.destinationSelectionKind());
        assertEquals(0, snapshot.destinationCountBefore());
        SlotHarness harness = automaticSlotHarness();
        TestObjects.setField(
                harness.activeTransfer(),
                StoreDepositTransferAttemptRegistry.ActiveTransfer.class,
                "terminalCursor",
                "fixture:physical-stackx" + terminalCursorCount
        );
        Map<String, Object> terminal = fields(
                harness.activeTransfer().terminalFields("TASK_STOP_BEGIN")
        );
        assertTrue(String.valueOf(terminal.get("terminalCursor")).endsWith("x54"));
        assertEquals("UNAVAILABLE", terminal.get("durableEffect"));
        assertEquals(false, terminal.get("stableOrServerSnapshotAvailable"));

        SlotHarness incomplete = automaticSlotHarness();
        assertEquals("UNAVAILABLE", field(
                incomplete.activeTransfer().terminalFields("LOCAL_CLICK_DID_NOT_RETURN"),
                "terminalCursor"
        ));
        String moveTask = source("src/main/java/adris/altoclef/tasks/slot/MoveItemToSlotTask.java");
        String slotMixin = source("src/main/java/adris/altoclef/mixins/SlotClickMixin.java");
        String transferDiagnostics = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/transfer/StoreDepositTransferDiagnostics.java"
        );
        String slotDiagnostics = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/transfer/StoreDepositSlotActionDiagnostics.java"
        );
        String transferRegistry = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/transfer/StoreDepositTransferAttemptRegistry.java"
        );
        assertTrue(moveTask.contains("currentHeld.getCount() + currentlyPlaced <= toMove.getTargetCount()"));
        assertTrue(moveTask.contains("clickSlot(destination, 1, SlotActionType.PICKUP)"));
        assertTrue(moveTask.contains("atDestination.getCount() >= toMove.getTargetCount()"));
        assertTrue(slotMixin.contains("return cursor == null ? null : cursor.copy()"));
        assertTrue(slotDiagnostics.contains("if (cursorAfter != null)"));
        assertTrue(slotDiagnostics.contains("action.cursorAfterObserved = true"));
        assertTrue(slotDiagnostics.contains("action.transfer.recordTerminalCursor(action.cursorAfter)"));
        assertTrue(transferRegistry.contains("terminalCursor = stack(cursorAfter)"));
        assertTrue(transferRegistry.contains("+ \"x\" + stack.getCount()"));
        assertTrue(transferDiagnostics.contains("STORE_DEPOSIT_TRANSFER_BEGIN"));
    }

    @Test
    @DisplayName("scenario 3 [assertions 3-4]: strict exact-fit rejection and destination churn replacement")
    void strictExactFitRejectsEqualityWhileDestinationIdentityParticipatesInChildEquality() throws IOException {
        int roomLeft = 63;
        int sourceCount = 63;
        boolean acceptPartial = false;
        assertFalse(acceptPartial || roomLeft > sourceCount);
        StoreDepositTransferSelectionSnapshot exactFit = new StoreDepositTransferSelectionSnapshot(
                new BlockPos(1, 64, 2), target(63), new PlayerSlot(9), "fixture:source", sourceCount,
                new PlayerSlot(10), "STACKABLE_EXISTING", 1, roomLeft, acceptPartial, 1
        );
        assertEquals(63, exactFit.roomLeft());
        assertEquals(63, exactFit.selectedSourceCandidateCount());
        assertEquals("STACKABLE_EXISTING", exactFit.destinationSelectionKind());
        assertFalse(exactFit.acceptPartial());

        String inventory = source("src/main/java/adris/altoclef/trackers/storage/InventorySubTracker.java");
        assertTrue(inventory.contains("if (acceptPartial || roomLeft > item.getCount())"));
        assertTrue(inventory.contains("result.add(airSlot)"));
        String selectionSource = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/transfer/StoreDepositTransferSelectionSnapshot.java"
        );
        assertTrue(selectionSource.contains("return \"EMPTY_SLOT\""));
        StoreDepositTransferSelectionSnapshot unavailableDestinationPrestate =
                new StoreDepositTransferSelectionSnapshot(
                        new BlockPos(1, 64, 2),
                        target(10),
                        new PlayerSlot(9),
                        "fixture:source",
                        10,
                        new PlayerSlot(10),
                        "UNAVAILABLE_NOT_RETAINED",
                        -1,
                        -1,
                        false,
                        2
                );
        Map<String, Object> unavailableDestinationFields = fields(
                unavailableDestinationPrestate.fields()
        );
        assertEquals("UNAVAILABLE_NOT_RETAINED",
                unavailableDestinationPrestate.destinationSelectionKind());
        assertEquals("UNAVAILABLE_NOT_RETAINED",
                unavailableDestinationFields.get("destinationCountBefore"));
        assertEquals("UNAVAILABLE_NOT_RETAINED", unavailableDestinationFields.get("roomLeft"));
        assertEquals(false,
                unavailableDestinationFields.get("destinationPrestateObservationComplete"));
        String storeTask = source("src/main/java/adris/altoclef/tasks/container/StoreInContainerTask.java");
        assertTrue(storeTask.contains("captureWithoutDestinationStack("));
        assertFalse(storeTask.contains("StorageHelper.getItemStackInSlot(destination)"));

        ItemTarget frozenTarget = target(10);
        ExposedMoveItemToSlotTask first = new ExposedMoveItemToSlotTask(frozenTarget, new PlayerSlot(9));
        ExposedMoveItemToSlotTask sameDestination = new ExposedMoveItemToSlotTask(frozenTarget, new PlayerSlot(9));
        ExposedMoveItemToSlotTask changedDestination = new ExposedMoveItemToSlotTask(frozenTarget, new PlayerSlot(10));
        assertTrue(first.sameTask(sameDestination));
        assertFalse(first.sameTask(changedDestination));
    }

    @Test
    @DisplayName("scenario 4 [assertion 5]: one slot action owns ordered multiple slot mutations")
    void oneSlotActionOwnsOrderedMutationIdentities() throws IOException {
        SlotHarness harness = automaticSlotHarness();
        long actionSequence = harness.activeTransfer().nextSlotActionSequence();
        String actionId = harness.activeTransfer().transferAttemptId() + "-action-" + actionSequence;
        String firstMutation = actionId + "-mutation-1";
        String secondMutation = actionId + "-mutation-2";

        assertNotEquals("UNAVAILABLE", actionId);
        assertEquals(actionId + "-mutation-1", firstMutation);
        assertEquals(actionId + "-mutation-2", secondMutation);
        assertNotEquals(firstMutation, secondMutation);
        String slotDiagnostics = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/transfer/StoreDepositSlotActionDiagnostics.java"
        );
        assertTrue(slotDiagnostics.contains(
                "transfer.transferAttemptId() + \"-action-\" + sequence"
        ));
        assertTrue(slotDiagnostics.contains(
                "action.slotActionId + \"-mutation-\" + ordinal"
        ));
        assertTrue(slotDiagnostics.contains("action.mutationCount++"));
        assertTrue(slotDiagnostics.contains("currentMutation.set(mutation)"));
    }

    @Test
    @DisplayName("scenario 5 [assertions 6-7]: both tracker roles share a mutation and local effect is not durable proof")
    void bothTrackerRolesShareMutationIdentityAndLocalMutationLeavesDurabilityUnavailable() throws IOException {
        SlotHarness harness = automaticSlotHarness();
        String actionId = harness.activeTransfer().transferAttemptId()
                + "-action-" + harness.activeTransfer().nextSlotActionSequence();
        String mutationId = actionId + "-mutation-1";
        TrackerBinding root = new TrackerBinding(
                "operation", "ROOT_ANY_CONTAINER", null, 1L, true
        );
        TrackerBinding target = new TrackerBinding(
                "operation", "TARGET_CONTAINER", new BlockPos(1, 64, 2), 1L, true
        );
        Map<String, String> mutationByTrackerRole = Map.of(
                root.trackerRole(), mutationId,
                target.trackerRole(), mutationId
        );

        assertEquals(Set.of("ROOT_ANY_CONTAINER", "TARGET_CONTAINER"), mutationByTrackerRole.keySet());
        assertEquals(Set.of(mutationId), Set.copyOf(mutationByTrackerRole.values()));
        String slotDiagnostics = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/transfer/StoreDepositSlotActionDiagnostics.java"
        );
        int observationStart = slotDiagnostics.indexOf("public void observeTrackerMutation(");
        int observationEnd = slotDiagnostics.indexOf("public Object[] currentMutationFields()", observationStart);
        String observationBody = slotDiagnostics.substring(observationStart, observationEnd);
        assertTrue(observationBody.contains("MutationContext mutation = activeMutation()"));
        assertTrue(observationBody.contains("mutation.trackerRoles.add(binding.trackerRole())"));
        assertFalse(observationBody.contains("new MutationContext"));
        assertTrue(slotDiagnostics.contains("\"trackerRolesObserved\", Set.copyOf(trackerRoles)"));
        assertTrue(slotDiagnostics.contains("\"mutationObservationSource\", \"LOCAL_INTERNAL_CLICK\""));
        assertTrue(slotDiagnostics.contains("\"stableOrServerSnapshotAvailable\", false"));
        assertTrue(slotDiagnostics.contains("\"durableEffect\", \"UNAVAILABLE\""));
        assertTrue(slotDiagnostics.contains(
                "\"missingBoundaries\", \"SERVER_SLOT_UPDATE,POST_ACTION_STABLE,HANDLER_REVISION\""
        ));
        String effectDiagnostics = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/effect/StoreDepositEffectDiagnostics.java"
        );
        assertTrue(effectDiagnostics.contains("currentSlotMutationId"));
    }

    @Test
    @DisplayName("scenario 6 [assertion 8]: tracker subscription generation and active-at-mutation state")
    void trackerResubscriptionAdvancesGenerationWithoutLosingActiveState() {
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        Task root = new TestTask("root");
        ContainerStoredTracker tracker = new ContainerStoredTracker(slot -> true);
        bindings.registerRoot(root, "AUTO_DEPOSIT_ALL_CHAIN", automaticContext());
        bindings.bindTracker(root, tracker, "ROOT_ANY_CONTAINER", null);

        TrackerBinding initial = bindings.trackerBinding(tracker);
        TrackerBinding firstStart = bindings.markTrackerSubscriptionStarted(tracker);
        TrackerBinding stopped = bindings.markTrackerSubscriptionStopped(tracker);
        TrackerBinding secondStart = bindings.markTrackerSubscriptionStarted(tracker);

        assertEquals(0L, initial.subscriptionGeneration());
        assertFalse(initial.subscriptionActive());
        assertEquals(1L, firstStart.subscriptionGeneration());
        assertTrue(firstStart.subscriptionActive());
        assertEquals(1L, stopped.subscriptionGeneration());
        assertFalse(stopped.subscriptionActive());
        assertEquals(2L, secondStart.subscriptionGeneration());
        assertTrue(secondStart.subscriptionActive());
        Map<String, Object> activeMutation = fields(effectFields(secondStart, true));
        Map<String, Object> inactiveMutation = fields(effectFields(stopped, true));
        assertEquals(2L, activeMutation.get("subscriptionGeneration"));
        assertEquals(true, activeMutation.get("subscriptionActiveAtMutation"));
        assertEquals(false, inactiveMutation.get("subscriptionActiveAtMutation"));
        assertEquals("1,64,2", activeMutation.get("lastBlockPosInteractionAtEvent"));
        assertEquals(true, activeMutation.get("predicateEvaluated"));
        assertEquals(true, activeMutation.get("predicateResult"));
    }

    @Test
    @DisplayName("scenario 7 [assertions 9-10]: signed deltas and ROOT/TARGET predicate asymmetry")
    void signedDeltasCoverPositiveNegativeReplacementAndTrackerPredicateAsymmetry() throws IOException {
        TrackerBinding root = new TrackerBinding("operation", "ROOT_ANY_CONTAINER", null, 1L, true);
        TrackerBinding target = new TrackerBinding(
                "operation", "TARGET_CONTAINER", new BlockPos(1, 64, 2), 1L, true
        );

        Object positive = signedDelta("stone", 3, "stone", 8);
        Object negative = signedDelta("stone", 8, "stone", 3);
        Object replacement = signedDelta("stone", 3, "dirt", 5);
        Map<String, Object> rootAccepted = fields(effectFields(root, true));
        Map<String, Object> targetRejected = fields(effectFields(target, false));

        assertEquals(5, positive);
        assertEquals(-5, negative);
        assertEquals("-3,5", replacement);
        assertEquals("ROOT_ANY_CONTAINER", rootAccepted.get("trackerRole"));
        assertEquals(true, rootAccepted.get("acceptPredicateResult"));
        assertEquals(true, rootAccepted.get("predicateEvaluated"));
        assertEquals(true, rootAccepted.get("predicateResult"));
        assertEquals("TARGET_CONTAINER", targetRejected.get("trackerRole"));
        assertEquals(false, targetRejected.get("acceptPredicateResult"));
        assertEquals(true, targetRejected.get("predicateEvaluated"));
        assertEquals(false, targetRejected.get("predicateResult"));

        //20260902_kpopmodder: Follow the effect-family implementation after preserving its facade entry points.
        String effectFields = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/event/effect/StoreDepositEffectEventFields.java"
        );
        String rootSource = source("src/main/java/adris/altoclef/tasks/container/DepositAllTask.java");
        String targetSource = source("src/main/java/adris/altoclef/tasks/container/StoreInContainerTask.java");
        assertTrue(effectFields.contains("return new Delta(2, itemName(before), -count(before), itemName(after), count(after))"));
        assertTrue(effectFields.contains("int amount = count(after) - count(before)"));
        assertTrue(rootSource.contains("new ContainerStoredTracker(slot -> true)"));
        assertTrue(targetSource.contains("openContainer.isPresent() && openContainer.get().equals(targetContainer)"));
    }

    @Test
    @DisplayName("scenario 8 [assertions 11-13]: notStored retains, caps, and removes targets")
    void notStoredProjectionPreservesFullPartialAndZeroAvailableSemantics() throws IOException {
        ItemTarget target10 = target(10);
        StoreDepositNotStoredDiagnostics.Decision full = new StoreDepositNotStoredDiagnostics.Decision(
                target10, 1, false, true, true, 10, false, -1, target10
        );
        ItemTarget partial9 = new ItemTarget(target10, 9);
        StoreDepositNotStoredDiagnostics.Decision partial = new StoreDepositNotStoredDiagnostics.Decision(
                target10, 0, false, true, true, 9, true, 9, partial9
        );
        StoreDepositNotStoredDiagnostics.Decision zero = new StoreDepositNotStoredDiagnostics.Decision(
                target10, 0, false, true, false, -1, false, -1, null
        );

        assertEquals(10, full.output().getTargetCount());
        assertEquals(10, full.combinedAvailableCount());
        assertEquals(9, partial.output().getTargetCount());
        assertEquals(9, partial.combinedAvailableCount());
        assertNull(zero.output());
        assertEquals(0, zero.combinedAvailableCount());
        assertEquals("REMOVED", fields(zero.fields()).get("notStoredOutput"));

        String tracker = source("src/main/java/adris/altoclef/tasks/container/ContainerStoredTracker.java");
        String depositAll = source("src/main/java/adris/altoclef/tasks/container/DepositAllTask.java");
        assertTrue(tracker.contains("if (firstAvailableCount < target.getTargetCount())"));
        assertTrue(tracker.contains("output = new ItemTarget(target, secondAvailableCount)"));
        assertTrue(tracker.contains("output = target"));
        assertEquals(2, occurrences(tracker, "mod.getItemStorage().getItemCount(target)"));
        assertTrue(depositAll.contains("getUnstoredItemTargetsYouCanStore(AltoClef.getInstance(), _toStore).length == 0"));
    }

    @Test
    @DisplayName("scenario 9 [assertion 14]: available count includes cursor and conversion input while breakdown stays unavailable")
    void availableCountCompositionUsesExistingCombinedValueWithoutDiagnosticRescan() throws IOException {
        int playerInventory = 8;
        int cursor = 2;
        int conversionInput = 4;
        int productionCombined = playerInventory + cursor + conversionInput;
        StoreDepositNotStoredDiagnostics.Decision decision = new StoreDepositNotStoredDiagnostics.Decision(
                target(20), 0, false, true, true, productionCombined, false, -1,
                new ItemTarget(target(20), productionCombined)
        );
        Map<String, Object> diagnosticFields = fields(decision.fields());

        assertEquals(14, decision.combinedAvailableCount());
        assertEquals("UNAVAILABLE", diagnosticFields.get("playerInventoryAvailableCount"));
        assertEquals("UNAVAILABLE", diagnosticFields.get("cursorAvailableCount"));
        assertEquals("UNAVAILABLE", diagnosticFields.get("conversionInputAvailableCount"));
        assertEquals("PLAYER_CURSOR_CONVERSION_COMPONENT_BREAKDOWN", diagnosticFields.get("missingBoundaries"));

        String storage = source("src/main/java/adris/altoclef/trackers/storage/ItemStorageTracker.java");
        String inventory = source("src/main/java/adris/altoclef/trackers/storage/InventorySubTracker.java");
        String diagnostics = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/transfer/StoreDepositNotStoredDiagnostics.java"
        );
        assertTrue(storage.contains("inventory.getItemCount(true, false, items) + inConversionSlots"));
        assertTrue(inventory.contains("if (includeCursor)"));
        assertTrue(inventory.contains("result.add(StorageHelper.getItemStackInCursorSlot())"));
        assertFalse(diagnostics.contains("getItemStorage()"));
        assertFalse(diagnostics.contains("getUnstoredItemTargetsYouCanStore("));
    }

    @Test
    @DisplayName("scenario 10 [assertion 15]: parent onTick and reconciliation precede child tick")
    void parentBeforeChildTickOrderingRemainsUnchanged() throws IOException {
        String task = source("src/main/java/adris/altoclef/tasksystem/Task.java");
        int parentTick = task.indexOf("Task newSub = onTick();");
        int reconciliation = task.indexOf("StoreDepositDiagnostics.logChildReconciliation(", parentTick);
        int childTick = task.indexOf("sub.tick(parentChain);", reconciliation);

        assertTrue(parentTick >= 0);
        assertTrue(reconciliation > parentTick);
        assertTrue(childTick > reconciliation);

        String maintenance = source(
                "src/main/java/lavi/minecraft/task/container/deposit/auto/maintenance/AutoDepositMaintenanceTask.java"
        );
        String maintenanceDiagnostics = source(
                "src/main/java/lavi/minecraft/task/container/deposit/auto/maintenance/diagnostics/AutoDepositMaintenanceDiagnostics.java"
        );
        int trustedBranch = maintenance.indexOf("case DEPOSIT_TRUSTED");
        int trustedRegistration = maintenance.indexOf(
                "diagnostics.registerTrustedChild(",
                trustedBranch
        );
        int trustedReturn = maintenance.indexOf("return trustedStore;", trustedRegistration);
        assertTrue(trustedBranch >= 0);
        assertTrue(trustedRegistration > trustedBranch);
        assertTrue(trustedReturn > trustedRegistration);
        assertTrue(maintenance.contains("diagnostics.registerGeneralChild("));
        assertTrue(maintenanceDiagnostics.contains("? generalTaskIndex + 1"));
        assertTrue(maintenanceDiagnostics.contains("ChatClefDiagnostics.isBoundaryEnabled()"));
        assertTrue(maintenanceDiagnostics.contains("registeredChild == childTask"));
        assertEquals(1, occurrences(
                maintenanceDiagnostics,
                "StoreDepositDiagnostics.registerAutomaticMaintenanceChild("
        ));
        String facade = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/StoreDepositDiagnostics.java"
        );
        int automaticChildMethod = facade.indexOf(
                "public static void registerAutomaticMaintenanceChild("
        );
        int existingChildGate = facade.indexOf(
                "StoreDepositOperationState existing = BINDINGS.stateFor(childTask);",
                automaticChildMethod
        );
        int terminalChildRegistration = facade.indexOf(
                "AUTOMATIC_TERMINALS.registerChild(maintenanceTask, childTask, childIndex);",
                existingChildGate
        );
        assertTrue(existingChildGate > automaticChildMethod);
        assertTrue(terminalChildRegistration > existingChildGate);
        assertTrue(facade.contains("if (primaryChild != null)"));
        String pressureChain = source(
                "src/main/java/lavi/minecraft/task/container/deposit/auto/DepositAllInventoryPressureChain.java"
        );
        assertFalse(pressureChain.contains("Task primaryDepositTask = task.primaryDepositTask()"));
        assertTrue(pressureChain.contains(
                "plan.context().epoch(),\n                    null"
        ));
    }

    @Test
    @DisplayName("scenario 11 [assertions 16-18]: invalidation, reconciliation, and unavailable provenance stay separate")
    void checkFalseInvalidationDoesNotCloseRouteChildOrReconstructProgressProvenance() throws IOException {
        StoreContainerRouteState route = new StoreContainerRouteState(0L);
        Object routeChild = new Object();
        BlockPos target = new BlockPos(4, 64, 8);
        route.stageRouteCandidate(routeChild, target, 3, true);
        RouteCandidateReconciliation installed = route.reconcileRouteCandidate(
                routeChild, false, true, routeChild
        );
        route.recordChildReconciliation("ROOT_ROUTE", null, routeChild, true, false);
        int replacementCountBeforeInvalidation = route.rootRouteChildReplacementCount();

        RouteMovementObservation invalidated = route.recordMovementObservation(
                target, "MOVEMENT_PROGRESS_FAILED", true, false, true
        );

        assertEquals("CANDIDATE_INSTALLED", installed.outcome());
        assertEquals(1L, invalidated.progressCheckInvocationId());
        assertTrue(invalidated.progressCheckEvaluated());
        assertFalse(invalidated.progressCheckOk());
        assertEquals(1L, invalidated.selectedCandidateGenerationBefore());
        assertEquals(0L, invalidated.selectedCandidateGenerationAfter());
        assertEquals(replacementCountBeforeInvalidation, route.rootRouteChildReplacementCount());
        assertTrue(route.isCurrentRouteChild(routeChild));

        route.recordChildReconciliation("ROOT_ROUTE", routeChild, null, true, true);
        assertFalse(route.isCurrentRouteChild(routeChild));
        assertEquals(replacementCountBeforeInvalidation + 1, route.rootRouteChildReplacementCount());

        String depositAll = source("src/main/java/adris/altoclef/tasks/container/DepositAllTask.java");
        String movement = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/route/StoreDepositMovementDiagnostics.java"
        );
        assertEquals(1, occurrences(depositAll, "_progressChecker.check(mod)"));
        assertEquals(1, occurrences(depositAll, "StoreDepositDiagnostics.observeAutomaticMovementResult("));
        assertFalse(movement.contains("_progressChecker"));
        assertFalse(movement.contains(".check("));
        assertFalse(movement.contains(".reset("));
        assertFalse(movement.contains("setProgress("));
        assertTrue(movement.contains("\"progressMode\", \"UNAVAILABLE\""));
        assertTrue(movement.contains("\"progressBaseline\", \"UNAVAILABLE\""));
        assertTrue(movement.contains("\"progressElapsed\", \"UNAVAILABLE\""));
        assertTrue(movement.contains("PROGRESS_MODE,BASELINE,ELAPSED"));
        assertFalse(movement.contains("String semanticKey = invocationId"));

        Task autoRoot = new TestTask("auto-root");
        StoreDepositOperationState autoState = new StoreDepositOperationState(
                new StoreDepositOperationContext("auto-store", "AUTO_DEPOSIT_ALL_CHAIN", autoRoot, 0L, 0L)
        );
        autoState.attachAutomaticContext(new StoreDepositAutomaticContext(
                true, 1L, 1L, "auto-1", "maintenance-1", "child-1", 0, "pressure-1"
        ));
        StoreContainerRouteState autoRoute = autoState.routeState();
        Task firstRouteChild = new TestTask("route-1");
        Task nextRouteChild = new TestTask("route-2");
        autoRoute.stageRouteCandidate(firstRouteChild, target, 1, true);
        autoRoute.reconcileRouteCandidate(firstRouteChild, false, true, firstRouteChild);
        autoRoute.recordChildReconciliation("ROOT_ROUTE", null, firstRouteChild, true, false);
        autoRoute.stageRouteCandidate(nextRouteChild, target.add(1, 0, 0), 2, true);
        Map<String, Object> activeRouteIdentity = fields(
                StoreDepositEventFields.activeRouteIdentityFields(autoState)
        );
        assertEquals("auto-store-candidate-1", activeRouteIdentity.get("selectedCandidateGenerationId"));
        assertEquals("auto-store-attempt-1", activeRouteIdentity.get("storeAttemptId"));
        assertEquals("auto-store-route-child-1", activeRouteIdentity.get("routeChildLifecycleId"));
    }

    @Test
    @DisplayName("scenario 12 [assertion 19]: HEAD binding verdict differs from observation-time verdict")
    void headCaptureVerdictAndObservationLookupVerdictRemainTypedAndIndependent() {
        Task root = new TestTask("root");
        StoreDepositOperationState state = new StoreDepositOperationState(
                new StoreDepositOperationContext("operation", "AUTO_DEPOSIT_ALL_CHAIN", root, 0L, 0L)
        );
        BlockPos target = new BlockPos(1, 64, 2);
        StoreDepositInteractionContext head = new StoreDepositInteractionContext(
                1L,
                "operation",
                "operation-attempt-1",
                1L,
                1L,
                1L,
                "OPEN_EXISTING",
                1L,
                "route.Child",
                "route-child",
                "route.Leaf",
                "route-leaf",
                0,
                "CURRENT_PURSUIT",
                target,
                "ACTIVE_TASK_IDENTITY_BINDING_AND_TARGET_VALUE",
                "EXACT",
                10L
        );
        state.routeState().recordParentDecision(
                "OBTAIN_CHEST", false, null, false, false, false, false,
                null, "NO_RAW_CLOSEST", "hash"
        );

        Map<String, Object> observation = fields(StoreDepositInteractionDiagnosticFields.fields(head, state));
        assertEquals("EXACT", observation.get("headBindingVerdict"));
        assertEquals("ROUTE_BRANCH_CHANGED", observation.get("observationBindingVerdict"));
        assertNotEquals(observation.get("headBindingVerdict"), observation.get("observationBindingVerdict"));
        StoreDepositInteractionBindingRegistry registry =
                new StoreDepositInteractionBindingRegistry();
        registry.bind(head, 10L);
        assertEquals(
                StoreDepositInteractionBindingRegistry.LookupStatus.FOUND,
                registry.lookup(head.interactionId(), 50L).status()
        );
        assertEquals(
                StoreDepositInteractionBindingRegistry.LookupStatus.EXPIRED,
                registry.lookup(head.interactionId(), 51L).status()
        );
        Map<String, Object> expired = fields(
                StoreDepositInteractionDiagnosticFields.unavailableObservationFields(
                        registry.lookup(head.interactionId(), 51L).context(),
                        StoreDepositInteractionBindingRegistry.LookupStatus.EXPIRED
                )
        );
        assertEquals("EXACT", expired.get("headBindingVerdict"));
        assertEquals("BINDING_EXPIRED", expired.get("observationBindingVerdict"));
    }

    @Test
    @DisplayName("scenario 13 [assertion 20]: Carry On temporal edge without identity is not exact attribution")
    void carryOnTemporalEdgeWithRetainedTargetAndUnavailableIdentityIsNotExactAttribution() throws Exception {
        BlockPos target = new BlockPos(1, 64, 2);
        BlockInteractionContext context = new BlockInteractionContext(
                91L,
                10L,
                new BlockInteractionTargetInfo(
                        true, "container", "minecraft:chest", "Chest", "facing=north", target
                ),
                null,
                null,
                null,
                true
        );
        CarryOnObservation before = CarryOnObservation.observed("2.1.2.7", false);
        CarryOnObservation after = CarryOnObservation.observed("2.1.2.7", true);

        Class<?> evidenceType = Class.forName(
                "lavi.minecraft.integration.carryon.container.CarryOnContainerPickupEvidence"
        );
        Method classify = evidenceType.getDeclaredMethod(
                "classify", BlockInteractionContext.class, CarryOnObservation.class
        );
        classify.setAccessible(true);
        Object evidence = classify.invoke(null, context, after);

        assertEquals(CarryOnCarryState.AVAILABLE_NOT_CARRYING, before.state());
        assertEquals(CarryOnCarryState.AVAILABLE_CARRYING, after.state());
        assertEquals(target, context.targetPosition());
        assertEquals("unavailable", after.carriedBlockId());
        assertEquals("STRONG_TEMPORAL_ATTRIBUTION", ((Enum<?>) evidence).name());
        assertNotEquals("CONFIRMED_TARGET_IDENTITY", ((Enum<?>) evidence).name());
    }

    @Test
    @DisplayName("scenario 14 [assertion 21]: lifecycle terminal scopes have separate exact counts")
    void automaticLifecycleScopesKeepSeparateExactCountsAndHandoffFlags() throws IOException {
        StoreDepositAutomaticLifecycleLedger ledger = new StoreDepositAutomaticLifecycleLedger();
        Task maintenance = new TestTask("maintenance");
        Task userRoot = new TestTask("user-root");
        Task itemRootOne = new TestTask("item-root-1");
        Task itemRootTwo = new TestTask("item-root-2");
        ledger.beginRun(maintenance, userRoot, 77L);
        StoreDepositAutomaticContext firstChild = ledger.registerChild(maintenance, itemRootOne, 0);
        StoreDepositAutomaticContext secondChild = ledger.registerChild(maintenance, itemRootTwo, 1);

        ledger.expectTerminalScopeIdentity(firstChild, "SLOT_MUTATION", "mutation-1");
        ledger.expectTerminalScopeIdentity(firstChild, "TRANSFER", "transfer-1");
        ledger.expectTerminalScopeIdentity(firstChild, "CANDIDATE_INVALIDATION", "candidate-1");
        ledger.expectTerminalScopeIdentity(firstChild, "ROUTE_RECONCILIATION", "reconcile-1");
        ledger.expectTerminalScopeIdentity(firstChild, "ROUTE_CHILD", "route-1");
        ledger.recordScope(firstChild, "SLOT_MUTATION", "mutation-1", "LOCAL_MUTATION");
        ledger.recordScope(firstChild, "TRANSFER", "transfer-1", "RECONCILED");
        ledger.recordScope(firstChild, "CANDIDATE_INVALIDATION", "candidate-1", "CHECK_FALSE");
        ledger.recordScope(firstChild, "ROUTE_RECONCILIATION", "reconcile-1", "REPLACED");
        ledger.recordScope(firstChild, "ROUTE_CHILD", "route-1", "STOPPED");
        ledger.recordScope(
                firstChild,
                "PER_ITEM_ROOT",
                StoreDepositOperationContext.identity(itemRootOne),
                "NATURAL_FINISH"
        );
        ledger.recordScope(
                secondChild,
                "PER_ITEM_ROOT",
                StoreDepositOperationContext.identity(itemRootTwo),
                "NATURAL_FINISH"
        );
        StoreDepositAutomaticLifecycleLedger.TerminalRecord duplicate = ledger.recordScope(
                firstChild, "TRANSFER", "transfer-1", "DUPLICATE"
        );
        ledger.recordMaintenanceTerminal(maintenance, "DONE", "RUNNING");
        ledger.recordPressureOwnedRunClose(maintenance, "OWNED_RUN_DONE");
        ledger.recordRunToWait(maintenance, "REARM", "WAIT_FOR_REARM");
        ledger.observeUserTaskResume(userRoot);
        ledger.observeUserTaskNaturalCompletion(userRoot);
        ledger.recordCoverageCloseForUserTask(userRoot, "USER_TASK_TERMINAL");

        StoreDepositAutomaticLifecycleLedger.Snapshot snapshot = ledger.snapshotFor(maintenance);
        assertTrue(duplicate.duplicate());
        assertEquals(firstChild.autoOperationId(), secondChild.autoOperationId());
        assertNotEquals(firstChild.autoChildOperationId(), secondChild.autoChildOperationId());
        assertEquals(1, snapshot.terminalCount("SLOT_MUTATION"));
        assertEquals(1, snapshot.terminalCount("TRANSFER"));
        assertEquals(1, snapshot.terminalCount("CANDIDATE_INVALIDATION"));
        assertEquals(1, snapshot.terminalCount("ROUTE_RECONCILIATION"));
        assertEquals(1, snapshot.terminalCount("ROUTE_CHILD"));
        assertEquals(1, snapshot.expectedTerminalCount("SLOT_MUTATION"));
        assertEquals(1, snapshot.expectedTerminalCount("TRANSFER"));
        assertEquals(1, snapshot.expectedTerminalCount("CANDIDATE_INVALIDATION"));
        assertEquals(1, snapshot.expectedTerminalCount("ROUTE_RECONCILIATION"));
        assertEquals(1, snapshot.expectedTerminalCount("ROUTE_CHILD"));
        assertEquals(2, snapshot.terminalCount("PER_ITEM_ROOT"));
        assertEquals(1, snapshot.terminalCount("MAINTENANCE_LOGICAL_TERMINAL"));
        assertEquals(1, snapshot.terminalCount("PRESSURE_OWNED_RUN"));
        assertEquals(1, snapshot.terminalCount("RUNNING_TO_WAIT_FOR_REARM"));
        assertEquals(1, snapshot.terminalCount("USER_TASK_RESUME"));
        assertEquals(1, snapshot.terminalCount("USER_TASK_NATURAL_COMPLETION"));
        assertEquals(1, snapshot.terminalCount("AUTOMATIC_DIAGNOSTIC_COVERAGE"));
        assertTrue(snapshot.maintenanceLogicalTerminal());
        assertTrue(snapshot.pressureOwnedRunClosed());
        assertTrue(snapshot.userTaskResumeObserved());
        assertTrue(snapshot.userTaskNaturalCompletionObserved());
        assertTrue(snapshot.diagnosticCoverageClosed());
        assertTrue(snapshot.lifecycleCoverageComplete());
        assertEquals("NONE", snapshot.missingLifecycleBoundaries());
        assertEquals("WAIT_FOR_REARM", snapshot.nextLifecycleState());

        Task secondMaintenance = new TestTask("maintenance-2");
        StoreDepositAutomaticContext secondRun = ledger.beginRun(secondMaintenance, userRoot, 78L);
        ledger.recordPressureOwnedRunClose(secondMaintenance, "OWNED_RUN_DONE");
        StoreDepositAutomaticLifecycleLedger.TerminalRecord secondResume =
                ledger.observeUserTaskResume(userRoot);
        List<StoreDepositAutomaticLifecycleLedger.TerminalRecord> naturalCompletions =
                ledger.observeUserTaskNaturalCompletions(userRoot);
        List<StoreDepositAutomaticLifecycleLedger.TerminalRecord> coverageCloses =
                ledger.recordCoverageClosesForUserTask(userRoot, "USER_TASK_TERMINAL");
        assertNotEquals(firstChild.autoOperationId(), secondRun.autoOperationId());
        assertEquals(secondRun.autoOperationId(), secondResume.context().autoOperationId());
        assertEquals(1, naturalCompletions.size());
        assertEquals(secondRun.autoOperationId(), naturalCompletions.get(0).context().autoOperationId());
        assertEquals(1, coverageCloses.size());
        assertEquals(secondRun.autoOperationId(), coverageCloses.get(0).context().autoOperationId());
        assertFalse(coverageCloses.get(0).snapshot().lifecycleCoverageComplete());
        assertTrue(coverageCloses.get(0).snapshot().missingLifecycleBoundaries()
                .contains("MAINTENANCE_LOGICAL_TERMINAL"));
        assertTrue(coverageCloses.get(0).snapshot().missingLifecycleBoundaries()
                .contains("RUNNING_TO_WAIT_FOR_REARM"));

        Task partialMaintenance = new TestTask("maintenance-partial-child-close");
        Task partialUserRoot = new TestTask("user-root-partial-child-close");
        Task partialChildOne = new TestTask("partial-child-1");
        Task partialChildTwo = new TestTask("partial-child-2");
        StoreDepositAutomaticContext partialContext = ledger.beginRun(
                partialMaintenance,
                partialUserRoot,
                79L
        );
        ledger.registerChild(partialMaintenance, partialChildOne, 0);
        ledger.registerChild(partialMaintenance, partialChildTwo, 1);
        ledger.recordScope(
                partialContext,
                "PER_ITEM_ROOT",
                StoreDepositOperationContext.identity(partialChildOne),
                "NATURAL_FINISH"
        );
        ledger.recordMaintenanceTerminal(partialMaintenance, "DONE", "RUNNING");
        ledger.recordPressureOwnedRunClose(partialMaintenance, "OWNED_RUN_DONE");
        ledger.recordRunToWait(partialMaintenance, "REARM", "WAIT_FOR_REARM");
        ledger.observeUserTaskResume(partialUserRoot);
        ledger.observeUserTaskNaturalCompletion(partialUserRoot);
        StoreDepositAutomaticLifecycleLedger.TerminalRecord partialCoverage =
                ledger.recordCoverageCloseForUserTask(
                        partialUserRoot,
                        "USER_TASK_TERMINAL"
                );
        assertEquals(2, partialCoverage.snapshot().registeredChildCount());
        assertEquals(1, partialCoverage.snapshot().terminalCount("PER_ITEM_ROOT"));
        assertFalse(partialCoverage.snapshot().lifecycleCoverageComplete());
        assertTrue(partialCoverage.snapshot().missingLifecycleBoundaries()
                .contains("PER_ITEM_ROOT_TERMINALS(expected=2,observed=1)"));

        Task wrongChildMaintenance = new TestTask("maintenance-wrong-child-identities");
        Task expectedChildOne = new TestTask("expected-child-1");
        Task expectedChildTwo = new TestTask("expected-child-2");
        StoreDepositAutomaticContext wrongChildContext = ledger.beginRun(
                wrongChildMaintenance,
                null,
                80L
        );
        ledger.registerChild(wrongChildMaintenance, expectedChildOne, 0);
        ledger.registerChild(wrongChildMaintenance, expectedChildTwo, 1);
        ledger.recordScope(wrongChildContext, "PER_ITEM_ROOT", "unrelated-child-x", "NATURAL_FINISH");
        ledger.recordScope(wrongChildContext, "PER_ITEM_ROOT", "unrelated-child-y", "NATURAL_FINISH");
        ledger.recordMaintenanceTerminal(wrongChildMaintenance, "DONE", "RUNNING");
        ledger.recordPressureOwnedRunClose(wrongChildMaintenance, "OWNED_RUN_DONE");
        ledger.recordRunToWait(wrongChildMaintenance, "REARM", "WAIT_FOR_REARM");
        StoreDepositAutomaticLifecycleLedger.TerminalRecord wrongChildCoverage =
                ledger.recordCoverageClose(
                        wrongChildMaintenance,
                        "NO_USER_TASK_COVERAGE_CLOSE"
                );
        assertEquals(2, wrongChildCoverage.snapshot().terminalCount("PER_ITEM_ROOT"));
        assertFalse(wrongChildCoverage.snapshot().terminalIdentityCoverageComplete());
        assertTrue(wrongChildCoverage.snapshot().missingLifecycleBoundaries()
                .contains("MISSING_PER_ITEM_ROOT_TERMINAL_IDENTITIES"));
        assertTrue(wrongChildCoverage.snapshot().missingLifecycleBoundaries()
                .contains("UNEXPECTED_PER_ITEM_ROOT_TERMINAL_IDENTITIES"));

        Task missingTransferMaintenance = new TestTask("maintenance-missing-transfer-close");
        StoreDepositAutomaticContext missingTransferContext = ledger.beginRun(
                missingTransferMaintenance,
                null,
                81L
        );
        ledger.expectTerminalScopeIdentity(
                missingTransferContext,
                "TRANSFER",
                "transfer-open-without-close"
        );
        ledger.expectTerminalScopeIdentity(
                missingTransferContext,
                "ROUTE_CHILD",
                "route-child-expected"
        );
        ledger.recordScope(
                missingTransferContext,
                "ROUTE_CHILD",
                "route-child-unexpected",
                "WRONG_IDENTITY_CLOSE"
        );
        ledger.recordScope(
                missingTransferContext,
                "CANDIDATE_INVALIDATION",
                "candidate-invalidation-unexpected",
                "WRONG_CANDIDATE_IDENTITY_CLOSE"
        );
        ledger.expectTerminalScopeIdentity(
                missingTransferContext,
                "CANDIDATE_INVALIDATION",
                "candidate-invalidation-expected"
        );
        ledger.recordScope(
                missingTransferContext,
                "CANDIDATE_OBSERVATION_WITHOUT_EXPECTATION",
                "",
                "MISSING_CANDIDATE_IDENTITY"
        );
        ledger.recordScope(
                missingTransferContext,
                "PER_ITEM_ROOT",
                "implicit-item-root",
                "NATURAL_FINISH"
        );
        ledger.recordMaintenanceTerminal(missingTransferMaintenance, "DONE", "RUNNING");
        ledger.recordPressureOwnedRunClose(missingTransferMaintenance, "OWNED_RUN_DONE");
        ledger.recordRunToWait(missingTransferMaintenance, "REARM", "WAIT_FOR_REARM");
        StoreDepositAutomaticLifecycleLedger.TerminalRecord missingTransferCoverage =
                ledger.recordCoverageClose(
                        missingTransferMaintenance,
                        "NO_USER_TASK_COVERAGE_CLOSE"
                );
        assertFalse(missingTransferCoverage.snapshot().lifecycleCoverageComplete());
        assertTrue(missingTransferCoverage.snapshot().missingLifecycleBoundaries()
                .contains("EXPECTED_TRANSFER_TERMINALS(expected=1,observed=0)"));
        assertTrue(missingTransferCoverage.snapshot().missingLifecycleBoundaries()
                .contains("MISSING_ROUTE_CHILD_TERMINAL_IDENTITIES"));
        assertTrue(missingTransferCoverage.snapshot().missingLifecycleBoundaries()
                .contains("UNEXPECTED_ROUTE_CHILD_TERMINAL_IDENTITIES"));
        assertTrue(missingTransferCoverage.snapshot().missingLifecycleBoundaries()
                .contains("MISSING_CANDIDATE_INVALIDATION_TERMINAL_IDENTITIES"));
        assertTrue(missingTransferCoverage.snapshot().missingLifecycleBoundaries()
                .contains("UNEXPECTED_CANDIDATE_INVALIDATION_TERMINAL_IDENTITIES"));
        assertTrue(missingTransferCoverage.snapshot().missingLifecycleBoundaries()
                .contains("UNAVAILABLE_TERMINAL_IDENTITIES"));
        assertFalse(missingTransferCoverage.snapshot().terminalIdentityCoverageComplete());

        Task unavailableExpectedMaintenance = new TestTask("maintenance-unavailable-expected-identity");
        StoreDepositAutomaticContext unavailableExpectedContext = ledger.beginRun(
                unavailableExpectedMaintenance,
                null,
                82L
        );
        ledger.expectTerminalScopeIdentity(
                unavailableExpectedContext,
                "TRANSFER",
                ""
        );
        ledger.recordScope(
                unavailableExpectedContext,
                "PER_ITEM_ROOT",
                "implicit-item-root",
                "NATURAL_FINISH"
        );
        ledger.recordMaintenanceTerminal(unavailableExpectedMaintenance, "DONE", "RUNNING");
        ledger.recordPressureOwnedRunClose(unavailableExpectedMaintenance, "OWNED_RUN_DONE");
        ledger.recordRunToWait(unavailableExpectedMaintenance, "REARM", "WAIT_FOR_REARM");
        StoreDepositAutomaticLifecycleLedger.TerminalRecord unavailableExpectedCoverage =
                ledger.recordCoverageClose(
                        unavailableExpectedMaintenance,
                        "NO_USER_TASK_COVERAGE_CLOSE"
                );
        assertFalse(unavailableExpectedCoverage.snapshot().lifecycleCoverageComplete());
        assertFalse(unavailableExpectedCoverage.snapshot().terminalIdentityCoverageComplete());
        assertTrue(unavailableExpectedCoverage.snapshot().missingLifecycleBoundaries()
                .contains("EXPECTED_TERMINAL_IDENTITY|TRANSFER|UNAVAILABLE"));

        StoreDepositAutomaticLifecycleLedger evictionCoverageLedger =
                new StoreDepositAutomaticLifecycleLedger();
        StoreDepositEmissionGate evictionCoverageGate = new StoreDepositEmissionGate();
        StoreDepositAutomaticTerminalDiagnostics evictionCoverageTerminals =
                new StoreDepositAutomaticTerminalDiagnostics(
                        evictionCoverageLedger,
                        evictionCoverageGate
                );
        StoreDepositBindingRegistry evictionCoverageBindings = new StoreDepositBindingRegistry();
        Task evictionCoverageMaintenance = new TestTask("maintenance-transfer-registry-cap");
        Task evictionCoverageParent = new TestTask("transfer-registry-parent");
        ChatClefDiagnostics.setBoundaryEnabled(true);
        StoreDepositAutomaticContext evictionCoverageContext = evictionCoverageTerminals.beginRun(
                evictionCoverageMaintenance,
                null,
                83L
        );
        evictionCoverageBindings.registerRoot(
                evictionCoverageParent,
                "AUTO_DEPOSIT_ALL_CHAIN",
                evictionCoverageContext
        );
        StoreDepositTransferAttemptRegistry evictionCoverageRegistry =
                new StoreDepositTransferAttemptRegistry(
                        evictionCoverageBindings,
                        evictionCoverageTerminals
                );
        String evictionCoverageOutput = captureOutput(() -> {
            for (int index = 0; index < 257; index++) {
                Task candidateTask = new TestTask("transfer-registry-candidate-" + index);
                evictionCoverageRegistry.stage(
                        evictionCoverageParent,
                        candidateTask,
                        selection(target(10), 64)
                );
                evictionCoverageRegistry.reconcile(
                        evictionCoverageParent,
                        null,
                        candidateTask,
                        false,
                        true,
                        candidateTask
                );
            }
        });
        StoreDepositAutomaticLifecycleLedger.Snapshot evictionCoverageSnapshot =
                evictionCoverageLedger.snapshotFor(evictionCoverageMaintenance);
        assertEquals(0, evictionCoverageSnapshot.terminalCount("TRANSFER"));
        assertEquals(257, evictionCoverageSnapshot.expectedTerminalCount("TRANSFER"));
        assertFalse(evictionCoverageSnapshot.terminalIdentityCoverageComplete());
        assertTrue(evictionCoverageSnapshot.missingLifecycleBoundaries()
                .contains("TRANSFER_ATTEMPT_REGISTRY"));
        assertTrue(evictionCoverageOutput.contains("STORE_DEPOSIT_AUTOMATIC_COVERAGE_GAP"));
        assertTrue(evictionCoverageOutput.contains("DIAGNOSTIC_REGISTRY_EVICTED"));

        StoreDepositAutomaticLifecycleLedger stagedEvictionLedger =
                new StoreDepositAutomaticLifecycleLedger();
        StoreDepositEmissionGate stagedEvictionGate = new StoreDepositEmissionGate();
        StoreDepositAutomaticTerminalDiagnostics stagedEvictionTerminals =
                new StoreDepositAutomaticTerminalDiagnostics(
                        stagedEvictionLedger,
                        stagedEvictionGate
                );
        StoreDepositBindingRegistry stagedEvictionBindings = new StoreDepositBindingRegistry();
        Task stagedEvictionMaintenance = new TestTask("maintenance-staged-transfer-registry-cap");
        Task stagedEvictionParent = new TestTask("staged-transfer-registry-parent");
        StoreDepositAutomaticContext stagedEvictionContext = stagedEvictionTerminals.beginRun(
                stagedEvictionMaintenance,
                null,
                84L
        );
        stagedEvictionBindings.registerRoot(
                stagedEvictionParent,
                "AUTO_DEPOSIT_ALL_CHAIN",
                stagedEvictionContext
        );
        StoreDepositTransferAttemptRegistry stagedEvictionRegistry =
                new StoreDepositTransferAttemptRegistry(
                        stagedEvictionBindings,
                        stagedEvictionTerminals
                );
        String stagedEvictionOutput = captureOutput(() -> {
            for (int index = 0; index < 257; index++) {
                stagedEvictionRegistry.stage(
                        stagedEvictionParent,
                        new TestTask("staged-transfer-registry-candidate-" + index),
                        selection(target(10), 64)
                );
            }
        });
        StoreDepositAutomaticLifecycleLedger.Snapshot stagedEvictionSnapshot =
                stagedEvictionLedger.snapshotFor(stagedEvictionMaintenance);
        assertEquals(0, stagedEvictionSnapshot.expectedTerminalCount("TRANSFER"));
        assertEquals(0, stagedEvictionSnapshot.terminalCount("TRANSFER"));
        assertFalse(stagedEvictionSnapshot.terminalIdentityCoverageComplete());
        assertTrue(stagedEvictionSnapshot.missingLifecycleBoundaries()
                .contains("STAGED_TRANSFER_ATTEMPT_REGISTRY"));
        assertTrue(stagedEvictionOutput.contains("STORE_DEPOSIT_AUTOMATIC_COVERAGE_GAP"));

        String transferRegistry = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/transfer/StoreDepositTransferAttemptRegistry.java"
        );
        String slotActions = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/transfer/StoreDepositSlotActionDiagnostics.java"
        );
        String movement = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/route/StoreDepositMovementDiagnostics.java"
        );
        String childReconciliation = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/lifecycle/StoreDepositChildReconciliationDiagnostics.java"
        );
        assertTrue(transferRegistry.indexOf("active.put(candidateChild, promoted);")
                < transferRegistry.indexOf("\"TRANSFER\",\n                    transferAttemptId"));
        assertTrue(slotActions.contains("\"SLOT_ACTION\",\n                action.slotActionId"));
        assertTrue(slotActions.contains("\"SLOT_MUTATION\",\n                mutation.slotMutationId"));
        assertTrue(movement.contains("\"ROUTE_RECONCILIATION\","));
        int routeStateReconciliation = childReconciliation.indexOf(
                "state.routeState().recordChildReconciliation("
        );
        int routeChildExpectation = childReconciliation.indexOf(
                "movementDiagnostics.expectActiveRouteChildTerminal("
        );
        assertTrue(routeStateReconciliation >= 0);
        assertTrue(routeChildExpectation > routeStateReconciliation);
        assertTrue(movement.contains(
                "\"CANDIDATE_INVALIDATION\",\n                    generationBefore"
        ));
        assertTrue(movement.indexOf(
                "automaticTerminals.expectScopeIdentity(\n                    state.automaticContext(),\n                    \"CANDIDATE_INVALIDATION\""
        ) < movement.indexOf(
                "automaticTerminals.recordScopeIdentity(\n                    state.automaticContext(),\n                    \"CANDIDATE_INVALIDATION\""
        ));
    }

    @Test
    @DisplayName("scenario 15 [assertion 22]: modes, dedupe, rate, payload, session cap, and suppression summary")
    void diagnosticsModesAndAllBoundednessControlsRemainExplicit() throws IOException {
        DiagnosticModeController controller = new DiagnosticModeController(DiagnosticOutputMode.OFF);
        assertTrue(controller.isOff());
        assertFalse(controller.isBoundaryEnabled());
        controller.setBoundaryEnabled(true);
        assertEquals(DiagnosticOutputMode.BOUNDARY, controller.current());
        assertTrue(controller.isBoundaryEnabled());
        assertFalse(controller.isVerboseEnabled());
        DiagnosticModeController verbose = new DiagnosticModeController(DiagnosticOutputMode.VERBOSE);
        assertTrue(verbose.isBoundaryEnabled());
        assertTrue(verbose.isVerboseEnabled());

        StoreDepositEmissionGate dedupeAndRate = new StoreDepositEmissionGate();
        assertTrue(dedupeAndRate.shouldEmitDetail("operation", "STORE_DEPOSIT_SLOT_ACTION", "same"));
        assertFalse(dedupeAndRate.shouldEmitDetail("operation", "STORE_DEPOSIT_SLOT_ACTION", "same"));
        for (int index = 1; index < 16; index++) {
            assertTrue(dedupeAndRate.shouldEmitDetail(
                    "operation", "STORE_DEPOSIT_SLOT_ACTION", "distinct-" + index
            ));
        }
        assertFalse(dedupeAndRate.shouldEmitDetail(
                "operation", "STORE_DEPOSIT_SLOT_ACTION", "over-family-cap"
        ));
        assertTrue(dedupeAndRate.shouldEmitDetail(
                "operation", "STORE_DEPOSIT_MOVEMENT_RESULT", "movement-family-remains-available"
        ));
        assertTrue(dedupeAndRate.shouldEmitDetail(
                "operation", "STORE_DEPOSIT_SLOT_MUTATION", "mutation-family-remains-available"
        ));
        assertTrue(String.valueOf(field(
                dedupeAndRate.budgetSummaryFields("operation"),
                "storeBudgetOperationSuppressedCounts"
        )).contains("STORE_DEPOSIT_SLOT_ACTION=2"));

        StoreDepositEmissionGate policyWorstCase = new StoreDepositEmissionGate();
        assertTrue(policyWorstCase.shouldEmitDetail(
                "policy-operation", "AUTO_DEPOSIT_POLICY_SNAPSHOT", "snapshot"
        ));
        for (int index = 0; index < 41; index++) {
            assertTrue(policyWorstCase.shouldEmitDetail(
                    "policy-operation", "AUTO_DEPOSIT_POLICY_ITEM_DECISION", "item-" + index
            ));
        }
        for (int index = 0; index < 41; index++) {
            assertTrue(policyWorstCase.shouldEmitDetail(
                    "policy-operation", "AUTO_DEPOSIT_POLICY_STACK_FACT", "stack-" + index
            ));
        }
        assertTrue(policyWorstCase.shouldEmitDetail(
                "policy-operation", "AUTO_DEPOSIT_POLICY_SNAPSHOT", "bounded-headroom"
        ));
        assertFalse(policyWorstCase.shouldEmitDetail(
                "policy-operation", "AUTO_DEPOSIT_POLICY_SNAPSHOT", "over-family-cap"
        ));

        StoreDepositEmissionGate session = new StoreDepositEmissionGate();
        for (int index = 0; index < StoreDepositBudgetConstants.NONCRITICAL_DETAIL_CAP; index++) {
            assertTrue(session.shouldEmitDetail("operation-" + index, "SLICE_A_UNCATEGORIZED", "key"));
        }
        assertFalse(session.shouldEmitDetail("operation-over-cap", "SLICE_A_UNCATEGORIZED", "key"));
        assertEquals(StoreDepositBudgetConstants.NONCRITICAL_DETAIL_CAP, field(
                session.budgetSummaryFields("operation-over-cap"),
                "storeBudgetSessionDetailEmittedCount"
        ));

        StoreDepositEmissionGate lateSummaryGate = new StoreDepositEmissionGate();
        for (int index = 0; index < StoreDepositBudgetConstants.MAX_LATE_SUMMARIES; index++) {
            assertTrue(lateSummaryGate.shouldEmitLateSummary("automatic-operation-" + index));
        }
        assertFalse(lateSummaryGate.shouldEmitLateSummary("automatic-operation-over-cap"));
        assertTrue(lateSummaryGate.shouldEmitCoverageSuppressionSummary());
        assertFalse(lateSummaryGate.shouldEmitCoverageSuppressionSummary());
        assertEquals(
                StoreDepositBudgetConstants.CRITICAL_RESERVE_CAP,
                StoreDepositBudgetConstants.MAX_TERMINAL_GROUPS
                        * StoreDepositBudgetConstants.TERMINAL_GROUP_EVENT_COUNT
                        + StoreDepositBudgetConstants.MAX_EXCEPTION_SIGNATURES
                        + StoreDepositBudgetConstants.MAX_LATE_SUMMARIES
                        + StoreDepositBudgetConstants.MAX_CONTROL_EVENTS
                        + StoreDepositBudgetConstants.MAX_COVERAGE_SUPPRESSION_SUMMARIES
        );

        StoreDepositEmissionGate terminalExhaustionGate = new StoreDepositEmissionGate();
        for (int index = 0; index < StoreDepositBudgetConstants.MAX_TERMINAL_GROUPS; index++) {
            assertTrue(terminalExhaustionGate.reserveTerminalGroup(
                    "reserved-terminal-operation-" + index
            ).reserved());
        }
        for (int index = 0; index < 1_000; index++) {
            assertTrue(terminalExhaustionGate.reserveTerminalGroup(
                    "exhausted-terminal-operation-" + index
            ).exhausted());
        }
        assertEquals(1_000L, field(
                terminalExhaustionGate.budgetSummaryFields("terminal-budget-check"),
                "storeBudgetTerminalReserveExhaustedOperations"
        ));

        StoreDepositEmissionGate saturatedCriticalGate = new StoreDepositEmissionGate();
        for (int index = 0; index < StoreDepositBudgetConstants.MAX_CONTROL_EVENTS; index++) {
            assertTrue(saturatedCriticalGate.shouldEmitControl(
                    "control-operation-" + index,
                    "control-event-" + index
            ));
        }
        assertFalse(saturatedCriticalGate.shouldEmitControl(
                "control-operation-over-cap",
                "control-event-over-cap"
        ));
        for (int index = 0; index < StoreDepositBudgetConstants.MAX_LATE_SUMMARIES; index++) {
            assertTrue(saturatedCriticalGate.shouldEmitLateSummary("late-operation-" + index));
        }
        assertFalse(saturatedCriticalGate.shouldEmitLateSummary("late-operation-over-cap"));

        StoreDepositAutomaticLifecycleLedger saturatedLedger =
                new StoreDepositAutomaticLifecycleLedger();
        StoreDepositAutomaticTerminalDiagnostics saturatedTerminals =
                new StoreDepositAutomaticTerminalDiagnostics(saturatedLedger, saturatedCriticalGate);
        ChatClefDiagnostics.setBoundaryEnabled(true);
        String coverageSuppressionOutput = captureOutput(() -> {
            Task firstSuppressedCoverage = new TestTask("first-suppressed-coverage");
            saturatedTerminals.beginRun(firstSuppressedCoverage, null, 91L);
            saturatedTerminals.recordCoverageClosed(
                    firstSuppressedCoverage,
                    "FIRST_COVERAGE_CLOSE_AFTER_ALL_SHARED_CAPS"
            );
            Task secondSuppressedCoverage = new TestTask("second-suppressed-coverage");
            saturatedTerminals.beginRun(secondSuppressedCoverage, null, 92L);
            saturatedTerminals.recordCoverageClosed(
                    secondSuppressedCoverage,
                    "SECOND_COVERAGE_CLOSE_AFTER_ALL_SHARED_CAPS"
            );
        });
        assertEquals(1, occurrences(
                coverageSuppressionOutput,
                "STORE_DEPOSIT_AUTOMATIC_COVERAGE_SUPPRESSION_SUMMARY"
        ));
        assertTrue(coverageSuppressionOutput.contains("FIRST_COVERAGE_CLOSE_AFTER_ALL_SHARED_CAPS"));
        assertTrue(coverageSuppressionOutput.contains("missingLifecycleBoundaries"));
        assertFalse(coverageSuppressionOutput.contains("SECOND_COVERAGE_CLOSE_AFTER_ALL_SHARED_CAPS"));

        DiagnosticBoundedEventText encoded = DiagnosticBoundedEventFormatter.format(
                "[SliceA] ",
                new Object[]{"event", "terminal"},
                new Object[]{"terminalReason", "x".repeat(500)},
                new Object[]{"optional", "y".repeat(500)},
                192
        );
        assertTrue(DiagnosticBoundedEventFormatter.utf8Length(encoded.text()) <= 192);
        assertTrue(encoded.partial());

        StoreDepositAutomaticLifecycleLedger terminalLedger =
                new StoreDepositAutomaticLifecycleLedger();
        Task maintenance = new TestTask("suppressed-maintenance");
        StoreDepositAutomaticContext terminalContext = terminalLedger.beginRun(
                maintenance,
                null,
                1L
        );
        StoreDepositAutomaticLifecycleLedger.TerminalRecord suppressedTerminal =
                terminalLedger.recordScope(
                        terminalContext,
                        "TRANSFER",
                        "transfer-suppressed",
                        "TASK_STOP_BEGIN"
                );
        StoreDepositAutomaticLifecycleLedger.Snapshot suppressedSnapshot =
                terminalLedger.recordTerminalEmissionSuppressed(suppressedTerminal);
        assertFalse(suppressedSnapshot.terminalIdentityCoverageComplete());
        assertEquals(1L, suppressedSnapshot.terminalEmissionSuppressedCount());
        assertEquals(1, suppressedSnapshot.terminalEmissionSuppressedCounts().get("TRANSFER"));
        assertEquals("TASK_STOP_BEGIN", suppressedSnapshot.terminalEmissionSuppressedReasons().get("TRANSFER"));

        StoreDepositAutomaticLifecycleLedger evictionLedger =
                new StoreDepositAutomaticLifecycleLedger();
        List<Task> maintenanceRuns = new ArrayList<>();
        for (int index = 0; index < 16; index++) {
            Task run = new TestTask("bounded-run-" + index);
            maintenanceRuns.add(run);
            evictionLedger.beginRun(run, null, index);
        }
        assertTrue(evictionLedger.contextForMaintenance(maintenanceRuns.get(0)).available());
        Task seventeenth = new TestTask("bounded-run-16");
        evictionLedger.beginRun(seventeenth, null, 16L);
        StoreDepositAutomaticLifecycleLedger.TerminalRecord eviction =
                evictionLedger.takePendingEviction();
        assertTrue(eviction.available());
        assertEquals("ACTIVE_RUN_LEDGER_CAP_EVICTION", eviction.terminalReason());
        assertFalse(eviction.snapshot().terminalIdentityCoverageComplete());
        assertEquals(1L, eviction.snapshot().activeRunLedgerEvictionCount());
        assertTrue(evictionLedger.contextForMaintenance(maintenanceRuns.get(0)).available());
        assertFalse(evictionLedger.contextForMaintenance(maintenanceRuns.get(1)).available());
        StoreDepositAutomaticLifecycleLedger.Snapshot suppressedEviction =
                evictionLedger.recordTerminalEmissionSuppressed(eviction);
        assertEquals(1L, suppressedEviction.activeRunEvictionEmissionSuppressedCount());
        assertEquals(1L, suppressedEviction.terminalEmissionSuppressedCount());

        String boundedLogger = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/budget/StoreDepositBoundedEventLogger.java"
        );
        String transferEmitter = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/transfer/StoreDepositTransferDiagnostics.java"
        );
        String movementEmitter = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/route/StoreDepositMovementDiagnostics.java"
        );
        String terminalEmitter = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/terminal/StoreDepositAutomaticTerminalDiagnostics.java"
        );
        String storeFacade = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/StoreDepositDiagnostics.java"
        );
        String policyEmitter = source(
                "src/main/java/lavi/minecraft/task/container/deposit/auto/policy/diagnostics/AutoDepositPolicyDiagnostics.java"
        );
        String criticalBudget = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/budget/StoreDepositCriticalBudget.java"
        );
        assertTrue(boundedLogger.contains("ChatClefDiagnostics.logBoundedBoundary"));
        assertTrue(boundedLogger.contains("MAX_EVENT_UTF8_BYTES = 8192"));
        assertFalse(transferEmitter.contains("ChatClefDiagnostics.logBoundary("));
        assertFalse(movementEmitter.contains("ChatClefDiagnostics.logBoundary("));
        assertFalse(terminalEmitter.contains("ChatClefDiagnostics.logBoundary("));
        assertTrue(transferEmitter.contains("StoreDepositBoundedEventLogger.log("));
        assertTrue(movementEmitter.contains("StoreDepositBoundedEventLogger.log("));
        assertTrue(terminalEmitter.contains("StoreDepositBoundedEventLogger.log("));
        assertTrue(storeFacade.contains("StoreDepositSharedBudget.emissionGate()"));
        assertTrue(policyEmitter.contains("StoreDepositSharedBudget.emissionGate()"));
        assertTrue(policyEmitter.contains("StoreDepositBoundedEventLogger.log("));
        assertTrue(policyEmitter.contains("AUTO_DEPOSIT_POLICY_STACK_FACT"));
        assertTrue(policyEmitter.contains("snapshot.automaticContext().autoOperationId()"));
        assertTrue(terminalEmitter.contains("STORE_DEPOSIT_AUTOMATIC_COVERAGE_SUPPRESSION_SUMMARY"));
        assertTrue(terminalEmitter.contains("shouldEmitCoverageSuppressionSummary()"));
        assertTrue(terminalEmitter.contains("snapshot.missingLifecycleBoundaries()"));
        assertFalse(criticalBudget.contains("exhaustedTerminalOperations"));
        assertTrue(criticalBudget.contains("long exhaustedTerminalOperationCount"));
        assertTrue(coverageSuppressionOutput.contains("diagnosticsMode=BOUNDARY"));
        assertTrue(coverageSuppressionOutput.contains("gameTick="));
        assertTrue(coverageSuppressionOutput.contains("dimension="));
        assertTrue(coverageSuppressionOutput.contains("topLevelTask="));
        assertTrue(coverageSuppressionOutput.contains("activeParentTask="));
        assertTrue(coverageSuppressionOutput.contains("activeChildTask="));
    }

    @Test
    @DisplayName("scenario 16 [assertions 23-24]: absent automatic context is a no-op and manual entry attribution is unchanged")
    void sharedObserversNoOpWithoutAutomaticContextAndManualSourcesStayManual() throws IOException {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        Task manualRoot = new TestTask("manual-root");
        Task candidate = new TestTask("candidate");
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositOperationState manualState = bindings.registerRoot(
                manualRoot, "BARE_DEPOSIT_ALL_COMMAND"
        );
        StoreDepositEmissionGate gate = new StoreDepositEmissionGate();
        ContainerStoredTracker manualTracker = new ContainerStoredTracker(slot -> true);
        bindings.bindTracker(manualRoot, manualTracker, "ROOT_ANY_CONTAINER", null);
        StoreDepositEffectDiagnostics effects = new StoreDepositEffectDiagnostics(bindings, gate);
        StoreDepositAutomaticTerminalDiagnostics terminals = new StoreDepositAutomaticTerminalDiagnostics(
                new StoreDepositAutomaticLifecycleLedger(), gate
        );
        StoreDepositMovementDiagnostics movement = new StoreDepositMovementDiagnostics(bindings, gate, terminals);
        movement.stageRouteCandidate(manualRoot, candidate, new BlockPos(1, 64, 2), 1, true);
        movement.observeMovementResult(
                manualRoot, new BlockPos(1, 64, 2), "MOVEMENT_PROGRESS_FAILED", true, false, true
        );

        StoreDepositTransferAttemptRegistry attempts = new StoreDepositTransferAttemptRegistry(bindings, terminals);
        StoreDepositSlotActionDiagnostics slots = new StoreDepositSlotActionDiagnostics(attempts, gate, terminals);
        slots.beginAction(candidate, new Object(), 7, 9, 0, SlotActionType.PICKUP, null);

        assertFalse(manualState.context().isAutomaticDepositOperation());
        assertFalse(manualState.automaticContext().available());
        assertFalse(effects.hasAutomaticContext(manualTracker));
        assertEquals(0L, manualState.routeState().selectedCandidateGeneration());
        assertEquals("UNAVAILABLE", slots.currentSlotActionId());

        StoreDepositOperationContext depositAll = context("BARE_DEPOSIT_ALL_COMMAND");
        StoreDepositOperationContext deposit = context("BARE_DEPOSIT_COMMAND");
        StoreDepositOperationContext storeHome = context("STORE_HOME");
        assertTrue(depositAll.isDepositAllOperation());
        assertFalse(depositAll.isAutomaticDepositOperation());
        assertFalse(deposit.isDepositAllOperation());
        assertFalse(deposit.isAutomaticDepositOperation());
        assertFalse(storeHome.isDepositAllOperation());
        assertFalse(storeHome.isAutomaticDepositOperation());

        String variants = source(
                "src/main/java/lavi/minecraft/diagnostics/command/deposit/DepositCommandVariant.java"
        );
        String storeHomeCommand = source(
                "src/main/java/lavi/minecraft/task/container/home/command/StoreHomeCommand.java"
        );
        String storedTracker = source(
                "src/main/java/adris/altoclef/tasks/container/ContainerStoredTracker.java"
        );
        assertTrue(variants.contains("DEPOSIT(\"deposit\", \"BARE_DEPOSIT_COMMAND\")"));
        assertTrue(variants.contains("DEPOSIT_ALL(\"deposit_all\", \"BARE_DEPOSIT_ALL_COMMAND\")"));
        assertFalse(storeHomeCommand.contains("StoreDepositDiagnostics"));
        int automaticGate = storedTracker.indexOf(
                "boolean automaticDiagnostics = diagnosticsEnabled"
        );
        int manualObservation = storedTracker.indexOf(
                "if (diagnosticsEnabled && !automaticDiagnostics)",
                automaticGate
        );
        int trackedMutation = storedTracker.indexOf(
                "if (!playerInventorySlot && acceptPredicateResult)", manualObservation
        );
        int automaticObservation = storedTracker.indexOf("if (automaticDiagnostics)", trackedMutation);
        assertTrue(automaticGate >= 0);
        assertTrue(manualObservation > automaticGate);
        assertTrue(trackedMutation > manualObservation);
        assertTrue(automaticObservation > trackedMutation);
        assertTrue(storedTracker.contains("String trackerTotalBefore = automaticDiagnostics"));
        int unstoredMethod = storedTracker.indexOf("getUnstoredItemTargetsYouCanStore");
        int unstoredBoundaryGate = storedTracker.indexOf(
                "boolean automaticDiagnostics = ChatClefDiagnostics.isBoundaryEnabled()",
                unstoredMethod
        );
        int unstoredLoop = storedTracker.indexOf("for (ItemTarget target : toStore)", unstoredBoundaryGate);
        int diagnosticCount = storedTracker.indexOf("? diagnosticStoredCount", unstoredLoop);
        int automaticNotStoredObservation = storedTracker.indexOf(
                "StoreDepositDiagnostics.observeAutomaticNotStoredDecision(",
                diagnosticCount
        );
        assertTrue(unstoredBoundaryGate > unstoredMethod);
        assertTrue(unstoredLoop > unstoredBoundaryGate);
        assertTrue(diagnosticCount > unstoredLoop);
        assertTrue(automaticNotStoredObservation > diagnosticCount);

        String storeDiagnostics = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/StoreDepositDiagnostics.java"
        );
        String pressureChain = source(
                "src/main/java/lavi/minecraft/task/container/deposit/auto/DepositAllInventoryPressureChain.java"
        );
        String maintenanceTask = source(
                "src/main/java/lavi/minecraft/task/container/deposit/auto/maintenance/AutoDepositMaintenanceTask.java"
        );
        String maintenanceDiagnostics = source(
                "src/main/java/lavi/minecraft/task/container/deposit/auto/maintenance/diagnostics/AutoDepositMaintenanceDiagnostics.java"
        );
        assertFalse(storeDiagnostics.contains("diagnosticPlayerPosition"));
        assertFalse(storeDiagnostics.contains("mod.getWorld()"));
        assertTrue(pressureChain.contains("if (task.diagnosticAutomaticRunEnabled())"));
        assertTrue(pressureChain.contains("if (activeDiagnosticMaintenanceTask != null)"));
        assertTrue(maintenanceTask.contains("diagnostics.recordTerminal("));
        assertTrue(maintenanceDiagnostics.contains("terminalRecorded"));
        assertTrue(maintenanceDiagnostics.contains("!ChatClefDiagnostics.isBoundaryEnabled()"));
    }

    @Test
    @DisplayName("automatic post-place handoff is one tick and manual deposit remains unchanged")
    void automaticPostPlaceHandoffIsScopedToTheGeneralMaintenanceFactory() throws IOException {
        String depositAll = source(
                "src/main/java/adris/altoclef/tasks/container/DepositAllTask.java"
        );
        String generalFactory = source(
                "src/main/java/lavi/minecraft/task/container/deposit/auto/maintenance/child/AutoDepositGeneralTaskFactory.java"
        );

        int selectedTargetRead = depositAll.indexOf(
                "Optional<BlockPos> selectedTarget = _targetState.selectedTarget();"
        );
        int handoffGate = depositAll.indexOf(
                "if (deferAfterCompletedPlacement())",
                selectedTargetRead
        );
        int openExistingBranch = depositAll.indexOf(
                "if (selectedTarget.isPresent())",
                handoffGate
        );
        int placementOwner = depositAll.indexOf(
                "return _placementTaskOwner.getOrCreate(",
                openExistingBranch
        );

        assertTrue(selectedTargetRead >= 0);
        assertTrue(handoffGate > selectedTargetRead);
        assertTrue(openExistingBranch > handoffGate);
        assertTrue(placementOwner > openExistingBranch);
        assertTrue(depositAll.contains("DepositAllPlacementTaskOwner.ephemeral()"));
        assertTrue(depositAll.contains("DepositAllPostPlaceHandoff.disabled()"));
        assertTrue(generalFactory.contains("DepositAllPlacementTaskOwner::retaining"));
        assertTrue(generalFactory.contains("DepositAllPostPlaceHandoff::singleTick"));
        assertFalse(depositAll.contains("CarryOn"));
        assertFalse(depositAll.contains("Input.SNEAK"));
        assertFalse(depositAll.contains("cancelEverything"));
    }

    private static SlotHarness automaticSlotHarness() {
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositEmissionGate gate = new StoreDepositEmissionGate();
        StoreDepositAutomaticTerminalDiagnostics terminals = new StoreDepositAutomaticTerminalDiagnostics(
                new StoreDepositAutomaticLifecycleLedger(), gate
        );
        StoreDepositTransferAttemptRegistry attempts = new StoreDepositTransferAttemptRegistry(bindings, terminals);
        StoreDepositSlotActionDiagnostics diagnostics = new StoreDepositSlotActionDiagnostics(
                attempts, gate, terminals
        );
        Task parent = new TestTask("store-parent");
        Task transfer = new TestTask("transfer");
        bindings.registerRoot(parent, "AUTO_DEPOSIT_ALL_CHAIN", automaticContext());
        attempts.stage(
                parent,
                transfer,
                selection(target(10), 64)
        );
        bindings.bindChild(parent, transfer);
        StoreDepositTransferAttemptRegistry.Reconciliation reconciliation = attempts.reconcile(
                parent, null, transfer, false, true, transfer
        );
        if (!reconciliation.available() || reconciliation.promoted() == null) {
            throw new AssertionError("automatic transfer candidate was not promoted");
        }
        return new SlotHarness(diagnostics, transfer, reconciliation.promoted());
    }

    private static StoreDepositTransferSelectionSnapshot selection(ItemTarget aggregate, int sourceCount) {
        return new StoreDepositTransferSelectionSnapshot(
                new BlockPos(1, 64, 2),
                aggregate,
                new PlayerSlot(9),
                "fixture:physical-stack-" + sourceCount,
                sourceCount,
                new PlayerSlot(10),
                "EMPTY_SLOT",
                0,
                64,
                false,
                2
        );
    }

    private static ItemTarget target(int count) {
        return new ItemTarget(new Item[]{null}, count);
    }

    private static StoreDepositAutomaticContext automaticContext() {
        return new StoreDepositAutomaticContext(
                true,
                1L,
                77L,
                "auto-deposit-1",
                "auto-deposit-1-maintenance-1",
                "auto-deposit-1-child-1",
                0,
                "auto-deposit-1-pressure-run-1"
        );
    }

    private static StoreDepositOperationContext context(String source) {
        return new StoreDepositOperationContext(
                "operation-" + source, source, new TestTask(source), 0L, 0L
        );
    }

    private static Object[] effectFields(TrackerBinding binding,
                                         boolean predicateResult) {
        return StoreDepositEventFields.effectObservationFields(
                null,
                binding,
                null,
                null,
                null,
                null,
                false,
                true,
                predicateResult,
                predicateResult ? "MATCH" : "POSITION_MISMATCH",
                true,
                new BlockPos(1, 64, 2),
                predicateResult ? "ACCEPT_POSITIVE_DELTA" : "REJECT_TARGET_PREDICATE"
        );
    }

    private static Object signedDelta(String beforeItem,
                                      int beforeCount,
                                      String afterItem,
                                      int afterCount) {
        if (!beforeItem.equals(afterItem)) {
            return (-beforeCount) + "," + afterCount;
        }
        return afterCount - beforeCount;
    }

    private static Map<String, Object> fields(Object[] values) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (values == null) {
            return result;
        }
        for (int index = 0; index + 1 < values.length; index += 2) {
            result.put(String.valueOf(values[index]), values[index + 1]);
        }
        return result;
    }

    private static Object field(Object[] values, String name) {
        Map<String, Object> fields = fields(values);
        if (!fields.containsKey(name)) {
            throw new AssertionError("Missing field: " + name);
        }
        return fields.get(name);
    }

    private static String source(String relativePath) throws IOException {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null) {
            Path candidate = current.resolve(relativePath);
            if (Files.isRegularFile(candidate)) {
                return Files.readString(candidate)
                        .replace("\r\n", "\n")
                        .replace('\r', '\n');
            }
            current = current.getParent();
        }
        throw new IOException("Unable to locate source file: " + relativePath);
    }

    private static int occurrences(String text, String token) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    private static String captureOutput(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    private record SlotHarness(StoreDepositSlotActionDiagnostics diagnostics,
                               Task transferTask,
                               StoreDepositTransferAttemptRegistry.ActiveTransfer activeTransfer) {
    }

    private static final class ExposedMoveItemToSlotTask extends MoveItemToSlotTask {
        private ExposedMoveItemToSlotTask(ItemTarget target, Slot destination) {
            super(target, destination, ignored -> List.of());
        }

        private boolean sameTask(Task other) {
            return isEqual(other);
        }
    }

    private static final class TestTask extends Task {
        private final String name;

        private TestTask(String name) {
            this.name = name;
        }

        @Override
        protected void onStart() {
        }

        @Override
        protected Task onTick() {
            return null;
        }

        @Override
        protected void onStop(Task interruptTask) {
        }

        @Override
        protected boolean isEqual(Task other) {
            return this == other;
        }

        @Override
        protected String toDebugString() {
            return name;
        }
    }
}
