package lavi.minecraft.diagnostics.container.store.deposit.transfer;

import adris.altoclef.tasks.slot.MoveItemToSlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.SlotHarness;
import lavi.minecraft.task.container.deposit.auto.policy.diagnostics.AutoDepositPolicyItemSnapshot;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.automaticSlotHarness;
import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.field;
import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.fields;
import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.occurrences;
import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.selection;
import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.source;
import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.target;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Keep each migrated Slice A characterization scenario with its owning responsibility.
class StoreDepositTransferSelectionContractTest {

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

    private static final class ExposedMoveItemToSlotTask extends MoveItemToSlotTask {
        private ExposedMoveItemToSlotTask(ItemTarget target, Slot destination) {
            super(target, destination, ignored -> List.of());
        }

        private boolean sameTask(Task other) {
            return isEqual(other);
        }
    }
}
