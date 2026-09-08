package lavi.minecraft.fabric.chatclef.bridge.command.execution;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.FabricChatClefCommandEffectTracker;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.FabricChatClefNoEffectTracker;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260907_kpopmodder: Lock effect capture to the exact normalized string sent to CommandExecutor.
class FabricChatClefCommandExecutionStateEffectTrackerFactoryTest {
    @Test
    void prefixlessInboundRequestCapturesEffectFromPrefixedNormalizedExecutionCommand() {
        FabricChatClefCommandRequest request = new FabricChatClefCommandRequest();
        request.requestId = "req-normalized-get";
        request.command = "get oak_log 2";
        request.source = "test";
        AtomicReference<String> capturedCommand = new AtomicReference<>();
        Function<String, FabricChatClefCommandEffectTracker> effectTrackerFactory =
                normalizedCommand -> {
                    capturedCommand.set(normalizedCommand);
                    return FabricChatClefNoEffectTracker.instance();
                };

        new FabricChatClefCommandExecutionState(
                new FabricChatClefCommandContext(
                        request,
                        "corr-normalized-get",
                        "session-normalized-get",
                        1L
                ),
                "@get oak_log 2",
                FabricChatClefTaskOwnershipEvidence.empty(),
                effectTrackerFactory
        );

        assertEquals("@get oak_log 2", capturedCommand.get());
    }
}
