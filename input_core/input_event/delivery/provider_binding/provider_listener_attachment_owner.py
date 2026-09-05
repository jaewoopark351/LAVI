#20260905_kpopmodder: Owns provider listener attachment state and exact callback removal.
from __future__ import annotations


class ProviderListenerAttachmentOwner:
    """Track live provider plugins and mutate only their listener collections."""

    def __init__(self) -> None:
        self._attached_plugins = {}

    def plugin_for(self, provider):
        return self._attached_plugins.get(provider)

    def providers(self) -> tuple:
        return tuple(self._attached_plugins)

    def attached_items(self) -> tuple:
        return tuple(self._attached_plugins.items())

    def remember(self, provider, plugin) -> None:
        self._attached_plugins[provider] = plugin

    def forget(self, provider) -> None:
        self._attached_plugins.pop(provider, None)

    def listener_list(self, plugin):
        listeners = getattr(plugin, "input_event_listeners", None)
        if listeners is None:
            listeners = []
            plugin.input_event_listeners = listeners
        return listeners

    def attach(self, plugin, listener) -> None:
        self.listener_list(plugin).append(listener)

    def detach(self, plugin, callbacks) -> None:
        listeners = getattr(plugin, "input_event_listeners", None)
        if listeners is None:
            return
        for callback in callbacks:
            self._remove_all(listeners, callback)

    def clear(self) -> None:
        self._attached_plugins.clear()

    def _remove_all(self, listeners, callback) -> None:
        while callback in listeners:
            listeners.remove(callback)


__all__ = ("ProviderListenerAttachmentOwner",)
