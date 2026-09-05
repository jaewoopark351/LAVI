#20260905_kpopmodder: Own StarCraft observation and coach-response wiring.
from __future__ import annotations

from input_core.input_event.provenance import STARCRAFT_REMASTERED


class StarCraftDirectInputWiring:
    def __init__(self, adapter_cache):
        self._adapter_cache = adapter_cache

    def wire(self, *, llm, starcraft_plugin) -> None:
        if starcraft_plugin is None:
            return
        callback = self._adapter_cache.get_or_create(
            source=STARCRAFT_REMASTERED,
            provider_id="StarCraftRemastered",
            event_kind="starcraft_output",
            output_callback=llm.receive_input,
        )
        starcraft_plugin.add_output_event_listener(callback)
        llm.add_output_event_listener(
            starcraft_plugin.receive_coach_response,
            full_response=True,
        )


__all__ = ("StarCraftDirectInputWiring",)
