package adris.altoclef.lavibridge.commands;

//20260725_kpopmodder: Added this factory to isolate goto request validation and command creation.

import java.util.Map;

public class LaviGotoCommandFactory {

    public LaviCommandSpec build(Map<String, Object> request) {
        String target = normalizeGotoTarget(request);
        return new LaviCommandSpec("goto", "goto " + target, request);
    }

    private static String normalizeGotoTarget(Map<String, Object> request) {
        Object target = request.get("target");
        if (target instanceof String text && !text.isBlank()) {
            return normalizeGotoTargetText(text);
        }

        String dimension = readOptionalDimension(request.get("dimension"));
        boolean hasX = request.containsKey("x");
        boolean hasY = request.containsKey("y");
        boolean hasZ = request.containsKey("z");

        StringBuilder result = new StringBuilder();
        if (hasX || hasZ) {
            if (!hasX || !hasZ) {
                throw new IllegalArgumentException("goto requires both x and z when either coordinate is provided.");
            }
            result.append(readCoordinate(request.get("x"), "x"));
            result.append(" ");
            if (hasY) {
                result.append(readCoordinate(request.get("y"), "y"));
                result.append(" ");
            }
            result.append(readCoordinate(request.get("z"), "z"));
        } else if (hasY) {
            result.append(readCoordinate(request.get("y"), "y"));
        }

        if (!dimension.isBlank()) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(dimension);
        }

        if (result.length() == 0) {
            throw new IllegalArgumentException("goto requires target, coordinates, or dimension.");
        }
        return result.toString();
    }

    private static String normalizeGotoTargetText(String text) {
        String trimmed = text.trim().replace(",", " ");
        String[] rawParts = trimmed.split("\\s+");
        if (rawParts.length == 0 || rawParts.length > 4) {
            throw new IllegalArgumentException("goto target must be [x z], [x y z], optional dimension, or dimension only.");
        }

        int numberCount = 0;
        boolean sawDimension = false;
        StringBuilder result = new StringBuilder();
        for (String rawPart : rawParts) {
            String part = rawPart.trim().toLowerCase();
            if (part.isBlank()) {
                continue;
            }
            if (part.matches("-?\\d+")) {
                if (sawDimension) {
                    throw new IllegalArgumentException("goto dimension must come after coordinates.");
                }
                numberCount += 1;
                if (numberCount > 3) {
                    throw new IllegalArgumentException("goto target accepts at most three coordinates.");
                }
            } else {
                if (sawDimension) {
                    throw new IllegalArgumentException("goto target accepts only one dimension.");
                }
                part = readOptionalDimension(part);
                sawDimension = true;
            }
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(part);
        }

        if (numberCount == 0 && !sawDimension) {
            throw new IllegalArgumentException("goto target requires coordinates or dimension.");
        }
        return result.toString();
    }

    private static int readCoordinate(Object value, String key) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text.trim());
        }
        throw new IllegalArgumentException(key + " must be an integer.");
    }

    private static String readOptionalDimension(Object value) {
        if (value == null) {
            return "";
        }
        String dimension = String.valueOf(value).trim().toLowerCase();
        if (dimension.isBlank()) {
            return "";
        }
        if (!dimension.equals("overworld") && !dimension.equals("nether") && !dimension.equals("end")) {
            throw new IllegalArgumentException("dimension must be overworld, nether, or end.");
        }
        return dimension;
    }
}
