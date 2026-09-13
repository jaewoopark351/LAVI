package lavi.minecraft.diagnostics.blocks.collection.state;

import lavi.minecraft.diagnostics.blocks.collection.context.BlockCollectionContext;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

//20260913_kpopmodder: Bound scanner and returned-list provenance without retaining either gameplay object.
public final class BlockCollectionRegistry {
    public static final int MAX_SCANNERS = 2;
    public static final int MAX_RETURNED_LISTS = 16;
    private final List<ScannerState> scanners = new ArrayList<>(MAX_SCANNERS);
    private long epoch, scannerSequence, scannerOmitted, unboundNulls, staleTokens, originMisses;
    private String lastRejection = "NONE";

    public synchronized BlockCollectionTransition begin(Object scanner, String mapIdentity, String collectionIdentity,
            BlockCollectionOperation operation, String boundary, BlockCollectionContext context) {
        if (scanner == null || operation == null || context == null) return BlockCollectionTransition.empty();
        ScannerState state = null;
        for (ScannerState candidate : scanners) if (candidate.scanner.get() == scanner) { state = candidate; break; }
        if (state == null) {
            if (scanners.size() >= MAX_SCANNERS) {
                scannerOmitted++; lastRejection = "PROCESS_SCANNER_ADMISSION_EXHAUSTED";
                return BlockCollectionTransition.empty();
            }
            state = new ScannerState(scanner, ++scannerSequence, epoch);
            scanners.add(state);
        }
        return state.ledger.begin(operation, boundary, mapIdentity, collectionIdentity, context);
    }

    public synchronized BlockCollectionTransition end(BlockCollectionToken token, boolean normal,
            Object returnedList, long tick, long nanos) {
        ScannerState state = state(token);
        if (state == null) { staleTokens++; lastRejection = "STALE_OR_UNKNOWN_OPERATION"; return BlockCollectionTransition.empty(); }
        BlockCollectionTransition transition = state.ledger.end(token, normal, tick, nanos);
        if (returnedList != null && transition.readOrigin() != null) {
            while (state.returnedLists.size() >= MAX_RETURNED_LISTS) { state.returnedLists.removeFirst(); state.listLinksEvicted++; }
            state.returnedLists.addLast(new ListOrigin(new WeakReference<>(returnedList), transition.readOrigin()));
        }
        return transition;
    }

    public synchronized BlockCollectionReadOrigin readOrigin(Object list) {
        if (list == null) return null;
        for (ScannerState state : scanners) for (ListOrigin link : state.returnedLists) {
            if (link.list.get() == list) return link.origin;
        }
        originMisses++;
        return null;
    }

    public synchronized BlockCollectionTransition nullConsumed(BlockCollectionReadOrigin origin, String consumerIdentity) {
        ScannerState state = origin == null ? null : state(origin.query());
        if (state == null) { unboundNulls++; return BlockCollectionTransition.empty(); }
        return state.ledger.nullConsumed(origin, consumerIdentity);
    }

    public synchronized void settled(BlockCollectionToken token, String wrapper, boolean admitted, boolean returned, String outcome) {
        ScannerState state = state(token);
        if (state != null) state.ledger.settled(wrapper, admitted, returned, outcome);
    }

    public synchronized Object[] finalSnapshotFields() {
        long[] totals = new long[18];
        long evicted = 0;
        int liveScanners = 0;
        String ledgerRejection = "NONE";
        for (ScannerState state : scanners) {
            long[] values = state.ledger.totals();
            for (int i = 0; i < totals.length; i++) totals[i] += values[i];
            evicted += state.listLinksEvicted;
            if (state.scanner.get() != null) liveScanners++;
            if (!"NONE".equals(state.ledger.lastRejection())) ledgerRejection = state.ledger.lastRejection();
        }
        return new Object[]{"blockCollectionScannerCount", scanners.size(), "blockCollectionScannerOmitted", scannerOmitted,
                "blockCollectionObservedEntries", totals[0], "blockCollectionNormalExits", totals[1],
                "blockCollectionAbnormalExits", totals[2], "blockCollectionOverlaps", totals[3],
                "blockCollectionNullConsumptions", totals[4], "blockCollectionActiveOmitted", totals[5],
                "blockCollectionFirstOmitted", totals[6], "blockCollectionEmissionAttempted", totals[7],
                "blockCollectionEmissionAdmitted", totals[8], "blockCollectionEmissionCallsReturned", totals[9],
                "blockCollectionEmissionFailures", totals[10], "blockCollectionActiveMarkers", totals[11],
                "blockCollectionListLinksEvicted", evicted, "blockCollectionUnboundNullConsumptions", unboundNulls,
                "blockCollectionLiveScanners", liveScanners, "blockCollectionStaleTokens", staleTokens,
                "blockCollectionReadOriginMisses", originMisses, "blockCollectionLastRegistryRejection", lastRejection,
                "blockCollectionSummaryCount", totals[12], "blockCollectionSummarySuppressedIntervals", totals[13],
                "blockCollectionSummaryAttempted", totals[14], "blockCollectionSummaryAdmitted", totals[15],
                "blockCollectionSummaryEmissionCallsReturned", totals[16], "blockCollectionSummaryExhaustedScanners", totals[17],
                "blockCollectionLastOutputRejection", ledgerRejection};
    }

    public synchronized void clear() {
        for (ScannerState state : scanners) {
            state.ledger.invalidate(); state.scanner.clear(); state.returnedLists.clear();
        }
        // Lifetime admission and frozen accounting survive mode/teardown invalidation.
        epoch++;
    }

    private ScannerState state(BlockCollectionToken token) {
        if (token == null || token.registryEpoch() != epoch) return null;
        for (ScannerState state : scanners) if (state.sequence == token.scannerSequence()) return state;
        return null;
    }

    private static final class ScannerState {
        final WeakReference<Object> scanner;
        final long sequence;
        final BlockCollectionLedger ledger;
        final ArrayDeque<ListOrigin> returnedLists = new ArrayDeque<>();
        long listLinksEvicted;
        ScannerState(Object scanner, long sequence, long epoch) {
            this.scanner = new WeakReference<>(scanner); this.sequence = sequence;
            this.ledger = new BlockCollectionLedger(sequence, epoch);
        }
    }
    private record ListOrigin(WeakReference<Object> list, BlockCollectionReadOrigin origin) { }
}
