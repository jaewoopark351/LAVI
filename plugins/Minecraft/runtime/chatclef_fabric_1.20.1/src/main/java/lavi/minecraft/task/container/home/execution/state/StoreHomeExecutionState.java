package lavi.minecraft.task.container.home.execution.state;

import lavi.minecraft.task.container.home.execution.operation.StoreHomeOperationProgress;
import lavi.minecraft.task.container.home.execution.state.candidate.StoreHomeCandidateAttemptState;
import lavi.minecraft.task.container.home.execution.state.candidate.StoreHomeCandidateQueueState;
import lavi.minecraft.task.container.home.execution.state.context.StoreHomeContextState;
import lavi.minecraft.task.container.home.execution.state.lifecycle.StoreHomeTaskLifecycleState;
import lavi.minecraft.task.container.home.execution.state.operation.StoreHomeOperationAccumulatorState;
import lavi.minecraft.task.container.home.execution.state.reporting.StoreHomePublishedPlanState;
import lavi.minecraft.task.container.home.execution.state.session.HomeStorageSessionState;

//20260829_kpopmodder: Compose focused STORE_HOME state owners without merging their lifecycles.
public final class StoreHomeExecutionState {
    private final StoreHomeTaskLifecycleState lifecycle =
            new StoreHomeTaskLifecycleState();
    private final StoreHomeOperationAccumulatorState operation;
    private final StoreHomeContextState context = new StoreHomeContextState();
    private final StoreHomeCandidateQueueState candidateQueue =
            new StoreHomeCandidateQueueState();
    private final StoreHomeCandidateAttemptState candidateAttempt =
            new StoreHomeCandidateAttemptState();
    private final HomeStorageSessionState session = new HomeStorageSessionState();
    private final StoreHomePublishedPlanState publishedPlan =
            new StoreHomePublishedPlanState();

    public StoreHomeExecutionState(StoreHomeOperationProgress operation) {
        this.operation = new StoreHomeOperationAccumulatorState(operation);
    }

    public StoreHomeTaskLifecycleState lifecycle() {
        return lifecycle;
    }

    public StoreHomeOperationAccumulatorState operation() {
        return operation;
    }

    public StoreHomeContextState context() {
        return context;
    }

    public StoreHomeCandidateQueueState candidateQueue() {
        return candidateQueue;
    }

    public StoreHomeCandidateAttemptState candidateAttempt() {
        return candidateAttempt;
    }

    public HomeStorageSessionState session() {
        return session;
    }

    public StoreHomePublishedPlanState publishedPlan() {
        return publishedPlan;
    }
}
