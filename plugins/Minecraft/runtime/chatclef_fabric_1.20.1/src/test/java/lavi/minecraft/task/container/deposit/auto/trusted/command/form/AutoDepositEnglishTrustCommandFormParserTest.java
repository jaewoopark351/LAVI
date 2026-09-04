package lavi.minecraft.task.container.deposit.auto.trusted.command.form;

import adris.altoclef.commandsystem.ArgParser;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
final class AutoDepositEnglishTrustCommandFormParserTest {
    private final AutoDepositEnglishTrustCommandFormParser parser =
            new AutoDepositEnglishTrustCommandFormParser();

    @Test
    void acceptsOnlyTheThreeExactEnglishForms() {
        assertEquals(
                Optional.of(AutoDepositTrustCommandForm.ENGLISH_SINGLE),
                parse("auto_deposit_trust")
        );
        assertEquals(
                Optional.of(AutoDepositTrustCommandForm.ENGLISH_AREA),
                parse("auto_deposit_trust area 16x16")
        );
        assertEquals(
                Optional.of(AutoDepositTrustCommandForm.ENGLISH_RADIUS),
                parse("auto_deposit_trust \uBC18\uACBD 16x16")
        );
    }

    @Test
    void rejectsMixedUnsupportedIncompleteAndTrailingForms() {
        String[] invalid = {
                "auto_deposit_trust \uC601\uC5ED 16x16",
                "auto_deposit_trust area",
                "auto_deposit_trust area 8x8",
                "auto_deposit_trust area 16x16 extra",
                "auto_deposit_trust radius 16x16"
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
