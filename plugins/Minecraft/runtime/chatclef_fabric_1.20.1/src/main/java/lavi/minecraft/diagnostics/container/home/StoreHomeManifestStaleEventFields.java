package lavi.minecraft.diagnostics.container.home;

import java.util.LinkedHashSet;
import java.util.Set;

//20260828_kpopmodder: Build the bounded STORE_HOME stale event without raw metadata.
public final class StoreHomeManifestStaleEventFields {
    private StoreHomeManifestStaleEventFields() {
    }

    public static Object[] required(StoreHomeManifestStaleEventSnapshot event) {
        StoreHomeManifestMismatchSnapshot mismatch = event.mismatch();
        StoreHomeStackIdentitySnapshot expected = mismatch.expected();
        StoreHomeStackIdentitySnapshot actual = mismatch.actual();
        StoreHomeHandlerSnapshot planHandler = event.planHandler();
        StoreHomeHandlerSnapshot failureHandler = event.failureHandler();
        StoreHomeScreenSlotSnapshot mapping = event.mapping();
        StoreHomeStackIdentitySnapshot resolved = mapping.resolvedStack();
        StoreHomeStackIdentitySnapshot cursor = failureHandler.cursor();
        StoreHomePendingTransferSnapshot pending = event.pending();
        StoreHomeDestinationSnapshot destination = event.destination();

        return new Object[]{
                "operationId", event.operationId(),
                "containerSessionOrdinal", availableIndex(event.containerSessionOrdinal()),
                "planRevision", event.planRevision(),
                "planCapturedClientTickId", availableLong(event.planCapturedClientTickId()),
                "failureClientTickId", availableLong(event.failureClientTickId()),
                "elapsedClientTicks", availableLong(event.elapsedClientTicks()),
                "phaseBeforeFailure", event.phaseBeforeFailure(),
                "failureStage", event.failureStage(),
                "validationReason", event.validationReason(),
                "dedupeKey", event.dedupeKey(),

                "manifestStepCount", event.manifestStepCount(),
                "manifestStepIndex", availableIndex(mismatch.manifestStepIndex()),
                "logicalPlayerSlot", availableIndex(mismatch.logicalPlayerSlot()),
                "slotLocation", mismatch.slotLocation(),
                "expectedSelectedMainSlot", mismatch.expectedSelectedMainSlotValue(),
                "actualSelectedMainSlot", mismatch.actualSelectedMainSlotValue(),
                "currentManifestStepLogicalSlot",
                availableIndex(event.currentManifestStepLogicalSlot()),
                "expectedRemainingCount", mismatch.expectedRemainingCountValue(),
                "transferPending", pending.pending(),
                "pendingLogicalSlot", pending.logicalSlotValue(),
                "confirmedItemCount", event.confirmedItemCount(),
                "touchedStackCount", event.touchedStackCount(),

                "expectedPresent", observedPresentValue(expected, mismatch.expectedPresent()),
                "expectedItemId", expected.itemIdValue(),
                "expectedCount", expected.countValue(),
                "expectedDamage", expected.damageValue(),
                "expectedMetadataDigest", expected.metadataDigest(),
                "actualPresent", observedPresentValue(actual, actual.present()),
                "actualItemId", actual.itemIdValue(),
                "actualCount", actual.countValue(),
                "actualDamage", actual.damageValue(),
                "actualMetadataDigest", actual.metadataDigest(),
                "itemIdEqual", mismatch.itemIdEqual(),
                "countEqual", mismatch.countEqual(),
                "damageEqual", mismatch.damageEqual(),
                "metadataDigestEqual", mismatch.metadataDigestEqual(),
                "fullFingerprintEqual", mismatch.fullFingerprintEqual(),

                "planScreenClass", planHandler.screenClass(),
                "planHandlerClass", planHandler.handlerClass(),
                "planHandlerIdentity", planHandler.handlerIdentity(),
                "planSyncId", planHandler.syncIdValue(),
                "planHandlerSlotCount", planHandler.handlerSlotCountValue(),
                "failureScreenClass", failureHandler.screenClass(),
                "failureHandlerClass", failureHandler.handlerClass(),
                "failureHandlerIdentity", failureHandler.handlerIdentity(),
                "failureSyncId", failureHandler.syncIdValue(),
                "failureHandlerSlotCount", failureHandler.handlerSlotCountValue(),
                "screenClassChanged", changed(
                        planHandler.screenClass(), failureHandler.screenClass()
                ),
                "handlerIdentityChanged", changed(
                        planHandler.handlerIdentity(), failureHandler.handlerIdentity()
                ),
                "syncIdChanged", changed(
                        planHandler.syncIdValue(), failureHandler.syncIdValue()
                ),

                "mappingObservedForDiagnosticsOnly", mapping.inspectionAttempted(),
                "mappingMatchCount", mapping.matchCountValue(),
                "resolvedWindowSlot", mapping.resolvedWindowSlotValue(),
                "resolvedSlotLogicalIndex", mapping.resolvedSlotLogicalIndexValue(),
                "resolvedSlotPresent", observedPresentValue(resolved, resolved.present()),
                "resolvedSlotItemId", resolved.itemIdValue(),
                "resolvedSlotCount", resolved.countValue(),
                "resolvedSlotDamage", resolved.damageValue(),
                "resolvedSlotMetadataDigest", resolved.metadataDigest(),
                "playerMainAndResolvedSlotEqual", mapping.playerMainAndResolvedSlotEqual(),

                "cursorEmpty", observedEmptyValue(cursor),
                "cursorItemId", cursor.itemIdValue(),
                "cursorCount", cursor.countValue(),
                "worldKey", event.worldKey(),
                "dimension", event.dimension(),
                "activeDestinationPresent", destination.active(),
                "trustedDestinationId", destination.destinationId(),
                "trustedPosition", destination.position(),
                "exactTrustedBindingMatched", destination.exactBindingMatched(),

                "metadataDigestAlgorithm", StoreHomeMetadataDigest.ALGORITHM,
                "metadataCanonicalizationVersion",
                StoreHomeMetadataDigest.CANONICALIZATION_VERSION,
                "diagnosticCaptureStatus", captureStatus(event),
                "diagnosticErrorClass", errorClasses(event)
        };
    }

