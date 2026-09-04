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
final class AutoDepositTrustCommandTest {
    @Test
    void noArgumentFormInvokesOnlyTheExistingSingleOperation() throws Exception {
        AtomicInteger singleCalls = new AtomicInteger();
        List<AutoDepositTrustCommandForm> bulkForms = new ArrayList<>();
        AtomicInteger completions = new AtomicInteger();
        AutoDepositTrustCommand command = new AutoDepositTrustCommand(
                ignored -> singleCalls.incrementAndGet(),
                (ignored, form) -> bulkForms.add(form)
        );

        command.run(new AltoClef(), "auto_deposit_trust", completions::incrementAndGet);

        assertEquals(1, singleCalls.get());
        assertEquals(List.of(), bulkForms);
        assertEquals(1, completions.get());
    }

    @Test
    void approvedEnglishBatchFormsInvokeOnlyTheBulkOperation() throws Exception {
        List<AutoDepositTrustCommandForm> bulkForms = new ArrayList<>();
        AtomicInteger singleCalls = new AtomicInteger();
        AtomicInteger completions = new AtomicInteger();
        AutoDepositTrustCommand command = new AutoDepositTrustCommand(
                ignored -> singleCalls.incrementAndGet(),
                (ignored, form) -> bulkForms.add(form)
        );

        command.run(
                new AltoClef(),
                "auto_deposit_trust area 16x16",
                completions::incrementAndGet
        );
        command.run(
                new AltoClef(),
                "auto_deposit_trust \uBC18\uACBD 16x16",
                completions::incrementAndGet
        );

        assertEquals(0, singleCalls.get());
        assertEquals(
                List.of(
                        AutoDepositTrustCommandForm.ENGLISH_AREA,
                        AutoDepositTrustCommandForm.ENGLISH_RADIUS
                ),
                bulkForms
        );
        assertEquals(2, completions.get());
    }

    @Test
    void malformedFormFailsClosedAndStillCompletesExactlyOnce() throws Exception {
        AtomicInteger singleCalls = new AtomicInteger();
        AtomicInteger bulkCalls = new AtomicInteger();
        AtomicInteger completions = new AtomicInteger();
        AutoDepositTrustCommand command = new AutoDepositTrustCommand(
                ignored -> singleCalls.incrementAndGet(),
                (ignored, form) -> bulkCalls.incrementAndGet()
        );

        assertThrows(
                CommandException.class,
                () -> command.run(
                        new AltoClef(),
                        "auto_deposit_trust area 16x16 extra",
                        completions::incrementAndGet
                )
        );

        assertEquals(0, singleCalls.get());
        assertEquals(0, bulkCalls.get());
        assertEquals(1, completions.get());
    }

    @Test
    void operationFailureStillCompletesExactlyOnce() throws Exception {
        AtomicInteger completions = new AtomicInteger();
        AutoDepositTrustCommand command = new AutoDepositTrustCommand(
                ignored -> {
                    throw new IllegalStateException("expected failure");
                },
                (ignored, form) -> { }
        );

        assertThrows(
                IllegalStateException.class,
                () -> command.run(
                        new AltoClef(),
                        "auto_deposit_trust",
                        completions::incrementAndGet
                )
        );

        assertEquals(1, completions.get());
    }
}
