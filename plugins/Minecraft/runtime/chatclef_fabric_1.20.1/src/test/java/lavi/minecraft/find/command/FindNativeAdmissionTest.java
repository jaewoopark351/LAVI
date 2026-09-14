//#if MC == 12001
package lavi.minecraft.find.command;

import adris.altoclef.commandsystem.CommandException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Repeated native admission cannot replace a non-idle root; idle work remains interruptible.
class FindNativeAdmissionTest {
    @Test void repeatedNonIdleAdmissionRemainsBusy() {
        for (int repeatedCommand = 0; repeatedCommand < 2; repeatedCommand++) {
            CommandException failure = assertThrows(CommandException.class, () -> FindNativeAdmission.requireAvailable(true, false));
            assertTrue(failure.getMessage().contains("non_idle_user_root_busy"));
        }
    }
    @Test void idleAndUnoccupiedRootsPermitAdmission() {
        assertDoesNotThrow(() -> FindNativeAdmission.requireAvailable(true, true));
        assertDoesNotThrow(() -> FindNativeAdmission.requireAvailable(false, false));
    }
}
//#endif
