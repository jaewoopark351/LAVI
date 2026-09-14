#20260914_kpopmodder: Describe one complete immutable runtime FIND catalog.
from __future__ import annotations

from dataclasses import dataclass
import hashlib
import re
import unicodedata

IDENTIFIER = re.compile(r"[a-z0-9_.-]+:[a-z0-9_./-]+\Z", re.ASCII)
DIGEST = re.compile(r"[0-9a-f]{64}\Z", re.ASCII)


def safe_text(value: object, maximum: int, *, empty: bool = False) -> bool:
    return bool(type(value) is str and (empty or bool(value))
                and len(value) <= maximum and value == unicodedata.normalize("NFC", value)
                and not any(unicodedata.category(char) in {"Cc", "Cf", "Cs", "Zl", "Zp"} for char in value))


@dataclass(frozen=True, slots=True)
class FindCatalogRecord:
    target_kind: str
    canonical_target_id: str
    translation_key: str
    korean_name: str
    english_name: str
    eligibility: str = ""

    @classmethod
    def decode(cls, value: object):
        if type(value) is not dict:
            return None
        keys = {"target_kind", "canonical_target_id", "translation_key", "korean_name", "english_name"}
        kind = value.get("target_kind")
        expected = keys | ({"eligibility"} if kind == "entity" else set())
        if set(value) != expected or type(kind) is not str or kind not in {"entity", "block", "item"}:
            return None
        identifier = value.get("canonical_target_id")
        if not safe_text(identifier, 128) or IDENTIFIER.fullmatch(identifier) is None:
            return None
        if not safe_text(value.get("translation_key"), 256):
            return None
        if not all(safe_text(value.get(key), 256, empty=True) for key in ("korean_name", "english_name")):
            return None
        eligibility = value.get("eligibility", "")
        if kind == "entity" and (type(eligibility) is not str or eligibility not in {"mob", "non_mob", "unknown"}):
            return None
        return cls(kind, identifier, value["translation_key"], value["korean_name"], value["english_name"], eligibility)

    def digest_line(self) -> str:
        return "\t".join((self.target_kind, self.canonical_target_id, self.translation_key,
                           self.korean_name, self.english_name, self.eligibility)) + "\n"


def catalog_digest(records: tuple[FindCatalogRecord, ...]) -> str:
    ordered = sorted(records, key=lambda record: (record.target_kind, record.canonical_target_id))
    return hashlib.sha256("".join(record.digest_line() for record in ordered).encode("utf-8")).hexdigest()


@dataclass(frozen=True, slots=True)
class FindCatalogSnapshot:
    session_id: str
    connection_generation: int
    resource_generation: int
    catalog_digest: str
    records: tuple[FindCatalogRecord, ...]
