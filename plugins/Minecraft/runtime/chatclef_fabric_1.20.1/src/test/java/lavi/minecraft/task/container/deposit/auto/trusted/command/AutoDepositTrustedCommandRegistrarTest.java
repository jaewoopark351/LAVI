package lavi.minecraft.task.container.deposit.auto.trusted.command;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandExecutor;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositOpenContainerBindingTracker;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoDepositTrustedCommandRegistrarTest {
    @Test
    void registersTheFourTrustedCommandsExactlyOnceWithDistinctMutableInstances() {
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

            assertEquals(4, executor.allCommands().size());
            assertNotNull(executor.get(AutoDepositTrustedCommandRegistrar.TRUST_COMMAND));
            assertNotNull(executor.get(AutoDepositTrustedCommandRegistrar.UNTRUST_COMMAND));
            assertNotNull(executor.get(AutoDepositTrustedCommandRegistrar.LIST_COMMAND));
            assertNotNull(executor.get(
                    AutoDepositTrustedCommandRegistrar.KOREAN_BULK_TRUST_COMMAND
            ));
            assertNotSame(
                    executor.get(AutoDepositTrustedCommandRegistrar.TRUST_COMMAND),
                    executor.get(AutoDepositTrustedCommandRegistrar.KOREAN_BULK_TRUST_COMMAND)
            );
            Object englishBulkOperation = bulkRegistrationOperation(executor.get(
                    AutoDepositTrustedCommandRegistrar.TRUST_COMMAND
            ));
            Object koreanBulkOperation = bulkRegistrationOperation(executor.get(
                    AutoDepositTrustedCommandRegistrar.KOREAN_BULK_TRUST_COMMAND
            ));
            assertTrue(englishBulkOperation
                    instanceof AutoDepositTrustedBulkRegistrationCommandOperation);
            assertSame(englishBulkOperation, koreanBulkOperation);
            assertTrue(registrar.registered());
            assertTrue(registrar.koreanBulkAliasRegistered());
        } finally {
            if (bindingTracker != null) {
                bindingTracker.stop();
            }
            set(field, previous);
        }
    }

    @Test
    void koreanAliasCollisionPreservesTheThreeEnglishCommandsWithoutFalseReadiness() {
        Field field = commandExecutorField();
        Object previous = get(field);
        AutoDepositOpenContainerBindingTracker bindingTracker = null;
        try {
            AltoClef mod = new AltoClef();
            CommandExecutor executor = new CommandExecutor(mod);
            set(field, executor);
            CollisionCommand collision = new CollisionCommand();
            executor.registerNewCommand(collision);
            bindingTracker = new AutoDepositOpenContainerBindingTracker(mod);
            AutoDepositTrustedCommandRegistrar registrar =
                    new AutoDepositTrustedCommandRegistrar(
                            AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                            bindingTracker
                    );

            registrar.register(mod);

            assertEquals(4, executor.allCommands().size());
            assertNotNull(executor.get(AutoDepositTrustedCommandRegistrar.TRUST_COMMAND));
            assertNotNull(executor.get(AutoDepositTrustedCommandRegistrar.UNTRUST_COMMAND));
            assertNotNull(executor.get(AutoDepositTrustedCommandRegistrar.LIST_COMMAND));
            assertSame(
                    collision,
                    executor.get(AutoDepositTrustedCommandRegistrar.KOREAN_BULK_TRUST_COMMAND)
            );
            assertTrue(registrar.registered());
            assertFalse(registrar.koreanBulkAliasRegistered());
        } finally {
            if (bindingTracker != null) {
                bindingTracker.stop();
            }
            set(field, previous);
        }
    }

    private static final class CollisionCommand extends Command {
        private CollisionCommand() {
            super(
                    AutoDepositTrustedCommandRegistrar.KOREAN_BULK_TRUST_COMMAND,
                    "test collision"
            );
        }

        @Override
        protected void call(AltoClef mod, ArgParser parser) {
            finish();
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

    private static Object bulkRegistrationOperation(Command command) {
        try {
            Field field = command.getClass().getDeclaredField("bulkRegistrationOperation");
            field.setAccessible(true);
            return field.get(command);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new AssertionError(
                    "Failed to read trusted command bulk registration operation",
                    exception
            );
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
