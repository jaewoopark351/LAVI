package lavi.minecraft.integration.toolselect.equip;

import lavi.minecraft.integration.toolselect.equip.execution.*;
import lavi.minecraft.integration.toolselect.equip.model.*;
import lavi.minecraft.integration.toolselect.equip.validation.ToolEquipRequestFactory;
import lavi.minecraft.integration.toolselect.equip.diagnostics.ToolEquipEvidenceFields;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Exercise actual attempt logic against controllable slot actions and delayed reflection.
class ToolEquipAttemptTest {
    private static final ToolStackValue FRESH = new ToolStackValue("diamond_shovel", 1, 0, "fresh-tag");
    private static final ToolStackValue WORN = new ToolStackValue("diamond_shovel", 1, 1111, "worn-tag");
    @Test void selectedSourceIsTheOnlySwappedDuplicateAndBothStacksArePreserved() {
        Inventory port = new Inventory(); port.slots[19] = FRESH; port.slots[20] = WORN;
        ToolStackValue displaced = port.slots[3];
        ToolEquipAttempt attempt = request(port, ToolEquipPurpose.SELECT_HAND, 19, 3);
        assertEquals(ToolEquipStatus.SELECTED, attempt.advance(port));
        assertEquals(1, port.swaps); assertEquals(19, port.lastSwapWindow);
        assertEquals(FRESH, port.slots[3]); assertEquals(displaced, port.slots[19]); assertEquals(WORN, port.slots[20]);
        assertEquals(3, port.selected); assertEquals(1, port.selections);
        int readsBeforeEvidence = port.reads;
        var evidence = evidence(attempt);
        assertEquals(readsBeforeEvidence, port.reads, "Evidence must reuse the action owner's frame without reading the port");
        assertEquals(true, evidence.get("selectedSourceAtExpectedLocation"));
        assertEquals(true, evidence.get("displacedDestinationAtSource"));
        assertEquals(true, evidence.get("selectedHandSlotMatches"));
        assertTrue(evidence.get("observedDestination").toString().contains("damage=0"));
        assertEquals("EXISTING_LOCAL_FRAME_READ_NOT_SERVER_ACK", evidence.get("observationAuthority"));
        assertEquals(ToolEquipStatus.SELECTED, attempt.advance(port)); assertEquals(1, port.swaps);
    }
    @Test void placementDoesNotSelectTheHandEvenWithEverySlotOccupied() {
        Inventory port = new Inventory(); port.slots[19] = FRESH; port.selected = 7;
        ToolStackValue displaced = port.slots[2];
        var attempt = request(port, ToolEquipPurpose.HOTBAR_PLACEMENT, 19, 2);
        assertEquals(ToolEquipStatus.PLACED, attempt.advance(port));
        assertEquals(7, port.selected); assertEquals(0, port.selections); assertEquals(displaced, port.slots[19]);
    }
    @Test void hotbarSourceNeedsNoSwapAndAlreadySelectedSourceNeedsNoAction() {
        Inventory port = new Inventory(); port.slots[4] = FRESH;
        assertEquals(ToolEquipStatus.PLACED, request(port, ToolEquipPurpose.HOTBAR_PLACEMENT, 4, 4).advance(port));
        assertEquals(0, port.swaps); assertEquals(0, port.selections);
        assertEquals(ToolEquipStatus.SELECTED, request(port, ToolEquipPurpose.SELECT_HAND, 4, 4).advance(port));
        assertEquals(1, port.selections); assertEquals(0, port.swaps);
        assertEquals(ToolEquipStatus.SELECTED, request(port, ToolEquipPurpose.SELECT_HAND, 4, 4).advance(port));
        assertEquals(1, port.selections);
    }
    @Test void changedSourceIsRejectedBeforeSelectionOrSwap() {
        Inventory port = new Inventory(); port.slots[19] = FRESH;
        var attempt = request(port, ToolEquipPurpose.SELECT_HAND, 19, 2); port.slots[19] = WORN;
        assertEquals(ToolEquipStatus.SOURCE_CHANGED, attempt.advance(port)); assertNoAction(port);
    }
    @Test void changedDestinationIsRejectedBeforeSelectionOrSwap() {
        Inventory port = new Inventory(); port.slots[19] = FRESH;
        var attempt = request(port, ToolEquipPurpose.SELECT_HAND, 19, 2); port.slots[2] = WORN;
        assertEquals(ToolEquipStatus.DESTINATION_CHANGED, attempt.advance(port)); assertNoAction(port);
    }
    @Test void cursorMappingAndExchangeRestrictionsDoNotTriggerWorkaroundClicks() {
        for (int condition = 0; condition < 5; condition++) {
            Inventory port = new Inventory(); port.slots[19] = FRESH;
            var attempt = request(port, ToolEquipPurpose.SELECT_HAND, 19, 2);
            if (condition == 0) port.mapping = false;
            if (condition == 1) port.cursorEmpty = false;
            if (condition == 2) port.exchange = false;
            if (condition == 3) port.usable = false;
            if (condition == 4) port.destinationAvailable = false;
            assertTrue(attempt.advance(port).terminal()); assertFalse(attempt.status().success()); assertNoAction(port);
        }
    }
    @Test void wrongWindowOrOriginalCallerValueIsNotReinterpretedAsTheCurrentSource() {
        Inventory port = new Inventory(); port.slots[19] = FRESH;
        var wrongWindow = new ToolEquipAttempt(ToolEquipRequestFactory.capture(port, ToolEquipPurpose.SELECT_HAND, 19, 5, FRESH, 2));
        assertEquals(ToolEquipStatus.INVALID_MAPPING, wrongWindow.advance(port));
        var changedBeforeCapture = new ToolEquipAttempt(ToolEquipRequestFactory.capture(port, ToolEquipPurpose.SELECT_HAND, 19, 19, WORN, 2));
        assertEquals(ToolEquipStatus.SOURCE_CHANGED, changedBeforeCapture.advance(port)); assertNoAction(port);
    }
    @Test void defenseBeforeExecutionDoesNotSpendTheAttemptAndResumeRunsOnce() {
        Inventory port = new Inventory(); port.slots[19] = FRESH; port.input = false;
        var attempt = request(port, ToolEquipPurpose.SELECT_HAND, 19, 2);
        for (int i = 0; i < 200; i++) assertEquals(ToolEquipStatus.PREEMPTED, attempt.advance(port));
        assertEquals(0, attempt.confirmationEvaluations()); assertNoAction(port);
        port.input = true; assertEquals(ToolEquipStatus.SELECTED, attempt.advance(port)); assertEquals(1, port.swaps);
    }
    @Test void delayedReflectionDoesNotRepeatSwapAndDefenseHandChangesDoNotFailPlacement() {
        Inventory port = new Inventory(); port.slots[19] = FRESH; port.reflectImmediately = false;
        var attempt = request(port, ToolEquipPurpose.HOTBAR_PLACEMENT, 19, 2);
        assertEquals(ToolEquipStatus.WAITING_CONFIRMATION, attempt.advance(port));
        port.input = false; port.selected = 8;
        for (int i = 0; i < 300; i++) assertEquals(ToolEquipStatus.PREEMPTED, attempt.advance(port));
        assertEquals(1, attempt.confirmationEvaluations()); assertEquals(1, port.swaps);
        port.applyPending(); port.input = true;
        assertEquals(ToolEquipStatus.PLACED, attempt.advance(port)); assertEquals(8, port.selected); assertEquals(0, port.selections);
    }
    @Test void absentReflectionHasFiniteFailureWithoutResendOrReverseSwap() {
        Inventory port = new Inventory(); port.slots[19] = FRESH; port.reflectImmediately = false;
        var attempt = request(port, ToolEquipPurpose.SELECT_HAND, 19, 2);
        for (int i = 0; i < 20; i++) attempt.advance(port);
        assertEquals(ToolEquipStatus.CONFIRMATION_TIMEOUT, attempt.status()); assertEquals(20, attempt.confirmationEvaluations());
        for (int i = 0; i < 30; i++) assertEquals(ToolEquipStatus.CONFIRMATION_TIMEOUT, attempt.advance(port));
        assertEquals(1, port.swaps); assertEquals(0, port.selections);
    }
    @Test void replacedRootOrHandlerCannotReceiveALateActionOrSuccess() {
        for (boolean submitted : new boolean[]{false, true}) {
            Inventory port = new Inventory(); port.slots[19] = FRESH; port.reflectImmediately = false;
            var attempt = request(port, ToolEquipPurpose.SELECT_HAND, 19, 2);
            if (submitted) attempt.advance(port);
            port.binding = new ToolEquipBinding(port.world, port.player, new Object(), port.lifetime, port.handler, 0);
            port.applyPending(); assertEquals(ToolEquipStatus.INVALID_BINDING, attempt.advance(port));
            assertEquals(submitted ? 1 : 0, port.swaps); assertEquals(0, port.selections);
        }
    }
    @Test void laterWearDoesNotChangeAnAlreadyConfirmedResult() {
        Inventory port = new Inventory(); port.slots[19] = FRESH;
        var attempt = request(port, ToolEquipPurpose.SELECT_HAND, 19, 2);
        assertEquals(ToolEquipStatus.SELECTED, attempt.advance(port));
        port.slots[2] = WORN; port.selected = 6;
        assertEquals(ToolEquipStatus.SELECTED, attempt.advance(port)); assertEquals(1, port.swaps); assertEquals(1, port.selections);
        assertEquals(FRESH, attempt.lastObservedFrame().destination(), "Later inventory changes cannot rewrite the confirmed observation");
        assertEquals(2, evidence(attempt).get("observedSelectedHotbar"));
    }
    @Test void equalRootObjectWithANewLogicalInvocationCannotConsumeTheOldAttempt() {
        Inventory port = new Inventory(); port.slots[19] = FRESH;
        var attempt = request(port, ToolEquipPurpose.SELECT_HAND, 19, 2);
        port.binding = new ToolEquipBinding(port.world, port.player, port.binding.root(), new Object(), port.handler, 0);
        assertEquals(ToolEquipStatus.INVALID_BINDING, attempt.advance(port)); assertNoAction(port);
    }
    @Test void nativeActionExceptionIsNotSwallowedOrRetried() {
        Inventory port = new Inventory(); port.slots[19] = FRESH;
        IllegalStateException failure = new IllegalStateException("native fixture"); port.throwOnSwap = failure;
        var attempt = request(port, ToolEquipPurpose.SELECT_HAND, 19, 2);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> attempt.advance(port)));
        port.throwOnSwap = null; attempt.advance(port); assertEquals(1, port.swaps);
    }
    private static ToolEquipAttempt request(Inventory port, ToolEquipPurpose purpose, int source, int destination) {
        return new ToolEquipAttempt(ToolEquipRequestFactory.capture(port, purpose, source, Inventory.window(source), port.slots[source], destination));
    }
    private static void assertNoAction(Inventory port) { assertEquals(0, port.swaps); assertEquals(0, port.selections); }
    private static java.util.Map<String, Object> evidence(ToolEquipAttempt attempt) {
        Object[] fields = ToolEquipEvidenceFields.capture(attempt);
        var result = new java.util.HashMap<String, Object>();
        for (int index = 0; index < fields.length; index += 2) result.put((String) fields[index], fields[index + 1]);
        return result;
    }
    private static final class Inventory implements ToolEquipPort {
        final Object world = new Object(), player = new Object(), handler = new Object(), lifetime = new Object();
        ToolEquipBinding binding = new ToolEquipBinding(world, player, new Object(), lifetime, handler, 0);
        final ToolStackValue[] slots = new ToolStackValue[36];
        boolean input = true, mapping = true, cursorEmpty = true, exchange = true, usable = true, destinationAvailable = true;
        boolean reflectImmediately = true;
        int selected = 1, swaps, selections, reads, lastSwapWindow, pendingSource = -1, pendingDestination;
        RuntimeException throwOnSwap;
        Inventory() { Arrays.setAll(slots, i -> new ToolStackValue("item-" + i, 1, 0, null)); }
        static int window(int inventory) { return inventory < 9 ? inventory + 36 : inventory; }
        @Override public ToolEquipFrame read(int source, int destination) {
            reads++;
            return new ToolEquipFrame(binding, slots[source], slots[destination], window(source), window(destination), selected,
                    mapping, cursorEmpty, exchange, input, usable, destinationAvailable);
        }
        @Override public void swap(int sourceWindow, int destinationHotbar) {
            swaps++; lastSwapWindow = sourceWindow;
            if (throwOnSwap != null) throw throwOnSwap;
            pendingSource = sourceWindow >= 36 ? sourceWindow - 36 : sourceWindow; pendingDestination = destinationHotbar;
            if (reflectImmediately) applyPending();
        }
        void applyPending() {
            if (pendingSource < 0) return;
            ToolStackValue old = slots[pendingDestination]; slots[pendingDestination] = slots[pendingSource]; slots[pendingSource] = old; pendingSource = -1;
        }
        @Override public void selectHotbar(int hotbar) { selections++; selected = hotbar; }
    }
}
