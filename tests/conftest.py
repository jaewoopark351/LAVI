#20260716_kpopmodder: Keep pytest temp artifacts inside the repository boundary.
import os
import tempfile
import uuid
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
TEST_TEMP_ROOT = PROJECT_ROOT / "test" / "test_Isolation" / "tmp"
TEST_TEMP_ROOT.mkdir(parents=True, exist_ok=True)

os.environ["TEMP"] = str(TEST_TEMP_ROOT)
os.environ["TMP"] = str(TEST_TEMP_ROOT)
tempfile.tempdir = str(TEST_TEMP_ROOT)


def _repo_mkdtemp(suffix=None, prefix=None, dir=None):
    """Create pytest temp dirs with normal write permissions on Windows."""
    base_dir = Path(dir) if dir is not None else TEST_TEMP_ROOT
    base_dir.mkdir(parents=True, exist_ok=True)
    safe_prefix = "tmp" if prefix is None else prefix
    safe_suffix = "" if suffix is None else suffix
    while True:
        candidate = base_dir / f"{safe_prefix}{uuid.uuid4().hex}{safe_suffix}"
        try:
            candidate.mkdir()
            return str(candidate)
        except FileExistsError:
            continue


class _RepoTemporaryDirectory:
    #20260716_kpopmodder: Avoid Python tempfile's Windows ACL issue in this repo.
    def __init__(self, suffix=None, prefix=None, dir=None, ignore_cleanup_errors=False):
        self.name = _repo_mkdtemp(suffix=suffix, prefix=prefix, dir=dir)

    def __enter__(self):
        return self.name

    def __exit__(self, exc_type, exc, tb):
        self.cleanup()
        return False

    def cleanup(self):
        #20260716_kpopmodder: Do not auto-delete test trees; .gitignore hides them.
        return None


tempfile.mkdtemp = _repo_mkdtemp
tempfile.TemporaryDirectory = _RepoTemporaryDirectory
