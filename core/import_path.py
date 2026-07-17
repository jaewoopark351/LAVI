#20260718_kpopmodder: Centralize explicit sys.path mutations used by legacy plugin imports.
from __future__ import annotations

import sys
from pathlib import Path


def ensure_import_path(path, *, prepend=False):
    resolved = str(Path(path).resolve())
    if resolved in sys.path:
        return resolved
    if prepend:
        sys.path.insert(0, resolved)
    else:
        sys.path.append(resolved)
    return resolved
