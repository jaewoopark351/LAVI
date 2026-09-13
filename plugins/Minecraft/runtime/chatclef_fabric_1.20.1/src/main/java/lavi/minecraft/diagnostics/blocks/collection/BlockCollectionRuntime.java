package lavi.minecraft.diagnostics.blocks.collection;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.blocks.collection.context.BlockCollectionContext;
import lavi.minecraft.diagnostics.blocks.collection.emission.BlockCollectionEmitter;
import lavi.minecraft.diagnostics.blocks.collection.state.*;
import lavi.minecraft.diagnostics.observation.format.ObservationFields;
import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleObserver;
import lavi.minecraft.diagnostics.session.lifecycle.registration.DiagnosticObserverRegistrationResult;
import lavi.minecraft.diagnostics.session.lifecycle.registration.DiagnosticObserverRegistrationStatus;
import lavi.minecraft.diagnostics.session.lifecycle.registration.DiagnosticOwnerRegistration;

//20260913_kpopmodder: Run diagnostic state mutations under session/owner locks, returning before native code runs.
final class BlockCollectionRuntime {
    private static final BlockCollectionRegistry REGISTRY = new BlockCollectionRegistry();
    private static final DiagnosticOwnerRegistration OWNER = new DiagnosticOwnerRegistration("block_collection", REGISTRY::clear);
    private static final DiagnosticSessionLifecycleObserver OBSERVER = new DiagnosticSessionLifecycleObserver() {
        @Override public void beforeModeOff() { REGISTRY.clear(); }
        @Override public Object[] finalSnapshotFields() { return REGISTRY.finalSnapshotFields(); }
        @Override public void afterCleanTeardownSnapshotAttempt(boolean returned) { REGISTRY.clear(); }
    };

    private BlockCollectionRuntime() { }

    static DiagnosticObserverRegistrationResult install() {
        if (OWNER.registrationResult().status() == DiagnosticObserverRegistrationStatus.UNREGISTERED)
            return ChatClefDiagnostics.registerSessionLifecycleOwner(OWNER, OBSERVER);
        return OWNER.registrationResult();
    }

    static BlockCollectionToken begin(Object scanner, Object map, Object collection,
            BlockCollectionOperation operation, String boundary) {
        install();
        return ChatClefDiagnostics.callIfDiagnosticsEligible(() -> OWNER.callIfAvailable(() -> {
            BlockCollectionTransition transition = REGISTRY.begin(scanner, identity(map), identity(collection), operation,
                    ObservationFields.text(boundary), BlockCollectionContext.capture());
            emit(transition);
            return transition.token();
        }, null), null);
    }

    static void end(BlockCollectionToken token, boolean normal, Object returnedList) {
        ChatClefDiagnostics.runIfDiagnosticsEligible(() -> OWNER.runIfAvailable(() -> emit(REGISTRY.end(
                token, normal, returnedList, ChatClefDiagnostics.currentClientTickId(), System.nanoTime()))));
    }

    static BlockCollectionReadOrigin readOrigin(Object list) {
        return ChatClefDiagnostics.callIfDiagnosticsEligible(() -> OWNER.callIfAvailable(() -> REGISTRY.readOrigin(list), null), null);
    }

    static void nullConsumed(BlockCollectionReadOrigin origin, Object consumer) {
        ChatClefDiagnostics.runIfDiagnosticsEligible(() -> OWNER.runIfAvailable(() -> emit(
                REGISTRY.nullConsumed(origin, identity(consumer)))));
    }

    static void disable() { OWNER.disable("DIAGNOSTIC_CAPTURE_FAILED"); }
    private static void emit(BlockCollectionTransition transition) {
        for (BlockCollectionEvent event : transition.events()) BlockCollectionEmitter.emit(REGISTRY, event);
    }
    private static String identity(Object value) {
        return value == null ? "UNAVAILABLE" : value.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(value));
    }
}
