package lavi.minecraft.diagnostics.container.store.deposit.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260826_kpopmodder: Verify manual and automatic deposit_all roots share detailed diagnostic classification.
class StoreDepositOperationContextTest {

    @Test
    void recognizesManualAndAutomaticDepositAllSources() {
        assertTrue(context("BARE_DEPOSIT_ALL_COMMAND").isDepositAllOperation());
        assertTrue(context("AUTO_DEPOSIT_ALL_CHAIN").isDepositAllOperation());
        assertFalse(context("BARE_DEPOSIT_COMMAND").isDepositAllOperation());
    }

    private static StoreDepositOperationContext context(String requestSource) {
        return new StoreDepositOperationContext(
                "operation-test",
                requestSource,
                null,
                0,
                0
        );
    }
}
