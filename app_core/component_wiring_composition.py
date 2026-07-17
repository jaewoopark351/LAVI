#20260717_kpopmodder: Compatibility facade for app composition wiring classes.
from app_core.composition_core.app_component_wiring_service import AppComponentWiringService
from app_core.composition_core.managed_component_wiring_result import (
    ManagedComponentWiringResult,
)

__all__ = [
    "AppComponentWiringService",
    "ManagedComponentWiringResult",
]
