#20260622_kpopmodder: Raw event logs are grouped into compact conversation episodes for recall.


class MemoryConsolidator:#20260622_kpopmodder: 원본 이벤트를 회상 가능한 사건 단위로 묶는다.
    """raw_events.jsonl 이벤트를 검색 가능한 대화 단위로 묶는다.

    원본 파일은 수정하지 않는다. 사용자 발화와 뒤따르는 AI 답변을 한 묶음으로
    만들어 검색 결과가 문장 하나가 아니라 당시 대화 맥락을 포함하도록 한다.
    """

    USER_EVENT = "user_message"
    ASSISTANT_EVENT = "assistant_message"
    SCREEN_EVENT = "screen_observation"
    SILENT_SCREEN_EVENT = "screen_observation_silent"

    def __init__(self, max_value_chars=1200):
        self.max_value_chars = max(100, int(max_value_chars))

    def consolidate(self, events):#20260622_kpopmodder: 사용자 대화와 화면 관찰을 사건 목록으로 변환한다.
        episodes = []
        pending_user = None

        for event in events or []:
            if not isinstance(event, dict):
                continue

            event_type = str(event.get("event_type", "")).strip()
            value = self._clean_value(event.get("value"))

            if not value:
                continue

            if event_type == self.USER_EVENT:
                if pending_user is not None:
                    episodes.append(self._make_user_episode(pending_user))
                pending_user = event
                continue

            if event_type == self.ASSISTANT_EVENT:
                if pending_user is None:
                    continue

                episodes.append(
                    self._make_conversation_episode(
                        pending_user,
                        event,
                    )
                )
                pending_user = None
                continue

            if event_type in {
                self.SCREEN_EVENT,
                self.SILENT_SCREEN_EVENT,
            }:
                episodes.append(self._make_screen_episode(event))

        if pending_user is not None:
            episodes.append(self._make_user_episode(pending_user))

        return episodes

    def _make_conversation_episode(self, user_event, assistant_event):
        user_value = self._clean_value(user_event.get("value"))
        assistant_value = self._clean_value(assistant_event.get("value"))
        text = f"사용자: {user_value}\nAI: {assistant_value}"

        return {
            "kind": "conversation",
            "text": self._truncate(text),
            "search_text": f"{user_value}\n{assistant_value}",
            "user_text": user_value,
            "assistant_text": assistant_value,
            "user_source": str(user_event.get("source", "")),
            "assistant_source": str(assistant_event.get("source", "")),
            #20260626_kpopmodder: Keep raw evidence for log-only recall tracing.
            "raw_event_ids": self._event_ids(user_event, assistant_event),
            "raw_line_hashes": self._line_hashes(user_event, assistant_event),
            "created_at": (
                user_event.get("created_at")
                or assistant_event.get("created_at")
                or ""
            ),
            "created_ts": (
                user_event.get("created_ts")
                or assistant_event.get("created_ts")
                or 0.0
            ),
        }

    def _make_user_episode(self, user_event):
        value = self._clean_value(user_event.get("value"))
        return {
            "kind": "user_message",
            "text": self._truncate(f"사용자: {value}"),
            "search_text": value,
            "user_text": value,
            "user_source": str(user_event.get("source", "")),
            #20260626_kpopmodder: Preserve source-row trace without exposing it to prompts.
            "raw_event_ids": self._event_ids(user_event),
            "raw_line_hashes": self._line_hashes(user_event),
            "created_at": user_event.get("created_at") or "",
            "created_ts": user_event.get("created_ts") or 0.0,
        }

    def _make_screen_episode(self, event):
        value = self._clean_value(event.get("value"))
        return {
            "kind": "screen_observation",
            "text": self._truncate(f"화면 관찰: {value}"),
            "search_text": value,
            "source": str(event.get("source", "")),
            #20260626_kpopmodder: Screen recall can be traced back to raw_events.sqlite3.
            "raw_event_ids": self._event_ids(event),
            "raw_line_hashes": self._line_hashes(event),
            "silent": (
                str(event.get("event_type", ""))
                == self.SILENT_SCREEN_EVENT
            ),
            "created_at": event.get("created_at") or "",
            "created_ts": event.get("created_ts") or 0.0,
        }

    def _clean_value(self, value):
        return " ".join(str(value or "").strip().split())

    def _truncate(self, value):
        value = str(value or "")
        if len(value) <= self.max_value_chars:
            return value
        return value[: self.max_value_chars] + "...[truncated]"

    def _event_ids(self, *events):#20260626_kpopmodder: Copy raw SQLite ids into consolidated episodes.
        ids = []
        for event in events:
            raw_event_id = event.get("raw_event_id")
            if raw_event_id is None:
                continue
            ids.append(raw_event_id)
        return ids

    def _line_hashes(self, *events):#20260626_kpopmodder: Keep JSONL hash evidence for integrity/debugging.
        hashes = []
        for event in events:
            line_hash = str(event.get("raw_line_hash", "") or "")
            if line_hash:
                hashes.append(line_hash)
        return hashes
