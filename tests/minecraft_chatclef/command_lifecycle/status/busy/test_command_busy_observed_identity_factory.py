#20260909_kpopmodder: Verify canonical busy identity decoding is immutable and fail closed.
from __future__ import annotations

import unittest
from collections.abc import Mapping
from dataclasses import FrozenInstanceError

from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandBusyObservedIdentity,
    CommandBusyObservedIdentityFactory,
)


class CommandBusyObservedIdentityFactoryTests(unittest.TestCase):
    def setUp(self) -> None:
        self.factory = CommandBusyObservedIdentityFactory()

    def test_decodes_only_the_canonical_nested_status_identity(self):
        commands = {
            "active_session_id": "session-a",
            "active_generation": 7,
            "active_request_id": "request-a",
            "active_command_message_id": "message-a",
        }

        identity = self.factory.create(
            {"status": {"details": {"commands": commands}}}
        )
        commands["active_request_id"] = "request-b"

        self.assertIs(type(identity), CommandBusyObservedIdentity)
        self.assertEqual("session-a", identity.active_session_id)
        self.assertEqual(7, identity.active_generation)
        self.assertEqual("request-a", identity.active_request_id)
        self.assertEqual("message-a", identity.active_command_message_id)
        with self.assertRaises(FrozenInstanceError):
            identity.active_request_id = "replacement"

    def test_malformed_or_hostile_mappings_return_none_without_raising(self):
        fixtures = (
            None,
            {},
            {"status": None},
            {"status": {"details": None}},
            {"status": {"details": {"commands": None}}},
            _ThrowingMapping(),
            {"status": _ThrowingMapping()},
            {"status": {"details": _ThrowingMapping()}},
            {"status": {"details": {"commands": _ThrowingMapping()}}},
        )

        for fixture in fixtures:
            with self.subTest(fixture=type(fixture).__name__):
                self.assertIsNone(self.factory.create(fixture))

    def test_invalid_field_types_bounds_and_whitespace_return_none(self):
        valid = {
            "active_session_id": "session-a",
            "active_generation": 1,
            "active_request_id": "request-a",
            "active_command_message_id": "message-a",
        }
        invalid_values = {
            "active_session_id": (None, "", " session-a", "session-a ", "x" * 161),
            "active_generation": (
                True,
                False,
                0,
                -1,
                1.0,
                "1",
                CommandBusyObservedIdentity.MAX_GENERATION + 1,
            ),
            "active_request_id": (None, "", " request-a", "x" * 161),
            "active_command_message_id": (None, "", "message-a ", "x" * 161),
        }

        for field, values in invalid_values.items():
            for value in values:
                with self.subTest(field=field, value_type=type(value).__name__):
                    commands = dict(valid)
                    commands[field] = value
                    self.assertIsNone(
                        self.factory.create(
                            {"status": {"details": {"commands": commands}}}
                        )
                    )


class _ThrowingMapping(Mapping):
    def __getitem__(self, _key):
        raise RuntimeError("unreadable")

    def __iter__(self):
        return iter(())

    def __len__(self):
        return 0

    def get(self, _key, _default=None):
        raise RuntimeError("unreadable")


if __name__ == "__main__":
    unittest.main()
