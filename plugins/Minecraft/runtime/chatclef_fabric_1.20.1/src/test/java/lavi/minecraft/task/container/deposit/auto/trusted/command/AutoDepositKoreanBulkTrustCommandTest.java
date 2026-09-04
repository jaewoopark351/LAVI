package lavi.minecraft.task.container.deposit.auto.trusted.command;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.CommandException;
import lavi.minecraft.task.container.deposit.auto.trusted.command.form.AutoDepositTrustCommandForm;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
final class AutoDepositKoreanBulkTrustCommandTest {
    @Test
    void invokesTheBulkOperationForOnlyTheApprovedKoreanForms() throws Exception {
        List<AutoDepositTrustCommandForm> forms = new ArrayList<>();
        AtomicInteger completions = new AtomicInteger();
        AutoDepositKoreanBulkTrustCommand command =
                new AutoDepositKoreanBulkTrustCommand(
                        (ignored, form) -> forms.add(form)
                );

        command.run(
                new AltoClef(),
                AutoDepositKoreanBulkTrustCommand.COMMAND_NAME + " \uC601\uC5ED 16x16",
                completions::incrementAndGet
        );
        command.run(
                new AltoClef(),
                AutoDepositKoreanBulkTrustCommand.COMMAND_NAME + " \uBC18\uACBD 16x16",
                completions::incrementAndGet
        );

        assertEquals(
                List.of(
                        AutoDepositTrustCommandForm.KOREAN_AREA,
                        AutoDepositTrustCommandForm.KOREAN_RADIUS
                ),
                forms
        );
        assertEquals(2, completions.get());
    }

    @Test
    void noArgumentAndMixedFormsFailClosedAndCompleteOncePerAttempt() throws Exception {
        AtomicInteger bulkCalls = new AtomicInteger();
        AtomicInteger completions = new AtomicInteger();
        AutoDepositKoreanBulkTrustCommand command =
                new AutoDepositKoreanBulkTrustCommand(
                        (ignored, form) -> bulkCalls.incrementAndGet()
                );

        assertThrows(
                CommandException.class,
                () -> command.run(
                        new AltoClef(),
                        AutoDepositKoreanBulkTrustCommand.COMMAND_NAME,
                        completions::incrementAndGet
                )
        );
        assertThrows(
                CommandException.class,
                () -> command.run(
                        new AltoClef(),
                        AutoDepositKoreanBulkTrustCommand.COMMAND_NAME + " area 16x16",
                        completions::incrementAndGet
                )
        );

        assertEquals(0, bulkCalls.get());
        assertEquals(2, completions.get());
    }
}
