package lavi.minecraft.diagnostics.baritone.builder;

//20260913_kpopmodder: Read raw path counts without invoking the pre-verification movements getter.
public interface BuilderPathView {
    int lavi$positionsCount();
    int lavi$movementsCount();
    boolean lavi$verified();
}
