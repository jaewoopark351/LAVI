package adris.altoclef.mixins.diagnostics;

import baritone.Baritone;
import baritone.utils.BaritoneProcessHelper;
import lavi.minecraft.diagnostics.baritone.builder.BuilderProcessOwnerView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//20260913_kpopmodder: Bind the passive owner accessor to the class that actually declares the field.
@Mixin(value = BaritoneProcessHelper.class, remap = false)
public interface BaritoneProcessHelperDiagnosticAccessor extends BuilderProcessOwnerView {
    @Override
    @Accessor(value = "baritone", remap = false)
    Baritone lavi$ownerBaritone();
}
