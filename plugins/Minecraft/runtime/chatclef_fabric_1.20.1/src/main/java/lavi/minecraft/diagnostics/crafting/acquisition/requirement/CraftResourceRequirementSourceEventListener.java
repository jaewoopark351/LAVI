package lavi.minecraft.diagnostics.crafting.acquisition.requirement;

//20260901_kpopmodder: Forward one existing source event without depending on a backend bridge.
@FunctionalInterface
public interface CraftResourceRequirementSourceEventListener {
    void onVisibleTaskReturn(Object sourceTask);
}
