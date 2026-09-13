//#if MC == 12001
package lavi.minecraft.task.movement.gotoresult.tracking;

import lavi.minecraft.task.movement.gotoresult.model.GotoTerminalSnapshot;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Task outcome freezing is independent of logs, retries and later player movement.
class GotoTerminalStateTest {
    @Test void firstTerminalOutcomeCannotBeReplacedInEitherDirection() {
        var arrival = new GotoTerminalSnapshot("ARRIVED", "NONE", true, true, true, "prepared_goto_terminal", "minecraft:overworld");
        var failure = new GotoTerminalSnapshot("FAILED", "HANDOFF_SHORTAGE", false, true, false, "prepared_goto_terminal", "minecraft:overworld");
        for (GotoTerminalSnapshot first : new GotoTerminalSnapshot[]{arrival, failure}) {
            var state = new GotoTerminalState();
            state.commit(null);
            assertNull(state.snapshot());
            state.commit(first);
            state.commit(arrival);
            state.commit(failure);
            assertSame(first, state.snapshot());
        }
    }
}
//#endif
