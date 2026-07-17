#20260717_kpopmodder: Compatibility facade for optional plugin composition classes.
from app_core.composition_core.optional_plugin_composition_context import (
    OptionalPluginCompositionContext,
)
from app_core.composition_core.optional_plugin_composition_result import (
    OptionalPluginCompositionResult,
)
from app_core.composition_core.optional_plugin_composition_service import (
    OptionalPluginCompositionService,
)
from app_core.composition_core.optional_plugin_spec import OptionalPluginSpec

__all__ = [
    "OptionalPluginCompositionContext",
    "OptionalPluginCompositionResult",
    "OptionalPluginCompositionService",
    "OptionalPluginSpec",
]
