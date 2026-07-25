package adris.altoclef.lavibridge.commands;

//20260725_kpopmodder: Added this factory to isolate equip request validation and action spec creation.

import java.util.Map;

public class LaviEquipCommandFactory implements LaviTypedCommandFactory {

    private final LaviEquipRequestValidator validator;
    private final LaviEquipCommandBuilder commandBuilder;

    public LaviEquipCommandFactory() {
        this(new LaviEquipRequestValidator(), new LaviEquipCommandBuilder());
    }

    public LaviEquipCommandFactory(
            LaviEquipRequestValidator validator,
            LaviEquipCommandBuilder commandBuilder
    ) {
        this.validator = validator;
        this.commandBuilder = commandBuilder;
    }

    @Override
    public String getActionType() {
        return "equip";
    }

    @Override
    public LaviCommandSpec build(Map<String, Object> request) {
        return commandBuilder.build(validator.validate(request));
    }
}
