#20260901_kpopmodder: Classify only the exact Carry On configuration files used by the controlled Fabric profile.
from __future__ import annotations

from pathlib import Path


CARRY_ON_OPTIONS_ROLE = "OPTIONS"
CARRY_ON_CLIENT_CONFIG_ROLE = "CLIENT_CONFIG"
CARRY_ON_COMMON_CONFIG_ROLE = "COMMON_CONFIG"
CARRY_ON_REQUIRED_CONFIGURATION_ROLES = frozenset(
    (
        CARRY_ON_OPTIONS_ROLE,
        CARRY_ON_CLIENT_CONFIG_ROLE,
        CARRY_ON_COMMON_CONFIG_ROLE,
    )
)


def automatic_deposit_carry_on_configuration_role(
    instance_root: Path,
    candidate: Path,
) -> str:
    expected = {
        _canonical(instance_root.joinpath("options.txt")): CARRY_ON_OPTIONS_ROLE,
        _canonical(
            instance_root.joinpath("config", "carryon-client.json")
        ): CARRY_ON_CLIENT_CONFIG_ROLE,
        _canonical(
            instance_root.joinpath("config", "carryon-common.json")
        ): CARRY_ON_COMMON_CONFIG_ROLE,
    }
    return expected.get(_canonical(candidate), "")


def _canonical(path: Path) -> str:
    return str(path.resolve(strict=False)).casefold()
