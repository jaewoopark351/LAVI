#20260718_kpopmodder: Added this module to keep Silero probabilities separate from speech segment decisions.
from __future__ import annotations

from collections import deque
import math
from typing import Any

import numpy as np


class VadStateMachine:
    #20260718_kpopmodder: Converts frame probabilities into loose speech chunks with pre/post roll.
    def __init__(self, settings: Any):
        self.settings = settings
        self.sample_rate = int(settings.sample_rate)
        self.frame_samples = int(settings.frame_samples)
        self.pre_roll_samples = self._ms_to_samples(settings.pre_roll_ms)
        self.post_roll_samples = self._ms_to_samples(settings.post_roll_ms)
        self.min_speech_samples = self._ms_to_samples(
            settings.min_speech_duration_ms
        )
        self.min_silence_samples = self._ms_to_samples(
            settings.min_silence_duration_ms
        )
        self.max_pre_roll_frames = max(
            1,
            int(math.ceil(self.pre_roll_samples / float(self.frame_samples))),
        )
        self.reset()

    @property
    def recording(self) -> bool:
        return self._recording

    def reset(self) -> None:
        self._pre_roll_frames = deque(maxlen=self.max_pre_roll_frames)
        self._frames = []
        self._confirm_frames = 0
        self._recording = False
        self._ending = False
        self._silence_samples = 0
        self._post_roll_remaining = 0

    def process_frame(self, frame: Any, speech_probability: float):
        audio_frame = np.asarray(frame, dtype=np.float32).reshape(-1)
        if audio_frame.shape[0] != self.frame_samples:
            raise ValueError(
                "VAD state frame size mismatch: "
                f"expected={self.frame_samples}, actual={audio_frame.shape[0]}"
            )

        probability = float(speech_probability)
        self._pre_roll_frames.append(audio_frame.copy())

        if not self._recording:
            if probability >= float(self.settings.speech_threshold):
                self._confirm_frames += 1
            else:
                self._confirm_frames = 0

            if self._confirm_frames < int(self.settings.start_confirm_frames):
                return None

            self._recording = True
            self._ending = False
            self._silence_samples = 0
            self._post_roll_remaining = 0
            self._frames = self._trim_pre_roll(list(self._pre_roll_frames))
            return None

        if self._ending:
            self._frames.append(audio_frame.copy())
            self._post_roll_remaining -= self.frame_samples
            if self._post_roll_remaining <= 0:
                return self._finalize()
            return None

        self._frames.append(audio_frame.copy())
        if probability < float(self.settings.release_threshold):
            self._silence_samples += self.frame_samples
        else:
            self._silence_samples = 0

        if self._silence_samples < self.min_silence_samples:
            return None

        self._ending = True
        self._post_roll_remaining = self.post_roll_samples
        if self._post_roll_remaining <= 0:
            return self._finalize()
        return None

    def flush(self):
        if not self._recording:
            self.reset()
            return None
        return self._finalize()

    def _finalize(self):
        audio = (
            np.concatenate(self._frames, axis=0).astype(np.float32, copy=False)
            if self._frames
            else np.zeros(0, dtype=np.float32)
        )
        self.reset()
        if audio.shape[0] < self.min_speech_samples:
            return None
        return audio.reshape(-1, 1)

    def _trim_pre_roll(self, frames):
        if not frames:
            return []
        audio = np.concatenate(frames, axis=0).astype(np.float32, copy=False)
        if self.pre_roll_samples > 0:
            audio = audio[-self.pre_roll_samples :]
        return [
            audio[index : index + self.frame_samples].copy()
            for index in range(0, audio.shape[0], self.frame_samples)
            if audio[index : index + self.frame_samples].shape[0]
        ]

    def _ms_to_samples(self, value_ms: int | float) -> int:
        return int(round(self.sample_rate * float(value_ms) / 1000.0))
