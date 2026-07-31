package lavi.minecraft.diagnostics.postplace;

import net.minecraft.client.network.ClientPlayerEntity;

//20260731_kpopmodder: Provide a generic post-place container observer hook without a Carry On dependency.
public interface PostPlaceContainerInteractionObserver {
    default void beforeInteract(PostPlaceContainerOpenIntent intent) {
    }

    default void afterInteract(PostPlaceContainerOpenIntent intent, ClientPlayerEntity player, Object interactResult) {
    }
}
