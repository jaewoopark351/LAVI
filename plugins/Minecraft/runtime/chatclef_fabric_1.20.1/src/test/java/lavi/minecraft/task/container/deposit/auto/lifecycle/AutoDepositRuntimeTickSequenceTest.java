package lavi.minecraft.task.container.deposit.auto.lifecycle;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260829_kpopmodder: Directly fix the binding-tracker-before-pressure-chain runtime order.
class AutoDepositRuntimeTickSequenceTest {
    @Test
    void bindingTrackerAlwaysTicksBeforePressureChain() {
        List<String> order = new ArrayList<>();
        AutoDepositRuntimeTickSequence sequence =
                new AutoDepositRuntimeTickSequence(() -> order.add("binding"));

        sequence.runInOrder(() -> order.add("pressure"));

        assertEquals(List.of("binding", "pressure"), order);
    }
}
