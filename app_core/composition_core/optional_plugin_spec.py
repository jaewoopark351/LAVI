#20260717_kpopmodder: Isolates declarative optional plugin assembly rules.
from dataclasses import dataclass
from typing import Callable

from .optional_plugin_composition_context import OptionalPluginCompositionContext


@dataclass(frozen=True)
class OptionalPluginSpec:
    #20260717_kpopmodder: Declarative optional plugin assembly rule.
    module_name: str
    attribute_name: str
    lifecycle_component: bool = False
    startup_component: bool = False
    kwargs_factory: Callable[[OptionalPluginCompositionContext], dict] = (
        lambda context: {}
    )
