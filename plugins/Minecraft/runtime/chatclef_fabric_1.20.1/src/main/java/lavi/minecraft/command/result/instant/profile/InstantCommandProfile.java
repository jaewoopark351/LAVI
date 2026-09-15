package lavi.minecraft.command.result.instant.profile;

import java.util.Set;

//20260915_kpopmodder: Closed native immediate-command grammar and early-rejection observation policy.
public final class InstantCommandProfile {
    private static final Set<String> IMMEDIATE = Set.of("gamma", "chatclef", "overlay", "reload_settings",
            "resetmemory", "scan", "auto_deposit_trust", "auto_deposit_untrust", "auto_deposit_trusted_list");
    private static final Set<String> EARLY_REJECTION = Set.of("give", "follow");
    private InstantCommandProfile() { }
    public static String name(String prefixlessCommand) {
        String name = prefixlessCommand.trim().split("\\s+", 2)[0];
        return name.equals("\uC790\uB3D9\uBCF4\uAD00\uB4F1\uB85D") ? "auto_deposit_trust" : name;
    }
    public static boolean immediate(String command) { return IMMEDIATE.contains(command); }
    public static boolean observed(String command) { return immediate(command) || EARLY_REJECTION.contains(command); }
}
