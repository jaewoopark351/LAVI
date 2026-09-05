#20260905_kpopmodder: Compose focused ordinary-command admission collaborators.
from __future__ import annotations

from .fabric_chatclef_command_admission_coordinator import (
    FabricChatClefCommandAdmissionCoordinator,
)
from .fabric_chatclef_command_admission_decision_factory import (
    FabricChatClefCommandAdmissionDecisionFactory,
)
from .fabric_chatclef_command_ownership_committer import (
    FabricChatClefCommandOwnershipCommitter,
)
from .fabric_chatclef_command_request_normalizer import normalize_command_request
from .fabric_chatclef_ordinary_admission_inspector import (
    FabricChatClefOrdinaryAdmissionInspector,
)


class FabricChatClefCommandAdmissionComponentGraph:
    def __init__(
        self,
        *,
        connection_ownership,
        command_lock,
        now_ms,
        stop_control_admission_barrier=None,
    ) -> None:
        self.inspector = FabricChatClefOrdinaryAdmissionInspector(
            connection_ownership=connection_ownership,
            now_ms=now_ms,
            stop_control_admission_barrier=stop_control_admission_barrier,
        )
        self.ownership_committer = FabricChatClefCommandOwnershipCommitter(
            connection_ownership=connection_ownership,
            command_lock=command_lock,
        )
        self.decision_factory = FabricChatClefCommandAdmissionDecisionFactory()
        self.coordinator = FabricChatClefCommandAdmissionCoordinator(
            command_lock=command_lock,
            request_normalizer=normalize_command_request,
            inspector=self.inspector,
            ownership_committer=self.ownership_committer,
            decision_factory=self.decision_factory,
            now_ms=now_ms,
        )
