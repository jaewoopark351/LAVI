package lavi.minecraft.diagnostics.mining.baritone.executor;

import java.util.function.LongSupplier;

//20260830_kpopmodder: Preserve the package API as a thin state-machine compatibility facade.
final class BaritoneExecutorProgressState {
    private final BaritoneExecutorProgressStateMachine stateMachine;

    BaritoneExecutorProgressState() {
        this(System::nanoTime);
    }

    BaritoneExecutorProgressState(LongSupplier monotonicNanos) {
        stateMachine = new BaritoneExecutorProgressStateMachine(monotonicNanos);
    }

    synchronized void recordHead(BaritoneExecutorProgressSnapshot snapshot) {
        stateMachine.recordHead(snapshot);
    }

    synchronized Emission recordReturn(BaritoneExecutorProgressSnapshot snapshot) {
        return Emission.wrap(stateMachine.recordReturn(snapshot));
    }

    static final class Emission {
        private final BaritoneExecutorProgressEmission delegate;

        private Emission(BaritoneExecutorProgressEmission delegate) {
            this.delegate = delegate;
        }

        static Emission suppressed() {
            return wrap(BaritoneExecutorProgressEmission.suppressed());
        }

        static Emission emit(BaritoneExecutorProgressSnapshot headSnapshot,
                             String reason,
                             String changedFields,
                             int localRepeatCount,
                             long ticksSinceExecutorAdvance,
                             long ticksSincePlayerMovement,
                             long ticksSinceTargetDistanceImprovement,
                             long ticksSinceAnyProgress,
                             String executorPositionDeltaSinceHead,
                             String executorPositionDeltaSinceLastReturn,
                             String playerDisplacementSinceHead,
                             String playerDisplacementSinceLastReturn,
                             String targetDistanceDeltaSinceHead,
                             String targetDistanceDeltaSinceLastReturn,
                             String fingerprintCore) {
            return wrap(BaritoneExecutorProgressEmission.emit(
                    headSnapshot,
                    reason,
                    changedFields,
                    localRepeatCount,
                    ticksSinceExecutorAdvance,
                    ticksSincePlayerMovement,
                    ticksSinceTargetDistanceImprovement,
                    ticksSinceAnyProgress,
                    executorPositionDeltaSinceHead,
                    executorPositionDeltaSinceLastReturn,
                    playerDisplacementSinceHead,
                    playerDisplacementSinceLastReturn,
                    targetDistanceDeltaSinceHead,
                    targetDistanceDeltaSinceLastReturn,
                    fingerprintCore
            ));
        }

        private static Emission wrap(BaritoneExecutorProgressEmission delegate) {
            return new Emission(delegate);
        }

        boolean emit() { return delegate.emit(); }
        BaritoneExecutorProgressSnapshot headSnapshot() { return delegate.headSnapshot(); }
        String reason() { return delegate.reason(); }
        String changedFields() { return delegate.changedFields(); }
        int localRepeatCount() { return delegate.localRepeatCount(); }
        long ticksSinceExecutorAdvance() { return delegate.ticksSinceExecutorAdvance(); }
        long ticksSincePlayerMovement() { return delegate.ticksSincePlayerMovement(); }
        long ticksSinceTargetDistanceImprovement() { return delegate.ticksSinceTargetDistanceImprovement(); }
        long ticksSinceAnyProgress() { return delegate.ticksSinceAnyProgress(); }
        String executorPositionDeltaSinceHead() { return delegate.executorPositionDeltaSinceHead(); }
        String executorPositionDeltaSinceLastReturn() { return delegate.executorPositionDeltaSinceLastReturn(); }
        String playerDisplacementSinceHead() { return delegate.playerDisplacementSinceHead(); }
        String playerDisplacementSinceLastReturn() { return delegate.playerDisplacementSinceLastReturn(); }
        String targetDistanceDeltaSinceHead() { return delegate.targetDistanceDeltaSinceHead(); }
        String targetDistanceDeltaSinceLastReturn() { return delegate.targetDistanceDeltaSinceLastReturn(); }
        String fingerprint(String eventName) { return delegate.fingerprint(eventName); }
    }
}
