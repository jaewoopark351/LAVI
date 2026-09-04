#20260905_kpopmodder: Owns optional plugin-to-plugin callback composition only.


class OptionalPluginCallbackWiring:
    def wire(self, *, starcraft_plugin=None, screen_vision=None):
        if starcraft_plugin is None:
            return
        starcraft_plugin.set_screen_observation_provider(
            lambda: getattr(screen_vision, "last_screen_observation", "")
            if screen_vision is not None
            else ""
        )


__all__ = ["OptionalPluginCallbackWiring"]
