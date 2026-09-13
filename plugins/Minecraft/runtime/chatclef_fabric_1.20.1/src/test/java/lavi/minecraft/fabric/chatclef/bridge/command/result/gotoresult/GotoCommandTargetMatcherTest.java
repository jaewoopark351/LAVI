//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.command.result.gotoresult;

import adris.altoclef.commandsystem.GotoTarget;
import lavi.minecraft.task.movement.gotoresult.model.GotoTargetSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Keep result admission aligned with existing signed-integer and outer-parenthesis syntax.
class GotoCommandTargetMatcherTest {
    @Test void balancedOuterParenthesesAndPositiveSignsMatchTheExistingParser() throws Exception {
        for (String arguments : List.of("500 80 -950", "(500 80 -950)", "(500 80 -950 overworld)",
                "+500 +80 -950", "(+500 +80 -950)", "(+500 +80 -950 OVERWORLD)",
                "(+2147483647 -2147483648 +0)")) {
            GotoTarget nativeTarget = GotoTarget.parseRemainder(arguments);
            assertEquals(GotoTarget.GotoTargetCoordType.XYZ, nativeTarget.getType());
            var snapshot = new GotoTargetSnapshot(nativeTarget.getX(), nativeTarget.getY(), nativeTarget.getZ(),
                    nativeTarget.getDimension() == null ? null : nativeTarget.getDimension().name().toLowerCase(Locale.ROOT),
                    "minecraft:overworld");
            String command = "@goto " + arguments;
            assertTrue(GotoCommandTargetMatcher.sameDimensionDirectXyz(command, "minecraft:overworld"), command);
            assertTrue(GotoCommandTargetMatcher.matches(command, snapshot), command);
        }
    }

    @Test void bothBoundariesRejectMalformedOrUnsupportedWholeCommands() {
        var target = new GotoTargetSnapshot(500, 80, -950, null, "minecraft:overworld");
        for (String command : List.of("@goto (500 80 -950", "@goto 500 80 -950)", "@goto ((500 80 -950))",
                "@goto (500 80 -950) extra", "@goto (500 80 -950 extra)", "@goto (500 80 -950 overworld extra)",
                "@goto (500 80 -950) overworld", "@goto (500 -950)", "@goto (80)", "@goto (overworld)",
                "@goto (+2147483648 80 -950)", "@goto (500 80 -950))", "@goto (+ 80 -950)",
                "@goto (+500 +80 -950);stop", "@goto (+500 +80 -950)\nstop")) {
            assertFalse(GotoCommandTargetMatcher.sameDimensionDirectXyz(command, "minecraft:overworld"), command);
            assertFalse(GotoCommandTargetMatcher.matches(command, target), command);
        }
    }

    @Test void parenthesizedDimensionStillMustMatchTheAdmittedWorldAndExactTarget() {
        String command = "@goto (+500 +80 -950 nether)";
        var sameWorld = new GotoTargetSnapshot(500, 80, -950, "nether", "minecraft:the_nether");
        assertTrue(GotoCommandTargetMatcher.sameDimensionDirectXyz(command, "minecraft:the_nether"));
        assertTrue(GotoCommandTargetMatcher.matches(command, sameWorld));
        assertFalse(GotoCommandTargetMatcher.sameDimensionDirectXyz(command, "minecraft:overworld"));
        assertFalse(GotoCommandTargetMatcher.matches(command,
                new GotoTargetSnapshot(501, 80, -950, "nether", "minecraft:the_nether")));
        assertFalse(GotoCommandTargetMatcher.matches(command,
                new GotoTargetSnapshot(500, 80, -950, null, "minecraft:the_nether")));
    }
}
//#endif
