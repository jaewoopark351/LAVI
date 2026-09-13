package lavi.minecraft.diagnostics.blocks.collection.format;

import lavi.minecraft.diagnostics.blocks.collection.state.BlockCollectionIntervalSnapshot;
import lavi.minecraft.diagnostics.blocks.collection.state.BlockCollectionReadOrigin;
import lavi.minecraft.diagnostics.blocks.collection.state.BlockCollectionToken;
import lavi.minecraft.diagnostics.blocks.collection.state.BlockCollectionWriteEvidence;
import lavi.minecraft.diagnostics.blocks.collection.state.output.BlockCollectionOutputSnapshot;
import lavi.minecraft.diagnostics.observation.format.ObservationFields;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//20260913_kpopmodder: Format frozen diagnostic evidence without owning interval or output decisions.
public final class BlockCollectionEventFields {
    private BlockCollectionEventFields() { }

    public static Object[] exit(BlockCollectionToken token, String phase, boolean returnedNormally,
            long tick, long nanos, long writeGeneration, boolean overlap, BlockCollectionWriteEvidence evidence) {
        return ObservationFields.concat(new Object[]{"phase", phase, "normalReturn", returnedNormally,
                "exitTick", tick, "exitNanos", nanos, "writeGenerationBefore", token.writeGenerationBefore(),
                "writeGenerationAfter", writeGeneration, "writeGenerationChanged", writeGeneration != token.writeGenerationBefore(),
                "intervalOverlapObserved", overlap, "exceptionType", returnedNormally ? "NONE" : "NOT_CAPTURED_NATIVE_PROPAGATION"},
                returnedNormally ? new Object[0] : writeEvidence(evidence));
    }

    public static Object[] nullConsumed(BlockCollectionReadOrigin origin, String consumerIdentity) {
        return ObservationFields.concat(new Object[]{"phase", "NULL_CONSUMED",
                "consumerBoundary", "UserBlockRangeTracker.updateState.removeIf.getBlockState",
                "consumerIdentity", consumerIdentity, "copiedSourceCount", origin.copiedSources(),
                "readWriteGenerationAfter", origin.writeGenerationAfter(),
                "readIntervalOverlapObserved", origin.crossThreadOverlapObserved(),
                "nullSourceAttribution", "UNPROVEN_MULTIPLE_COPY_SOURCES", "nativeNullArgumentPreserved", true},
                writeEvidence(origin.writeEvidence()));
    }

    public static Object[] overlap(BlockCollectionToken current, BlockCollectionToken peer, String direction) {
        return new Object[]{"phase", "OVERLAP", "overlapDirection", direction,
                "peerSequence", peer.sequence(), "peerThreadId", peer.context().threadId(),
                "peerThreadName", peer.context().threadName(), "peerOperation", peer.operation().name(),
                "peerNativeBoundary", peer.boundary(), "peerEntryTick", peer.context().tick(),
                "peerCollectionIdentity", peer.collectionIdentity(), "sameCollectionIdentity",
                current.collectionIdentity().equals(peer.collectionIdentity()),
                "overlapEvidence", "DIAGNOSTIC_ENTRY_EXIT_INTERVALS_NOT_A_DATA_RACE_PROOF"};
    }

    public static Object[] writeEvidence(BlockCollectionWriteEvidence evidence) {
        BlockCollectionToken first = evidence.firstOverlapWrite(), latest = evidence.latestObservedWrite();
        return new Object[]{"peerWriteEvidence", first == null ? "NO_RETAINED_OVERLAP_WRITE" : "RETAINED_FIRST_OVERLAP_WRITE",
                "peerWriteSequence", first == null ? 0 : first.sequence(),
                "peerWriteNativeBoundary", first == null ? "NONE" : first.boundary(),
                "peerWriteThreadId", first == null ? 0 : first.context().threadId(),
                "peerWriteThreadName", first == null ? "NONE" : first.context().threadName(),
                "peerWriteCollectionIdentity", first == null ? "NONE" : first.collectionIdentity(),
                "peerWriteEntryTick", first == null ? -1 : first.context().tick(),
                "overlapWriteCount", evidence.overlapWriteCount(), "additionalOverlapWriteAmbiguityCount", Math.max(0, evidence.overlapWriteCount() - 1),
                "latestObservedWriteSequence", latest == null ? 0 : latest.sequence(),
                "latestObservedWriteNativeBoundary", latest == null ? "NONE" : latest.boundary(),
                "latestObservedWriteThreadId", latest == null ? 0 : latest.context().threadId(),
                "latestObservedWriteCollectionIdentity", latest == null ? "NONE" : latest.collectionIdentity(),
                "latestObservedWriteEntryTick", latest == null ? -1 : latest.context().tick(),
                "writeCausalAttribution", "NOT_PROVEN_CAUSE"};
    }

    public static Object[] withCounters(Object[] details, BlockCollectionIntervalSnapshot interval,
            BlockCollectionOutputSnapshot output) {
        List<Object> fields = new ArrayList<>(64);
        Collections.addAll(fields, details);
        Collections.addAll(fields, "observedEntryCount", interval.entered(), "normalExitCount", interval.normalExits(),
                "abnormalExitCount", interval.abnormalExits(), "overlapCount", interval.overlaps(), "nullConsumedCount", interval.nulls(),
                "activeMarkerCount", interval.activeMarkers(), "activeMarkerOmittedCount", interval.activeOmitted(),
                "firstSemanticCount", output.firstSemanticCount(), "firstSemanticOmittedCount", output.firstOmitted(),
                "suppressedDetailCount", output.suppressed(), "summaryCount", output.summaries(),
                "summaryAllowanceExhausted", output.summaryAllowanceExhausted(),
                "summarySuppressedIntervals", output.summarySuppressed(),
                "priorAttemptedCount", output.attempted(), "priorAdmittedCount", output.admitted(),
                "priorEmissionCallsReturned", output.callsReturned(), "priorEmissionFailures", output.emissionFailures(),
                "priorOutputOutcome", output.lastOutput(), "priorLastRejection", output.lastRejection());
        return fields.toArray();
    }
}
