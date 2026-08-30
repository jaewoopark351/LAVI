package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPolicyEngine;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPolicyLoader;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void lazyRegistrationCreatesOneRuntimeAndOneAutomaticPressureChain() {
        Field instanceField = field(AltoClef.class, "instance");
        Object previousInstance = get(instanceField, null);
        try {
            TestAltoClef mod = new TestAltoClef();
            TaskRunner runner = new TaskRunner(mod);
            mod.runner = runner;
            set(instanceField, null, mod);
            AutoDepositTrustedDestinationRepository repository =
                    AutoDepositTrustedDestinationRepository.inMemoryEmpty();
            AutoDepositPolicyEngine engine = engine(repository);

            DepositAllAutoEntrypoint entrypoint = new DepositAllAutoEntrypoint(
                    ignored -> AutoDepositRuntime.create(mod, repository, engine)
            );
            invokeRegisterIfReady(entrypoint);
            AutoDepositRuntime firstRuntime = registeredRuntime(entrypoint);
            invokeRegisterIfReady(entrypoint);

            assertSame(firstRuntime, registeredRuntime(entrypoint));
            assertSame(repository, firstRuntime.trustedRepository());
            assertSame(engine, firstRuntime.policyEngine());
            assertEquals(1, registeredChains(runner).stream()
                    .filter(DepositAllInventoryPressureChain.class::isInstance)
                    .count());
            assertSame(firstRuntime.pressureChain(), registeredChains(runner).stream()
                    .filter(DepositAllInventoryPressureChain.class::isInstance)
                    .findFirst()
                    .orElseThrow());
        } finally {
            set(instanceField, null, previousInstance);
        }
    }

    @Test
    void failedPreparationLeavesNoOrphanAndControlledRetryRegistersExactlyOnce() {
        Field instanceField = field(AltoClef.class, "instance");
        Object previousInstance = get(instanceField, null);
        try {
            TestAltoClef mod = new TestAltoClef();
            TaskRunner runner = new TaskRunner(mod);
            mod.runner = runner;
            set(instanceField, null, mod);
            AutoDepositTrustedDestinationRepository repository =
                    AutoDepositTrustedDestinationRepository.inMemoryEmpty();
            AutoDepositTrustedDestinationRepository mismatchedRepository =
                    AutoDepositTrustedDestinationRepository.inMemoryEmpty();
            AutoDepositPolicyEngine matchingEngine = engine(repository);
            AutoDepositPolicyEngine mismatchedEngine = engine(mismatchedRepository);
            AtomicInteger attempts = new AtomicInteger();

            DepositAllAutoEntrypoint entrypoint = new DepositAllAutoEntrypoint(ignored -> {
                AutoDepositPolicyEngine selected = attempts.getAndIncrement() == 0
                        ? mismatchedEngine
                        : matchingEngine;
                return AutoDepositRuntime.create(mod, repository, selected);
            });

            assertThrows(AssertionError.class, () -> invokeRegisterIfReady(entrypoint));
            assertNull(registeredRuntime(entrypoint));
            assertEquals(0, registeredChains(runner).stream()
                    .filter(DepositAllInventoryPressureChain.class::isInstance)
                    .count());

            invokeRegisterIfReady(entrypoint);
            AutoDepositRuntime runtime = registeredRuntime(entrypoint);
            invokeRegisterIfReady(entrypoint);

            assertNotNull(runtime);
            assertSame(runtime, registeredRuntime(entrypoint));
            assertEquals(2, attempts.get());
            assertEquals(1, registeredChains(runner).stream()
                    .filter(DepositAllInventoryPressureChain.class::isInstance)
                    .count());
        } finally {
            set(instanceField, null, previousInstance);
        }
    }

    private static AutoDepositPolicyEngine engine(
            AutoDepositTrustedDestinationRepository repository) {
        return new AutoDepositPolicyEngine(
                new AutoDepositPolicyLoader().loadOrFailClosed(),
                repository
        );
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

    private static AutoDepositRuntime registeredRuntime(DepositAllAutoEntrypoint entrypoint) {
        return (AutoDepositRuntime) get(
                field(DepositAllAutoEntrypoint.class, "runtime"),
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
