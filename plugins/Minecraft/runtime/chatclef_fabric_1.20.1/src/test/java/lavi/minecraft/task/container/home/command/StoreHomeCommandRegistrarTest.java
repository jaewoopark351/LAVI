package lavi.minecraft.task.container.home.command;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.CommandExecutor;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

//20260827_kpopmodder: Added focused tests for canonical manual command registration.
class StoreHomeCommandRegistrarTest {
    @Test
    void registersCanonicalManualCommandExactlyOnce() {
        Field field = commandExecutorField();
        Object previous = get(field);
        try {
            AltoClef mod = new AltoClef();
            CommandExecutor executor = new CommandExecutor(mod);
            set(field, executor);
            StoreHomeTaskFactory factory = new StoreHomeTaskFactory(
                    AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                    AutoDepositExactOpenContainerBinding.UNAVAILABLE
            );
            StoreHomeCommandRegistrar registrar = new StoreHomeCommandRegistrar(factory);

            registrar.register(mod);
            registrar.register(mod);

            assertEquals(1, executor.allCommands().size());
            assertNotNull(executor.get(StoreHomeCommand.COMMAND_NAME));
        } finally {
            set(field, previous);
        }
    }

    private static Field commandExecutorField() {
        try {
            Field field = AltoClef.class.getDeclaredField("commandExecutor");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException exception) {
            throw new AssertionError("Missing AltoClef command executor field", exception);
        }
    }

    private static Object get(Field field) {
        try {
            return field.get(null);
        } catch (IllegalAccessException exception) {
            throw new AssertionError("Failed to read AltoClef command executor", exception);
        }
    }

    private static void set(Field field, Object value) {
        try {
            field.set(null, value);
        } catch (IllegalAccessException exception) {
            throw new AssertionError("Failed to write AltoClef command executor", exception);
        }
    }
}
