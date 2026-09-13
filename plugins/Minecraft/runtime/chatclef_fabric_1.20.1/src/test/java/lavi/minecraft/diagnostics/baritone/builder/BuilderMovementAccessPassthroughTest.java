package lavi.minecraft.diagnostics.baritone.builder;

import adris.altoclef.mixins.diagnostics.BuilderProcessDiagnosticMixin;
import baritone.api.pathing.calc.IPath;
import baritone.pathing.path.PathExecutor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.junit.jupiter.api.Test;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises the actual hook body; runtime Mixin installation remains a separate check. */
class BuilderMovementAccessPassthroughTest {
    @Test void exactReceiverAndIndexReachOriginalExactlyOnce() throws Throwable {
        Object expected = new Object();
        List<?> receiver = List.of(expected);
        AtomicInteger calls = new AtomicInteger();
        Object result = invoke(receiver, 0, args -> {
            calls.incrementAndGet(); assertSame(receiver, args[0]); assertEquals(0, args[1]);
            return ((List<?>) args[0]).get((Integer) args[1]);
        });
        assertSame(expected, result); assertEquals(1, calls.get());
    }
    @Test void originalExceptionObjectEscapesWithoutFallbackOrSecondCall() {
        RuntimeException expected = new IndexOutOfBoundsException("original");
        AtomicInteger calls = new AtomicInteger();
        Throwable actual = assertThrows(IndexOutOfBoundsException.class,
                () -> invoke(List.of(), 0, args -> { calls.incrementAndGet(); throw expected; }));
        assertSame(expected, actual); assertEquals(1, calls.get());
    }
    private static Object invoke(List<?> receiver, int index, Operation<Object> operation) throws Throwable {
        BuilderProcessDiagnosticMixin hook = new BuilderProcessDiagnosticMixin() { };
        Method method = BuilderProcessDiagnosticMixin.class.getDeclaredMethod("lavi$observeMovementAccess",
                List.class, int.class, Operation.class, PathExecutor.class, LocalRef.class);
        method.setAccessible(true);
        LocalRef<IPath> path = new LocalRef<>() {
            private IPath value;
            @Override public IPath get() { return value; }
            @Override public void set(IPath value) { this.value = value; }
        };
        try { return method.invoke(hook, receiver, index, operation, null, path); }
        catch (InvocationTargetException wrapper) { throw wrapper.getCause(); }
    }
}
