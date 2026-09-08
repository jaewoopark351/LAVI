#20260907_kpopmodder: Prove the public raw-feedback port accepts only its exact immutable GUI event.
from __future__ import annotations

import types
import unittest

from input_core.input_event.contracts.lavi_input_event import LaviInputEvent
from plugins.Minecraft.fabric.chatclef.extension.minecraft_fabric_chatclef_command_feedback_facade import (
    MinecraftFabricChatClefCommandFeedbackFacade,
)


class CommandNameOnlyFacadeBoundaryTests(unittest.TestCase):
    def setUp(self):
        self.facade = MinecraftFabricChatClefCommandFeedbackFacade(
            types.SimpleNamespace()
        )

    def test_exact_final_raw_gui_event_can_issue_feedback_only_grant(self):
        grant = self.facade.command_name_only_grant(
            "@get stone 2",
            input_event=_event(text="@get stone 2"),
        )

        self.assertIsNotNone(grant)
        self.assertEqual("get", grant.descriptor.command_name)
        self.assertEqual("raw_typed", grant.descriptor.detail_level)

    def test_duck_typed_event_with_identical_fields_is_rejected(self):
        exact = _event(text="@get stone 2")
        forged = types.SimpleNamespace(
            **{
                field: getattr(exact, field)
                for field in (
                    "text",
                    "source",
                    "event_id",
                    "event_kind",
                    "final",
                    "provider_id",
                    "fallback_payload",
                )
            }
        )

        self.assertIsNone(
            self.facade.command_name_only_grant(
                "@get stone 2",
                input_event=forged,
            )
        )

    def test_wrong_event_authority_fields_are_each_rejected(self):
        mutations = (
            {"source": "lavi_gui_korean"},
            {"provider_id": "another_provider"},
            {"event_kind": "minecraft_korean_gui_submit"},
            {"final": False},
            {"text": "@get dirt 2"},
        )

        for changes in mutations:
            with self.subTest(changes=changes):
                event_fields = {"text": "@get stone 2", **changes}
                event = _event(**event_fields)
                self.assertIsNone(
                    self.facade.command_name_only_grant(
                        "@get stone 2",
                        input_event=event,
                    )
                )

    def test_non_string_command_is_rejected_before_decoding(self):
        self.assertIsNone(
            self.facade.command_name_only_grant(
                types.SimpleNamespace(),
                input_event=_event(text="@get stone 2"),
            )
        )


def _event(
    *,
    text: str,
    source: str = "lavi_gui",
    event_id: str = "a" * 32,
    event_kind: str = "minecraft_raw_gui_submit",
    final: bool = True,
    provider_id: str = "minecraft_fabric_chatclef_ui",
) -> LaviInputEvent:
    return LaviInputEvent(
        text=text,
        source=source,
        event_id=event_id,
        event_kind=event_kind,
        final=final,
        provider_id=provider_id,
        fallback_payload=None,
    )


if __name__ == "__main__":
    unittest.main()
