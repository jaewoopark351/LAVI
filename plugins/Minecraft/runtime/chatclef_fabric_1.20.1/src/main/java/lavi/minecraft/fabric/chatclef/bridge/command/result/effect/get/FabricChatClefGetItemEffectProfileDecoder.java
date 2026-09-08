package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get;

//20260907_kpopmodder: Decode only the registered GetCommand's single-target grammar.
final class FabricChatClefGetItemEffectProfileDecoder {
    private static final int MAX_TARGET_NAME_LENGTH = 256;
    private static final String LEGACY_EXACT_COMMAND = "get diamond_pickaxe 1";
    private static final FabricChatClefGetItemCatalogueResolver LIVE_CATALOGUE =
            new FabricChatClefTaskCatalogueTargetResolver();

    private FabricChatClefGetItemEffectProfileDecoder() {
    }

    static FabricChatClefGetItemEffectProfile decode(String command) {
        return decode(command, LIVE_CATALOGUE);
    }

    static FabricChatClefGetItemEffectProfile decode(
            String command,
            FabricChatClefGetItemCatalogueResolver catalogueResolver
    ) {
        if (command == null
                || catalogueResolver == null
                || containsUnsupportedWhitespaceOrControl(command)) {
            return FabricChatClefGetItemEffectProfile.untracked();
        }
        String normalized = normalizeExecutionCommand(command);
        if (normalized.isEmpty()) {
            return FabricChatClefGetItemEffectProfile.untracked();
        }
        String[] tokens = normalized.split(" ");
        if ((tokens.length != 2 && tokens.length != 3) || !"get".equals(tokens[0])) {
            return FabricChatClefGetItemEffectProfile.untracked();
        }
        String target = tokens[1];
        if (target.isEmpty() || target.length() > MAX_TARGET_NAME_LENGTH) {
            return FabricChatClefGetItemEffectProfile.untracked();
        }
        Integer requestedCount = tokens.length == 2
                ? Integer.valueOf(1)
                : positiveCount(tokens[2]);
        if (requestedCount == null) {
            return FabricChatClefGetItemEffectProfile.untracked();
        }
        FabricChatClefGetItemCatalogueResolution resolution =
                catalogueResolver.resolve(target);
        if (resolution == null || !resolution.resolved()) {
            return FabricChatClefGetItemEffectProfile.untracked();
        }
        return FabricChatClefGetItemEffectProfile.tracked(
                target,
                requestedCount,
                resolution.targetMatchIds(),
                LEGACY_EXACT_COMMAND.equals(normalized)
        );
    }

    private static Integer positiveCount(String value) {
        try {
            int count = Integer.parseInt(value);
            return count > 0 ? count : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String normalizeExecutionCommand(String command) {
        String normalized = command.trim().replaceAll(" +", " ");
        if (normalized.startsWith("@")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static boolean containsUnsupportedWhitespaceOrControl(String command) {
        for (int offset = 0; offset < command.length(); ) {
            int codePoint = command.codePointAt(offset);
            if (codePoint != ' '
                    && (Character.isWhitespace(codePoint)
                    || Character.isSpaceChar(codePoint)
                    || Character.isISOControl(codePoint))) {
                return true;
            }
            offset += Character.charCount(codePoint);
        }
        return false;
    }
}
