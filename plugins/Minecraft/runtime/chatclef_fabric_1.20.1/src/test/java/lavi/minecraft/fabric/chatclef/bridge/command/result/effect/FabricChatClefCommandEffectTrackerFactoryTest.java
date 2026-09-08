package lavi.minecraft.fabric.chatclef.bridge.command.result.effect;

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

//20260907_kpopmodder: Verify unsupported families remain on the behavior-preserving no-effect path.
class FabricChatClefCommandEffectTrackerFactoryTest {
    @Test
    void unsupportedAndAbsentCommandsSelectTheNoEffectTracker() {
        assertSame(
                FabricChatClefNoEffectTracker.instance(),
                FabricChatClefCommandEffectTrackerFactory.capture("deposit diamond 1")
        );
        assertSame(
                FabricChatClefNoEffectTracker.instance(),
                FabricChatClefCommandEffectTrackerFactory.capture(null)
        );
    }

    @Test
    void bracketedAndMultiTargetGetFormsRemainOnTheCautiousNoEffectPath() {
        assertSame(
                FabricChatClefNoEffectTracker.instance(),
                FabricChatClefCommandEffectTrackerFactory.capture(
                        "@get [oak_log 1,spruce_log 1]"
                )
        );
        assertSame(
                FabricChatClefNoEffectTracker.instance(),
                FabricChatClefCommandEffectTrackerFactory.capture(
                        "@get oak_log 1 spruce_log 1"
                )
        );
    }

    @Test
    void noEffectTrackerPreservesExistingResultData() {
        FabricChatClefCommandResultDataPayload base =
                FabricChatClefCommandResultDataPayload.fromMap(
                        Map.of("result_reason", "matching_task_finished")
                );

        FabricChatClefCommandResultDataPayload result =
                FabricChatClefNoEffectTracker.instance()
                        .fromMatchingCompletion(base);

        assertSame(base, result);
        assertEquals("matching_task_finished", result.toMap().get("result_reason"));
    }
}
