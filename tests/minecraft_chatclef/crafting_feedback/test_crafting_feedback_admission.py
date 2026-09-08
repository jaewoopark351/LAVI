#20260907_kpopmodder: Lock the exact trusted quantity-one craft admission scope.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.transport.command_feedback.crafting import (
    CraftingFeedbackAdmissionAuthorizer,
    CraftingFeedbackAdmissionGrant,
)


class CraftingFeedbackAdmissionTests(unittest.TestCase):
    def test_only_exact_diamond_pickaxe_quantity_one_craft_is_admitted(self):
        authorizer = CraftingFeedbackAdmissionAuthorizer(
            live_proof_validator=lambda proof, _event: proof is _PROOF
        )

        admitted = authorizer.issue(
            event=_EVENT,
            korean_eligibility_proof=_PROOF,
            translation=_translation(),
        )
        acquisition = authorizer.issue(
            event=SimpleNamespace(**{**_EVENT.__dict__, "text": "다이아 곡괭이 가져와줘"}),
            korean_eligibility_proof=_PROOF,
            translation=_translation(original_text="다이아 곡괭이 가져와줘"),
        )
        quantity_two = authorizer.issue(
            event=_EVENT,
            korean_eligibility_proof=_PROOF,
            translation=_translation(quantity=2, command="get diamond_pickaxe 2"),
        )

        self.assertIs(type(admitted), CraftingFeedbackAdmissionGrant)
        self.assertEqual("craft", admitted.acquisition_verb_class)
        self.assertEqual("get_item", admitted.intent_kind)
        self.assertEqual("다이아 곡괭이", admitted.spoken_item_label)
        self.assertIsNone(acquisition)
        self.assertIsNone(quantity_two)
        with self.assertRaises(TypeError):
            CraftingFeedbackAdmissionGrant(
                acquisition_verb_class="craft",
                command="get diamond_pickaxe 1",
                command_source="lavi_chat_ui",
                event_id="a" * 32,
                event_kind="chat_submit",
                input_source="lavi_chat_ui",
                intent_kind="get_item",
                provider_id="lavi_chat_ui",
                requested_count=1,
                spoken_item_label="다이아 곡괭이",
                target_item="diamond_pickaxe",
            )


def _translation(
    *,
    quantity: int = 1,
    command: str = "get diamond_pickaxe 1",
    original_text: str = "다이아 곡괭이 만들어줘",
):
    return {
        "status": "validated",
        "executable": True,
        "command": command,
        "resolved_target": "diamond_pickaxe",
        "intent": {
            "intent_type": "get_item",
            "quantity": quantity,
            "language": "ko",
            "original_text": original_text,
        },
    }


_PROOF = object()
_EVENT = SimpleNamespace(
    text="다이아 곡괭이 만들어줘",
    source="lavi_chat_ui",
    provider_id="lavi_chat_ui",
    event_kind="chat_submit",
    final=True,
    event_id="a" * 32,
)


if __name__ == "__main__":
    unittest.main()
