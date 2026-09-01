#20260901_kpopmodder: Bind the first StoreHome manifest event to one external opaque run identity.
from __future__ import annotations

from ....diagnostic_record import ProductionDiagnosticRecord
from ...record_contract import required_field


def verify_p1_supervised_run_manifest_event(
    record: ProductionDiagnosticRecord,
    *,
    expected_run_manifest_id: str,
) -> str | None:
    if record.event_name != "STORE_HOME_RUN_MANIFEST":
        return "P1_STORE_HOME_RUN_MANIFEST_EVENT_INVALID"
    envelope_id, error = required_field(record, "runManifestId")
    if error is not None:
        return error
    run_id, error = required_field(record, "runId")
    if error is not None:
        return error
    if envelope_id != expected_run_manifest_id or run_id != expected_run_manifest_id:
        return "P1_STORE_HOME_RUN_MANIFEST_ID_NOT_EXPECTED"
    source, error = required_field(record, "runManifestIdSource")
    if error is not None:
        return error
    if source != "EXTERNAL_BUILD_DEPLOY_MANIFEST":
        return "P1_STORE_HOME_RUN_MANIFEST_SOURCE_NOT_EXTERNAL"
    return None
