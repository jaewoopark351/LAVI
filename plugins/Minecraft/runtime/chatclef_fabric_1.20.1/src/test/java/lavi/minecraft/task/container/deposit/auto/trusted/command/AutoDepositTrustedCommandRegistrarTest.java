package lavi.minecraft.task.container.deposit.auto.trusted.command;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.CommandExecutor;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositOpenContainerBindingTracker;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AutoDepositTrustedCommandRegistrarTest {
    @Test
    void registersTheThreeTrustedCommandsExactlyOnce() {
        Field field = commandExecutorField();
        Object previous = get(field);
        AutoDepositOpenContainerBindingTracker bindingTracker = null;
        try {
            AltoClef mod = new AltoClef();
            CommandExecutor executor = new CommandExecutor(mod);
            set(field, executor);
            bindingTracker = new AutoDepositOpenContainerBindingTracker(mod);
            AutoDepositTrustedCommandRegistrar registrar =
                    new AutoDepositTrustedCommandRegistrar(
                            AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                            bindingTracker
                    );

            registrar.register(mod);
            registrar.register(mod);

            assertEquals(3, executor.allCommands().size());
            assertNotNull(executor.get(AutoDepositTrustedCommandRegistrar.TRUST_COMMAND));
            assertNotNull(executor.get(AutoDepositTrustedCommandRegistrar.UNTRUST_COMMAND));
            assertNotNull(executor.get(AutoDepositTrustedCommandRegistrar.LIST_COMMAND));
        } finally {
            if (bindingTracker != null) {
                bindingTracker.stop();
            }
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
