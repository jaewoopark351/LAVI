package lavi.minecraft.integration.carryon;

import net.minecraft.client.network.ClientPlayerEntity;

//20260731_kpopmodder: Expose Carry On state reading through a narrow optional boundary.
public interface CarryOnStateReader {
    CarryOnObservation observe(ClientPlayerEntity player);
}
