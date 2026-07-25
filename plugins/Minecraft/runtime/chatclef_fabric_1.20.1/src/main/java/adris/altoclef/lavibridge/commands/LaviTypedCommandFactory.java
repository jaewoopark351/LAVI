package adris.altoclef.lavibridge.commands;

//20260725_kpopmodder: Added this interface so new LAVI actions can register their own command factory.

import java.util.Map;

public interface LaviTypedCommandFactory {

    String getActionType();

    LaviCommandSpec build(Map<String, Object> request);
}
