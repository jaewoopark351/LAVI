package lavi.minecraft.integration.carryon.reflection;

import lavi.minecraft.integration.carryon.CarryOnResolutionStatus;

import java.lang.reflect.Method;

//20260731_kpopmodder: Store validated Carry On reflection handles and incompatibility reasons together.
public record CarryOnReflectionResolution(CarryOnResolutionStatus status,
                                          String version,
                                          Throwable cause,
                                          Method getCarryDataMethod,
                                          Method isCarryingMethod) {
    static CarryOnReflectionResolution resolved(String version, Method getCarryDataMethod, Method isCarryingMethod) {
        return new CarryOnReflectionResolution(CarryOnResolutionStatus.RESOLVED, version, null, getCarryDataMethod, isCarryingMethod);
    }

    static CarryOnReflectionResolution incompatible(String version, Throwable cause) {
        return new CarryOnReflectionResolution(CarryOnResolutionStatus.INCOMPATIBLE, version, cause, null, null);
    }
}
