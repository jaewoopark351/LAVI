package lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260831_kpopmodder: Keep reconciliation overflow detection pure and explicitly named.
class StoreDepositTerminalReconcilerTest {
    @Test
    void reconciliationSumOverflowIsPureAndDetectable() {
        EnumMap<StoreDepositTerminalClassification, Long> classifications =
                zeroed(StoreDepositTerminalClassification.class);
        EnumMap<StoreDepositTerminalScope, Long> scopes =
                zeroed(StoreDepositTerminalScope.class);

        StoreDepositTerminalReconciler.Result first =
                StoreDepositTerminalReconciler.reconcile(
                        0L,
                        Long.MAX_VALUE,
                        1L,
                        0L,
                        0L,
                        0L,
                        0L,
                        classifications,
                        scopes,
                        0L,
                        0L,
                        0
                );
        StoreDepositTerminalReconciler.Result second =
                StoreDepositTerminalReconciler.reconcile(
                        0L,
                        Long.MAX_VALUE,
                        1L,
                        0L,
                        0L,
                        0L,
                        0L,
                        classifications,
                        scopes,
                        0L,
                        0L,
                        0
                );

        assertEquals(first, second);
        assertTrue(first.sumSaturated());
        assertFalse(first.equationsHold());
        assertTrue(classifications.values().stream().allMatch(count -> count == 0L));
        assertTrue(scopes.values().stream().allMatch(count -> count == 0L));
    }

    private static <K extends Enum<K>> EnumMap<K, Long> zeroed(Class<K> type) {
        EnumMap<K, Long> values = new EnumMap<>(type);
        for (K value : type.getEnumConstants()) {
            values.put(value, 0L);
        }
        return values;
    }
}
