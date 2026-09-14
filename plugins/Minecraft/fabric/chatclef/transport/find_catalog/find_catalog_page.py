#20260914_kpopmodder: Hold one validated immutable catalog page without transport or lifecycle state.
from dataclasses import dataclass
from .find_catalog_snapshot import FindCatalogRecord


@dataclass(frozen=True, slots=True)
class FindCatalogPage:
    resource_generation: int
    catalog_digest: str
    page_index: int
    page_count: int
    record_count: int
    records: tuple[FindCatalogRecord, ...]
    byte_size: int
