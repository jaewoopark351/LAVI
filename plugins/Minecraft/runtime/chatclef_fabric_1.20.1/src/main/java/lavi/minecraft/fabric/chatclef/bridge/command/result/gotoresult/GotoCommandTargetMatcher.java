//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.command.result.gotoresult;

import lavi.minecraft.task.movement.gotoresult.model.GotoTargetSnapshot;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//20260913_kpopmodder: Admission remains owned by the command parser; projection accepts only its exact XYZ subset.
final class GotoCommandTargetMatcher {
    private static final Pattern XYZ = Pattern.compile("^([+-]?[0-9]+) ([+-]?[0-9]+) ([+-]?[0-9]+)(?: (overworld|nether|end))?$", Pattern.CASE_INSENSITIVE);
    private GotoCommandTargetMatcher() { }

    static boolean sameDimensionDirectXyz(String command, String worldDimension) {
        if (command == null || worldDimension == null) return false;
        Matcher parsed = parse(command);
        if (!parsed.matches()) return false;
        try {
            return matches(command, new GotoTargetSnapshot(Integer.parseInt(parsed.group(1)),
                    Integer.parseInt(parsed.group(2)), Integer.parseInt(parsed.group(3)),
                    parsed.group(4) == null ? null : parsed.group(4).toLowerCase(java.util.Locale.ROOT), worldDimension));
        } catch (NumberFormatException error) { return false; }
    }

    static boolean matches(String command, GotoTargetSnapshot target) {
        if (command == null || target == null || target.worldDimension() == null) return false;
        Matcher parsed = parse(command);
        if (!parsed.matches()) return false;
        try {
            String dimension = parsed.group(4);
            return Integer.parseInt(parsed.group(1)) == target.x()
                    && Integer.parseInt(parsed.group(2)) == target.y()
                    && Integer.parseInt(parsed.group(3)) == target.z()
                    && (dimension == null ? target.requestedDimension() == null
                        : dimension.equalsIgnoreCase(target.requestedDimension()))
                    && (target.requestedDimension() == null || Objects.equals(target.worldDimension(),
                        "minecraft:" + ("end".equals(target.requestedDimension()) ? "the_end"
                        : "nether".equals(target.requestedDimension()) ? "the_nether" : "overworld")));
        } catch (NumberFormatException error) {
            return false;
        }
    }

    /** Accept the parser's optional single outer pair, while keeping the entire XYZ body closed. */
    private static Matcher parse(String command) {
        String text = command.trim();
        String remainder;
        if (text.regionMatches(true, 0, "@goto ", 0, 6)) {
            remainder = text.substring(6);
        } else if (text.regionMatches(true, 0, "goto ", 0, 5)) {
            remainder = text.substring(5);
        } else {
            return XYZ.matcher("");
        }
        if (remainder.startsWith("(") && remainder.endsWith(")")) {
            remainder = remainder.substring(1, remainder.length() - 1);
        }
        return XYZ.matcher(remainder);
    }
}
//#endif
