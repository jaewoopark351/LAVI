package lavi.minecraft.command.attack;

import adris.altoclef.tasks.ResourceTask;
import com.mojang.authlib.GameProfile;
import lavi.minecraft.testsupport.TestObjects;
import lavi.minecraft.testsupport.HeadlessMinecraftClientSession;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;

class KoreanAttackNativePredicateTest {
    @BeforeAll static void bootstrap() { SharedConstants.createGameVersion(); Bootstrap.initialize(); }
    private HeadlessMinecraftClientSession clientSession;
    @BeforeEach void bindClient() { clientSession = HeadlessMinecraftClientSession.inGame(); }
    @AfterEach void releaseClient() { clientSession.close(); }

    @Test void actualTaskPredicateRejectsSameNamedPlayerCreatedAfterKoreanScopeClosed() throws Exception {
        Object task;
        try (var scope = KoreanAttackExecutionScope.begin("attack zombie 3", Map.of("natural_language", Map.of("language", "ko")))) {
            task = task(KoreanAttackExecutionScope.mobOnly());
        }
        var laterPlayer = TestObjects.allocate(NamedPlayer.class);
        assertFalse(predicate(task).test(laterPlayer));
        assertTrue(predicate(task).test(TestObjects.allocate(TestZombie.class)));
    }

    @Test void nativeEnglishConstructorStillAcceptsSameNamedPlayer() throws Exception {
        Constructor<?> constructor = taskClass().getDeclaredConstructor(String.class, int.class);
        constructor.setAccessible(true);
        Object task = constructor.newInstance("zombie", 3);
        assertTrue(predicate(task).test(TestObjects.allocate(NamedPlayer.class)));
    }

    @Test void tasksWithDifferentPlayerPermissionsCannotBeReusedAsEqual() throws Exception {
        Object korean = task(true);
        Object english = task(false);
        var equals = taskClass().getDeclaredMethod("isEqualResource", ResourceTask.class);
        equals.setAccessible(true);
        assertEquals(false, equals.invoke(korean, english));
        assertEquals(true, equals.invoke(korean, task(true)));
    }

    private static Class<?> taskClass() throws Exception {
        return Class.forName("adris.altoclef.commands.AttackPlayerOrMobCommand$AttackAndGetDropsTask");
    }
    private Object task(boolean mobOnly) throws Exception {
        var constructor = taskClass().getDeclaredConstructor(String.class, int.class, boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance("zombie", 3, mobOnly);
    }
    @SuppressWarnings("unchecked") private Predicate<Entity> predicate(Object task) throws Exception {
        var field = taskClass().getDeclaredField("_shouldAttackPredicate");
        field.setAccessible(true);
        return (Predicate<Entity>) field.get(task);
    }
    private static final class NamedPlayer extends OtherClientPlayerEntity {
        private NamedPlayer() { super(null, new GameProfile(UUID.randomUUID(), "zombie")); }
        @Override public Text getName() { return Text.literal("zombie"); }
    }
    private static final class TestZombie extends ZombieEntity {
        private TestZombie() { super(EntityType.ZOMBIE, null); }
        @Override public EntityType<?> getType() { return EntityType.ZOMBIE; }
    }
}
