package lavi.minecraft.diagnostics.baritone.builder;

import baritone.behavior.PathingBehavior;

//20260913_kpopmodder: Expose only the executor's existing diagnostic owner.
public interface BuilderExecutorView {
    PathingBehavior lavi$pathingBehavior();
}
