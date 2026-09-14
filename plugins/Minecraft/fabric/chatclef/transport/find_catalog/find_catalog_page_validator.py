#20260914_kpopmodder: Validate the closed FIND catalog wire shape and finite payload limits.
import json

from .find_catalog_page import FindCatalogPage
from .find_catalog_snapshot import DIGEST, FindCatalogRecord


class FindCatalogPageValidator:
    MAX_RECORDS = 50_000
    MAX_PAGES = 256
    MAX_PAGE_RECORDS = 256
    MAX_PAGE_BYTES = 64 * 1024
    MAX_TOTAL_BYTES = 8 * 1024 * 1024
    MAX_GENERATION = 2**63 - 1

    def decode(self, payload, generation):
        keys = {"event", "catalog_version", "resource_generation", "connection_generation", "catalog_digest",
                "page_index", "page_count", "record_count", "complete", "records"}
        if type(payload) is not dict or set(payload) != keys:
            return None, "invalid_page_keys"
        integers = tuple(payload.get(key) for key in ("catalog_version", "resource_generation", "connection_generation", "page_index", "page_count", "record_count"))
        if (any(type(value) is not int for value in integers) or integers[0] != 1
                or not 0 <= integers[1] <= self.MAX_GENERATION or integers[2] != generation
                or not 1 <= integers[4] <= self.MAX_PAGES or not 0 <= integers[3] < integers[4]
                or not 1 <= integers[5] <= self.MAX_RECORDS or payload["complete"] is not True
                or type(payload["catalog_digest"]) is not str or DIGEST.fullmatch(payload["catalog_digest"]) is None
                or type(payload["records"]) is not list or not 1 <= len(payload["records"]) <= self.MAX_PAGE_RECORDS):
            return None, "invalid_page_values"
        try:
            size = len(json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8"))
        except (TypeError, ValueError, UnicodeError):
            return None, "unencodable_page"
        if size > self.MAX_PAGE_BYTES:
            return None, "page_byte_cap"
        records = tuple(FindCatalogRecord.decode(record) for record in payload["records"])
        if any(record is None for record in records):
            return None, "invalid_record"
        return FindCatalogPage(integers[1], payload["catalog_digest"], integers[3], integers[4], integers[5], records, size), None

    def invalidation_reason(self, payload, generation):
        if (type(payload) is not dict or set(payload) != {"event", "catalog_version", "resource_generation", "connection_generation", "reason"}
                or type(payload["catalog_version"]) is not int or payload["catalog_version"] != 1
                or type(payload["resource_generation"]) is not int or not 0 <= payload["resource_generation"] <= self.MAX_GENERATION
                or type(payload["connection_generation"]) is not int or payload["connection_generation"] != generation
                or type(payload["reason"]) is not str or payload["reason"] not in {"CATALOG_REPLACED", "CATALOG_INCOMPLETE"}):
            return "invalid_invalidation"
        return payload["reason"]
