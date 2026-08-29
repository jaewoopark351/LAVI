package lavi.minecraft.task.container.home.execution;

import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.command.StoreHomeTaskFactory;
import lavi.minecraft.task.container.home.execution.state.StoreHomeExecutionState;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutLifecycle;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertSame;

//20260829_kpopmodder: Prove the StoreHomeTask facade and its collaborators share one state and timeout identity.
class StoreHomeTaskObjectGraphIdentityTest {
    @Test
    void rootControllerAndViewShareTheExactExecutionState() {
        StoreHomeTask task = task();
        StoreHomeExecutionState state = (StoreHomeExecutionState) field(
                task, "state"
        );
        Object controller = field(task, "lifecycleController");
        Object view = field(task, "view");

        assertSame(state, field(controller, "state"));
        assertSame(state, field(view, "state"));
    }

    @Test
    void rootControllerAndTimeoutCollaboratorsShareTheExactLifecycle() {
        StoreHomeTask task = task();
        StoreHomeTimeoutLifecycle timeoutLifecycle =
                (StoreHomeTimeoutLifecycle) field(task, "timeoutLifecycle");
        Object controller = field(task, "lifecycleController");
        Object terminator = field(controller, "terminator");
        Object candidateStarter = field(controller, "candidateStarter");
        Object candidateTimeoutObserver = field(
                controller, "candidateTimeoutObserver"
        );
        Object navigationStep = field(controller, "navigationStep");
        Object sessionStep = field(controller, "sessionStep");
        Object timeoutDecisionApplier = field(
                controller, "timeoutDecisionApplier"
        );
        Object candidateRejector = field(
                navigationStep, "candidateRejector"
        );
        Object sessionActivator = field(navigationStep, "sessionActivator");
        Object transferResultHandler = field(
                sessionStep, "transferResultHandler"
        );

        assertSame(timeoutLifecycle, field(controller, "timeoutLifecycle"));
        assertTimeoutLifecycle(timeoutLifecycle, terminator);
        assertTimeoutLifecycle(timeoutLifecycle, candidateStarter);
        assertTimeoutLifecycle(timeoutLifecycle, candidateTimeoutObserver);
        assertTimeoutLifecycle(timeoutLifecycle, navigationStep);
        assertTimeoutLifecycle(timeoutLifecycle, candidateRejector);
        assertTimeoutLifecycle(timeoutLifecycle, sessionActivator);
        assertTimeoutLifecycle(timeoutLifecycle, transferResultHandler);
        assertTimeoutLifecycle(timeoutLifecycle, timeoutDecisionApplier);
    }

    private static StoreHomeTask task() {
        return new StoreHomeTaskFactory(
                AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                AutoDepositExactOpenContainerBinding.UNAVAILABLE
        ).create();
    }

    private static void assertTimeoutLifecycle(
            StoreHomeTimeoutLifecycle expected,
            Object collaborator) {
        assertSame(expected, field(collaborator, "timeoutLifecycle"));
    }

    private static Object field(Object owner, String fieldName) {
        try {
            Field field = owner.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(owner);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new AssertionError(
                    "Failed to read " + owner.getClass().getName()
                            + "." + fieldName,
                    exception
            );
        }
    }
}
