#20260626_kpopmodder: Build raw-adjacent derived-memory reference rows from append-only raw events.
import re
import time

from memory_core.memory_consolidator import MemoryConsolidator


EXCLUDED_SOURCES = {
    "memory_command",
    "memory_command_response",
}


def clean_text(text):
    return " ".join(str(text or "").strip().split())


def normalize_text(text):
    return re.sub(
        r"[^0-9a-zA-Z\uac00-\ud7a3]+",
        "",
        clean_text(text).lower(),
    )


def topic_key_for_text(text):
    normalized = clean_text(text).lower()
    topics = (
        ("youtube", ("youtube", "\uc720\ud29c\ube0c")),
        ("screenvision", ("screenvision", "screen vision")),
        ("gptsovits", ("gpt-sovits", "gptsovits", "tts")),
        ("voiceinput", ("voiceinput", "stt", "whisper", "microphone", "mic")),
        ("gpu", ("cuda", "gpu", "vram")),
        ("memory", ("memory", "raw_events", "derived_memory")),
        ("code", ("python", "vscode", "visual studio code", "code")),
        ("game", ("game", "steam", "ghost hunter")),
        ("browser", ("browser", "chrome", "edge")),
        ("video", ("video", "stream", "movie")),
    )
    for topic, keywords in topics:
        if any(keyword in normalized for keyword in keywords):
            return topic
    return ""


