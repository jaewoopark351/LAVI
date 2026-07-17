#20260717_kpopmodder: Added this module to keep shared plugin contract helpers class-free.
from plugin_system.contracts_core.contract_constants import PLUGIN_LIFECYCLE_METHODS
from plugin_system.contracts_core.plugin_contract_issue import PluginContractIssue


def _is_non_empty_text(value):
    return isinstance(value, str) and bool(value.strip())


def _add_text_sequence_issues(issues, value, path):
    if not isinstance(value, (list, tuple)):
        issues.append(PluginContractIssue(
            code="contract_invalid_sequence",
            message=f"{path} must be a list or tuple of strings",
            path=path,
        ))
        return
    for item in value:
        if not _is_non_empty_text(item):
            issues.append(PluginContractIssue(
                code="contract_invalid_sequence_item",
                message=f"{path} entries must be non-empty strings",
                path=path,
            ))


def _text_tuple(value):
    if value is None:
        return ()
    if isinstance(value, str):
        text = value.strip()
        return (text,) if text else ()
    if isinstance(value, (list, tuple)):
        return tuple(str(item).strip() for item in value if str(item).strip())
    return ()


def _contract_to_dict(value):
    if value is None:
        return {}
    if hasattr(value, "to_dict"):
        return value.to_dict()
    if isinstance(value, dict):
        return dict(value)
    return {}


def validate_plugin_lifecycle(
    target,
    plugin_id="",
    required_methods=PLUGIN_LIFECYCLE_METHODS,
):
    #20260717_kpopmodder: Runtime plugin instances must expose the common lifecycle surface.
    issues = []
    target_name = getattr(target, "__name__", target.__class__.__name__)
    label = str(plugin_id or target_name or "plugin")
    for method_name in required_methods:
        method = getattr(target, method_name, None)
        if callable(method):
            continue
        issues.append(PluginContractIssue(
            code="contract_missing_lifecycle_callable",
            message=f"{label}.{method_name} must be callable",
            path=f"lifecycle.{method_name}",
        ))
    return tuple(issues)


def _diagnostic_to_dict(value):
    if value is None:
        return {}
    if hasattr(value, "to_dict"):
        return value.to_dict()
    if isinstance(value, dict):
        return dict(value)
    return {"message": str(value)}
