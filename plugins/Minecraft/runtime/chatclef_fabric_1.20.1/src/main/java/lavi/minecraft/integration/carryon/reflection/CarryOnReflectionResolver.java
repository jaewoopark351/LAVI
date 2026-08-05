package lavi.minecraft.integration.carryon.reflection;

import lavi.minecraft.integration.carryon.CarryOnResolutionStatus;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

//20260731_kpopmodder: Isolate Carry On reflection lookup and validation from state observation.
public final class CarryOnReflectionResolver {
    private static final String DATA_MANAGER_CLASS = "tschipp.carryon.common.carry.CarryOnDataManager";
    private static final String GET_CARRY_DATA_METHOD = "getCarryData";
    private static final String IS_CARRYING_METHOD = "isCarrying";
    private static final String GET_BLOCK_METHOD = "getBlock";
    private static final String GET_BLOCK_STATE_METHOD = "getBlockState";

    private Method getCarryDataMethod;
    private Method isCarryingMethod;
    private Method carriedBlockMethod;
    private Method carriedBlockStateMethod;
    private CarryOnResolutionStatus resolutionStatus = CarryOnResolutionStatus.UNRESOLVED;
    private String incompatibleVersion = "unknown";
    private Throwable incompatibleCause;

    public CarryOnReflectionResolution resolve(ClientPlayerEntity player, String version) {
        if (resolutionStatus == CarryOnResolutionStatus.RESOLVED) {
            return CarryOnReflectionResolution.resolved(
                    version,
                    getCarryDataMethod,
                    isCarryingMethod,
                    carriedBlockMethod,
                    carriedBlockStateMethod
            );
        }
        if (resolutionStatus == CarryOnResolutionStatus.INCOMPATIBLE) {
            return CarryOnReflectionResolution.incompatible(incompatibleVersion, incompatibleCause);
        }

        try {
            Class<?> managerClass = Class.forName(DATA_MANAGER_CLASS, false, CarryOnReflectionResolver.class.getClassLoader());
            getCarryDataMethod = findGetCarryDataMethod(managerClass, player);
            Class<?> dataClass = getCarryDataMethod.getReturnType();
            isCarryingMethod = findIsCarryingMethod(dataClass);
            carriedBlockMethod = findOptionalNoArgMethod(dataClass, GET_BLOCK_METHOD, Block.class);
            carriedBlockStateMethod = findOptionalNoArgMethod(dataClass, GET_BLOCK_STATE_METHOD, BlockState.class);
            resolutionStatus = CarryOnResolutionStatus.RESOLVED;
            return CarryOnReflectionResolution.resolved(
                    version,
                    getCarryDataMethod,
                    isCarryingMethod,
                    carriedBlockMethod,
                    carriedBlockStateMethod
            );
        } catch (ClassNotFoundException | NoSuchMethodException | LinkageError | RuntimeException e) {
            resolutionStatus = CarryOnResolutionStatus.INCOMPATIBLE;
            incompatibleVersion = version;
            incompatibleCause = e;
            return CarryOnReflectionResolution.incompatible(version, e);
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

    private Method findOptionalNoArgMethod(Class<?> dataClass, String methodName, Class<?> expectedReturnType) {
        for (Method method : dataClass.getMethods()) {
            if (!methodName.equals(method.getName()) || method.getParameterCount() != 0) {
                continue;
            }
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!expectedReturnType.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            return method;
        }
        return null;
    }
}
