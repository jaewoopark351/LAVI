#20260715_kpopmodder: Verify the StarCraft2 runtime object graph is assembled outside the UI module.
import os
import unittest

from plugins.StarCraft2.starcraft2_core.starcraft2_runtime_factory import (
    StarCraft2RuntimeBundle,
    StarCraft2RuntimeFactory,
)


class StarCraft2RuntimeFactoryTests(unittest.TestCase):
    def test_create_returns_connected_runtime_bundle(self):
        plugin_root = os.path.join(os.getcwd(), "plugins", "StarCraft2")

        bundle = StarCraft2RuntimeFactory().create(
            plugin_root,
            ["Terran", "Zerg", "Protoss", "Random"],
        )

        self.assertIsInstance(bundle, StarCraft2RuntimeBundle)
        self.assertIs(bundle.facade_service.config_manager, bundle.config_manager)
        self.assertIs(bundle.facade_service.engine_registry, bundle.engine_registry)
        self.assertIs(bundle.facade_service.state, bundle.state)
        self.assertIs(bundle.facade_service.ladder_proxy, bundle.ladder_proxy)
        self.assertIs(bundle.facade_service.event_bus, bundle.event_bus)
        self.assertIs(bundle.facade_service.runtime_context, bundle.runtime_context)
        self.assertIs(bundle.facade_service.local_match_service, bundle.local_match_service)
        self.assertIs(bundle.local_match_service.ladder_proxy, bundle.ladder_proxy)
        self.assertIs(bundle.local_match_service.event_bus, bundle.event_bus)
        self.assertNotIn("runtime_context", bundle.local_match_service.__dict__)
        self.assertTrue(callable(bundle.local_match_service._runtime_snapshot_provider))
        self.assertIs(
            bundle.local_match_service.line_callback.__self__,
            bundle.ladder_proxy_event_service,
        )


if __name__ == "__main__":
    unittest.main()