    public static Object[] optional(StoreHomeManifestStaleEventSnapshot event) {
        StoreHomeManifestMismatchSnapshot mismatch = event.mismatch();
        StoreHomePendingTransferSnapshot pending = event.pending();
        return new Object[]{
                "expectedDisposition", mismatch.expectedDisposition(),
                "expectedDispositionReason", mismatch.expectedDispositionReason(),
                "actualObservationSource", mismatch.actualObservationSource(),
                "expectedMetadataPresent", mismatch.expected().metadataPresent(),
                "actualMetadataPresent", mismatch.actual().metadataPresent(),
                "resolvedSlotMetadataPresent",
                event.mapping().resolvedStack().metadataPresent(),
                "resolvedSlotPlayerInventory",
                event.mapping().resolvedSlotPlayerInventoryValue(),
                "pendingDestinationKey", pending.destinationKeyValue(),
                "pendingSourceWindowSlot", pending.sourceWindowSlotValue(),
                "pendingSourceCountBefore", pending.sourceCountBeforeValue(),
                "pendingDestinationCountBefore", pending.destinationCountBeforeValue(),
                "pendingElapsedTicks", pending.elapsedTicksValue(),
                "planWorldTime", event.planHandler().worldTimeValue(),
                "failureWorldTime", event.failureHandler().worldTimeValue(),
                "mappingCaptureStatus", event.mapping().captureStatus(),
                "maxDetailedEventsPerOperation", 1,
                "sampling", "none",
                "behaviorEffect", "none"
        };
    }

    private static Object availableIndex(int value) {
        return value >= 0 ? value : StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
    }

    private static Object availableLong(long value) {
        return value >= 0L ? value : StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
    }

    private static Object observedPresentValue(
            StoreHomeStackIdentitySnapshot snapshot,
            boolean present) {
        return snapshot.identityObserved()
                ? present
                : StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
    }

    private static Object observedEmptyValue(StoreHomeStackIdentitySnapshot snapshot) {
        return snapshot.identityObserved()
                ? !snapshot.present()
                : StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
    }

    private static String changed(Object before, Object after) {
        if (StoreHomeStackIdentitySnapshot.NOT_AVAILABLE.equals(before)
                || StoreHomeStackIdentitySnapshot.NOT_AVAILABLE.equals(after)) {
            return StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
        }
        return Boolean.toString(!String.valueOf(before).equals(String.valueOf(after)));
    }

    private static String captureStatus(StoreHomeManifestStaleEventSnapshot event) {
        return complete(event.mismatch().captureStatus())
                && complete(event.planHandler().captureStatus())
                && complete(event.failureHandler().captureStatus())
                && complete(event.mapping().captureStatus())
                && complete(event.destination().captureStatus())
                ? "complete"
                : "partial";
    }

    private static boolean complete(String status) {
        return "complete".equals(status);
    }

    private static String errorClasses(StoreHomeManifestStaleEventSnapshot event) {
        Set<String> errors = new LinkedHashSet<>();
        addError(errors, event.mismatch().errorClass());
        addError(errors, event.planHandler().errorClass());
        addError(errors, event.failureHandler().errorClass());
        addError(errors, event.mapping().errorClass());
        addError(errors, event.destination().errorClass());
        return errors.isEmpty() ? "none" : String.join(",", errors);
    }

    private static void addError(Set<String> errors, String error) {
        if (error != null && !error.isBlank() && !"none".equals(error)) {
            errors.add(error);
        }
    }
}
