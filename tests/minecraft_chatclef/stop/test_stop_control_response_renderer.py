#20260905_kpopmodder: Verify STOP wording tables are immutable and behavior-preserving.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.response.stop import (
    StopControlResponseRenderer,
)


class StopControlResponseRendererTests(unittest.TestCase):
    def test_local_and_wire_tables_are_immutable(self):
        renderer = StopControlResponseRenderer()

        with self.assertRaises(TypeError):
            renderer._LOCAL["accepted"] = "changed"
        with self.assertRaises(TypeError):
            renderer._WIRE["deadline_exceeded"] = "changed"

        self.assertEqual(
            "마크 AI에게 멈춤을 요청했어요.",
            renderer.render_local("accepted"),
        )
        self.assertEqual("멈출게", renderer.render_start())
        self.assertEqual(
            "멈췄어",
            renderer.render_terminal(
                status="completed",
                control_outcome="stopped",
                reason="stopped",
            ),
        )
        self.assertEqual(
            "중지 요청 시간이 지나 실행하지 않았어요.",
            renderer.render_terminal(
                status="deadline_exceeded",
                control_outcome="rejected",
                reason="deadline_exceeded",
            ),
        )


if __name__ == "__main__":
    unittest.main()
