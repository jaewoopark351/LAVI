package lavi.minecraft.task.container.deposit.auto.trusted.command.form;

import adris.altoclef.commandsystem.ArgParser;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
//20260904_kpopmodder: Parse the Korean batch-only alias without English or single-form fallback.
public final class AutoDepositKoreanBulkTrustCommandFormParser {
    public Optional<AutoDepositTrustCommandForm> parse(ArgParser parser) {
        Objects.requireNonNull(parser, "parser");
        String[] exposedUnits = parser.getArgUnits();
        String[] units = exposedUnits == null
                ? new String[0]
                : Arrays.copyOf(exposedUnits, exposedUnits.length);
        if (units.length != 2 || !"16x16".equals(units[1])) {
            return Optional.empty();
        }
        if ("\uC601\uC5ED".equals(units[0])) {
            return Optional.of(AutoDepositTrustCommandForm.KOREAN_AREA);
        }
        if ("\uBC18\uACBD".equals(units[0])) {
            return Optional.of(AutoDepositTrustCommandForm.KOREAN_RADIUS);
        }
        return Optional.empty();
    }
}
