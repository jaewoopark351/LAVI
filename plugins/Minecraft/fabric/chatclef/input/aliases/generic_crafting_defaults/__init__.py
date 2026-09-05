#20260905_kpopmodder: Export Feature-B profile, candidate, quantity, and activation boundaries.
from .generic_crafting_default_rule import GenericCraftingDefaultRule
from .generic_crafting_defaults_activation_receipt import (
    GenericCraftingDefaultsActivationReceipt,
)
from .generic_crafting_defaults_activation_registry import (
    GenericCraftingDefaultsActivationRegistry,
)
from .generic_crafting_defaults_activation_state import (
    GenericCraftingDefaultsActivationState,
)
from .generic_crafting_defaults_admission import GenericCraftingDefaultsAdmission
from .generic_crafting_defaults_admission_decision import (
    GenericCraftingDefaultsAdmissionDecision,
)
from .generic_crafting_defaults_candidate import GenericCraftingDefaultsCandidate
from .generic_crafting_defaults_candidate_detector import (
    GenericCraftingDefaultsCandidateDetector,
)
from .generic_crafting_defaults_profile import GenericCraftingDefaultsProfile
from .generic_crafting_defaults_translation_projection import (
    GenericCraftingDefaultsTranslationProjection,
)
from .generic_crafting_quantity_shape import GenericCraftingQuantityShape
from .generic_crafting_quantity_shape_guard import (
    GenericCraftingQuantityShapeGuard,
)

__all__ = (
    "GenericCraftingDefaultRule",
    "GenericCraftingDefaultsActivationReceipt",
    "GenericCraftingDefaultsActivationRegistry",
    "GenericCraftingDefaultsActivationState",
    "GenericCraftingDefaultsAdmission",
    "GenericCraftingDefaultsAdmissionDecision",
    "GenericCraftingDefaultsCandidate",
    "GenericCraftingDefaultsCandidateDetector",
    "GenericCraftingDefaultsProfile",
    "GenericCraftingDefaultsRouteOwner",
    "GenericCraftingDefaultsTranslationProjection",
    "GenericCraftingQuantityShape",
    "GenericCraftingQuantityShapeGuard",
)


def __getattr__(name: str):
    if name == "GenericCraftingDefaultsRouteOwner":
        from .generic_crafting_defaults_route_owner import (
            GenericCraftingDefaultsRouteOwner,
        )

        return GenericCraftingDefaultsRouteOwner
    raise AttributeError(name)
