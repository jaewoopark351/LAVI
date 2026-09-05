#20260905_kpopmodder: Resolves and locates providers by their loader-owned descriptor identity.
from __future__ import annotations


class ProviderDescriptorResolver:
    """Read the exact non-empty provider ID from a loader descriptor."""

    def descriptor_id(self, provider) -> str:
        handle = getattr(provider, "handle", None)
        descriptor = getattr(handle, "descriptor", None)
        provider_id = getattr(descriptor, "id", None)
        if type(provider_id) is not str or not provider_id:
            return ""
        return provider_id

    def find(self, providers, provider_id):
        for provider in providers:
            if self.descriptor_id(provider) == provider_id:
                return provider
        return None


__all__ = ("ProviderDescriptorResolver",)
