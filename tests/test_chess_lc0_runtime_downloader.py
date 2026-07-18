#20260718_kpopmodder: Tests Chess LC0 runtime restore without network access.
import shutil
import unittest
import uuid
from pathlib import Path

from plugins.Chess.chess_core.lc0_runtime_downloader import (
    DEFAULT_LC0_DOWNLOAD_REPO_ID,
    LC0RuntimeDownloader,
)


PROJECT_ROOT = Path(__file__).resolve().parents[1]
TEST_TMP_ROOT = PROJECT_ROOT / ".test_tmp"


class ChessLC0RuntimeDownloaderTests(unittest.TestCase):
    def _make_temp_dir(self) -> Path:
        TEST_TMP_ROOT.mkdir(exist_ok=True)
        path = TEST_TMP_ROOT / f"chess_lc0_runtime_{uuid.uuid4().hex}"
        path.mkdir()
        self.addCleanup(lambda: shutil.rmtree(path, ignore_errors=True))
        return path

    def _write_minimum_runtime(self, runtime_dir: Path) -> None:
        for relative_path in (
            "BT4-1024x15x32h-swa-6147500-policytune-332.pb.gz",
            "cublas64_12.dll",
            "cublasLt64_12.dll",
            "cudart64_12.dll",
            "lc0.exe",
            "mimalloc-override.dll",
            "mimalloc-redirect.dll",
        ):
            path = runtime_dir / relative_path
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(b"stub")

    def test_downloader_skips_when_runtime_is_ready(self):
        def fail_download(**kwargs):
            raise AssertionError("download should not be called")

        runtime_dir = self._make_temp_dir() / "lc0"
        self._write_minimum_runtime(runtime_dir)

        result = LC0RuntimeDownloader(fail_download).ensure_runtime(str(runtime_dir))

        self.assertTrue(result["ok"])
        self.assertFalse(result["downloaded"])
        self.assertEqual("runtime_present", result["skipped"])

    def test_downloader_restores_missing_runtime_files(self):
        calls = []

        def fake_download(**kwargs):
            calls.append(kwargs)
            Path(kwargs["destination"]).write_bytes(b"stub")

        runtime_dir = self._make_temp_dir() / "lc0"
        result = LC0RuntimeDownloader(fake_download).ensure_runtime(
            str(runtime_dir),
            repo_id=DEFAULT_LC0_DOWNLOAD_REPO_ID,
            revision="main",
            subdir="lc0-v0.32.1-windows-gpu-nvidia-cuda12",
            files=("lc0.exe", "BT4-1024x15x32h-swa-6147500-policytune-332.pb.gz"),
            required_files=(
                "lc0.exe",
                "BT4-1024x15x32h-swa-6147500-policytune-332.pb.gz",
            ),
        )

        self.assertTrue(result["ok"])
        self.assertTrue(result["downloaded"])
        self.assertEqual(2, len(calls))
        self.assertEqual(
            "https://huggingface.co/jaewoopark96/lc0-v0.32.1-windows-gpu-nvidia-cuda12/resolve/main/lc0-v0.32.1-windows-gpu-nvidia-cuda12/lc0.exe?download=true",
            calls[0]["url"],
        )

    def test_downloader_reports_disabled_missing_runtime(self):
        runtime_dir = self._make_temp_dir() / "lc0"

        result = LC0RuntimeDownloader().ensure_runtime(
            str(runtime_dir),
            enabled=False,
        )

        self.assertFalse(result["ok"])
        self.assertFalse(result["downloaded"])
        self.assertEqual("lc0_runtime_download_disabled", result["error"])

    def test_downloader_rejects_unsafe_runtime_file_path(self):
        runtime_dir = self._make_temp_dir() / "lc0"

        result = LC0RuntimeDownloader().ensure_runtime(
            str(runtime_dir),
            files=("..\\outside.dll",),
        )

        self.assertFalse(result["ok"])
        self.assertEqual("lc0_runtime_download_failed", result["error"])


if __name__ == "__main__":
    unittest.main()
