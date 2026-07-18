#20260718_kpopmodder: Added this module to run Silero VAD ONNX on CPU without the silero-vad package.
from __future__ import annotations

from pathlib import Path
from typing import Any

import numpy as np


class SileroOnnxVad:
    #20260718_kpopmodder: Reuses one ONNX Runtime session and recurrent state across microphone frames.
    def __init__(
        self,
        model_path: str,
        *,
        sample_rate: int = 16000,
        frame_samples: int = 512,
        inter_op_num_threads: int = 1,
        intra_op_num_threads: int = 1,
    ):
        self.model_path = str(Path(model_path).resolve())
        self.sample_rate = int(sample_rate)
        self.frame_samples = int(frame_samples)
        self.context_samples = 64 if self.sample_rate == 16000 else 32
        self._last_batch_size = 1
        self._last_sample_rate = self.sample_rate
        self._session = self._create_session(
            inter_op_num_threads=inter_op_num_threads,
            intra_op_num_threads=intra_op_num_threads,
        )
        self.reset()

    def reset(self) -> None:
        self._state = np.zeros((2, 1, 128), dtype=np.float32)
        self._context = np.zeros((1, self.context_samples), dtype=np.float32)
        self._last_batch_size = 1
        self._last_sample_rate = self.sample_rate

    def warm_up(self) -> None:
        self.reset()
        self.predict(np.zeros(self.frame_samples, dtype=np.float32))
        self.reset()

    def predict(self, frame: Any) -> float:
        audio = np.asarray(frame, dtype=np.float32).reshape(1, -1)
        if audio.shape[1] != self.frame_samples:
            raise ValueError(
                "Silero VAD frame size mismatch: "
                f"expected={self.frame_samples}, actual={audio.shape[1]}"
            )

        model_input = np.concatenate((self._context, audio), axis=1)
        outputs = self._session.run(
            None,
            {
                "input": model_input.astype(np.float32, copy=False),
                "state": self._state.astype(np.float32, copy=False),
                "sr": np.array(self.sample_rate, dtype=np.int64),
            },
        )
        probability = float(np.asarray(outputs[0]).reshape(-1)[0])
        self._state = np.asarray(outputs[1], dtype=np.float32)
        self._context = model_input[:, -self.context_samples:].astype(
            np.float32,
            copy=False,
        )
        self._last_sample_rate = self.sample_rate
        return probability

    def _create_session(
        self,
        *,
        inter_op_num_threads: int,
        intra_op_num_threads: int,
    ):
        try:
            import onnxruntime
        except Exception as e:
            raise RuntimeError(f"onnxruntime_unavailable: {e}") from e

        providers = list(onnxruntime.get_available_providers())
        if "CPUExecutionProvider" not in providers:
            raise RuntimeError(
                "onnxruntime_cpu_provider_unavailable: "
                f"available={providers}"
            )

        options = onnxruntime.SessionOptions()
        options.inter_op_num_threads = max(1, int(inter_op_num_threads))
        options.intra_op_num_threads = max(1, int(intra_op_num_threads))
        return onnxruntime.InferenceSession(
            self.model_path,
            providers=["CPUExecutionProvider"],
            sess_options=options,
        )
