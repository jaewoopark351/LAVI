package lavi.minecraft.integration.carryon;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.ClientPlayerEntity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

public final class ReflectiveCarryOnStateReader {
    private static final String MOD_ID = "carryon";
    private static final String DATA_MANAGER_CLASS = "tschipp.carryon.common.carry.CarryOnDataManager";
    private static final String GET_CARRY_DATA_METHOD = "getCarryData";
    private static final String IS_CARRYING_METHOD = "isCarrying";

    private Method getCarryDataMethod;
    private Method isCarryingMethod;
    private boolean resolved;

    public CarryOnObservation observe(ClientPlayerEntity player) {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return CarryOnObservation.absent();
        }

        String version = version();
        if (player == null) {
            return CarryOnObservation.unreadable(version, null);
        }

        try {
            resolve(player);
            Object data = getCarryDataMethod.invoke(null, player);
            if (data == null) {
                return CarryOnObservation.unreadable(version, null);
            }
            Object carrying = isCarryingMethod.invoke(data);
            if (!(carrying instanceof Boolean)) {
                return CarryOnObservation.unreadable(version, null);
            }
            return CarryOnObservation.observed(version, (Boolean) carrying);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return CarryOnObservation.incompatible(version, e);
        } catch (IllegalAccessException e) {
            return CarryOnObservation.unreadable(version, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            return CarryOnObservation.failed(version, cause);
        } catch (RuntimeException e) {
            return CarryOnObservation.failed(version, e);
        }
    }

    private void resolve(ClientPlayerEntity player) throws ClassNotFoundException, NoSuchMethodException {
        if (resolved) {
            return;
        }

        Class<?> managerClass = Class.forName(DATA_MANAGER_CLASS, false, ReflectiveCarryOnStateReader.class.getClassLoader());
        getCarryDataMethod = findGetCarryDataMethod(managerClass, player);
        Class<?> dataClass = getCarryDataMethod.getReturnType();
        isCarryingMethod = dataClass.getMethod(IS_CARRYING_METHOD);
        resolved = true;
    }

    private Method findGetCarryDataMethod(Class<?> managerClass, ClientPlayerEntity player) throws NoSuchMethodException {
        for (Method method : managerClass.getMethods()) {
            if (!GET_CARRY_DATA_METHOD.equals(method.getName()) || method.getParameterCount() != 1) {
                continue;
            }
            if (method.getParameterTypes()[0].isAssignableFrom(player.getClass())) {
                return method;
            }
        }
        throw new NoSuchMethodException(GET_CARRY_DATA_METHOD);
    }

    private String version() {
        Optional<String> version = FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString());
        return version.orElse("unknown");
    }
}
