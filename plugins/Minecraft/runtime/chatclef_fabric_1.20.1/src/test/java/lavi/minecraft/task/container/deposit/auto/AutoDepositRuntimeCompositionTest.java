package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.TaskRunner;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPolicyEngine;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPolicyLoader;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertSame;

//20260829_kpopmodder: Prove one shared repository and exact-binding tracker across the restored runtime graph.
class AutoDepositRuntimeCompositionTest {
    @Test
    void runtimeSharesRepositoryEngineAndExactBindingAcrossEveryStoragePath() {
        TestAltoClef mod = new TestAltoClef();
        mod.runner = new TaskRunner(mod);
        AutoDepositTrustedDestinationRepository repository =
                AutoDepositTrustedDestinationRepository.inMemoryEmpty();
        AutoDepositPolicyEngine engine = new AutoDepositPolicyEngine(
                new AutoDepositPolicyLoader().loadOrFailClosed(),
                repository
        );

        AutoDepositRuntime runtime = AutoDepositRuntime.create(mod, repository, engine);
        DepositAllInventoryPressureChain chain = runtime.pressureChain();
        Object tracker = runtime.openContainerBindingTracker();

        assertSame(repository, runtime.trustedRepository());
        assertSame(repository, runtime.policyEngine().trustedRepository());
        assertSame(engine, read(chain, "policyEngine"));
        assertSame(repository, read(chain, "trustedRepository"));
        assertSame(tracker, read(chain, "exactOpenContainerBinding"));
        assertSame(repository, read(runtime.trustedCommandRegistrar(), "repository"));
        assertSame(repository, read(runtime.storeHomeTaskFactory(), "repository"));
        assertSame(tracker, read(runtime.storeHomeTaskFactory(), "exactOpenContainerBinding"));
        assertSame(runtime.storeHomeTaskFactory(),
                read(runtime.storeHomeCommandRegistrar(), "taskFactory"));

        Object targetResolver = read(runtime.trustedCommandRegistrar(), "targetResolver");
        assertSame(tracker, read(targetResolver, "openBinding"));
    }

    private static Object read(Object owner, String fieldName) {
        try {
            Field field = owner.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(
                    "Failed to inspect shared runtime field "
                            + owner.getClass().getName() + "." + fieldName,
                    exception
            );
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
