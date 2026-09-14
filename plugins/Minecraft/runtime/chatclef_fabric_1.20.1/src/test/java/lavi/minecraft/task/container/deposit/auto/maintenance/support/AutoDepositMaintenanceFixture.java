package lavi.minecraft.task.container.deposit.auto.maintenance.support;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.UserTaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositContextSnapshot;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;
import lavi.minecraft.testsupport.HeadlessMinecraftClientSession;
import lavi.minecraft.testsupport.TestObjects;
import lavi.minecraft.testsupport.auto.AutoDepositPlanFixtureFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

//20260914_kpopmodder: Exercise real Task lifecycle with bounded headless context and restore singleton ownership.
public final class AutoDepositMaintenanceFixture implements AutoCloseable {
    private final Object previousMod;
    private final HeadlessMinecraftClientSession session;
    private final AltoClef mod;
    private final List<AutoDepositContextSnapshot> contexts = new ArrayList<>();

    public AutoDepositMaintenanceFixture() {
        previousMod = readStatic();
        session = HeadlessMinecraftClientSession.outOfGame();
        mod = new AltoClef();
        writeStatic(mod);
    }

    public AutoDepositPlan plan(int start) {
        AutoDepositContextSnapshot context = new AutoDepositContextSnapshot(new Object(),
                Dimension.OVERWORLD, "maintenance-result-test", 1L, null, null, List.of());
        AutoDepositPlan plan = AutoDepositPlanFixtureFactory.generalPlan(context, null, start, 5, 1);
        contexts.add(context);
        return plan;
    }

    public UserTaskChain chain() {
        // Construction retains a non-null production world identity; only the headless tick binds its null world.
        contexts.forEach(context -> TestObjects.setField(context, AutoDepositContextSnapshot.class, "worldIdentity", null));
        return new UserTaskChain(new TaskRunner(mod));
    }

    @Override
    public void close() {
        writeStatic(previousMod);
        session.close();
    }

    private static Object readStatic() {
        try {
            Field field = AltoClef.class.getDeclaredField("instance");
            field.setAccessible(true);
            return field.get(null);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }

    private static void writeStatic(Object value) {
        try {
            Field field = AltoClef.class.getDeclaredField("instance");
            field.setAccessible(true);
            field.set(null, value);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
}
