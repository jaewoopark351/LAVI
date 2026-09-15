package lavi.minecraft.command.result.instant;

import adris.altoclef.AltoClef;
import net.minecraft.client.MinecraftClient;
import java.util.Map;

//20260915_kpopmodder: Read back existing setting owners after a successful synchronous invocation.
public final class InstantSettingResultReader {
    private InstantSettingResultReader() { }

    public static void observe(String prefixlessCommand) {
        String[] parts = prefixlessCommand.trim().split("\\s+");
        String name = parts[0];
        try {
            AltoClef mod = AltoClef.getInstance();
            switch (name) {
                case "gamma" -> {
                    double requested = parts.length == 1 ? 1.0 : Double.parseDouble(parts[1]);
                    //#if MC >= 11904
                    double applied = MinecraftClient.getInstance().options.getGamma().getValue();
                    //#else
                    //$$ double applied = MinecraftClient.getInstance().options.gamma;
                    //#endif
                    if (!Double.isFinite(requested) || !Double.isFinite(applied)) break;
                    boolean matches = Double.compare(requested, applied) == 0;
                    InstantCommandResultCapture.record(name, matches, matches ? "SETTING_APPLIED" : "VALUE_NOT_APPLIED",
                            Map.of("requested", requested, "value", applied));
                }
                case "chatclef" -> {
                    boolean expected = parts.length > 1 && parts[1].equalsIgnoreCase("on");
                    boolean actual = mod.getAiBridge().getEnabled();
                    InstantCommandResultCapture.record(name, expected == actual, "SETTING_APPLIED", Map.of("enabled", actual));
                }
                case "resetmemory" -> {
                    int stored = mod.getAiBridge().conversationHistory().getListJSON().size();
                    // Native clear deliberately retains the first base-system prompt.
                    int count = Math.max(0, stored - 1);
                    InstantCommandResultCapture.record(name, count == 0, "MEMORY_CLEARED",
                            Map.of("remaining_messages", count, "base_prompt_retained", stored > 0));
                }
                case "reload_settings" -> InstantCommandResultCapture.recordOutcome(name, "unknown", "RELOAD_RETURNED",
                        Map.of("callback_returned", true, "configuration_files_verified", false));
                default -> { }
            }
        } catch (RuntimeException ignored) {
            // A readback gap remains RESULT_UNAVAILABLE and does not affect native execution.
        }
    }
}
