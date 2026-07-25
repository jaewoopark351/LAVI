package adris.altoclef.lavibridge.commands;

//20260725_kpopmodder: Added this factory to keep get-and-equip validation separate from execution.

import java.util.Map;

public class LaviGetAndEquipCommandFactory implements LaviTypedCommandFactory {

    private final LaviGetItemRequestValidator validator;
    private final LaviGetAndEquipCommandBuilder commandBuilder;

    public LaviGetAndEquipCommandFactory() {
        this(new LaviGetItemRequestValidator(), new LaviGetAndEquipCommandBuilder());
    }

    public LaviGetAndEquipCommandFactory(
            LaviGetItemRequestValidator validator,
            LaviGetAndEquipCommandBuilder commandBuilder
    ) {
        this.validator = validator;
        this.commandBuilder = commandBuilder;
    }

    @Override
    public String getActionType() {
        return "get-and-equip";
    }

    @Override
    public LaviCommandSpec build(Map<String, Object> request) {
        return commandBuilder.build(validator.validate(request));
    }
}
