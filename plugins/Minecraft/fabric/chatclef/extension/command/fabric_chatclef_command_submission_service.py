#20260827_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from typing import Any, Callable

from plugins.Minecraft.fabric.chatclef.extension.command.fabric_chatclef_command_request_factory import (
    FabricChatClefCommandRequestFactory,
)
from plugins.Minecraft.fabric.chatclef.extension.command.fabric_chatclef_command_result_payload_factory import (
    FabricChatClefCommandResultPayloadFactory,
)


class FabricChatClefCommandSubmissionService:
    def __init__(
        self,
        *,
        adapter: object,
        record_command: Callable[[dict[str, Any]], None],
        record_result: Callable[[dict[str, Any], str], None],
        request_factory: FabricChatClefCommandRequestFactory | None = None,
        result_factory: FabricChatClefCommandResultPayloadFactory | None = None,
    ):
        self._adapter = adapter
        self._record_command = record_command
        self._record_result = record_result
        self._request_factory = request_factory or FabricChatClefCommandRequestFactory()
        self._result_factory = result_factory or FabricChatClefCommandResultPayloadFactory()

    def submit(self, command: Any) -> dict[str, Any]:
        request = self._request_factory.build(command)
        self._record_command(request.to_dict())
        result = self._adapter.submit_command(request)
        payload = self._result_factory.build(result)
        self._record_result(payload, "submit_command")
        return payload
