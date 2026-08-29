package lavi.minecraft.task.container.home.execution;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.task.container.home.execution.state.StoreHomeExecutionState;
import lavi.minecraft.task.container.home.execution.task.lifecycle.StoreHomeTaskLifecycleController;
import lavi.minecraft.task.container.home.execution.task.view.StoreHomeTaskView;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutLifecycle;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260829_kpopmodder: Lock the StoreHomeTask thin-facade refactor boundary.
class StoreHomeTaskStructureTest {
    @Test
    void rootKeepsTaskLifecycleAndReflectedRuntimeStateContracts() {
        assertTrue(Task.class.isAssignableFrom(StoreHomeTask.class));
        assertEquals(
                StoreHomeExecutionState.class,
                declaredField("state").getType()
        );
        assertEquals(
                StoreHomeTimeoutLifecycle.class,
                declaredField("timeoutLifecycle").getType()
        );

        Set<String> methods = declaredMethodNames();
        assertTrue(methods.contains("onStart"));
        assertTrue(methods.contains("onTick"));
        assertTrue(methods.contains("onStop"));
        assertTrue(methods.contains("result"));
        assertTrue(methods.contains("phase"));
        assertTrue(methods.contains("plan"));
        assertTrue(methods.contains("outcome"));
    }

    @Test
    void independentWorkflowStagesStayOutsideTheRootTask() {
        Set<Class<?>> collaborators = Arrays.stream(
                        StoreHomeTask.class.getDeclaredFields()
                )
                .map(Field::getType)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                StoreHomeTimeoutLifecycle.class,
                StoreHomeExecutionState.class,
                StoreHomeTaskLifecycleController.class,
                StoreHomeTaskView.class
        ), collaborators);

        Set<String> rootMethods = declaredMethodNames();
        assertFalse(rootMethods.contains("initialize"));
        assertFalse(rootMethods.contains("ensureActiveCandidate"));
        assertFalse(rootMethods.contains("tickNavigation"));
        assertFalse(rootMethods.contains("activateSession"));
        assertFalse(rootMethods.contains("tickSession"));
        assertFalse(rootMethods.contains("handleTransferResult"));
        assertFalse(rootMethods.contains("rejectActiveCandidate"));
        assertFalse(rootMethods.contains("finishExhausted"));
        assertFalse(rootMethods.contains("finish"));
        assertFalse(rootMethods.contains("recordProgress"));
    }

    @Test
    void bothExistingConstructorEntryPointsRemainAvailable() {
        Set<Integer> constructorArities = Arrays.stream(
                        StoreHomeTask.class.getConstructors()
                )
                .map(constructor -> constructor.getParameterCount())
                .collect(Collectors.toSet());

        assertTrue(constructorArities.contains(9));
        assertTrue(constructorArities.contains(15));
    }

    private static Field declaredField(String name) {
        try {
            return StoreHomeTask.class.getDeclaredField(name);
        } catch (NoSuchFieldException exception) {
            throw new AssertionError("Missing StoreHomeTask field " + name, exception);
        }
    }

    private static Set<String> declaredMethodNames() {
        return Arrays.stream(StoreHomeTask.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
    }
}
