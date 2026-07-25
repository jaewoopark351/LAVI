package adris.altoclef.lavibridge.commands;

//20260725_kpopmodder: Added this class to keep HTTP request validation and AltoClef command text creation isolated.

import java.util.Map;

public class LaviCommandRequestFactory {

    private final LaviGetItemCommandFactory getItemCommandFactory = new LaviGetItemCommandFactory();
    private final LaviGotoCommandFactory gotoCommandFactory = new LaviGotoCommandFactory();

    public LaviCommandSpec getItem(Map<String, Object> request) {
        return getItemCommandFactory.build(request);
    }

    public LaviCommandSpec gotoTarget(Map<String, Object> request) {
        return gotoCommandFactory.build(request);
    }
}
