package lavi.minecraft.task.container.deposit.auto.trusted.command.form;

import adris.altoclef.commandsystem.ArgParser;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
final class AutoDepositKoreanBulkTrustCommandFormParserTest {
    private static final String COMMAND = "\uC790\uB3D9\uBCF4\uAD00\uB4F1\uB85D";

    private final AutoDepositKoreanBulkTrustCommandFormParser parser =
            new AutoDepositKoreanBulkTrustCommandFormParser();

    @Test
    void acceptsOnlyTheTwoExactKoreanBatchForms() {
        assertEquals(
                Optional.of(AutoDepositTrustCommandForm.KOREAN_AREA),
                parse(COMMAND + " \uC601\uC5ED 16x16")
        );
        assertEquals(
                Optional.of(AutoDepositTrustCommandForm.KOREAN_RADIUS),
                parse(COMMAND + " \uBC18\uACBD 16x16")
        );
    }

    @Test
    void rejectsNoArgumentMixedUnsupportedAndTrailingForms() {
        String[] invalid = {
                COMMAND,
                COMMAND + " area 16x16",
                COMMAND + " \uC601\uC5ED",
                COMMAND + " \uC601\uC5ED 8x8",
                COMMAND + " \uBC18\uACBD 16x16 extra"
        };

        for (String command : invalid) {
            assertTrue(parse(command).isEmpty(), command);
        }
    }

    private Optional<AutoDepositTrustCommandForm> parse(String command) {
        ArgParser upstreamParser = new ArgParser();
        upstreamParser.loadArgs(command, true);
        return parser.parse(upstreamParser);
    }
}
