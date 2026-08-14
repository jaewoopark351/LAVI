package lavi.minecraft.integration.carryon.container;

//20260815_kpopmodder: Name bounded container-open postcondition outcomes without deciding task behavior.
enum CarryOnContainerPostconditionOutcome {
    OBSERVATION_PENDING,
    GUI_OPENED,
    GUI_OPEN_DELAYED,
    CARRY_ON_PICKUP_CONFIRMED,
    CARRY_ON_PICKUP_STRONGLY_ATTRIBUTED,
    TARGET_REMOVED_WITHOUT_GUI,
    NO_GUI_TARGET_STILL_PRESENT,
    OBSERVATION_WINDOW_EXPIRED
}
