package lavi.minecraft.diagnostics.baritone.correlation;

import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.behavior.PathingBehavior;
import baritone.pathing.calc.AbstractNodeCostSearch;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.baritone.builder.BuilderPathSnapshot;
import lavi.minecraft.diagnostics.baritone.builder.BuilderTaskContextSnapshot;
import lavi.minecraft.diagnostics.observation.ObservationActivation;
import lavi.minecraft.diagnostics.observation.ObservationDiagnostics;
import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleObserver;
import lavi.minecraft.diagnostics.session.lifecycle.registration.DiagnosticOwnerRegistration;

import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.lang.ref.WeakReference;

//20260913_kpopmodder: Bind render-thread request provenance before scheduling, never from a later worker command.
public final class BuilderTraceRegistry {
    private static final Map<PathingBehavior, BuilderTraceState> STATES = new WeakHashMap<>();
    private static final ThreadLocal<WorkerBinding> WORKER = new ThreadLocal<>();
    private static final AtomicLong GENERATIONS = new AtomicLong();
    //20260913_kpopmodder: Late path workers cannot revive a diagnostic owner whose registration failed.
    private static final DiagnosticOwnerRegistration LIFECYCLE =
            new DiagnosticOwnerRegistration("builder-path-trace", BuilderTraceRegistry::clear);
    static {
        ChatClefDiagnostics.registerSessionLifecycleOwner(LIFECYCLE, new DiagnosticSessionLifecycleObserver() {
            @Override public void beforeModeOff() { clear(); }
            @Override public void afterCleanTeardownSnapshotAttempt(boolean returned) { clear(); }
        });
    }
    private BuilderTraceRegistry() { }

    public static boolean isAvailable() { return LIFECYCLE.isAvailable(); }

    public static BuilderTraceState owner(PathingBehavior behavior) {
        if (!isAvailable() || behavior == null || !ChatClefDiagnostics.isBoundaryEnabled()) return null;
        BuilderTraceState state;
        synchronized (STATES) { state = STATES.get(behavior); }
        return state != null && state.current() ? state : null;
    }

    public static void scheduled(PathingBehavior behavior, AbstractNodeCostSearch finder, Goal goal) {
        ChatClefDiagnostics.runIfDiagnosticsEligible(() -> LIFECYCLE.runIfAvailable(() ->
                scheduledEligible(behavior, finder, goal)));
    }

    private static void scheduledEligible(PathingBehavior behavior, AbstractNodeCostSearch finder, Goal goal) {
        if (finder == null) return;
        BuilderTraceState state = observeOwner(behavior);
        if (state == null || !state.current()) return;
        long generation = GENERATIONS.incrementAndGet();
        String request = commandOrigin();
        String goalValue = bound(String.valueOf(goal), 256);
        BuilderPathProvenance origin = new BuilderPathProvenance(generation, request, BuilderTaskContextSnapshot.capture(), goalValue,
                "CALCULATION_SCHEDULED", "none", "none", BuilderPathSnapshot.capture(null));
        state.ledger.bind(finder, origin);
        state.event("BARITONE_BUILDER_PATH_REQUEST", "SCHEDULED", Long.toString(generation), origin.fields());
    }

    public static BuilderTraceState observeOwner(PathingBehavior behavior) {
        return ChatClefDiagnostics.callIfDiagnosticsEligible(() -> LIFECYCLE.callIfAvailable(() ->
                observeOwnerEligible(behavior), null), null);
    }

    private static BuilderTraceState observeOwnerEligible(PathingBehavior behavior) {
        if (behavior == null) return null;
        // The existing caller is the client thread; world() is a read-only context getter.
        ObservationActivation activation = ObservationDiagnostics.captureActivation(behavior, behavior.ctx.world());
        BuilderTraceState existing = owner(behavior);
        if (existing != null) return existing;
        BuilderTraceState replacement = new BuilderTraceState(
                ObservationDiagnostics.open(activation, "builder", "builder-path-lifecycle"));
        // Always acquire the shared eligibility lock BEFORE this registry lock (OFF uses that same order).
        return ChatClefDiagnostics.callIfDiagnosticsEligible(() -> {
            if (!replacement.current()) return null;
            synchronized (STATES) {
                BuilderTraceState previous = STATES.put(behavior, replacement);
                if (previous != null) previous.ledger.invalidate();
            }
            return replacement;
        }, null);
    }

    public static void workerEnter(PathingBehavior behavior, AbstractNodeCostSearch finder) {
        WORKER.remove();
        try {
            ChatClefDiagnostics.runIfDiagnosticsEligible(() -> LIFECYCLE.runIfAvailable(() -> {
            BuilderTraceState state = owner(behavior);
            if (state == null) return;
            BuilderPathProvenance origin = state.ledger.find(finder);
            if (origin != null) WORKER.set(new WorkerBinding(state, origin, new WeakReference<>(finder)));
            }));
        } catch (RuntimeException | LinkageError failure) {
            ObservationDiagnostics.captureFailed("builder", failure.getClass().getSimpleName());
        }
    }
    public static void workerExit() {
        WorkerBinding binding = WORKER.get();
        WORKER.remove();
        if (binding != null) binding.state.ledger.remove(binding.finder.get());
    }
    public static WorkerBinding worker() {
        WorkerBinding binding = WORKER.get();
        if (binding != null && binding.state.current()) return binding;
        WORKER.remove();
        return null;
    }
    public static BuilderTraceState containing(Object path) {
        ArrayList<BuilderTraceState> states;
        synchronized (STATES) { states = new ArrayList<>(STATES.values()); }
        for (BuilderTraceState state : states) {
            if (state.current() && state.ledger.find(path) != null) return state;
        }
        return null;
    }
    public static synchronized void clear() {
        synchronized (STATES) {
            for (BuilderTraceState state : STATES.values()) state.ledger.invalidate();
            STATES.clear();
        }
        WORKER.remove();
    }
    public static String commandOrigin() {
        Object[] fields = ChatClefDiagnostics.withCommandContextFields();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i + 1 < fields.length; i += 2) {
            String key = String.valueOf(fields[i]);
            if (key.equals("commandRequestId") || key.equals("commandCorrelationId")
                    || key.equals("commandSessionId") || key.equals("commandConnectionGeneration")
                    || key.equals("commandRootTaskIdentity")) {
                result.append(key).append('=').append(bound(String.valueOf(fields[i + 1]), 160)).append(';');
            }
        }
        return result.isEmpty() ? "UNBOUND" : bound(result.toString(), 768);
    }
    public static String bound(String text, int cap) { return text.length() <= cap ? text : text.substring(0, cap); }
    public record WorkerBinding(BuilderTraceState state, BuilderPathProvenance origin, WeakReference<Object> finder) { }
}
