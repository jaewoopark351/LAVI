package lavi.minecraft.integration.carryon.reflection;

import lavi.minecraft.integration.carryon.CarryOnCarriedBlockIdentity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

//20260805_kpopmodder: Read optional carried-block identity through side-effect-free getters only.
public final class CarryOnCarriedBlockIdentityReader {
    private CarryOnCarriedBlockIdentityReader() {
    }

    public static CarryOnCarriedBlockIdentity read(Object carryData, CarryOnReflectionResolution resolution) {
        if (carryData == null || resolution == null) {
            return CarryOnCarriedBlockIdentity.unavailable();
        }
        try {
            CarryOnCarriedBlockIdentity fromState = readBlockState(carryData, resolution.carriedBlockStateMethod());
            if (!"unavailable".equals(fromState.blockState())) {
                return fromState;
            }
            return readBlock(carryData, resolution.carriedBlockMethod());
        } catch (RuntimeException | LinkageError error) {
            return CarryOnCarriedBlockIdentity.unavailable(error);
        }
    }

    private static CarryOnCarriedBlockIdentity readBlockState(Object carryData, Method method) {
        if (method == null) {
            return CarryOnCarriedBlockIdentity.unavailable();
        }
        try {
            Object value = method.invoke(carryData);
            if (!(value instanceof BlockState blockState)) {
                return CarryOnCarriedBlockIdentity.unavailable();
            }
            Block block = blockState.getBlock();
            return CarryOnCarriedBlockIdentity.observed(
                    block.getTranslationKey(),
                    String.valueOf(block),
                    String.valueOf(blockState)
            );
        } catch (IllegalAccessException | InvocationTargetException error) {
            return CarryOnCarriedBlockIdentity.unavailable(error);
        }
    }

    private static CarryOnCarriedBlockIdentity readBlock(Object carryData, Method method) {
        if (method == null) {
            return CarryOnCarriedBlockIdentity.unavailable();
        }
        try {
            Object value = method.invoke(carryData);
            if (!(value instanceof Block block)) {
                return CarryOnCarriedBlockIdentity.unavailable();
            }
            return CarryOnCarriedBlockIdentity.observed(
                    block.getTranslationKey(),
                    String.valueOf(block),
                    "unavailable"
            );
        } catch (IllegalAccessException | InvocationTargetException error) {
            return CarryOnCarriedBlockIdentity.unavailable(error);
        }
    }
}
