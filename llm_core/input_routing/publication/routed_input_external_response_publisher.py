#20260905_kpopmodder: Own routed external-response publisher invocation.
from __future__ import annotations

from llm_core.routed_response import RoutedResponsePresentationMetadata


class RoutedInputExternalResponsePublisher:
    def __init__(self, *, response_publisher_callback, diagnostics):
        if not callable(response_publisher_callback):
            raise TypeError("response_publisher_callback must be callable")
        self._response_publisher_callback = response_publisher_callback
        self._diagnostics = diagnostics

    def publish(
        self,
        *,
        message,
        decision,
        response_text: str,
        raise_failure: bool = False,
    ):
        try:
            publisher = self._response_publisher_callback()
            route_kind = str(
                getattr(decision, "route_kind", "minecraft_command")
                or "minecraft_command"
            )
            source = str(
                getattr(
                    decision,
                    "response_source",
                    "minecraft_chatclef",
                )
                or "minecraft_chatclef"
            )
            presentation_metadata = getattr(
                decision,
                "presentation_metadata",
                None,
            )
            presentation_detail_log = self._bounded_presentation_detail_log(
                getattr(decision, "presentation_detail_log", "")
            )
            if (
                presentation_metadata is None
                and source == "minecraft_chatclef"
                and route_kind
                in {
                    "crafting_lifecycle",
                    "crafting_status_query",
                    "command_lifecycle",
                    "command_status_query",
                    "stop_control",
                }
            ):
                presentation_metadata = (
                    RoutedResponsePresentationMetadata.minecraft(
                        detail_log=presentation_detail_log,
                    )
                )
            elif (
                type(presentation_metadata)
                is RoutedResponsePresentationMetadata
                and presentation_detail_log
                and not presentation_metadata.detail_log
            ):
                presentation_metadata = RoutedResponsePresentationMetadata(
                    source_kind=presentation_metadata.source_kind,
                    badge_label=presentation_metadata.badge_label,
                    detail_log=presentation_detail_log,
                )
            return publisher.emit_capability_response(
                response_text,
                emission_capability=getattr(
                    decision,
                    "response_emission_capability",
                    None,
                ),
                event=message,
                source=source,
                route_kind=route_kind,
                response_kind=str(
                    getattr(decision, "response_kind", "immediate")
                    or "immediate"
                ),
                presentation_metadata=presentation_metadata,
                #20260907_kpopmodder: Voice generators are drained off-screen, so
                # use the existing async UI sink while Chat UI responses keep
                # their direct-yield presentation path.
                send_ui=(
                    getattr(message, "source", None) == "voice_input_final"
                    and presentation_metadata is not None
                ),
            )
        except Exception:
            if raise_failure:
                raise
            self._diagnostics.log_failure("publication")
            return None

    @staticmethod
    def _bounded_presentation_detail_log(value: object) -> str:
        if (
            type(value) is not str
            or len(value) > 1024
            or any(
                ord(character) < 32 or ord(character) > 126
                for character in value
            )
        ):
            return ""
        return value


__all__ = ("RoutedInputExternalResponsePublisher",)
