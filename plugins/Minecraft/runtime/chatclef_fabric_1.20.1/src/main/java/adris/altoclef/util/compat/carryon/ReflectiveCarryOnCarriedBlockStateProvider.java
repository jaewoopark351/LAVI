package adris.altoclef.util.compat.carryon;

import adris.altoclef.util.logging.StateChangeLogger;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

//20260727_kpopmodder: Reads Carry On carried-block data via reflection so Carry On remains an optional mod.
public final class ReflectiveCarryOnCarriedBlockStateProvider implements CarriedBlockStateProvider {
    private static final String CARRY_DATA_MANAGER_CLASS = "tschipp.carryon.common.carry.CarryOnDataManager";
    private static final String CARRY_DATA_CLASS = "tschipp.carryon.common.carry.CarryOnData";
    private static final String CARRY_TYPE_CLASS = "tschipp.carryon.common.carry.CarryOnData$CarryType";

    private final Method getCarryData;
    private final Method isCarryingType;
    private final Method isCarryingAnything;
    private final Method getBlock;
    private final Object blockCarryType;
    private final Object entityCarryType;
    private final Object playerCarryType;
    private final CarryOnStateChangeReporter stateReporter = new CarryOnStateChangeReporter();
    private final StateChangeLogger debugLogger = new StateChangeLogger("CarryOnCompat.Reflection");

    public static Optional<CarriedBlockStateProvider> create() {
        try {
            return Optional.of(new ReflectiveCarryOnCarriedBlockStateProvider());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            return Optional.empty();
        }
    }

    private ReflectiveCarryOnCarriedBlockStateProvider() throws ReflectiveOperationException {
        Class<?> dataManagerClass = Class.forName(CARRY_DATA_MANAGER_CLASS);
        Class<?> dataClass = Class.forName(CARRY_DATA_CLASS);
        Class<?> carryTypeClass = Class.forName(CARRY_TYPE_CLASS);

        getCarryData = dataManagerClass.getMethod("getCarryData", PlayerEntity.class);
        isCarryingAnything = dataClass.getMethod("isCarrying");
        isCarryingType = dataClass.getMethod("isCarrying", carryTypeClass);
        getBlock = dataClass.getMethod("getBlock");

        blockCarryType = enumConstant(carryTypeClass, "BLOCK");
        entityCarryType = enumConstant(carryTypeClass, "ENTITY");
        playerCarryType = enumConstant(carryTypeClass, "PLAYER");
    }

    @Override
    public boolean isCarryingBlock(PlayerEntity player) {
        return getCarriedBlockState(player).isPresent();
    }

    @Override
    public Optional<BlockState> getCarriedBlockState(PlayerEntity player) {
        CarryOnState state = readState(player);
        return state.blockState();
    }

    @Override
    public Optional<Identifier> getCarriedBlockId(PlayerEntity player) {
        return getCarriedBlockState(player)
                .map(BlockState::getBlock)
                .map(Registries.BLOCK::getId);
    }

    @Override
    public boolean isCarryingBlock(PlayerEntity player, Block expectedBlock) {
        if (expectedBlock == null) {
            return false;
        }
        return getCarriedBlockState(player)
                .map(BlockState::getBlock)
                .map(expectedBlock::equals)
                .orElse(false);
    }

    private CarryOnState readState(PlayerEntity player) {
        if (player == null) {
            return CarryOnState.none();
        }
        try {
            Object carryData = getCarryData.invoke(null, player);
            if (carryData == null) {
                stateReporter.report(player, "NONE");
                return CarryOnState.none();
            }
            if (isCarrying(carryData, blockCarryType)) {
                Object blockState = getBlock.invoke(carryData);
                if (blockState instanceof BlockState state) {
                    Identifier id = Registries.BLOCK.getId(state.getBlock());
                    stateReporter.report(player, "BLOCK " + id);
                    return CarryOnState.block(state);
                }
                stateReporter.report(player, "BLOCK unknown");
                debugLogger.state("carry-on block state invalid",
                        "Carry On block state lookup returned non-BlockState: "
                                + (blockState == null ? "null" : blockState.getClass().getName()));
                return CarryOnState.none();
            }
            String carryType = nonBlockCarryType(carryData);
            stateReporter.report(player, carryType);
            return CarryOnState.none();
        } catch (IllegalStateException error) {
            debugLogger.state("carry-on invalid state " + error.getClass().getSimpleName(),
                    "Carry On state lookup failed safely: " + error.getClass().getSimpleName()
                            + ": " + error.getMessage());
            stateReporter.report(player, "NONE");
            return CarryOnState.none();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            debugLogger.state("carry-on reflection failure " + error.getClass().getSimpleName(),
                    "Carry On reflection lookup failed safely: " + error.getClass().getSimpleName()
                            + ": " + error.getMessage());
            stateReporter.report(player, "NONE");
            return CarryOnState.none();
        }
    }

    private boolean isCarrying(Object carryData, Object carryType) throws ReflectiveOperationException {
        try {
            return Boolean.TRUE.equals(isCarryingType.invoke(carryData, carryType));
        } catch (InvocationTargetException error) {
            throw unwrapInvocation(error);
        }
    }

    private String nonBlockCarryType(Object carryData) throws ReflectiveOperationException {
        if (isCarrying(carryData, entityCarryType)) {
            return "ENTITY";
        }
        if (isCarrying(carryData, playerCarryType)) {
            return "PLAYER";
        }
        try {
            if (Boolean.TRUE.equals(isCarryingAnything.invoke(carryData))) {
                return "UNKNOWN";
            }
        } catch (InvocationTargetException error) {
            throw unwrapInvocation(error);
        }
        return "NONE";
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object enumConstant(Class<?> enumClass, String name) {
        return Enum.valueOf((Class<? extends Enum>) enumClass.asSubclass(Enum.class), name);
    }

    private ReflectiveOperationException unwrapInvocation(InvocationTargetException error) throws ReflectiveOperationException {
        Throwable cause = error.getCause();
        if (cause instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (cause instanceof Error fatalError) {
            throw fatalError;
        }
        return error;
    }

    private record CarryOnState(Optional<BlockState> blockState) {
        private static CarryOnState none() {
            return new CarryOnState(Optional.empty());
        }

        private static CarryOnState block(BlockState blockState) {
            return new CarryOnState(Optional.of(blockState));
        }
    }
}
