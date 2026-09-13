package lavi.minecraft.diagnostics.blocks.collection.state;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static lavi.minecraft.diagnostics.blocks.collection.state.BlockCollectionLedgerTest.*;

//20260913_kpopmodder: Mode changes invalidate leases while lifetime quotas and accounting remain visible.
class BlockCollectionRegistryTest {
    @Test
    void aReturnedListLinksToItsOriginalReadAndNullConsumptionNeverInspectsTheList() {
        BlockCollectionRegistry registry = new BlockCollectionRegistry();
        Object scanner = new Object();
        Object list = new Object() { @Override public String toString() { throw new AssertionError("No inspection"); } };
        var query = registry.begin(scanner, "map", "none", BlockCollectionOperation.READ_QUERY, "query", context(1, 0, 0));
        var copy = registry.begin(scanner, "map", "set", BlockCollectionOperation.READ_COPY, "copy", context(1, 0, 0));
        registry.end(copy.token(), true, null, 0, 1);
        registry.end(query.token(), true, list, 0, 2);
        var origin = registry.readOrigin(list);
        assertEquals(query.token().sequence(), origin.query().sequence());
        var consumed = registry.nullConsumed(origin, "consumer");
        assertEquals("NULL_CONSUMED", consumed.events().get(0).semantic());
        assertEquals(1L, field(consumed.events().get(0).fields(), "copiedSourceCount"));
        assertEquals("UNPROVEN_MULTIPLE_COPY_SOURCES", field(consumed.events().get(0).fields(), "nullSourceAttribution"));
    }

    @Test
    void clearingDoesNotRenewTheTwoScannerLifetimeAllowanceOrReviveOldTokens() {
        BlockCollectionRegistry registry = new BlockCollectionRegistry();
        Object firstScanner = new Object();
        var first = registry.begin(firstScanner, "map", "set", BlockCollectionOperation.READ_COPY, "copy", context(1, 0, 0));
        registry.end(first.token(), false, null, 0, 1);
        registry.clear();
        assertTrue(registry.end(first.token(), true, null, 1, 2).events().isEmpty());
        var second = registry.begin(firstScanner, "map", "set", BlockCollectionOperation.READ_COPY, "copy", context(1, 1, 2));
        assertNotNull(second.token());
        assertNotEquals(first.token().registryEpoch(), second.token().registryEpoch());
        registry.clear();
        assertNull(registry.begin(new Object(), "map", "set", BlockCollectionOperation.READ_COPY, "copy", context(1, 2, 3)).token());
        Object[] totals = registry.finalSnapshotFields();
        assertEquals(2, field(totals, "blockCollectionScannerCount"));
        assertEquals(1L, field(totals, "blockCollectionAbnormalExits"));
        assertEquals(1L, field(totals, "blockCollectionScannerOmitted"));
        assertEquals("PROCESS_SCANNER_ADMISSION_EXHAUSTED", field(totals, "blockCollectionLastRegistryRejection"));
        assertEquals(0, field(totals, "blockCollectionLiveScanners"));
    }

    @Test
    void returnedListLinksAreBoundedAndLateUnknownNullConsumptionIsCounted() {
        BlockCollectionRegistry registry = new BlockCollectionRegistry();
        Object scanner = new Object();
        Object firstList = new Object();
        for (int i = 0; i < 17; i++) {
            Object list = i == 0 ? firstList : new Object();
            var query = registry.begin(scanner, "map", "none", BlockCollectionOperation.READ_QUERY, "query", context(1, 0, 0));
            registry.end(query.token(), true, list, 0, 1);
        }
        assertNull(registry.readOrigin(firstList));
        assertTrue(registry.nullConsumed(null, "consumer").events().isEmpty());
        assertEquals(1L, field(registry.finalSnapshotFields(), "blockCollectionListLinksEvicted"));
        assertEquals(1L, field(registry.finalSnapshotFields(), "blockCollectionUnboundNullConsumptions"));
    }
}
