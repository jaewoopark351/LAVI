package lavi.minecraft.find.approach.input;

import java.util.EnumMap;
import java.util.Map;
import baritone.api.utils.input.Input;
import lavi.minecraft.find.diagnostics.FindLog;
import lavi.minecraft.integration.input.lease.ForcedInputLeaseChannel;
import lavi.minecraft.integration.input.lease.ForcedInputLeaseLedger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Exercise the real lease ledger and consumer against unrelated writer handoffs.
class FindOwnedMovementInputsTest {
    private static class Channel implements ForcedInputLeaseChannel {
        final ForcedInputLeaseLedger ledger = new ForcedInputLeaseLedger();
        final EnumMap<Input, Boolean> forced = new EnumMap<>(Input.class);
        public boolean lavi$claimForcedInput(Input input, Object owner) {
            return ledger.claim(input, owner, forced.getOrDefault(input, false));
        }
        public boolean lavi$writeForcedInput(Input input, Object owner, boolean down) {
            if (!ledger.owns(input, owner)) return false;
            forced.put(input, down); return true;
        }
        public boolean lavi$releaseForcedInput(Input input, Object owner) {
            if (!lavi$writeForcedInput(input, owner, false)) return false;
            return ledger.retire(input, owner);
        }
        public boolean lavi$ownsForcedInput(Input input, Object owner) { return ledger.owns(input, owner); }
        void nativeWrite(Input input, boolean value) { ledger.externalWrite(input); forced.put(input, value); }
    }
    private FindLog log() { return new FindLog("test", (event, values) -> { }); }

    @Test void forbiddenClickIsRejectedBeforeAnyMovementAndDefenseKeysSurvive() {
        var channel = new Channel(); channel.nativeWrite(Input.CLICK_RIGHT, true);
        var inputs = new FindOwnedMovementInputs(channel, log());
        assertFalse(inputs.apply(Map.of(Input.MOVE_FORWARD, true, Input.CLICK_LEFT, true)));
        assertFalse(channel.forced.getOrDefault(Input.MOVE_FORWARD, false));
        assertTrue(channel.forced.get(Input.CLICK_RIGHT));
        assertTrue(inputs.quiet());
    }
    @Test void sameBooleanNativeWriterWinsAndCleanupDoesNotClearIt() {
        var channel = new Channel(); var inputs = new FindOwnedMovementInputs(channel, log());
        assertTrue(inputs.apply(Map.of(Input.MOVE_FORWARD, true)));
        channel.nativeWrite(Input.MOVE_FORWARD, true);
        assertFalse(inputs.apply(Map.of(Input.MOVE_FORWARD, true)));
        inputs.release();
        assertTrue(channel.forced.get(Input.MOVE_FORWARD));
        assertTrue(inputs.quiet());
    }
    @Test void ordinaryCleanupReleasesOnlyOwnKeysAndLoggingCannotChangeTheDecision() {
        var channel = new Channel(); channel.nativeWrite(Input.SNEAK, true);
        var inputs = new FindOwnedMovementInputs(channel, new FindLog("test", (event, values) -> { throw new RuntimeException(); }));
        assertTrue(inputs.apply(Map.of(Input.MOVE_FORWARD, true, Input.SPRINT, true)));
        inputs.release();
        assertFalse(channel.forced.get(Input.MOVE_FORWARD));
        assertFalse(channel.forced.get(Input.SPRINT));
        assertTrue(channel.forced.get(Input.SNEAK));
    }
    @Test void partialAcquisitionRollsBackOnlyAlreadyClaimedKeys() {
        var channel = new Channel(); channel.nativeWrite(Input.MOVE_BACK, true);
        var inputs = new FindOwnedMovementInputs(channel, log());
        var staged = new EnumMap<Input, Boolean>(Input.class);
        staged.put(Input.MOVE_FORWARD, true); staged.put(Input.MOVE_BACK, true);
        assertFalse(inputs.apply(staged));
        assertFalse(channel.forced.getOrDefault(Input.MOVE_FORWARD, false));
        assertTrue(channel.forced.get(Input.MOVE_BACK));
    }
    @Test void failedOwnReleaseCannotFabricateQuiescence() {
        var channel = new Channel() {
            @Override public boolean lavi$releaseForcedInput(Input input, Object owner) { return false; }
        };
        var inputs = new FindOwnedMovementInputs(channel, log());
        assertTrue(inputs.apply(Map.of(Input.MOVE_FORWARD, true)));
        inputs.release();
        assertFalse(inputs.quiet());
        assertTrue(inputs.owns(Input.MOVE_FORWARD));
        assertTrue(channel.forced.get(Input.MOVE_FORWARD));
    }
    @Test void externalFalseWriteInvalidatesLeaseAndCannotBeSilentlyReacquiredInApply() {
        var channel = new Channel(); var inputs = new FindOwnedMovementInputs(channel, log());
        assertTrue(inputs.apply(Map.of(Input.MOVE_FORWARD, true)));
        channel.nativeWrite(Input.MOVE_FORWARD, false);
        assertFalse(inputs.apply(Map.of(Input.MOVE_FORWARD, true)));
        assertFalse(channel.forced.get(Input.MOVE_FORWARD));
        assertTrue(inputs.quiet());
    }
}
