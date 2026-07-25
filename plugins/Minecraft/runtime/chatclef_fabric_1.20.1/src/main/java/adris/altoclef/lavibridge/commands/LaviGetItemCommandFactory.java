package adris.altoclef.lavibridge.commands;

//20260725_kpopmodder: Added this factory to isolate get-item request validation and command creation.

import java.util.Map;

public class LaviGetItemCommandFactory implements LaviTypedCommandFactory {

    private final LaviGetItemRequestValidator validator;
    private final LaviGetItemCommandBuilder commandBuilder;

    public LaviGetItemCommandFactory() {
        this(new LaviGetItemRequestValidator(), new LaviGetItemCommandBuilder());
    }

    public LaviGetItemCommandFactory(
            LaviGetItemRequestValidator validator,
            LaviGetItemCommandBuilder commandBuilder
    ) {
        this.validator = validator;
        this.commandBuilder = commandBuilder;
    }

    @Override
    public String getActionType() {
        return "get-item";
    }

    @Override
    public LaviCommandSpec build(Map<String, Object> request) {
        return commandBuilder.build(validator.validate(request));
    }
}
