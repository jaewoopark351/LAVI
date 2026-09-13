//#if MC == 12001
package lavi.minecraft.task.movement.gotopreflight.diagnostics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

//20260913_kpopmodder: Keep signatures semantic and output bounded without querying live goals.
class GotoPreparationLogFormatterTest {
    @Test
    void originalOwnerIdentityAndSemanticSignaturesDoNotUseRuntimeDetail() {
        Object owner = new Object();
        assertEquals(Integer.toHexString(System.identityHashCode(owner)), GotoPreparationLogFormatter.operation(owner));
        assertEquals("PHASE:NATIVE:ARRIVAL_CLEANUP",
                GotoPreparationLogFormatter.requiredSignature("PHASE from=NATIVE to=ARRIVAL_CLEANUP player=123"));
        assertEquals("FAILED:HANDOFF_SHORTAGE",
                GotoPreparationLogFormatter.requiredSignature("FAILED reason=HANDOFF_SHORTAGE detail=anything operation=random"));
        assertNull(GotoPreparationLogFormatter.requiredSignature("FAILED reason=random_identifier"));
        assertNull(GotoPreparationLogFormatter.requiredSignature("PHASE from=random_identifier to=TERMINAL"));
    }

    @Test
    void detailAndContextValuesAreBoundedAndContextArrayIsCopied() {
        Object[] source = {"commandRequestId", "x".repeat(400) + "\nnext", "commandContextAvailable", true};
        Object[] snapshot = GotoPreparationLogFormatter.contextSnapshot(source);
        source[1] = "changed";
        assertEquals(256, ((String) snapshot[1]).length());
        assertFalse(((String) snapshot[1]).contains("\n"));
        assertEquals(1536, GotoPreparationLogFormatter.bounded("d".repeat(2000), 1536).length());
        assertEquals("a b c d", GotoPreparationLogFormatter.bounded("a\rb\nc\td", 100));
    }

    @Test
    void malformedOrUnboundedContextCannotExtendTheRecordWithoutLimit() {
        assertEquals("invalid_snapshot", GotoPreparationLogFormatter.contextSnapshot(new Object[]{"odd"})[3]);
        assertEquals(18, GotoPreparationLogFormatter.contextSnapshot(new Object[100]).length);
    }
}
//#endif
