#20260831_kpopmodder: Preserve one immutable Carry On artifact and observation identity.
from __future__ import annotations

from dataclasses import dataclass

from .carry_on_presence import AutomaticDepositCarryOnPresence


@dataclass(frozen=True, slots=True)
class AutomaticDepositCarryOnIdentity:
    presence: AutomaticDepositCarryOnPresence
    loaded: bool
    jar_path: str = ""
    jar_sha256: str = ""
    version: str = ""
    config_fingerprint: str = ""
    loader_mod_list_fingerprint: str = ""
    mods_directory_fingerprint: str = ""
    observation_reason: str = ""
