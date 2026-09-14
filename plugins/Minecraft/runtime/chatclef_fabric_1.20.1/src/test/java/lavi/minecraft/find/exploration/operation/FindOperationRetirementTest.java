//#if MC == 12001
package lavi.minecraft.find.exploration.operation;

import java.lang.reflect.Field;
import lavi.minecraft.fabric.chatclef.bridge.command.result.find.FabricChatClefFindOperationRetirement;
import lavi.minecraft.find.command.FindNativeCompletionObserver;
import lavi.minecraft.find.model.FindRequest;
import lavi.minecraft.find.result.FindOutcome;
import lavi.minecraft.find.result.FindTaskResultSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Behavioral retirement is not diagnostic closure and observer state always detaches.
class FindOperationRetirementTest {
    private static final class Source implements FindTaskResultSource {
        int retired, diagnostics;
        boolean throwCleanup;
        @Override public FindRequest request() { return new FindRequest("entity", "minecraft:villager", "report", "catalog", 1); }
        @Override public String operationId() { return "op"; }
        @Override public FindOutcome outcome() { return null; }
        @Override public void retireOwnedResources(String reason) { retired++; if (throwCleanup) throw new IllegalStateException("cleanup"); }
        @Override public void diagnosticRetired(String reason) { diagnostics++; }
        @Override public void suppressNativePresentation() { }
        @Override public boolean nativePresentationSuppressed() { return false; }
    }
    @Test void externalDeadlineRetiresExactOperationWithoutDependingOnDiagnosticClosure() {
        Source source = new Source();
        FabricChatClefFindOperationRetirement.retire(source, "bridge_deadline_exceeded");
        assertEquals(1, source.retired); assertEquals(0, source.diagnostics);
        FabricChatClefFindOperationRetirement.retire(null, "deadline");
        FabricChatClefFindOperationRetirement.retire(new Object(), "deadline");
        assertEquals(1, source.retired);
    }
    @Test void nativeObserverDetachesEvenWhenBehaviorCleanupThrows() throws Exception {
        Source source = new Source(); source.throwCleanup = true;
        var observer = new FindNativeCompletionObserver();
        Field field = FindNativeCompletionObserver.class.getDeclaredField("source"); field.setAccessible(true); field.set(observer, source);
        assertThrows(IllegalStateException.class, () -> observer.retire("root_replaced"));
        assertNull(field.get(observer)); assertEquals(1, source.retired);
        observer.retire("again"); assertEquals(1, source.retired);
    }
}
//#endif
