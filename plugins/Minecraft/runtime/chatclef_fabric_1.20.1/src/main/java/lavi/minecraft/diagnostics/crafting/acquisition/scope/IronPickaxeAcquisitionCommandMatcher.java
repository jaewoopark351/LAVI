package lavi.minecraft.diagnostics.crafting.acquisition.scope;

//20260901_kpopmodder: Match only the incident command without broadening command semantics.
public final class IronPickaxeAcquisitionCommandMatcher {
    private IronPickaxeAcquisitionCommandMatcher() {
    }

    public static boolean matches(String command) {
        if (command == null) {
            return false;
        }
        String normalized = command.trim();
        if (normalized.startsWith("@")) {
            normalized = normalized.substring(1).trim();
        }
        return "get iron_pickaxe 1".equals(normalized);
    }
}
