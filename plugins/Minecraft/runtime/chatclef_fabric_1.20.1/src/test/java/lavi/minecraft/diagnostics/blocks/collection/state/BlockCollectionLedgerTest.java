package lavi.minecraft.diagnostics.blocks.collection.state;

import lavi.minecraft.diagnostics.blocks.collection.context.BlockCollectionContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Assert evidence semantics and finite metadata, not a repaired gameplay collection.
class BlockCollectionLedgerTest {
    @Test
    void firstKeysExcludeChangingCollectionIdentitiesAndSequences() {
        BlockCollectionLedger ledger = new BlockCollectionLedger(1, 0);
        List<BlockCollectionEvent> events = new ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            var start = ledger.begin(BlockCollectionOperation.READ_COPY, "native-copy-" + i, "map", "set-" + i, context(1, 0, 0));
            events.addAll(start.events());
            events.addAll(ledger.end(start.token(), true, 0, 0).events());
        }
        assertEquals(List.of("READ_COPY:ENTER", "READ_COPY:EXIT_NORMAL"), events.stream().map(BlockCollectionEvent::semantic).toList());
        assertEquals(1_000, ledger.totals()[0]);
        assertEquals(1_000, ledger.totals()[1]);
        assertEquals(0, ledger.totals()[6]);
    }

    @Test
    void aWriterCanEnterWhileAReadTokenRemainsOpenAndBothIntervalsRetainTheOverlap() throws Exception {
        BlockCollectionLedger ledger = new BlockCollectionLedger(1, 0);
        var read = ledger.begin(BlockCollectionOperation.READ_QUERY, "query", "map", "UNAVAILABLE", context(1, 0, 0));
        AtomicReference<BlockCollectionTransition> writer = new AtomicReference<>();
        CountDownLatch completed = new CountDownLatch(1);
        Thread thread = new Thread(() -> {
            var write = ledger.begin(BlockCollectionOperation.WRITE_CLEAR, "clear", "map", "set", context(2, 1, 1));
            writer.set(write);
            ledger.end(write.token(), true, 1, 2);
            completed.countDown();
        });
        thread.start();
        assertTrue(completed.await(3, TimeUnit.SECONDS), "No diagnostic monitor may span the native read interval");
        var end = ledger.end(read.token(), true, 2, 3);
        assertTrue(writer.get().events().stream().anyMatch(e -> e.semantic().equals("OVERLAP:WRITE_WHILE_READING")));
        assertTrue(end.readOrigin().crossThreadOverlapObserved());
        assertEquals(1, end.readOrigin().writeGenerationAfter());
        assertEquals(0, read.token().writeGenerationBefore());
        thread.join(3_000);
    }

    @Test
    void sameThreadNestedOperationsAreNotReportedAsCrossThreadOverlap() {
        BlockCollectionLedger ledger = new BlockCollectionLedger(1, 0);
        var read = ledger.begin(BlockCollectionOperation.READ_QUERY, "query", "map", "none", context(1, 0, 0));
        var copy = ledger.begin(BlockCollectionOperation.READ_COPY, "copy", "map", "set", context(1, 0, 0));
        assertEquals(read.token().sequence(), copy.token().parentReadSequence());
        ledger.end(copy.token(), true, 0, 1);
        var finished = ledger.end(read.token(), true, 0, 2);
        assertEquals(1, finished.readOrigin().copiedSources());
        assertFalse(finished.readOrigin().crossThreadOverlapObserved());
        assertEquals(0, ledger.totals()[3]);
    }

    @Test
    void normalAndAbnormalReturnHaveSeparateFirstSignatures() {
        BlockCollectionLedger ledger = new BlockCollectionLedger(1, 0);
        var first = ledger.begin(BlockCollectionOperation.READ_COPY, "copy", "map", "set", context(1, 0, 0));
        assertEquals("READ_COPY:EXIT_NORMAL", ledger.end(first.token(), true, 0, 1).events().get(0).semantic());
        var second = ledger.begin(BlockCollectionOperation.READ_COPY, "copy", "map", "set", context(1, 0, 2));
        var failed = ledger.end(second.token(), false, 0, 3);
        assertEquals("READ_COPY:EXIT_ABNORMAL", failed.events().get(0).semantic());
        assertEquals("NOT_CAPTURED_NATIVE_PROPAGATION", field(failed.events().get(0).fields(), "exceptionType"));
        assertEquals(1, ledger.totals()[2]);
        assertTrue(ledger.end(second.token(), false, 0, 4).events().isEmpty());
    }

    @Test
    void activeMetadataCapacityDoesNotPreventFurtherNativeIntervals() {
        BlockCollectionLedger ledger = new BlockCollectionLedger(1, 0);
        List<BlockCollectionToken> tokens = new ArrayList<>();
        for (int i = 0; i < 20; i++) tokens.add(ledger.begin(BlockCollectionOperation.READ_QUERY, "query", "map", "none", context(i, 0, 0)).token());
        assertEquals(16, tokens.stream().filter(BlockCollectionToken::retained).count());
        assertEquals(4, ledger.totals()[5]);
        for (var token : tokens) ledger.end(token, true, 0, 1);
        assertEquals(20, ledger.totals()[1]);
        assertEquals(0, ledger.totals()[11]);
    }

    @Test
    void summariesRequireBothClocksAndExhaustionRemainsInFrozenTotals() {
        BlockCollectionLedger ledger = new BlockCollectionLedger(1, 0);
        var token = ledger.begin(BlockCollectionOperation.READ_QUERY, "query", "map", "none", context(1, 0, 0)).token();
        assertTrue(ledger.end(token, true, 200, 9_999_999_999L).events().stream().noneMatch(e -> e.wrapper().equals("BLOCK_COLLECTION_SUMMARY")));
        int summaries = 0;
        for (int i = 1; i <= 17; i++) {
            long nanos = i * 10_000_000_000L;
            token = ledger.begin(BlockCollectionOperation.READ_QUERY, "query", "map", "none", context(1, i * 200L, nanos)).token();
            var end = ledger.end(token, true, i * 200L, nanos);
            summaries += (int) end.events().stream().filter(e -> e.wrapper().equals("BLOCK_COLLECTION_SUMMARY")).count();
        }
        assertEquals(16, summaries);
        assertEquals(1, ledger.totals()[13]);
        assertEquals(1, ledger.totals()[17]);
        assertEquals("LOCAL_SUMMARY_ALLOWANCE_EXHAUSTED", ledger.lastRejection());
        ledger.invalidate();
        assertEquals(16, ledger.totals()[12]);
        long[] frozen = ledger.totals();
        ledger.settled("BLOCK_COLLECTION_SUMMARY", true, true, "LATE_EMISSION_CALLS_RETURNED");
        assertArrayEquals(frozen, ledger.totals());
        assertEquals("LOCAL_SUMMARY_ALLOWANCE_EXHAUSTED", ledger.lastRejection());
    }

    @Test
    void laterAbnormalExitRetainsItsOwnPeerAfterRoutineOverlapWasAlreadyEmitted() {
        BlockCollectionLedger ledger = new BlockCollectionLedger(1, 0);
        var earlyRead = ledger.begin(BlockCollectionOperation.READ_COPY, "copy", "map", "early-set", context(1, 0, 0));
        var earlyWrite = ledger.begin(BlockCollectionOperation.WRITE_CLEAR, "early-clear", "map", "early-set", context(2, 0, 1));
        assertTrue(earlyWrite.events().stream().anyMatch(e -> e.semantic().equals("OVERLAP:WRITE_WHILE_READING")));
        ledger.end(earlyWrite.token(), true, 0, 2);
        ledger.end(earlyRead.token(), true, 0, 3);
        var failedRead = ledger.begin(BlockCollectionOperation.READ_COPY, "copy", "map", "later-set", context(1, 2, 4));
        var firstPeer = ledger.begin(BlockCollectionOperation.WRITE_ADD_ALL, "later-addAll", "map", "later-set", context(3, 2, 5));
        assertTrue(firstPeer.events().stream().noneMatch(e -> e.semantic().startsWith("OVERLAP:")), "Routine overlap was deduplicated");
        ledger.end(firstPeer.token(), true, 2, 6);
        var secondPeer = ledger.begin(BlockCollectionOperation.WRITE_CLEAR, "later-clear", "map", "later-set", context(4, 3, 7));
        ledger.end(secondPeer.token(), true, 3, 8);
        var failed = ledger.end(failedRead.token(), false, 3, 9).events().get(0);
        assertEquals("READ_COPY:EXIT_ABNORMAL", failed.semantic());
        assertEquals(firstPeer.token().sequence(), field(failed.fields(), "peerWriteSequence"));
        assertEquals("later-addAll", field(failed.fields(), "peerWriteNativeBoundary"));
        assertEquals(3L, field(failed.fields(), "peerWriteThreadId"));
        assertEquals(2L, field(failed.fields(), "peerWriteEntryTick"));
        assertEquals("later-set", field(failed.fields(), "peerWriteCollectionIdentity"));
        assertEquals(1L, field(failed.fields(), "additionalOverlapWriteAmbiguityCount"));
        assertEquals(secondPeer.token().sequence(), field(failed.fields(), "latestObservedWriteSequence"));
        assertEquals("NOT_PROVEN_CAUSE", field(failed.fields(), "writeCausalAttribution"));
    }

    @Test
    void laterNullConsumptionFreezesTheReturnedReadsPeerDespiteNewWrites() {
        BlockCollectionLedger ledger = new BlockCollectionLedger(1, 0);
        var early = ledger.begin(BlockCollectionOperation.READ_QUERY, "query", "map", "none", context(1, 0, 0));
        var earlyWrite = ledger.begin(BlockCollectionOperation.WRITE_CLEAR, "early-clear", "map", "set", context(2, 0, 1));
        ledger.end(earlyWrite.token(), true, 0, 2);
        ledger.end(early.token(), true, 0, 3);
        var read = ledger.begin(BlockCollectionOperation.READ_QUERY, "query", "map", "none", context(1, 1, 4));
        var peer = ledger.begin(BlockCollectionOperation.WRITE_ADD_ALL, "null-origin-addAll", "map", "null-origin-set", context(3, 1, 5));
        assertTrue(peer.events().stream().noneMatch(e -> e.semantic().startsWith("OVERLAP:")));
        ledger.end(peer.token(), true, 1, 6);
        var origin = ledger.end(read.token(), true, 1, 7).readOrigin();
        var unrelated = ledger.begin(BlockCollectionOperation.RESET, "after-return-reset", "map", "map", context(4, 2, 8));
        ledger.end(unrelated.token(), true, 2, 9);
        var consumed = ledger.nullConsumed(origin, "consumer").events().get(0);
        assertEquals("NULL_CONSUMED", consumed.semantic());
        assertEquals(peer.token().sequence(), field(consumed.fields(), "peerWriteSequence"));
        assertEquals(peer.token().sequence(), field(consumed.fields(), "latestObservedWriteSequence"));
        assertEquals("null-origin-addAll", field(consumed.fields(), "peerWriteNativeBoundary"));
        assertEquals("NOT_PROVEN_CAUSE", field(consumed.fields(), "writeCausalAttribution"));
        assertEquals("ACTIVE_COMMAND_AT_COLLECTION_ENTRY_NOT_PATH_SUBMISSION_BINDING", field(consumed.token().context().fields(), "contextAuthority"));
    }

    static BlockCollectionContext context(long thread, long tick, long nanos) {
        return new BlockCollectionContext(tick, nanos, thread, "worker-" + thread, "request", "correlation", "session", "1", "true", "none");
    }
    static Object field(Object[] fields, String key) {
        for (int i = 0; i + 1 < fields.length; i += 2) if (key.equals(fields[i])) return fields[i + 1];
        fail("Missing field " + key); return null;
    }
}
