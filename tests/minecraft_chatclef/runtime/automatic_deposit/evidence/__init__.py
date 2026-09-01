"""Immutable artifact, fixture, log-cursor, and run evidence contracts."""

from .artifact_identity import AutomaticDepositArtifactIdentity
from .artifact_identity_collection import AutomaticDepositArtifactIdentityCollection
from .carry_on_identity import AutomaticDepositCarryOnIdentity
from .carry_on_identity_collection import AutomaticDepositCarryOnIdentityCollection
from .carry_on_configuration_snapshot import (
    AutomaticDepositCarryOnConfigurationSnapshot,
)
from .fixture_manifest import AutomaticDepositFixtureManifest
from .mods_directory_snapshot import AutomaticDepositModsDirectorySnapshot
from .run_manifest import AutomaticDepositRunManifest
from .runtime_artifact_snapshot import AutomaticDepositRuntimeArtifactSnapshot

__all__ = (
    "AutomaticDepositArtifactIdentity",
    "AutomaticDepositArtifactIdentityCollection",
    "AutomaticDepositCarryOnIdentity",
    "AutomaticDepositCarryOnIdentityCollection",
    "AutomaticDepositCarryOnConfigurationSnapshot",
    "AutomaticDepositFixtureManifest",
    "AutomaticDepositModsDirectorySnapshot",
    "AutomaticDepositRunManifest",
    "AutomaticDepositRuntimeArtifactSnapshot",
)
