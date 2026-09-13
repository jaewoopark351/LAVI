package lavi.minecraft.diagnostics.baritone.builder;

import baritone.Baritone;

//20260913_kpopmodder: Expose only the existing process owner to passive observers.
public interface BuilderProcessOwnerView {
    Baritone lavi$ownerBaritone();
}
