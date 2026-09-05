package lavi.minecraft.fabric.chatclef.bridge.transport.inbound;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FabricChatClefInboundTextFrameAssemblerTest {
    @Test
    void emitsOnlyOneCompleteMessageAfterTheFinalFragment() {
        FabricChatClefInboundTextFrameAssembler assembler = new FabricChatClefInboundTextFrameAssembler();

        assertTrue(assembler.append("first-", false).isEmpty());
        assertEquals("first-second", assembler.append("second", true).orElseThrow());
        assertEquals("next", assembler.append("next", true).orElseThrow());
    }

    @Test
    void resetDiscardsFragmentsFromThePreviousConnection() {
        FabricChatClefInboundTextFrameAssembler assembler = new FabricChatClefInboundTextFrameAssembler();

        assertTrue(assembler.append("stale-", false).isEmpty());
        assembler.reset();

        assertEquals("fresh", assembler.append("fresh", true).orElseThrow());
    }
}
