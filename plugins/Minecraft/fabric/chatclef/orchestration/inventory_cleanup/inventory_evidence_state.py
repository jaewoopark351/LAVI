#20260901_kpopmodder: Keep the inventory evidence-state contract in one file.
from enum import Enum


class InventoryEvidenceState(str, Enum):
    AVAILABLE = "available"
    FULL = "full"
    UNKNOWN = "unknown"
