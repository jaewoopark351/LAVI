package lavi.minecraft.diagnostics.container.gui.tick;

//20260904_kpopmodder: Name only diagnostic client-tick windows without assigning gameplay authority.
public enum ContainerClientTickWindowState {
    UNAVAILABLE,
    ACTIVE_BEFORE_RETURN,
    ACTIVE_BOUNDARY_CLAIMED,
    BETWEEN_TICKS
}
