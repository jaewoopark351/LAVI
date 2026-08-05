package lavi.minecraft.integration.carryon.reflection;

import lavi.minecraft.integration.carryon.CarryOnResolutionStatus;

import java.lang.reflect.Method;

//20260731_kpopmodder: Store validated Carry On reflection handles and incompatibility reasons together.
public record CarryOnReflectionResolution(CarryOnResolutionStatus status,
                                          String version,
                                          Throwable cause,
                                          Method getCarryDataMethod,
                                          Method isCarryingMethod,
                                          Method carriedBlockMethod,
                                          Method carriedBlockStateMethod) {
    static CarryOnReflectionResolution resolved(String version,
                                                Method getCarryDataMethod,
                                                Method isCarryingMethod,
                                                Method carriedBlockMethod,
                                                Method carriedBlockStateMethod) {
        return new CarryOnReflectionResolution(
                CarryOnResolutionStatus.RESOLVED,
                version,
                null,
                getCarryDataMethod,
                isCarryingMethod,
                carriedBlockMethod,
                carriedBlockStateMethod
        );
    }

    static CarryOnReflectionResolution incompatible(String version, Throwable cause) {
        return new CarryOnReflectionResolution(CarryOnResolutionStatus.INCOMPATIBLE, version, cause, null, null, null, null);
    }
}
