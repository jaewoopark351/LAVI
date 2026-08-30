package lavi.minecraft.testsupport;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public final class TestObjects {
    private static final sun.misc.Unsafe UNSAFE = unsafe();
    private static final ReflectionFactory REFLECTION_FACTORY = ReflectionFactory.getReflectionFactory();

    private TestObjects() {
    }

    public static <T> T allocate(Class<T> type) {
        try {
            Constructor<Object> objectConstructor = Object.class.getDeclaredConstructor();
            Constructor<?> constructor = REFLECTION_FACTORY.newConstructorForSerialization(type, objectConstructor);
            constructor.setAccessible(true);
            return type.cast(constructor.newInstance());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new AssertionError("Failed to allocate test instance for " + type.getName(), cause);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return allocateUnsafe(type, e);
        }
    }

    public static <T> T allocateBootstrapped(Class<T> type) {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        return allocate(type);
    }

    private static <T> T allocateUnsafe(Class<T> type, Throwable reflectionFailure) {
        try {
            return type.cast(UNSAFE.allocateInstance(type));
        } catch (InstantiationException e) {
            AssertionError error = new AssertionError("Failed to allocate test instance for " + type.getName(), e);
            error.addSuppressed(reflectionFailure);
            throw error;
        }
    }

    public static void setField(Object target, Class<?> owner, String name, Object value) {
        try {
            Field field = owner.getDeclaredField(name);
            UNSAFE.putObject(target, UNSAFE.objectFieldOffset(field), value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to set test field " + owner.getName() + "." + name, e);
        }
    }

    private static sun.misc.Unsafe unsafe() {
        try {
            Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (sun.misc.Unsafe) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
