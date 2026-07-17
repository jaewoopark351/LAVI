#20260626_kpopmodder: Central GPU placement config for multi-GPU plugin pinning.
import copy
import json
import os
import re

from core.logger import log_print
from core.paths import get_lavi_paths


class GPUDeviceManager:#20260626_kpopmodder
    DEFAULT_CONFIG = {
        "default_device": "cuda:0",
        "VoiceInput": {
            "device": "cuda:0",
        },
        "ScreenVision": {
            "device": "cuda:0",
            "device_map": "auto",
            "max_memory": {},
        },
        "GPTSoVITS": {
            "cuda_visible_devices": "",
        },
        "preflight": {
            "enabled": True,
            "check_cuda_available": True,
            "check_device_exists": True,
            "check_vram": True,
            "min_free_vram_mb": 3000,
            "warn_only": True,
        },
        "startup_vram_preflight": {
            "enabled": True,
            "screenvision_min_free_gib": 6.0,
            "delay_screenvision_auto_load": True,
        },
    }

    def __init__(self, config_path=None):
        self.paths = get_lavi_paths()
        self.config_path = str(config_path or self.paths.config_path("gpu_device_config.json"))
        self.config_loaded_from_file = False
        self.config_load_failed = False
        self.config = self._load_config()
        self._torch = None
        self._torch_checked = False
        self._gpu_log_done = False
        self._startup_vram_preflight_result = None

    def _load_config(self):
        default_config = copy.deepcopy(self.DEFAULT_CONFIG)

        if not os.path.exists(self.config_path):
            log_print(
                "[GPUDeviceManager] config not found. using safe defaults: "
                f"{self.config_path}"
            )
            return default_config

        try:
            with open(self.config_path, "r", encoding="utf-8") as file:
                loaded_config = json.load(file)

            if not isinstance(loaded_config, dict):
                raise ValueError("top-level JSON value must be an object")

            self.config_loaded_from_file = True
            merged_config = self._merge_config(default_config, loaded_config)
            if (
                "preflight" not in loaded_config
                and isinstance(loaded_config.get("startup_vram_preflight"), dict)
            ):
                merged_config["preflight"] = (
                    self._preflight_config_from_legacy(
                        merged_config.get("preflight", {}),
                        loaded_config.get("startup_vram_preflight", {}),
                    )
                )
            return merged_config

        except Exception as e:
            self.config_load_failed = True
            log_print(
                f"[GPUDeviceManager] WARNING: config load failed: {e}. "
                "using safe defaults."
            )
            return default_config

    def _merge_config(self, base_config, override_config):
        merged = copy.deepcopy(base_config)

        for key, value in override_config.items():
            if isinstance(value, dict) and isinstance(merged.get(key), dict):
                merged[key].update(value)
            else:
                merged[key] = value

        return merged

    def _preflight_config_from_legacy(self, base_config, legacy_config):
        preflight_config = copy.deepcopy(base_config)
        if not isinstance(preflight_config, dict):
            preflight_config = {}

        min_free_gib = self._float_value(
            legacy_config.get("screenvision_min_free_gib"),
            6.0,
        )
        delay_auto_load = self._bool_value(
            legacy_config.get("delay_screenvision_auto_load"),
            True,
        )

        preflight_config.update({
            "enabled": self._bool_value(legacy_config.get("enabled"), True),
            "check_cuda_available": True,
            "check_device_exists": True,
            "check_vram": True,
            "min_free_vram_mb": int(min_free_gib * 1024),
            #20260628_kpopmodder: Legacy delay=true means strict VRAM gating.
            "warn_only": not delay_auto_load,
        })
        return preflight_config

    def _get_torch(self):
        if self._torch_checked:
            return self._torch

        self._torch_checked = True

        try:
            import torch

            self._torch = torch
        except Exception as e:
            self._torch = None
            log_print(
                f"[GPUDeviceManager] WARNING: torch import failed. "
                f"fallback=cpu ({e})"
            )

        return self._torch

    def _cuda_device_count(self):
        torch = self._get_torch()

        if torch is None:
            return 0

        try:
            if not torch.cuda.is_available():
                return 0

            return int(torch.cuda.device_count())
        except Exception as e:
            log_print(
                f"[GPUDeviceManager] WARNING: CUDA query failed. "
                f"fallback=cpu ({e})"
            )
            return 0

    def log_detected_gpus(self):
        if self._gpu_log_done:
            return

        self._gpu_log_done = True
        torch = self._get_torch()

        if torch is None:
            return

        count = self._cuda_device_count()

        if count <= 0:
            log_print("[GPUDeviceManager] WARNING: CUDA unavailable. fallback=cpu")
            return

        for index in range(count):
            try:
                name = torch.cuda.get_device_name(index)
            except Exception as e:
                name = f"unknown ({e})"

            log_print(f"[GPUDeviceManager] detected: {index} = {name}")

    def _fallback_device(self):
        count = self._cuda_device_count()

        if count > 0:
            return "cuda:0"

        return "cpu"

    def _parse_cuda_index(self, device):
        if isinstance(device, int):
            return device

        text = str(device).strip().lower()

        if text == "cuda":
            return 0

        match = re.fullmatch(r"cuda:(\d+)", text)
        if match:
            return int(match.group(1))

        if text.isdigit():
            return int(text)

        return None

    def _get_section_value(self, plugin_name, key, default=None):
        if (
            not self.config_loaded_from_file
            and not self.config_load_failed
            and default is not None
        ):
            return default

        section = self.config.get(plugin_name, {})

        if isinstance(section, dict) and key in section:
            return section.get(key)

        if default is not None:
            return default

        default_section = self.DEFAULT_CONFIG.get(plugin_name, {})
        if isinstance(default_section, dict):
            return default_section.get(key)

        return None

    def validate_device(self, device, plugin_name=None):
        requested = str(device or self.config.get("default_device") or "").strip()

        if not requested:
            requested = "cuda:0"

        if requested.lower() == "cpu":
            return "cpu"

        cuda_index = self._parse_cuda_index(requested)

        if cuda_index is None:
            fallback = self._fallback_device()
            log_print(
                f"[GPUDeviceManager] WARNING: {plugin_name or 'device'} "
                f"requested unsupported device {requested!r}. fallback={fallback}"
            )
            return fallback

        count = self._cuda_device_count()

        if count <= 0:
            log_print("[GPUDeviceManager] WARNING: CUDA unavailable. fallback=cpu")
            return "cpu"

        if cuda_index >= count:
            fallback = self._fallback_device()
            log_print(
                f"[GPUDeviceManager] WARNING: {plugin_name or 'device'} "
                f"requested cuda:{cuda_index} but only {count} CUDA device(s) "
                f"available. fallback={fallback}"
            )
            return fallback

        return f"cuda:{cuda_index}"

    def _device_map_value(self, value, plugin_name):
        if isinstance(value, str) and value.strip().lower() == "cpu":
            return "cpu"

        cuda_index = self._parse_cuda_index(value)

        if cuda_index is None:
            return value

        count = self._cuda_device_count()

        if count <= 0:
            log_print("[GPUDeviceManager] WARNING: CUDA unavailable. fallback=cpu")
            return "cpu"

        if cuda_index >= count:
            fallback = self._fallback_device()
            log_print(
                f"[GPUDeviceManager] WARNING: {plugin_name} requested "
                f"device_map cuda:{cuda_index} but only {count} CUDA device(s) "
                f"available. fallback={fallback}"
            )
            fallback_index = self._parse_cuda_index(fallback)
            return "cpu" if fallback_index is None else fallback_index

        return cuda_index

    def get_device(self, plugin_name, default=None):#20260626_kpopmodder
        self.log_detected_gpus()
        device = self._get_section_value(plugin_name, "device", default)
        resolved_device = self.validate_device(device, plugin_name=plugin_name)
        log_print(f"[GPUDeviceManager] {plugin_name} -> {resolved_device}")
        return resolved_device

    def get_device_map(self, plugin_name, default=None):#20260626_kpopmodder
        self.log_detected_gpus()
        device_map = self._get_section_value(plugin_name, "device_map", default)

        if device_map in (None, ""):
            return None

        if isinstance(device_map, str):
            text = device_map.strip()
            if text.lower() in ("none", "null"):
                return None
            return text

        if not isinstance(device_map, dict):
            log_print(
                f"[GPUDeviceManager] WARNING: {plugin_name} device_map must be "
                f"dict/string. ignored: {device_map!r}"
            )
            return default

        return {
            str(key): self._device_map_value(value, plugin_name)
            for key, value in device_map.items()
        }

    def get_max_memory(self, plugin_name, default=None):#20260626_kpopmodder
        max_memory = self._get_section_value(plugin_name, "max_memory", default)

        if max_memory in (None, ""):
            return None

        if not isinstance(max_memory, dict):
            log_print(
                f"[GPUDeviceManager] WARNING: {plugin_name} max_memory must be "
                f"dict. ignored: {max_memory!r}"
            )
            return default

        count = self._cuda_device_count()
        resolved = {}

        for key, value in max_memory.items():
            key_text = str(key).strip()

            if key_text.isdigit():
                key_value = int(key_text)

                if count <= 0 or key_value >= count:
                    log_print(
                        f"[GPUDeviceManager] WARNING: {plugin_name} "
                        f"max_memory key {key_value} is not available. ignored."
                    )
                    continue
            else:
                key_value = key

            resolved[key_value] = value

        return resolved or None

    def _parse_cuda_visible_devices(self, value):#20260626_kpopmodder
        if value is None:
            return [], []

        valid = []
        invalid = []

        for item in str(value).split(","):
            text = item.strip()

            if not text:
                continue

            if text.isdigit():
                valid.append(text)
            else:
                invalid.append(text)

        return valid, invalid

    def validate_cuda_visible_devices(
        self,
        value,
        plugin_name,
        default=None,
    ):#20260626_kpopmodder
        if value in (None, ""):
            return None

        count = self._cuda_device_count()

        if count <= 0:
            log_print(
                f"[GPUDeviceManager] WARNING: CUDA unavailable. ignore "
                f"{plugin_name}.cuda_visible_devices={value!r}"
            )
            return None

        requested, malformed = self._parse_cuda_visible_devices(value)
        valid = []
        invalid = list(malformed)

        for device_text in requested:
            device_index = int(device_text)

            if device_index < count:
                valid.append(str(device_index))
            else:
                invalid.append(device_text)

        if invalid:
            log_print(
                f"[GPUDeviceManager] WARNING: invalid CUDA_VISIBLE_DEVICES "
                f"for {plugin_name}: {invalid}, device_count={count}"
            )

        if valid:
            resolved = ",".join(valid)
            log_print(
                f"[GPUDeviceManager] {plugin_name} -> "
                f"CUDA_VISIBLE_DEVICES={resolved}"
            )
            return resolved

        if default not in (None, "") and str(default).strip() != str(value).strip():
            fallback = self.validate_cuda_visible_devices(
                default,
                plugin_name,
                default=None,
            )

            if fallback is not None:
                log_print(
                    f"[GPUDeviceManager] {plugin_name} "
                    f"CUDA_VISIBLE_DEVICES fallback={fallback}"
                )
                return fallback

        log_print(
            f"[GPUDeviceManager] WARNING: no valid CUDA_VISIBLE_DEVICES "
            f"for {plugin_name}: {value!r}. fallback=None"
        )
        return None

    def get_cuda_visible_devices(self, plugin_name, default=None):#20260626_kpopmodder
        value = self._get_section_value(
            plugin_name,
            "cuda_visible_devices",
            default,
        )

        return self.validate_cuda_visible_devices(
            value,
            plugin_name,
            default=default,
        )

    def apply_cuda_visible_devices(
        self,
        env,
        plugin_name,
        value,
        default=None,
        validate=True,
    ):#20260717_kpopmodder: Centralize child-process CUDA_VISIBLE_DEVICES env mutation.
        if validate:
            resolved = self.validate_cuda_visible_devices(
                value,
                plugin_name,
                default=default,
            )
        else:
            resolved = str(value or "").strip()
        if resolved is None:
            return None
        if not resolved:
            return None
        env["CUDA_VISIBLE_DEVICES"] = resolved
        return resolved

    def log_startup_vram_preflight(
        self,
        plugin_names=("VoiceInput", "ScreenVision", "GPTSoVITS"),
        force=False,
    ):#20260627_kpopmodder: Warn when fragile realtime plugins share a low-VRAM GPU.
        if self._startup_vram_preflight_result is not None and not force:
            return dict(self._startup_vram_preflight_result)

        config = self._preflight_config()
        enabled = self._bool_value(config.get("enabled"), True)
        check_cuda_available = self._bool_value(
            config.get("check_cuda_available"),
            True,
        )
        check_device_exists = self._bool_value(
            config.get("check_device_exists"),
            True,
        )
        check_vram = self._bool_value(config.get("check_vram"), True)
        min_free_vram_mb = self._int_value(
            config.get("min_free_vram_mb"),
            3000,
        )
        warn_only = self._bool_value(config.get("warn_only"), True)
        result = {
            "enabled": enabled,
            "check_cuda_available": check_cuda_available,
            "check_device_exists": check_device_exists,
            "check_vram": check_vram,
            "min_free_vram_mb": min_free_vram_mb,
            "warn_only": warn_only,
            "cuda_available": False,
            "device_count": 0,
            "placements": {},
            "device_checks": {},
            "shared_gpu_plugins": {},
            "free_vram_mb": {},
            "free_vram_gib": {},
            "warnings": [],
            "screenvision_auto_load_allowed": True,
            "screenvision_delay_reason": "",
        }
        log_print(
            "[GPUDeviceManager] preflight "
            f"enabled={self._bool_text(enabled)} "
            f"warn_only={self._bool_text(warn_only)} "
            f"check_cuda_available={self._bool_text(check_cuda_available)} "
            f"check_device_exists={self._bool_text(check_device_exists)} "
            f"check_vram={self._bool_text(check_vram)} "
            f"min_free_vram_mb={min_free_vram_mb}"
        )
        if not enabled:
            self._startup_vram_preflight_result = result
            return dict(result)

        torch = self._get_torch()
        if torch is not None:
            try:
                result["cuda_available"] = bool(torch.cuda.is_available())
            except Exception as e:
                self._log_preflight_warning(
                    result,
                    f"CUDA availability query failed: {e}",
                    warn_only,
                )

            if result["cuda_available"]:
                try:
                    result["device_count"] = int(torch.cuda.device_count())
                except Exception as e:
                    self._log_preflight_warning(
                        result,
                        f"CUDA device_count query failed: {e}",
                        warn_only,
                    )

        if check_cuda_available:
            log_print(
                "[GPUDeviceManager] preflight CUDA "
                f"available={self._bool_text(result['cuda_available'])} "
                f"device_count={result['device_count']}"
            )
            if not result["cuda_available"] or result["device_count"] <= 0:
                self._log_preflight_warning(
                    result,
                    "CUDA unavailable or no CUDA device detected",
                    warn_only,
                )

        placements = {}
        if check_device_exists or check_vram:
            for plugin_name in plugin_names or []:
                check = self._preflight_plugin_check(plugin_name)
                result["device_checks"][plugin_name] = dict(check)
                placements[plugin_name] = check.get("cuda_index")
                if check_device_exists:
                    requested = check.get("requested")
                    resolved = check.get("resolved")
                    status = check.get("status")
                    log_print(
                        "[GPUDeviceManager] preflight "
                        f"{plugin_name} requested={requested!r} "
                        f"resolved={resolved!r} status={status}"
                    )
                    warning = check.get("warning")
                    if warning:
                        self._log_preflight_warning(
                            result,
                            f"{plugin_name}: {warning}",
                            warn_only,
                        )

        result["placements"] = dict(placements)
        if placements:
            #20260630_kpopmodder: Keep GPU assignment summary visible in startup logs.
            assignment_text = ", ".join(
                f"{plugin_name}->"
                f"{self._preflight_assignment_text(cuda_index)}"
                for plugin_name, cuda_index in placements.items()
            )
            log_print(
                "[GPUDeviceManager] preflight summary "
                f"min_free_vram_mb={min_free_vram_mb} "
                f"assignments={assignment_text}"
            )

        shared = {}
        for plugin_name, cuda_index in placements.items():
            if cuda_index is None:
                continue
            shared.setdefault(cuda_index, []).append(plugin_name)
        shared = {
            cuda_index: names
            for cuda_index, names in shared.items()
            if len(names) >= 2
        }
        result["shared_gpu_plugins"] = dict(shared)

        for cuda_index, names in shared.items():
            log_print(
                "[GPUDeviceManager] WARNING: shared GPU placement "
                f"cuda:{cuda_index} -> {', '.join(names)}. "
                "Monitor VRAM before enabling ScreenVision auto-load."
            )

        if check_vram:
            screenvision_index = placements.get("ScreenVision")
            for cuda_index in sorted({
                index for index in placements.values() if index is not None
            }):
                free_mb = self._free_vram_mb(cuda_index)
                if free_mb is not None:
                    free_gib = float(free_mb) / 1024
                    result["free_vram_mb"][cuda_index] = free_mb
                    result["free_vram_gib"][cuda_index] = free_gib
                    status = "ok"
                    if free_mb < min_free_vram_mb:
                        status = "warning"
                    log_print(
                        "[GPUDeviceManager] preflight VRAM "
                        f"cuda:{cuda_index} free_vram_mb={free_mb} "
                        f"threshold_mb={min_free_vram_mb} status={status}"
                    )
                    if free_mb < min_free_vram_mb:
                        reason = (
                            f"cuda:{cuda_index} free VRAM "
                            f"{free_mb}MB < {min_free_vram_mb}MB"
                        )
                        self._log_preflight_warning(
                            result,
                            reason,
                            warn_only,
                        )
                        if cuda_index == screenvision_index and not warn_only:
                            result["screenvision_delay_reason"] = reason
                            result["screenvision_auto_load_allowed"] = False
                            log_print(
                                "[GPUDeviceManager] WARNING: "
                                f"{reason}. "
                                "ScreenVision Auto Watch model load will be "
                                "delayed."
                            )
                else:
                    self._log_preflight_warning(
                        result,
                        f"cuda:{cuda_index} free VRAM unavailable",
                        warn_only,
                    )

        self._startup_vram_preflight_result = result
        return dict(result)

    def _preflight_config(self):
        config = self.config.get("preflight", {})
        if isinstance(config, dict):
            return config
        return {}

    def _preflight_plugin_check(self, plugin_name):
        if plugin_name == "GPTSoVITS":
            requested = self._get_section_value(
                plugin_name,
                "cuda_visible_devices",
                None,
            )
            if requested is None:
                requested = self.DEFAULT_CONFIG.get(plugin_name, {}).get(
                    "cuda_visible_devices"
                )
            resolved = self.validate_cuda_visible_devices(
                requested,
                plugin_name,
            )
            cuda_index = self._gpt_sovits_cuda_index()
            warning = ""
            if cuda_index is None:
                warning = (
                    f"no valid CUDA_VISIBLE_DEVICES for requested={requested!r}"
                )
            return {
                "requested": requested,
                "resolved": resolved,
                "cuda_index": cuda_index,
                "status": "warning" if warning else "ok",
                "warning": warning,
            }

        requested = self._get_section_value(plugin_name, "device", None)
        if requested is None:
            default_section = self.DEFAULT_CONFIG.get(plugin_name, {})
            if isinstance(default_section, dict):
                requested = default_section.get("device")

        resolved = self.validate_device(requested, plugin_name=plugin_name)
        requested_index = self._parse_cuda_index(requested)
        resolved_index = None
        if str(resolved).lower() != "cpu":
            resolved_index = self._parse_cuda_index(resolved)

        warning = ""
        if requested_index is not None and requested_index != resolved_index:
            warning = f"requested cuda:{requested_index} resolved {resolved}"
        elif (
            requested_index is None
            and str(requested).strip().lower() != "cpu"
        ):
            warning = f"unsupported device {requested!r} resolved {resolved}"

        return {
            "requested": requested,
            "resolved": resolved,
            "cuda_index": resolved_index,
            "status": "warning" if warning else "ok",
            "warning": warning,
        }

    def _log_preflight_warning(self, result, message, warn_only):
        result["warnings"].append(message)
        log_print(
            "[GPUDeviceManager] WARNING: preflight "
            f"{message}. warn_only={self._bool_text(warn_only)}"
        )

    def _preflight_assignment_text(self, cuda_index):#20260630_kpopmodder
        if cuda_index is None:
            return "cpu/none"
        return f"cuda:{cuda_index}"

    def _free_vram_mb(self, cuda_index):
        torch = self._get_torch()
        if torch is None:
            return None
        try:
            if not torch.cuda.is_available():
                return None
            free_bytes, _total_bytes = torch.cuda.mem_get_info(cuda_index)
            return int(free_bytes / (1024 ** 2))
        except Exception:
            return None

    def _bool_text(self, value):
        return "true" if bool(value) else "false"

    def _int_value(self, value, default):
        try:
            return int(value)
        except Exception:
            return int(default)

    def should_delay_screenvision_auto_load(self):#20260627_kpopmodder
        result = self.log_startup_vram_preflight()
        return not bool(result.get("screenvision_auto_load_allowed", True))

    def screenvision_auto_load_warning(self):#20260627_kpopmodder
        result = self.log_startup_vram_preflight()
        reason = str(result.get("screenvision_delay_reason", "") or "")
        if reason:
            return (
                "[ScreenVision] Auto Watch delayed by GPU VRAM preflight: "
                f"{reason}. Free VRAM or adjust config\\gpu_device_config.json."
            )
        return (
            "[ScreenVision] Auto Watch delayed by GPU VRAM preflight. "
            "Free VRAM or adjust config\\gpu_device_config.json."
        )

    def _plugin_cuda_placements(self, plugin_names):
        placements = {}
        for plugin_name in plugin_names or []:
            if plugin_name == "GPTSoVITS":
                placements[plugin_name] = self._gpt_sovits_cuda_index()
                continue
            device = self._get_section_value(plugin_name, "device", None)
            if device is None:
                default_section = self.DEFAULT_CONFIG.get(plugin_name, {})
                if isinstance(default_section, dict):
                    device = default_section.get("device")
            placements[plugin_name] = self._validated_cuda_index(
                device,
                plugin_name,
            )
        return placements

    def _validated_cuda_index(self, device, plugin_name):
        resolved = self.validate_device(device, plugin_name=plugin_name)
        if str(resolved).lower() == "cpu":
            return None
        return self._parse_cuda_index(resolved)

    def _gpt_sovits_cuda_index(self):
        value = self._get_section_value("GPTSoVITS", "cuda_visible_devices", None)
        if value is None:
            value = self.DEFAULT_CONFIG.get("GPTSoVITS", {}).get(
                "cuda_visible_devices"
            )
        count = self._cuda_device_count()
        if count <= 0:
            return None
        valid, _invalid = self._parse_cuda_visible_devices(value)
        for device_text in valid:
            device_index = int(device_text)
            if device_index < count:
                return device_index
        return None

    def _free_vram_gib(self, cuda_index):
        torch = self._get_torch()
        if torch is None:
            return None
        try:
            if not torch.cuda.is_available():
                return None
            free_bytes, _total_bytes = torch.cuda.mem_get_info(cuda_index)
            return float(free_bytes) / (1024 ** 3)
        except Exception:
            return None

    def _bool_value(self, value, default):
        if value is None:
            return bool(default)
        return str(value).strip().lower() not in {"0", "false", "no", "off"}

    def _float_value(self, value, default):
        try:
            return float(value)
        except Exception:
            return float(default)

    def log_startup_summary(self, plugin_names):#20260626_kpopmodder
        self.log_detected_gpus()

        for plugin_name in plugin_names:
            if plugin_name == "GPTSoVITS":
                self.get_cuda_visible_devices(plugin_name)
            else:
                self.get_device(plugin_name)


gpu_device_manager = GPUDeviceManager()
