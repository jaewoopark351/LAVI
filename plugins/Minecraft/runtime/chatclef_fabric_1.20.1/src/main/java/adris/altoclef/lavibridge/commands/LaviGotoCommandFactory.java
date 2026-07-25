package adris.altoclef.lavibridge.commands;

//20260725_kpopmodder: Added this factory to isolate goto request validation and command creation.

import java.util.Map;

public class LaviGotoCommandFactory implements LaviTypedCommandFactory {

    private final LaviGotoRequestValidator validator;
    private final LaviGotoCommandBuilder commandBuilder;

    public LaviGotoCommandFactory() {
        this(new LaviGotoRequestValidator(), new LaviGotoCommandBuilder());
    }

    public LaviGotoCommandFactory(
            LaviGotoRequestValidator validator,
            LaviGotoCommandBuilder commandBuilder
    ) {
        this.validator = validator;
        this.commandBuilder = commandBuilder;
    }

    @Override
    public String getActionType() {
        return "goto";
    }

    @Override
    public LaviCommandSpec build(Map<String, Object> request) {
        return commandBuilder.build(validator.validate(request));
    }
}
