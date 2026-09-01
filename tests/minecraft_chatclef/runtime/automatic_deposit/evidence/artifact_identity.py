#20260831_kpopmodder: Bind source, deployed, and loaded ChatClef artifact identity.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class AutomaticDepositArtifactIdentity:
    source_jar_path: str
    source_jar_sha256: str
    deployed_jar_path: str
    deployed_jar_sha256: str
    loaded_code_source: str
    discovered_chatclef_jars: tuple[str, ...]
