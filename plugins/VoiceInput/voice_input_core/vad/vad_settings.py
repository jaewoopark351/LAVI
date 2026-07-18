#20260718_kpopmodder: Added this module to keep VAD configuration in one class.
from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Mapping


DEFAULT_SILERO_VAD_MODEL_PATH = "models/vad/silero_vad_16k_op15.onnx"
DEFAULT_SILERO_VAD_MODEL_URL = (
    "https://raw.githubusercontent.com/snakers4/silero-vad/master/"
    "src/silero_vad/data/silero_vad_16k_op15.onnx"
)
DEFAULT_SILERO_VAD_SHA256 = (
    "7ed98ddbad84ccac4cd0aeb3099049280713df825c610a8ed34543318f1b2c49"
)


@dataclass(frozen=True)
class VadSettings:
    #20260718_kpopmodder: One object carries the loose Silero VAD thresholds used by VoiceInput.
    enabled: bool = True
    auto_download: bool = True
    legacy_fallback_enabled: bool = False
    model_path: str = DEFAULT_SILERO_VAD_MODEL_PATH
    model_url: str = DEFAULT_SILERO_VAD_MODEL_URL
    model_sha256: str = DEFAULT_SILERO_VAD_SHA256
    sample_rate: int = 16000
    frame_samples: int = 512
    speech_threshold: float = 0.45
    release_threshold: float = 0.30
    start_confirm_frames: int = 3
    min_speech_duration_ms: int = 128
    min_silence_duration_ms: int = 450
    pre_roll_ms: int = 256
    post_roll_ms: int = 160
    download_timeout_sec: float = 60.0
    inter_op_num_threads: int = 1
    intra_op_num_threads: int = 1

    @classmethod
    def from_config(cls, config: Mapping[str, Any] | None) -> "VadSettings":
        source = _vad_source(config or {})
        return cls(
            enabled=_bool_value(source, "vad_enabled", "enabled", True),
            auto_download=_bool_value(
                source,
                "vad_auto_download",
                "auto_download",
                True,
            ),
            legacy_fallback_enabled=_bool_value(
                source,
                "vad_legacy_fallback_enabled",
                "legacy_fallback_enabled",
                False,
            ),
            model_path=_text_value(
                source,
                "vad_model_path",
                "model_path",
                DEFAULT_SILERO_VAD_MODEL_PATH,
            ),
            model_url=_text_value(
                source,
                "vad_model_url",
                "model_url",
                DEFAULT_SILERO_VAD_MODEL_URL,
            ),
            model_sha256=_text_value(
                source,
                "vad_model_sha256",
                "model_sha256",
                DEFAULT_SILERO_VAD_SHA256,
            ),
            sample_rate=_int_value(source, "vad_sample_rate", "sample_rate", 16000),
            frame_samples=_int_value(
                source,
                "vad_frame_samples",
                "frame_samples",
                512,
            ),
            speech_threshold=_float_value(
                source,
                "vad_speech_threshold",
                "speech_threshold",
                0.45,
            ),
            release_threshold=_float_value(
                source,
                "vad_release_threshold",
                "release_threshold",
                0.30,
            ),
            start_confirm_frames=_int_value(
                source,
                "vad_start_confirm_frames",
                "start_confirm_frames",
                3,
            ),
            min_speech_duration_ms=_int_value(
                source,
                "vad_min_speech_duration_ms",
                "min_speech_duration_ms",
                128,
            ),
            min_silence_duration_ms=_int_value(
                source,
                "vad_min_silence_duration_ms",
                "min_silence_duration_ms",
                450,
            ),
            pre_roll_ms=_int_value(
                source,
                "vad_pre_roll_ms",
                "pre_roll_ms",
                256,
            ),
            post_roll_ms=_int_value(
                source,
                "vad_post_roll_ms",
                "post_roll_ms",
                160,
            ),
            download_timeout_sec=_float_value(
                source,
                "vad_download_timeout_sec",
                "download_timeout_sec",
                60.0,
            ),
            inter_op_num_threads=_int_value(
                source,
                "vad_inter_op_num_threads",
                "inter_op_num_threads",
                1,
            ),
            intra_op_num_threads=_int_value(
                source,
                "vad_intra_op_num_threads",
                "intra_op_num_threads",
                1,
            ),
        ).normalized()

    def normalized(self) -> "VadSettings":
        return VadSettings(
            enabled=bool(self.enabled),
            auto_download=bool(self.auto_download),
            legacy_fallback_enabled=bool(self.legacy_fallback_enabled),
            model_path=self.model_path or DEFAULT_SILERO_VAD_MODEL_PATH,
            model_url=self.model_url or DEFAULT_SILERO_VAD_MODEL_URL,
            model_sha256=self.model_sha256 or DEFAULT_SILERO_VAD_SHA256,
            sample_rate=16000,
            frame_samples=512,
            speech_threshold=_clamp_float(self.speech_threshold, 0.01, 0.99),
            release_threshold=_clamp_float(self.release_threshold, 0.01, 0.99),
            start_confirm_frames=max(1, int(self.start_confirm_frames)),
            min_speech_duration_ms=max(0, int(self.min_speech_duration_ms)),
            min_silence_duration_ms=max(0, int(self.min_silence_duration_ms)),
            pre_roll_ms=max(0, int(self.pre_roll_ms)),
            post_roll_ms=max(0, int(self.post_roll_ms)),
            download_timeout_sec=max(1.0, float(self.download_timeout_sec)),
            inter_op_num_threads=max(1, int(self.inter_op_num_threads)),
            intra_op_num_threads=max(1, int(self.intra_op_num_threads)),
        )


def _vad_source(config: Mapping[str, Any]) -> Mapping[str, Any]:
    nested = config.get("vad")
    if isinstance(nested, Mapping):
        merged = dict(config)
        merged.update(nested)
        return merged
    return config


def _bool_value(source: Mapping[str, Any], flat_key: str, nested_key: str, default: bool) -> bool:
    value = source.get(flat_key, source.get(nested_key, default))
    if isinstance(value, bool):
        return value
    return str(value).strip().lower() in {"1", "true", "yes", "on"}


def _text_value(source: Mapping[str, Any], flat_key: str, nested_key: str, default: str) -> str:
    return str(source.get(flat_key, source.get(nested_key, default)) or "").strip()


def _int_value(source: Mapping[str, Any], flat_key: str, nested_key: str, default: int) -> int:
    try:
        return int(source.get(flat_key, source.get(nested_key, default)))
    except (TypeError, ValueError):
        return int(default)


def _float_value(source: Mapping[str, Any], flat_key: str, nested_key: str, default: float) -> float:
    try:
        return float(source.get(flat_key, source.get(nested_key, default)))
    except (TypeError, ValueError):
        return float(default)


def _clamp_float(value: float, minimum: float, maximum: float) -> float:
    return min(maximum, max(minimum, float(value)))
