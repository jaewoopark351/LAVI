package lavi.minecraft.integration.input.lease;

import baritone.api.utils.input.Input;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Later native writers supersede a lease even when the boolean stays true.
class ForcedInputLeaseLedgerTest {
    @Test void exactIdentityAndExistingForcedInputsAreProtected() {
        var ledger = new ForcedInputLeaseLedger();
        Object owner = new Object(), other = new Object();
        assertFalse(ledger.claim(Input.MOVE_FORWARD, owner, true));
        assertTrue(ledger.claim(Input.MOVE_FORWARD, owner, false));
        assertFalse(ledger.claim(Input.MOVE_FORWARD, other, false));
        assertFalse(ledger.retire(Input.MOVE_FORWARD, other));
        assertTrue(ledger.owns(Input.MOVE_FORWARD, owner));
    }

    @Test void externalSameValueWriteInvalidatesRatherThanReleasingTheNewWriter() {
        var ledger = new ForcedInputLeaseLedger();
        Object owner = new Object();
        assertTrue(ledger.claim(Input.MOVE_FORWARD, owner, false));
        ledger.externalWrite(Input.MOVE_FORWARD);
        assertFalse(ledger.owns(Input.MOVE_FORWARD, owner));
        assertFalse(ledger.retire(Input.MOVE_FORWARD, owner));
        assertFalse(ledger.claim(Input.MOVE_FORWARD, owner, true));
    }

    @Test void anotherKeyAndNullWriteDoNotRetireTheLease() {
        var ledger = new ForcedInputLeaseLedger();
        Object owner = new Object();
        ledger.claim(Input.MOVE_FORWARD, owner, false);
        ledger.externalWrite(Input.CLICK_RIGHT);
        ledger.externalWrite(null);
        assertTrue(ledger.owns(Input.MOVE_FORWARD, owner));
        ledger.externalClear();
        assertFalse(ledger.owns(Input.MOVE_FORWARD, owner));
    }
}
