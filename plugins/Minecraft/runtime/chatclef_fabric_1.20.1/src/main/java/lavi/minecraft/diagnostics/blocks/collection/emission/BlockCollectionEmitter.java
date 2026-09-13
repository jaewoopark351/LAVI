package lavi.minecraft.diagnostics.blocks.collection.emission;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.blocks.collection.state.BlockCollectionEvent;
import lavi.minecraft.diagnostics.blocks.collection.state.BlockCollectionRegistry;
import lavi.minecraft.diagnostics.blocks.collection.state.BlockCollectionToken;
import lavi.minecraft.diagnostics.observation.format.ObservationFields;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticDispatchResult;
import java.util.function.Function;

//20260913_kpopmodder: Emit exact reserved families through the existing 8 KiB physical output boundary.
public final class BlockCollectionEmitter {
    private BlockCollectionEmitter() { }

    public static void emit(BlockCollectionRegistry registry, BlockCollectionEvent event) {
        emit(registry, event, value -> ChatClefDiagnostics.logBoundedBoundaryWithDispatchResult(
                value.wrapper(), value.semantic(), null, 8_192, fields(value), new Object[0]));
    }

    static void emit(BlockCollectionRegistry registry, BlockCollectionEvent event,
            Function<BlockCollectionEvent, DiagnosticDispatchResult> dispatch) {
        try {
            DiagnosticDispatchResult result = dispatch.apply(event);
            registry.settled(event.token(), event.wrapper(), result.admitted(), result.emissionCompleted(),
                    !result.admitted() ? "ADMISSION_REJECTED:" + result.admission().rejectionReason()
                            : result.emissionCompleted() ? "EMISSION_CALLS_RETURNED" : "EMISSION_FAILED");
        } catch (RuntimeException | LinkageError failure) {
            registry.settled(event.token(), event.wrapper(), false, false, "THREW:" + failure.getClass().getSimpleName());
        }
    }

    public static Object[] fields(BlockCollectionEvent event) {
        BlockCollectionToken token = event.token();
        return ObservationFields.concat(new Object[]{"diagnosticOwner", "block_collection",
                "scannerSequence", token.scannerSequence(), "registryEpoch", token.registryEpoch(),
                "operationSequence", token.sequence(), "operationKind", token.operation().name(),
                "nativeBoundary", token.boundary(), "mapIdentity", token.mapIdentity(),
                "collectionIdentity", token.collectionIdentity(), "parentReadSequence", token.parentReadSequence(),
                "entryWriteAttemptGeneration", token.writeGenerationBefore(), "activeMarkerRetained", token.retained(),
                "collectionContentsInspected", false, "gameplayEffect", "NONE",
                "filePersistence", "NOT_VERIFIED", "writerGenerationAuthority", "OBSERVED_WRITE_ENTRY_NOT_CONTENT_VERSION"},
                ObservationFields.concat(token.context().fields(), event.fields()));
    }
}
