#20260901_kpopmodder: Provide one complete hermetic JVM-owned status artifact fixture.
from __future__ import annotations

from ..test_production_status_observer import _production_status


def status_with_p1_supervised_runtime_artifact() -> dict[str, object]:
    status = _production_status()
    status["details"]["runtime_artifact"] = {
        "schema_version": "p1-supervised-runtime-artifact/v1",
        "backend_id": "fabric_chatclef",
        "loader_id": "fabric",
        "minecraft_version": "1.20.1",
        "chatclef_version": "1.20.1-0.18.23",
        "loaded_jar_path": "C:/minecraft/instance/mods/chatclef.jar",
        "loaded_jar_size": 7_512_843,
        "loaded_jar_sha256": "a" * 64,
        "sha256_evidence_source": "JVM_COMPUTED",
        "minecraft_process_id": 17_284,
        "session_id": "fabric-chatclef-session-1",
        "connection_generation": 7,
    }
    return status
