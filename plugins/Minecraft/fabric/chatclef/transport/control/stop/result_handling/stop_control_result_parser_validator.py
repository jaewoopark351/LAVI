#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.transport.control.stop.result_handling.stop_control_result_parser import StopControlResultParser


class StopControlResultParserValidator:
    def __init__(self, validator: object, parser: object | None = None):
        self._validator = validator
        self._parser = parser or StopControlResultParser()

    def parse_and_inspect(
        self,
        *,
        payload: dict,
        envelope: object,
        tracker: object,
    ) -> tuple[object | None, object | None, Exception | None]:
        result, error = self._parser.parse(payload)
        if error is not None:
            return None, None, error
        return result, self._validator.inspect(envelope, result, tracker), None


__all__ = ("StopControlResultParserValidator",)
