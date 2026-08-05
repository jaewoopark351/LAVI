package lavi.minecraft.integration.carryon;

import lavi.minecraft.integration.carryon.reflection.CarryOnModProbe;
import lavi.minecraft.integration.carryon.reflection.CarryOnModStatus;
import lavi.minecraft.integration.carryon.reflection.CarryOnCarriedBlockIdentityReader;
import lavi.minecraft.integration.carryon.reflection.CarryOnReflectionResolution;
import lavi.minecraft.integration.carryon.reflection.CarryOnReflectionResolver;
import net.minecraft.client.network.ClientPlayerEntity;

import java.lang.reflect.InvocationTargetException;

//20260730_kpopmodder: Keep Carry On reflection isolated from upstream ChatClef class loading.
public final class ReflectiveCarryOnStateReader implements CarryOnStateReader {
    private final CarryOnModProbe modProbe = new CarryOnModProbe();
    private final CarryOnReflectionResolver reflectionResolver = new CarryOnReflectionResolver();

    @Override
    public CarryOnObservation observe(ClientPlayerEntity player) {
        CarryOnModStatus probe = modProbe.probe();
        if (probe.error() != null) {
            return CarryOnObservation.failed(probe.version(), probe.error());
        }
        if (!probe.loaded()) {
            return CarryOnObservation.absent();
        }

        if (player == null) {
            return CarryOnObservation.unreadable(probe.version(), null);
        }

        try {
            CarryOnReflectionResolution resolution = reflectionResolver.resolve(player, probe.version());
            if (resolution.status() == CarryOnResolutionStatus.INCOMPATIBLE) {
                return CarryOnObservation.incompatible(resolution.version(), resolution.cause());
            }

            Object data = resolution.getCarryDataMethod().invoke(null, player);
            if (data == null) {
                return CarryOnObservation.unreadable(probe.version(), null);
            }
            Object carrying = resolution.isCarryingMethod().invoke(data);
            if (!(carrying instanceof Boolean)) {
                return CarryOnObservation.unreadable(probe.version(), null);
            }
            CarryOnCarriedBlockIdentity carriedBlockIdentity = (Boolean) carrying
                    ? CarryOnCarriedBlockIdentityReader.read(data, resolution)
                    : CarryOnCarriedBlockIdentity.notCarrying();
            return CarryOnObservation.observed(probe.version(), (Boolean) carrying, carriedBlockIdentity);
        } catch (IllegalAccessException e) {
            return CarryOnObservation.unreadable(probe.version(), e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            return CarryOnObservation.failed(probe.version(), cause);
        } catch (LinkageError e) {
            return CarryOnObservation.failed(probe.version(), e);
        } catch (RuntimeException e) {
            return CarryOnObservation.failed(probe.version(), e);
        }
    }
}
