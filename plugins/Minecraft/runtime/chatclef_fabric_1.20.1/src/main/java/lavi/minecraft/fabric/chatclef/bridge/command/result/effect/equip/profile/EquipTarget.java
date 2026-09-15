package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.profile;

import java.util.List;

//20260915_kpopmodder: Preserve a native target's quantity and complete immutable alternative set.
public record EquipTarget(int index, String nativeTarget, int requestedCount, List<EquipTargetMatch> matches) {
    public EquipTarget { matches = List.copyOf(matches); }
}
