#20260901_kpopmodder: Preserve the public builder import while responsibilities live in dedicated packages.
"""Compatibility facade for automatic-deposit verified evidence."""

from .assembly.verified_evidence_assembly import (
    build_automatic_deposit_verified_evidence,
)
from .assembly.store_home.p1_store_home_verified_evidence_assembly import (
    build_p1_store_home_verified_evidence,
)
from .preflight.evidence_preflight import (
    verify_automatic_deposit_evidence_preflight,
)

__all__ = (
    "build_automatic_deposit_verified_evidence",
    "build_p1_store_home_verified_evidence",
    "verify_automatic_deposit_evidence_preflight",
)