class DerivedMemoryBuilder:
    def __init__(
        self,
        consolidator=None,
        screen_similarity=0.82,
        conversation_similarity=0.90,
        min_normalized_chars=4,
    ):
        self.consolidator = consolidator or MemoryConsolidator()
        #20260627_kpopmodder: Keep fuzzy knobs for caller compatibility; raw fidelity mode does not merge rows.
        self.screen_similarity = float(screen_similarity)
        self.conversation_similarity = float(conversation_similarity)
        self.min_normalized_chars = max(1, int(min_normalized_chars))

    def build_items(self, raw_events):
        raw_events = list(raw_events or [])
        episodes = self.consolidator.consolidate(raw_events)
        items = []
        stats = {
            "raw_event_count": len(raw_events),
            "episode_count": len(episodes),
            "inserted_count": 0,
            "merged_duplicate_count": 0,
            "preserved_duplicate_row_count": 0,
            "skipped_noise_count": 0,
        }
        seen_normalized_keys = {}

        for episode in episodes:
            memory_item = self._episode_to_memory_item(episode)
            if memory_item is None:
                stats["skipped_noise_count"] += 1
                continue

            self._preserve_duplicate_normalized_key(
                memory_item,
                seen_normalized_keys,
                stats,
            )
            items.append(memory_item)

        self._apply_duplicate_group_metadata(items)
        stats["inserted_count"] = len(items)
        return items, stats

    def rebuild(
        self,
        raw_events,
        derived_store=None,
        clear=True,
        dry_run=False,
    ):
        items, stats = self.build_items(raw_events)

        if derived_store is None or dry_run:
            return stats

        if clear:
            derived_store.clear()

        inserted_count = 0
        for item in items:
            result = derived_store.upsert_memory(item)
            if result.get("action") == "inserted":
                inserted_count += 1

        stats["inserted_count"] = inserted_count
        return stats

    def _episode_to_memory_item(self, episode):
        if self._is_excluded_episode(episode):
            return None

        kind = clean_text(episode.get("kind"))
        if kind not in {
            "conversation",
            "user_message",
            "screen_observation",
        }:
            return None

        search_text = clean_text(episode.get("search_text"))
        text = clean_text(episode.get("text")) or search_text
        normalized_key = normalize_text(search_text)

        if self._is_noise(search_text, normalized_key):
            return None

        created_at = str(episode.get("created_at", "") or "")
        created_ts = self._float_or_zero(episode.get("created_ts"))
        now_ts = time.time()

        return {
            "kind": kind,
            "title": self._make_title(search_text),
            "summary": search_text,  #20260626_kpopmodder: Keep derived rows close to original episode text; do not synthesize summaries.
            "search_text": search_text,
            "normalized_key": normalized_key,
            "topic_key": topic_key_for_text(search_text),
            "source_event_count": 1,
            "duplicate_count": 0,
            "first_created_at": created_at,
            "last_created_at": created_at,
            "first_created_ts": created_ts,
            "last_created_ts": created_ts,
            "confidence": self._confidence_for_kind(kind),
            "metadata": {
                "derived_from": "raw_events",
                "episode_kind": kind,
                "source_event_ids": self._list_value(
                    episode.get("raw_event_ids"),
                ),
                "raw_line_hashes": self._list_value(
                    episode.get("raw_line_hashes"),
                ),
            },
            "created_ts": now_ts,
            "updated_ts": now_ts,
        }

    def _preserve_duplicate_normalized_key(
        self,
        memory_item,
        seen_normalized_keys,
        stats,
    ):
        key = (
            memory_item.get("kind", ""),
            memory_item.get("normalized_key", ""),
        )
        duplicate_index = seen_normalized_keys.get(key, 0) + 1
        seen_normalized_keys[key] = duplicate_index

        base_key = memory_item.get("normalized_key", "")
        metadata = memory_item.setdefault("metadata", {})
        if not isinstance(metadata, dict):
            metadata = {}
            memory_item["metadata"] = metadata
        metadata["base_normalized_key"] = base_key
        metadata["normalized_key_suffix_index"] = duplicate_index

        if duplicate_index <= 1:
            return

        memory_item["normalized_key"] = f"{base_key}__{duplicate_index}"
        stats["preserved_duplicate_row_count"] += 1

    def _apply_duplicate_group_metadata(self, items):
        groups = {}
        for item in items or []:
            metadata = item.get("metadata", {})
            if not isinstance(metadata, dict):
                metadata = {}
                item["metadata"] = metadata

            base_key = (
                metadata.get("base_normalized_key")
                or item.get("normalized_key", "")
            )
            metadata["base_normalized_key"] = base_key
            key = (item.get("kind", ""), base_key)
            groups.setdefault(key, []).append(item)

        for (_kind, base_key), group_items in groups.items():
            if not base_key or len(group_items) <= 1:
                continue

            source_event_ids = []
            raw_line_hashes = []
            first_created_at = ""
            last_created_at = ""
            first_created_ts = 0.0
            last_created_ts = 0.0

            for item in group_items:
                metadata = item.get("metadata", {})
                row_source_event_ids = self._list_value(
                    metadata.get("source_event_ids")
                )
                row_raw_event_ids = self._list_value(
                    metadata.get("raw_event_ids")
                )
                metadata["row_source_event_ids"] = list(row_source_event_ids)
                metadata["row_raw_event_ids"] = list(row_raw_event_ids)
                metadata["row_raw_line_hashes"] = self._list_value(
                    metadata.get("raw_line_hashes")
                )
                source_event_ids.extend(row_source_event_ids)
                source_event_ids.extend(row_raw_event_ids)
                raw_line_hashes.extend(metadata["row_raw_line_hashes"])

                item_first_ts = self._float_or_zero(
                    item.get("first_created_ts")
                )
                item_last_ts = self._float_or_zero(
                    item.get("last_created_ts")
                )
                if first_created_ts == 0.0 or (
                    item_first_ts > 0.0 and item_first_ts < first_created_ts
                ):
                    first_created_ts = item_first_ts
                    first_created_at = str(item.get("first_created_at", "") or "")
                if item_last_ts >= last_created_ts:
                    last_created_ts = item_last_ts
                    last_created_at = str(item.get("last_created_at", "") or "")

            source_event_ids = self._dedupe_list(source_event_ids)
            raw_line_hashes = self._dedupe_list(raw_line_hashes)
            source_event_count = len(group_items)
            duplicate_count = max(0, source_event_count - 1)

            for item in group_items:
                metadata = item.setdefault("metadata", {})
                if not isinstance(metadata, dict):
                    metadata = {}
                    item["metadata"] = metadata
                metadata["base_normalized_key"] = base_key
                metadata["base_normalized_key_count"] = source_event_count
                metadata["source_event_ids"] = list(source_event_ids)
                metadata["raw_event_ids"] = list(source_event_ids)
                metadata["raw_line_hashes"] = list(raw_line_hashes)
                item["source_event_count"] = source_event_count
                item["duplicate_count"] = duplicate_count
                item["first_created_at"] = first_created_at
                item["last_created_at"] = last_created_at
                item["first_created_ts"] = first_created_ts
                item["last_created_ts"] = last_created_ts

    #20260627_kpopmodder: Disabled merge helpers to keep derived rows raw-adjacent; normalized_key suffixes preserve duplicate source observations.
    # def _find_duplicate(self, memory_item, items):#20260626_kpopmodder
    #     if memory_item.get("kind") != "screen_observation":
    #         return None
    #
    #     for existing in items:
    #         if existing["kind"] != memory_item["kind"]:
    #             continue
    #
    #         if existing["normalized_key"] == memory_item["normalized_key"]:
    #             return existing
    #
    #     return None
    #
    # def _merge_memory_item(self, existing, incoming):#20260626_kpopmodder
    #     existing["source_event_count"] += incoming["source_event_count"]
    #     existing["duplicate_count"] += incoming["source_event_count"]
    #     existing["confidence"] = max(
    #         existing.get("confidence", 0.7),
    #         incoming.get("confidence", 0.7),
    #     )

    def _is_excluded_episode(self, episode):
        sources = (
            episode.get("source"),
            episode.get("user_source"),
            episode.get("assistant_source"),
        )
        return any(
            clean_text(source).lower() in EXCLUDED_SOURCES
            for source in sources
        )

    def _is_noise(self, search_text, normalized_key):
        if not search_text or len(normalized_key) < self.min_normalized_chars:
            return True

        if len(normalized_key) >= 20:
            unique_ratio = len(set(normalized_key)) / len(normalized_key)
            if unique_ratio < 0.12:
                return True

        if re.fullmatch(r"(.)\1{5,}", normalized_key):
            return True

        return False

    def _make_title(self, text):
        text = clean_text(text)
        if len(text) <= 80:
            return text
        return text[:77] + "..."

    def _confidence_for_kind(self, kind):
        if kind == "conversation":
            return 0.80
        if kind == "screen_observation":
            return 0.75
        return 0.65

    def _float_or_zero(self, value):
        try:
            return float(value)
        except Exception:
            return 0.0

    def _list_value(self, value):
        if isinstance(value, (list, tuple)):
            return list(value)
        if value is None:
            return []
        return [value]

    def _dedupe_list(self, values):
        result = []
        seen = set()
        for value in values or []:
            key = str(value)
            if key in seen:
                continue
            seen.add(key)
            result.append(value)
        return result
