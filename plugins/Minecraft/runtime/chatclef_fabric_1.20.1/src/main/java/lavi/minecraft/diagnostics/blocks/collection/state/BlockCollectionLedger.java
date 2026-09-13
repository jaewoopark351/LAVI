package lavi.minecraft.diagnostics.blocks.collection.state;

import lavi.minecraft.diagnostics.blocks.collection.context.BlockCollectionContext;
import lavi.minecraft.diagnostics.blocks.collection.format.BlockCollectionEventFields;
import lavi.minecraft.diagnostics.blocks.collection.state.output.BlockCollectionOutputAccounting;
import lavi.minecraft.diagnostics.blocks.collection.state.output.BlockCollectionOutputSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//20260913_kpopmodder: Track bounded diagnostic intervals; this monitor never encloses a native operation.
public final class BlockCollectionLedger {
    public static final int MAX_ACTIVE = 16;
    public static final int MAX_FIRST = BlockCollectionOutputAccounting.MAX_FIRST;
    public static final int MAX_SUMMARIES = BlockCollectionOutputAccounting.MAX_SUMMARIES;
    private final long scannerSequence;
    private final long epoch;
    private final Map<Long, Active> active = new LinkedHashMap<>();
    private final BlockCollectionOutputAccounting output = new BlockCollectionOutputAccounting();
    private boolean valid = true;
    private long sequence, writeGeneration, entered, normalExits, abnormalExits, overlaps, nulls;
    private long activeOmitted;
    private BlockCollectionToken latestObservedWrite;

    public BlockCollectionLedger(long scannerSequence, long epoch) {
        this.scannerSequence = scannerSequence;
        this.epoch = epoch;
    }

    public synchronized BlockCollectionTransition begin(BlockCollectionOperation operation, String boundary,
            String mapIdentity, String collectionIdentity, BlockCollectionContext context) {
        if (!valid) return BlockCollectionTransition.empty();
        entered++;
        long parentRead = 0;
        Active peerReader = null, peerWriter = null;
        for (Active prior : active.values()) {
            if (prior.token.context().threadId() == context.threadId()) {
                if (prior.token.operation() == BlockCollectionOperation.READ_QUERY) parentRead = prior.token.sequence();
                continue;
            }
            if (!prior.token.mapIdentity().equals(mapIdentity)) continue;
            if (prior.token.operation().writer()) peerWriter = prior;
            else peerReader = prior;
        }
        long before = writeGeneration;
        if (operation.writer()) writeGeneration++;
        boolean retained = active.size() < MAX_ACTIVE;
        BlockCollectionToken token = new BlockCollectionToken(scannerSequence, epoch, ++sequence,
                operation, boundary, mapIdentity, collectionIdentity, context, before, parentRead, retained);
        Active current = new Active(token);
        if (operation.writer()) latestObservedWrite = token;
        List<BlockCollectionEvent> events = new ArrayList<>(3);
        first(events, operation.name() + ":ENTER", token, "phase", "ENTER", "normalReturn", "NOT_YET_OBSERVED");
        output.observeEntryClock(context.tick(), context.nanos());
        if (operation.writer() && peerReader != null) overlap(events, current, peerReader, "WRITE_WHILE_READING");
        if (peerWriter != null) overlap(events, current, peerWriter,
                operation.writer() ? "WRITE_WHILE_WRITING" : "READ_WHILE_WRITING");
        // Mark every affected interval, while retaining only one peer in each bounded output proposal.
        for (Active prior : active.values()) {
            if (prior.token.context().threadId() != context.threadId()
                    && prior.token.mapIdentity().equals(mapIdentity)
                    && (prior.token.operation().writer() || operation.writer())) {
                prior.overlap = true;
                if (operation.writer() && !prior.token.operation().writer()) prior.observeWriter(token);
                if (!operation.writer() && prior.token.operation().writer()) current.observeWriter(prior.token);
            }
        }
        if (retained) active.put(token.sequence(), current);
        else { activeOmitted++; first(events, "ACTIVE_METADATA_CAPACITY", token, "phase", "ENTRY_NOT_RETAINED"); }
        if (operation == BlockCollectionOperation.READ_COPY && parentRead != 0) {
            Active parent = active.get(parentRead);
            if (parent != null) parent.copiedSources++;
        }
        return new BlockCollectionTransition(token, null, List.copyOf(events));
    }

