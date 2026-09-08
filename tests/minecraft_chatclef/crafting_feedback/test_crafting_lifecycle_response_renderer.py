#20260907_kpopmodder: Lock exact natural Korean crafting lifecycle wording.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.response.crafting_lifecycle import (
    CraftingLifecycleResponseRenderer,
)


class CraftingLifecycleResponseRendererTests(unittest.TestCase):
    def test_exact_start_progress_completion_and_fail_closed_wording(self):
        renderer = CraftingLifecycleResponseRenderer()

        self.assertEqual("다이아 곡괭이 만들어 줄게", renderer.render_start())
        self.assertEqual(
            "다이아 곡괭이 만드는 중이야",
            renderer.render_status("running"),
        )
        self.assertEqual(
            "다이아 곡괭이 다 만들었어",
            renderer.render_terminal(
                status="completed",
                verified=True,
            ),
        )
        self.assertEqual(
            (
                "다이아 곡괭이 만드는 작업은 끝났는데,\n"
                "다 만들어졌는지는 확인하지 못했어"
            ),
            renderer.render_terminal(
                status="completed",
                verified=False,
            ),
        )
        self.assertEqual(
            "다이아 곡괭이 만들지 못했어",
            renderer.render_terminal(
                status="failed",
                verified=False,
                dispatch_started=False,
            ),
        )
        self.assertEqual(
            "다이아 곡괭이 만들다가 실패했어",
            renderer.render_terminal(
                status="failed",
                verified=False,
                dispatch_started=True,
            ),
        )
        self.assertEqual(
            "다이아 곡괭이 만들기가 중단됐어",
            renderer.render_terminal(status="cancelled", verified=False),
        )
        self.assertEqual(
            "다이아 곡괭이 만들기가 시간 안에 끝나지 않았어",
            renderer.render_terminal(
                status="deadline_exceeded",
                verified=False,
            ),
        )
        self.assertEqual(
            "다이아 곡괭이 만들기 결과는 확인하지 못했어",
            renderer.render_terminal(status="unknown", verified=False),
        )


if __name__ == "__main__":
    unittest.main()
