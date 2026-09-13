package lavi.minecraft.diagnostics.blocks.collection;

import lavi.minecraft.diagnostics.blocks.collection.state.BlockCollectionOperation;
import lavi.minecraft.diagnostics.blocks.collection.state.BlockCollectionReadOrigin;
import lavi.minecraft.diagnostics.blocks.collection.state.BlockCollectionToken;
import lavi.minecraft.diagnostics.session.lifecycle.registration.DiagnosticObserverRegistrationResult;
import lavi.minecraft.diagnostics.session.lifecycle.registration.DiagnosticObserverRegistrationStatus;
import lavi.minecraft.diagnostics.session.lifecycle.registration.DiagnosticRegistrationFailures;

//20260913_kpopmodder: Isolate diagnostic initialization/capture failure before and inside native finally blocks.
public final class BlockCollectionDiagnostics {
    private static volatile boolean disabled;
    private BlockCollectionDiagnostics() { }

    public static DiagnosticObserverRegistrationResult install() {
        if (!disabled) try { return BlockCollectionRuntime.install(); }
        catch (RuntimeException | LinkageError failure) { failed(failure); }
        return new DiagnosticObserverRegistrationResult(DiagnosticObserverRegistrationStatus.INVALID_OBSERVER, -1, 32, 0);
    }

    public static BlockCollectionToken begin(Object scanner, Object map, Object collection,
            BlockCollectionOperation operation, String boundary) {
        if (!disabled) try { return BlockCollectionRuntime.begin(scanner, map, collection, operation, boundary); }
        catch (RuntimeException | LinkageError failure) { failed(failure); }
        return null;
    }

    public static void end(BlockCollectionToken token, boolean normalReturn, Object returnedList) {
        if (!disabled && token != null) try { BlockCollectionRuntime.end(token, normalReturn, returnedList); }
        catch (RuntimeException | LinkageError failure) { failed(failure); }
    }

    public static BlockCollectionReadOrigin readOrigin(Object list) {
        if (!disabled) try { return BlockCollectionRuntime.readOrigin(list); }
        catch (RuntimeException | LinkageError failure) { failed(failure); }
        return null;
    }

    public static void nullConsumed(BlockCollectionReadOrigin origin, Object consumer) {
        if (!disabled) try { BlockCollectionRuntime.nullConsumed(origin, consumer); }
        catch (RuntimeException | LinkageError failure) { failed(failure); }
    }

    private static void failed(Throwable failure) {
        disabled = true;
        try { BlockCollectionRuntime.disable(); } catch (RuntimeException | LinkageError ignored) { }
        try { DiagnosticRegistrationFailures.reportInitializationFailure("block_collection", "PASSIVE_CAPTURE", failure); }
        catch (RuntimeException | LinkageError ignored) { }
    }
}
