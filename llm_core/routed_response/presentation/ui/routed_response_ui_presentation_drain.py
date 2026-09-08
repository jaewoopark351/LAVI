#20260905_kpopmodder: Keep routed-response UI presentation in the established responsibility-split boundary.
#20260907_kpopmodder: Append queued asynchronous messages to the current Chat UI history.
from __future__ import annotations

import gradio as gr

from .routed_response_ui_presentation_fingerprint import (
    RoutedResponseUiPresentationFingerprint,
)
from .routed_response_ui_presentation_queue import (
    RoutedResponseUiPresentationQueue,
)


class RoutedResponseUiPresentationDrain:
    def __init__(self, presentation_queue) -> None:
        if type(presentation_queue) is not RoutedResponseUiPresentationQueue:
            raise TypeError("presentation_queue must be exact")
        self._presentation_queue = presentation_queue

    def append_to_history(self, history: object):
        current = list(history or [])
        epoch, pending = self._presentation_queue.snapshot()
        if not pending:
            return gr.skip()

        presented_count = self._presented_prefix_count(current, pending)
        if presented_count:
            self._presentation_queue.acknowledge_presented(
                epoch=epoch,
                items=pending[:presented_count],
            )
            current_epoch, pending = self._presentation_queue.snapshot()
            if current_epoch != epoch:
                return gr.skip()
        if not pending:
            return gr.skip()

        known_tokens = set()
        for item in current:
            presentation_identity = (
                RoutedResponseUiPresentationFingerprint.identity_from_message(
                    item
                )
            )
            if presentation_identity is not None:
                known_tokens.add(presentation_identity.token)
        additions = []
        for item in pending:
            fingerprint = RoutedResponseUiPresentationFingerprint.from_message(
                item
            )
            if (
                fingerprint is None
                or fingerprint.presentation_identity.token in known_tokens
            ):
                continue
            additions.append(item)
            known_tokens.add(fingerprint.presentation_identity.token)
        if not additions:
            return gr.skip()
        current.extend(additions)
        if not self._presentation_queue.is_epoch_current(epoch):
            return gr.skip()
        return current

    @classmethod
    def _presented_prefix_count(cls, history, pending) -> int:
        pending_fingerprints = []
        for item in pending:
            fingerprint = RoutedResponseUiPresentationFingerprint.from_message(
                item
            )
            if fingerprint is None:
                break
            pending_fingerprints.append(fingerprint)
        if not pending_fingerprints:
            return 0
        pending_fingerprint_set = set(pending_fingerprints)
        history_fingerprints = [
            fingerprint
            for item in history
            if (
                fingerprint := (
                    RoutedResponseUiPresentationFingerprint.from_message(item)
                )
            )
            is not None
            if fingerprint in pending_fingerprint_set
        ]
        count = 0
        for expected, observed in zip(
            pending_fingerprints,
            history_fingerprints,
        ):
            if observed != expected:
                break
            count += 1
        return count


__all__ = ("RoutedResponseUiPresentationDrain",)
