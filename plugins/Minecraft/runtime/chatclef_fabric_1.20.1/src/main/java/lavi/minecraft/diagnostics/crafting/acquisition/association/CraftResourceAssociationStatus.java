package lavi.minecraft.diagnostics.crafting.acquisition.association;

/**
 * Fail-closed ownership classification for one resource-acquisition observation.
 */
public enum CraftResourceAssociationStatus {
    COMMAND_ROOT_DESCENDANT,
    CONCURRENT_CHAIN_UNOWNED,
    UNKNOWN
}
