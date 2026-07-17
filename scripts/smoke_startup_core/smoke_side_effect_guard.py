#20260717_kpopmodder: Split smoke startup helper from legacy multi-class script for AGENTS 29.1.

import contextlib
import importlib
from pathlib import Path
import sys
import traceback
from unittest import mock

from .side_effect_attempt import SideEffectAttempt
from .smoke_timer import SmokeTimer

def _install_original_import_module_marker():
    if "_lavi_original_import_module" not in importlib.__dict__:
        importlib.__dict__["_lavi_original_import_module"] = importlib.import_module

class SmokeSideEffectGuard:
    def __init__(self, counters, disabled_optional_modules=None):
        self.counters = counters
        self.disabled_optional_modules = set(disabled_optional_modules or [])
        self.stack = contextlib.ExitStack()

    def __enter__(self):
        _install_original_import_module_marker()
        try:
            self._patch_existing("requests.get", "network_attempts")
            self._patch_existing("requests.request", "network_attempts")
            self._patch_existing(
                "requests.sessions.Session.request",
                "network_attempts",
            )
            self._patch_existing("urllib.request.urlopen", "network_attempts")
            self._patch_subprocess_popen()
            self._patch_existing(
                "tts_core.ffmpeg_manager.ensure_ffmpeg_exists",
                "ffmpeg_download_attempts",
            )
            self._patch_existing(
                "tts_core.ffmpeg_manager.download_ffmpeg",
                "download_attempts",
            )
            self._patch_existing(
                "LAV_utils.download_and_extract_zip",
                "download_attempts",
            )
            self._patch_loaded_model_factories()
            self.stack.enter_context(
                mock.patch("app_core.runtime_lifecycle.threading.Timer", SmokeTimer)
            )
            self._patch_existing("socket.create_connection", "network_attempts")
            self.stack.enter_context(
                mock.patch("importlib.import_module", self._guarded_import_module)
            )
            return self
        except Exception:
            self.stack.close()
            raise

    def __exit__(self, exc_type, exc, traceback):
        self.stack.close()
        return False

    def _patch_existing(self, target, counter_name):
        try:
            self.stack.enter_context(
                mock.patch(target, self._blocked(counter_name, target))
            )
        except (AttributeError, ModuleNotFoundError):
            return

    def _blocked(self, counter_name, label):
        def blocked(*args, **kwargs):
            current = getattr(self.counters, counter_name)
            setattr(self.counters, counter_name, current + 1)
            stack = "".join(traceback.format_stack(limit=8))
            self.counters.record_stack(label, stack)
            raise SideEffectAttempt(
                f"{label} attempted during smoke\n{stack}"
            )

        return blocked

    def _patch_subprocess_popen(self):
        import subprocess

        original_popen = subprocess.Popen
        guard = self

        class GuardedPopen(original_popen):
            #20260716_kpopmodder: Preserve Popen's class API, including Popen[bytes], while guarding calls.
            def __init__(self, *args, **kwargs):
                if guard._called_from_python_platform_probe():
                    super().__init__(*args, **kwargs)
                    return
                current = guard.counters.external_process_attempts
                guard.counters.external_process_attempts = current + 1
                stack = "".join(traceback.format_stack(limit=8))
                guard.counters.record_stack("subprocess.Popen", stack)
                raise SideEffectAttempt(
                    "subprocess.Popen attempted during smoke\n"
                    f"{stack}"
                )

        self.stack.enter_context(mock.patch("subprocess.Popen", GuardedPopen))

    def _called_from_python_platform_probe(self):
        for frame in traceback.extract_stack(limit=10):
            if Path(frame.filename).name == "platform.py":
                return True
        return False

    def _guarded_import_module(self, name, package=None):
        if name in self.disabled_optional_modules:
            self.counters.disabled_optional_imports += 1
            raise SideEffectAttempt(
                f"disabled optional module import attempted: {name}"
            )
        return self._original_import_module(name, package=package)

    def _patch_loaded_model_factories(self):
        for module_name in ("transformers", "huggingface_hub"):
            module = sys.modules.get(module_name)
            if module is None:
                continue
            for attribute_name in (
                "from_pretrained",
                "pipeline",
                "hf_hub_download",
                "snapshot_download",
            ):
                if hasattr(module, attribute_name):
                    self.stack.enter_context(
                        mock.patch.object(
                            module,
                            attribute_name,
                            self._blocked("model_load_attempts", attribute_name),
                        )
                    )

    @property
    def _original_import_module(self):
        return importlib.__dict__.get("_lavi_original_import_module")
