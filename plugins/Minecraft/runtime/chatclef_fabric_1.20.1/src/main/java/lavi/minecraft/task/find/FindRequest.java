//#if MC == 12001
//20260914_kpopmodder: Strict, finite FIND grammar shared by native command and bridge projection.
package lavi.minecraft.task.find;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

public record FindRequest(String kind, String query, String mode) {
    public static final Set<String> KINDS = Set.of("auto", "entity", "block", "item", "player");
    public static final Set<String> MODES = Set.of("approach", "report");

    public FindRequest {
        if (!KINDS.contains(kind) || !MODES.contains(mode) || query == null
                || query.isBlank() || query.codePointCount(0, query.length()) > 128
                || !query.equals(query.trim().replaceAll(" +", " "))
                || query.codePoints().anyMatch(c -> !allowed(c))) {
            throw new IllegalArgumentException("대상 이름과 종류를 확인해 줘.");
        }
        if (kind.equals("player") && !query.matches("[A-Za-z0-9_]{1,16}")) {
            throw new IllegalArgumentException("플레이어 이름은 영문, 숫자, 밑줄로 지정해 줘.");
        }
    }

    private static boolean allowed(int c) {
        int type = Character.getType(c);
        return Character.isLetterOrDigit(c) || type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK || type == Character.ENCLOSING_MARK
                || type == Character.LETTER_NUMBER || type == Character.OTHER_NUMBER
                || " _:/.-".indexOf(c) >= 0;
    }

    public static FindRequest parse(String command) {
        if (command == null || command.length() > 180 || command.codePoints().anyMatch(c ->
                Character.isISOControl(c) || Character.getType(c) == Character.FORMAT
                        || ";#\\\"'".indexOf(c) >= 0)) {
            throw new IllegalArgumentException("FIND에는 명령 구분자나 제어 문자를 넣을 수 없어.");
        }
        var tokens = new ArrayList<>(Arrays.asList(command.trim().split(" +")));
        String name = tokens.remove(0).toLowerCase(Locale.ROOT);
        if (!(name.equals("find") || name.equals("@find")) || tokens.isEmpty()
                || tokens.stream().anyMatch(token -> token.contains("@"))) {
            throw new IllegalArgumentException("사용법: @find [entity|block|item|player] 대상 [approach|report]");
        }
        String kind = KINDS.contains(tokens.get(0).toLowerCase(Locale.ROOT))
                ? tokens.remove(0).toLowerCase(Locale.ROOT) : "auto";
        String mode = !tokens.isEmpty() && MODES.contains(tokens.get(tokens.size() - 1).toLowerCase(Locale.ROOT))
                ? tokens.remove(tokens.size() - 1).toLowerCase(Locale.ROOT) : "approach";
        return new FindRequest(kind, String.join(" ", tokens), mode);
    }

    public String command() { return "find " + kind + " " + query + " " + mode; }
}
//#endif
