package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPolicyEngine;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class DepositAllAutoEntrypointRegistrationTest {
    private static final String ENTRYPOINT_CLASS =
            "lavi.minecraft.task.container.deposit.auto.DepositAllAutoEntrypoint";

    @Test
    void fabricManifestRegistersTheAutomaticEntrypointExactlyOnce() throws IOException {
        try (InputStream input = DepositAllAutoEntrypointRegistrationTest.class
                .getClassLoader()
                .getResourceAsStream("fabric.mod.json")) {
            assertNotNull(input, "fabric.mod.json must be available on the test runtime classpath");
            JsonNode mainEntrypoints = new ObjectMapper()
                    .readTree(input)
                    .path("entrypoints")
                    .path("main");

            long registrationCount = 0;
            for (JsonNode entrypoint : mainEntrypoints) {
                if (ENTRYPOINT_CLASS.equals(entrypoint.asText())) {
                    registrationCount++;
                }
            }
            assertEquals(1, registrationCount);
        }
    }

    @Test
    void lazyRegistrationAddsOnlyOneAutomaticChainToTheRunner() {
        Field instanceField = field(AltoClef.class, "instance");
        Object previousInstance = get(instanceField, null);
        try {
            TestAltoClef mod = new TestAltoClef();
            TaskRunner runner = new TaskRunner(mod);
            mod.runner = runner;
            set(instanceField, null, mod);

            DepositAllAutoEntrypoint entrypoint = new DepositAllAutoEntrypoint(
                    AutoDepositPolicyEngine::inMemoryDefault
            );
            invokeRegisterIfReady(entrypoint);
            DepositAllInventoryPressureChain firstChain = registeredChain(entrypoint);
            invokeRegisterIfReady(entrypoint);

            assertSame(firstChain, registeredChain(entrypoint));
            assertEquals(1, registeredChains(runner).stream()
                    .filter(DepositAllInventoryPressureChain.class::isInstance)
                    .count());
        } finally {
            set(instanceField, null, previousInstance);
        }
    }

    private static void invokeRegisterIfReady(DepositAllAutoEntrypoint entrypoint) {
        try {
            Method method = DepositAllAutoEntrypoint.class.getDeclaredMethod("registerIfReady");
            method.setAccessible(true);
            method.invoke(entrypoint);
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new AssertionError("Failed to invoke automatic deposit_all registration", exception);
        } catch (InvocationTargetException exception) {
            throw new AssertionError("Automatic deposit_all registration failed", exception.getCause());
        }
    }

    private static DepositAllInventoryPressureChain registeredChain(DepositAllAutoEntrypoint entrypoint) {
        return (DepositAllInventoryPressureChain) get(
                field(DepositAllAutoEntrypoint.class, "chain"),
                entrypoint
        );
    }

    @SuppressWarnings("unchecked")
    private static List<TaskChain> registeredChains(TaskRunner runner) {
        return (List<TaskChain>) get(field(TaskRunner.class, "chains"), runner);
    }

    private static Field field(Class<?> declaringClass, String fieldName) {
        try {
            Field field = declaringClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException exception) {
            throw new AssertionError("Missing lifecycle field " + declaringClass.getName() + "." + fieldName, exception);
        }
    }

    private static Object get(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException exception) {
            throw new AssertionError("Failed to read lifecycle field " + field.getName(), exception);
        }
    }

    private static void set(Field field, Object target, Object value) {
        try {
            field.set(target, value);
        } catch (IllegalAccessException exception) {
            throw new AssertionError("Failed to write lifecycle field " + field.getName(), exception);
        }
    }

    private static final class TestAltoClef extends AltoClef {
        private TaskRunner runner;

        @Override
        public TaskRunner getTaskRunner() {
            return runner;
        }
    }
}
