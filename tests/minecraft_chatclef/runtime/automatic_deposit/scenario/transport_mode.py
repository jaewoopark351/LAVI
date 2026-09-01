#20260831_kpopmodder: Separate automatic-deposit runtime transport ownership.
from __future__ import annotations

from enum import Enum


class AutomaticDepositTransportMode(str, Enum):
    SUPERVISED_LAVI_SUBMIT = "SUPERVISED_LAVI_SUBMIT"
    OPERATOR_MANUAL_OBSERVE_ONLY = "OPERATOR_MANUAL_OBSERVE_ONLY"
    NO_COMMAND_AUTOMATIC_TRIGGER = "NO_COMMAND_AUTOMATIC_TRIGGER"
