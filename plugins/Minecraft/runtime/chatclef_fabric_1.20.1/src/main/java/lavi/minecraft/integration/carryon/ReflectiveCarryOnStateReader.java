package lavi.minecraft.integration.carryon;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.ClientPlayerEntity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

//20260730_kpopmodder: Keep Carry On reflection isolated from upstream ChatClef class loading.
public final class ReflectiveCarryOnStateReader {
    private static final String MOD_ID = "carryon";
    private static final String DATA_MANAGER_CLASS = "tschipp.carryon.common.carry.CarryOnDataManager";
    private static final String GET_CARRY_DATA_METHOD = "getCarryData";
    private static final String IS_CARRYING_METHOD = "isCarrying";

    private Method getCarryDataMethod;
    private Method isCarryingMethod;
    private CarryOnResolutionStatus resolutionStatus = CarryOnResolutionStatus.UNRESOLVED;
    private String incompatibleVersion = "unknown";
    private Throwable incompatibleCause;

    public CarryOnObservation observe(ClientPlayerEntity player) {
        FabricProbe probe = probeFabricLoader();
        if (probe.error != null) {
            return CarryOnObservation.failed(probe.version, probe.error);
        }
        if (!probe.loaded) {
            return CarryOnObservation.absent();
        }

        if (player == null) {
            return CarryOnObservation.unreadable(probe.version, null);
        }

        try {
            Resolution resolution = resolve(player, probe.version);
            if (resolution.status == CarryOnResolutionStatus.INCOMPATIBLE) {
                return CarryOnObservation.incompatible(resolution.version, resolution.cause);
            }

            Object data = getCarryDataMethod.invoke(null, player);
            if (data == null) {
                return CarryOnObservation.unreadable(probe.version, null);
            }
            Object carrying = isCarryingMethod.invoke(data);
            if (!(carrying instanceof Boolean)) {
                return CarryOnObservation.unreadable(probe.version, null);
            }
            return CarryOnObservation.observed(probe.version, (Boolean) carrying);
        } catch (IllegalAccessException e) {
            return CarryOnObservation.unreadable(probe.version, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            return CarryOnObservation.failed(probe.version, cause);
        } catch (LinkageError e) {
            return CarryOnObservation.failed(probe.version, e);
        } catch (RuntimeException e) {
            return CarryOnObservation.failed(probe.version, e);
        }
    }

    //20260730_kpopmodder: Keep Carry On reflection cached and typed inside the optional LAVI bridge.
    private Resolution resolve(ClientPlayerEntity player, String version) {
        if (resolutionStatus == CarryOnResolutionStatus.RESOLVED) {
            return Resolution.resolved(version);
        }
        if (resolutionStatus == CarryOnResolutionStatus.INCOMPATIBLE) {
            return Resolution.incompatible(incompatibleVersion, incompatibleCause);
        }

        try {
            Class<?> managerClass = Class.forName(DATA_MANAGER_CLASS, false, ReflectiveCarryOnStateReader.class.getClassLoader());
            getCarryDataMethod = findGetCarryDataMethod(managerClass, player);
            Class<?> dataClass = getCarryDataMethod.getReturnType();
            isCarryingMethod = findIsCarryingMethod(dataClass);
            resolutionStatus = CarryOnResolutionStatus.RESOLVED;
            return Resolution.resolved(version);
        } catch (ClassNotFoundException | NoSuchMethodException | LinkageError | RuntimeException e) {
            resolutionStatus = CarryOnResolutionStatus.INCOMPATIBLE;
            incompatibleVersion = version;
            incompatibleCause = e;
            return Resolution.incompatible(version, e);
        }
    }

    private Method findGetCarryDataMethod(Class<?> managerClass, ClientPlayerEntity player) throws NoSuchMethodException {
        for (Method method : managerClass.getMethods()) {
            if (!GET_CARRY_DATA_METHOD.equals(method.getName()) || method.getParameterCount() != 1) {
                continue;
            }
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (Void.TYPE.equals(method.getReturnType()) || method.getReturnType().isPrimitive()) {
                continue;
            }
            if (method.getParameterTypes()[0].isAssignableFrom(player.getClass())) {
                return method;
            }
        }
        throw new NoSuchMethodException(GET_CARRY_DATA_METHOD);
    }

    private Method findIsCarryingMethod(Class<?> dataClass) throws NoSuchMethodException {
        Method method = dataClass.getMethod(IS_CARRYING_METHOD);
        if (Modifier.isStatic(method.getModifiers())) {
            throw new NoSuchMethodException(IS_CARRYING_METHOD + " must be an instance method");
        }
        if (method.getParameterCount() != 0) {
            throw new NoSuchMethodException(IS_CARRYING_METHOD + " must not require parameters");
        }
        Class<?> returnType = method.getReturnType();
        if (!Boolean.TYPE.equals(returnType) && !Boolean.class.equals(returnType)) {
            throw new NoSuchMethodException(IS_CARRYING_METHOD + " must return boolean");
        }
        return method;
    }

    private FabricProbe probeFabricLoader() {
        try {
            FabricLoader loader = FabricLoader.getInstance();
            boolean loaded = loader.isModLoaded(MOD_ID);
            String version = loaded
                    ? loader.getModContainer(MOD_ID)
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown")
                    : "absent";
            return FabricProbe.available(loaded, version);
        } catch (LinkageError | RuntimeException e) {
            return FabricProbe.failed(e);
        }
    }

    private static final class Resolution {
        private final CarryOnResolutionStatus status;
        private final String version;
        private final Throwable cause;

        private Resolution(CarryOnResolutionStatus status, String version, Throwable cause) {
            this.status = status;
            this.version = version;
            this.cause = cause;
        }

        private static Resolution resolved(String version) {
            return new Resolution(CarryOnResolutionStatus.RESOLVED, version, null);
        }

        private static Resolution incompatible(String version, Throwable cause) {
            return new Resolution(CarryOnResolutionStatus.INCOMPATIBLE, version, cause);
        }
    }

    private static final class FabricProbe {
        private final boolean loaded;
        private final String version;
        private final Throwable error;

        private FabricProbe(boolean loaded, String version, Throwable error) {
            this.loaded = loaded;
            this.version = version;
            this.error = error;
        }

        private static FabricProbe available(boolean loaded, String version) {
            return new FabricProbe(loaded, version, null);
        }

        private static FabricProbe failed(Throwable error) {
            return new FabricProbe(false, "unknown", error);
        }
    }
}
