package lavi.minecraft.diagnostics.container.home;

//20260828_kpopmodder: Name the exact STORE_HOME manifest-stale observation boundary.
public enum StoreHomeManifestFailureStage {
    ROOT_MANIFEST_REVALIDATION,
    EXECUTOR_SLOT_RESOLUTION,
    EXECUTOR_PRE_CLICK_SOURCE_VALIDATION,
    EXECUTOR_POST_CLICK_SOURCE_VALIDATION
}
