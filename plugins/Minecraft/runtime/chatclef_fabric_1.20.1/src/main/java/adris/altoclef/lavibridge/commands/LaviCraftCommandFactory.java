package adris.altoclef.lavibridge.commands;

//20260725_kpopmodder: Added this factory to keep craft validation separate from command text creation.

import java.util.Map;

public class LaviCraftCommandFactory implements LaviTypedCommandFactory {

    private final LaviGetItemRequestValidator validator;
    private final LaviCraftCommandBuilder commandBuilder;

    public LaviCraftCommandFactory() {
        this(new LaviGetItemRequestValidator(), new LaviCraftCommandBuilder());
    }

    public LaviCraftCommandFactory(
            LaviGetItemRequestValidator validator,
            LaviCraftCommandBuilder commandBuilder
    ) {
        this.validator = validator;
        this.commandBuilder = commandBuilder;
    }

    @Override
    public String getActionType() {
        return "craft";
    }

    @Override
    public LaviCommandSpec build(Map<String, Object> request) {
        return commandBuilder.build(validator.validate(request));
    }
}
