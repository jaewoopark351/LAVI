package lavi.minecraft.diagnostics.container.gui.tick;

//20260904_kpopmodder: Carry one immutable observation of the diagnostic client-tick window.
public record ContainerClientTickWindowSnapshot(
        ContainerClientTickWindowState state,
        boolean activeClientTickSerialPresent,
        long activeClientTickSerial,
        long lastIssuedClientTickSerial,
        long lastPublishedClientTickBoundarySerial) {
}
