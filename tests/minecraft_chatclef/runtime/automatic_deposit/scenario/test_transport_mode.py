#20260831_kpopmodder: Lock the three mutually exclusive transport modes.
from __future__ import annotations

import unittest

from .transport_mode import AutomaticDepositTransportMode


class AutomaticDepositTransportModeTests(unittest.TestCase):
    def test_exactly_three_transport_modes_exist(self):
        self.assertEqual(
            (
                "SUPERVISED_LAVI_SUBMIT",
                "OPERATOR_MANUAL_OBSERVE_ONLY",
                "NO_COMMAND_AUTOMATIC_TRIGGER",
            ),
            tuple(mode.value for mode in AutomaticDepositTransportMode),
        )


if __name__ == "__main__":
    unittest.main()
