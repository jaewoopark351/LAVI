package lavi.minecraft.integration.carryon;

//20260730_kpopmodder: Define optional Carry On observation states in the LAVI-owned integration layer.
public enum CarryOnCarryState {
    ABSENT,
    AVAILABLE_NOT_CARRYING,
    AVAILABLE_CARRYING,
    INCOMPATIBLE,
    STATE_UNREADABLE,
    OBSERVATION_FAILED
}
