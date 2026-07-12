#20260707_kpopmodder: Transformers Whisper backend replaces faster-whisper/CTranslate2 for VoiceInput.
import wave
from typing import Any, Dict, List, Optional, Tuple

from core.logger import log_print

from .base import STTInfo, STTResult, STTSegment


class TransformersWhisperBackend:
    def __init__(
        self,
        model_id: str = "openai/whisper-large-v3-turbo",
        device: str = "cuda:0",
        torch_dtype: str = "auto",
        language: str = "ko",
    ):
        self.model_id = model_id or "openai/whisper-large-v3-turbo"
        self.device = device or "cpu"
        self.torch_dtype_name = torch_dtype or "auto"
        self.language = language or "ko"
        self._pipeline = None
        self._load_pipeline()

    def _load_pipeline(self):
        try:
            import torch
            from transformers import AutoModelForSpeechSeq2Seq, AutoProcessor
            from transformers import pipeline
            import transformers.pipelines.automatic_speech_recognition as asr_pipeline
        except Exception as e:
            raise RuntimeError(
                "Transformers Whisper backend requires torch and transformers."
            ) from e

        self._disable_torchcodec_for_asr_pipeline(asr_pipeline)
        torch_dtype = self._resolve_torch_dtype(torch)
        pipeline_device = self._pipeline_device()

        log_print(
            "[TransformersWhisperBackend] loading "
            f"model={self.model_id}, device={self.device}, "
            f"torch_dtype={self.torch_dtype_name}"
        )

        model = AutoModelForSpeechSeq2Seq.from_pretrained(
            self.model_id,
            torch_dtype=torch_dtype,
            low_cpu_mem_usage=True,
            use_safetensors=True,
        )

        model.to(self.device)
        processor = AutoProcessor.from_pretrained(self.model_id)

        self._pipeline = pipeline(
            "automatic-speech-recognition",
            model=model,
            tokenizer=processor.tokenizer,
            feature_extractor=processor.feature_extractor,
            torch_dtype=torch_dtype,
            device=pipeline_device,
            chunk_length_s=30,
        )

    def _disable_torchcodec_for_asr_pipeline(self, asr_pipeline):
        #20260707_kpopmodder: Transformers 5.x imports torchcodec in ASR preprocess even for decoded arrays.
        try:
            asr_pipeline.is_torchcodec_available = lambda: False
            log_print(
                "[TransformersWhisperBackend] disabled torchcodec ASR "
                "preprocess path; using soundfile decoded wav arrays."
            )
        except Exception as e:
            log_print(
                "[TransformersWhisperBackend] torchcodec disable failed: "
                f"{e}"
            )

    def _resolve_torch_dtype(self, torch):
        dtype_name = str(self.torch_dtype_name or "auto").strip().lower()

        if dtype_name in ("", "auto"):
            dtype_name = "float16" if self.device.startswith("cuda") else "float32"

        if not self.device.startswith("cuda") and dtype_name == "float16":
            log_print(
                "[TransformersWhisperBackend] float16 requested on CPU. "
                "using float32."
            )
            dtype_name = "float32"

        dtype_map = {
            "float16": torch.float16,
            "fp16": torch.float16,
            "float32": torch.float32,
            "fp32": torch.float32,
            "bfloat16": torch.bfloat16,
            "bf16": torch.bfloat16,
        }

        if dtype_name not in dtype_map:
            fallback = "float16" if self.device.startswith("cuda") else "float32"
            log_print(
                "[TransformersWhisperBackend] unsupported torch_dtype "
                f"{self.torch_dtype_name!r}. using {fallback}."
            )
            dtype_name = fallback

        self.torch_dtype_name = dtype_name
        return dtype_map[dtype_name]

    def _pipeline_device(self):
        if self.device == "cpu":
            return -1

        if self.device == "cuda":
            return 0

        if self.device.startswith("cuda:"):
            try:
                return int(self.device.split(":", 1)[1])
            except Exception:
                return 0

        return self.device

    def transcribe(
        self,
        audio_path: str,
        language: Optional[str] = None,
        task: str = "transcribe",
    ) -> STTResult:
        if self._pipeline is None:
            self._load_pipeline()

        active_language = language if language not in (None, "") else self.language
        generate_kwargs: Dict[str, Any] = {"task": task or "transcribe"}

        if active_language and str(active_language).lower() != "auto":
            generate_kwargs["language"] = active_language

        audio_input, audio_duration = self._load_audio_input(audio_path)
        output = self._pipeline(
            audio_input,
            return_timestamps=True,
            generate_kwargs=generate_kwargs,
        )

        return self._to_result(
            output=output,
            audio_duration=audio_duration,
            language=active_language or "ko",
        )

    def _to_result(
        self,
        output: Any,
        audio_duration: float,
        language: str,
    ) -> STTResult:
        if isinstance(output, dict):
            text = str(output.get("text") or "").strip()
            raw_chunks = output.get("chunks") or []
        else:
            text = str(output or "").strip()
            raw_chunks = []

        segments = self._segments_from_chunks(raw_chunks, audio_duration)

        if not segments and text:
            segments = [
                STTSegment(
                    text=text,
                    start=0.0,
                    end=max(audio_duration, 0.0),
                    avg_logprob=0.0,
                    no_speech_prob=0.0,
                    avg_logprob_available=False,
                    no_speech_prob_available=False,
                )
            ]

        info = STTInfo(
            language=language or "ko",
            language_probability=1.0,
            avg_logprob=0.0,
            no_speech_prob=0.0,
            language_probability_available=False,
            avg_logprob_available=False,
            no_speech_prob_available=False,
        )

        return STTResult(
            text=text,
            segments=segments,
            language=info.language,
            language_probability=info.language_probability,
            avg_logprob=info.avg_logprob,
            no_speech_prob=info.no_speech_prob,
            language_probability_available=info.language_probability_available,
            avg_logprob_available=info.avg_logprob_available,
            no_speech_prob_available=info.no_speech_prob_available,
            info=info,
        )

    def _segments_from_chunks(
        self,
        chunks: List[Dict[str, Any]],
        audio_duration: float,
    ) -> List[STTSegment]:
        segments = []

        for chunk in chunks:
            if not isinstance(chunk, dict):
                continue

            text = str(chunk.get("text") or "").strip()
            if not text:
                continue

            start, end = self._parse_timestamp(chunk.get("timestamp"))

            if end <= start:
                end = max(audio_duration, start)

            segments.append(
                STTSegment(
                    text=text,
                    start=start,
                    end=end,
                    avg_logprob=0.0,
                    no_speech_prob=0.0,
                    avg_logprob_available=False,
                    no_speech_prob_available=False,
                )
            )

        return segments

    def _parse_timestamp(self, value: Any) -> Tuple[float, float]:
        if isinstance(value, (list, tuple)) and len(value) >= 2:
            return self._to_float(value[0]), self._to_float(value[1])

        return 0.0, 0.0

    def _to_float(self, value: Any) -> float:
        try:
            if value is None:
                return 0.0
            return float(value)
        except Exception:
            return 0.0

    def _load_audio_input(self, audio_path: str) -> Tuple[Dict[str, Any], float]:
        try:
            import soundfile as sf
        except Exception as e:
            raise RuntimeError(
                "Transformers Whisper backend requires soundfile to decode wav "
                "without torchcodec."
            ) from e

        audio_data, sampling_rate = sf.read(
            audio_path,
            dtype="float32",
            always_2d=False,
        )

        if getattr(audio_data, "ndim", 1) > 1:
            audio_data = audio_data.mean(axis=1)

        duration = 0.0
        try:
            if sampling_rate > 0:
                duration = float(len(audio_data)) / float(sampling_rate)
        except Exception:
            duration = self._audio_duration(audio_path)

        #20260707_kpopmodder: Passing decoded arrays avoids Transformers using torchcodec DLL loading on Windows.
        return {
            "array": audio_data,
            "sampling_rate": sampling_rate,
        }, duration

    def _audio_duration(self, audio_path: str) -> float:
        try:
            with wave.open(audio_path, "rb") as wav_file:
                frame_count = wav_file.getnframes()
                frame_rate = wav_file.getframerate()
                if frame_rate <= 0:
                    return 0.0
                return float(frame_count) / float(frame_rate)
        except Exception:
            return 0.0