    public synchronized BlockCollectionTransition end(BlockCollectionToken token, boolean returnedNormally,
            long tick, long nanos) {
        if (!matches(token)) return BlockCollectionTransition.empty();
        Active prior = active.remove(token.sequence());
        if (token.retained() && prior == null) return BlockCollectionTransition.empty();
        if (returnedNormally) normalExits++; else abnormalExits++;
        List<BlockCollectionEvent> events = new ArrayList<>(2);
        String phase = returnedNormally ? "EXIT_NORMAL" : "EXIT_ABNORMAL";
        boolean overlap = prior != null && prior.overlap;
        BlockCollectionWriteEvidence writeEvidence = new BlockCollectionWriteEvidence(
                prior == null ? null : prior.firstOverlapWrite, prior == null ? 0 : prior.overlapWriteCount, latestObservedWrite);
        first(events, token.operation().name() + ":" + phase, token,
                BlockCollectionEventFields.exit(token, phase, returnedNormally, tick, nanos, writeGeneration, overlap, writeEvidence));
        if (output.admitSummary(tick, nanos)) {
            events.add(event("BLOCK_COLLECTION_SUMMARY", "COARSE_INTERVAL_SUMMARY", token,
                    "phase", "SUMMARY", "summaryTick", tick));
        }
        BlockCollectionReadOrigin origin = token.operation() == BlockCollectionOperation.READ_QUERY && returnedNormally
                ? new BlockCollectionReadOrigin(token, writeGeneration, prior == null ? -1 : prior.copiedSources, overlap, writeEvidence) : null;
        return new BlockCollectionTransition(token, origin, List.copyOf(events));
    }

    public synchronized BlockCollectionTransition nullConsumed(BlockCollectionReadOrigin origin, String consumerIdentity) {
        if (origin == null || !matches(origin.query())) return BlockCollectionTransition.empty();
        nulls++;
        List<BlockCollectionEvent> events = new ArrayList<>(1);
        first(events, "NULL_CONSUMED", origin.query(), BlockCollectionEventFields.nullConsumed(origin, consumerIdentity));
        return new BlockCollectionTransition(origin.query(), origin, List.copyOf(events));
    }

    private void overlap(List<BlockCollectionEvent> events, Active current, Active peer, String direction) {
        current.overlap = true; peer.overlap = true; overlaps++;
        first(events, "OVERLAP:" + direction, current.token, BlockCollectionEventFields.overlap(current.token, peer.token, direction));
    }

    private void first(List<BlockCollectionEvent> events, String semantic, BlockCollectionToken token, Object... details) {
        if (!output.admitFirst(semantic)) return;
        events.add(event("BLOCK_COLLECTION_FIRST", semantic, token, details));
    }

    private BlockCollectionEvent event(String wrapper, String semantic, BlockCollectionToken token, Object... details) {
        BlockCollectionIntervalSnapshot interval = new BlockCollectionIntervalSnapshot(
                entered, normalExits, abnormalExits, overlaps, nulls, active.size(), activeOmitted);
        return new BlockCollectionEvent(wrapper, semantic, token,
                BlockCollectionEventFields.withCounters(details, interval, output.snapshot()));
    }

    public synchronized void settled(String wrapper, boolean admission, boolean returned, String outcome) {
        if (!valid) return;
        output.settled(wrapper, admission, returned, outcome);
    }

    public synchronized long[] totals() {
        BlockCollectionOutputSnapshot snapshot = output.snapshot();
        return new long[]{entered, normalExits, abnormalExits, overlaps, nulls, activeOmitted,
                snapshot.firstOmitted(), snapshot.attempted(), snapshot.admitted(), snapshot.callsReturned(), snapshot.emissionFailures(), active.size(),
                snapshot.summaries(), snapshot.summarySuppressed(), snapshot.summaryAttempted(), snapshot.summaryAdmitted(), snapshot.summaryReturned(),
                snapshot.summaryAllowanceExhausted() ? 1 : 0};
    }

    public synchronized String lastRejection() { return output.snapshot().lastRejection(); }

    public synchronized void invalidate() { valid = false; active.clear(); latestObservedWrite = null; }
    private boolean matches(BlockCollectionToken token) {
        return valid && token != null && token.scannerSequence() == scannerSequence && token.registryEpoch() == epoch;
    }

    private static final class Active {
        final BlockCollectionToken token;
        boolean overlap;
        long copiedSources;
        BlockCollectionToken firstOverlapWrite;
        long overlapWriteCount;
        Active(BlockCollectionToken token) { this.token = token; }
        void observeWriter(BlockCollectionToken writer) {
            if (firstOverlapWrite == null) firstOverlapWrite = writer;
            overlapWriteCount++;
        }
    }
}
