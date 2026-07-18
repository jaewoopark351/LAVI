#20260620_kpopmodder: VoiceInput helper modules are grouped under voice_input_core without changing behavior.
import io
from queue import Empty, Queue
import time

import numpy as np
import sounddevice as sd
import soundfile as sf

from core.logger import log_print
from .recorded_audio import RecordedAudio


class MicrophoneRecorder:#20260616_kpopmodder
    def __init__(
        self,
        input_device_index,
        mic_lock,
        liveTextbox,
        input_device_index_callback=None,#20260625_kpopmodder: Refresh Audio Settings input device before listening.
        vad_model=None,#20260718_kpopmodder: Optional Silero ONNX VAD gate for loose speech detection.
        vad_state_machine=None,#20260718_kpopmodder
        vad_required=False,#20260718_kpopmodder: Fail closed unless explicit legacy fallback is configured.
        vad_error="",
    ):
        self.input_device_index = input_device_index
        self.mic_lock = mic_lock
        self.liveTextbox = liveTextbox
        self.input_device_index_callback = input_device_index_callback
        self.vad_model = vad_model
        self.vad_state_machine = vad_state_machine
        self.vad_required = bool(vad_required)
        self.vad_error = str(vad_error or "")
        self._vad_unavailable_notice_printed = False
        self._last_vad_unavailable_log_time = 0.0

    def _input_device_exists(self, device_index):#20260625_kpopmodder
        if device_index is None:
            return True

        try:
            devices = sd.query_devices()
            device = devices[int(device_index)]
            return device["max_input_channels"] > 0
        except Exception as e:
            log_print(
                f"[MicrophoneRecorder] input device unavailable: "
                f"{device_index} ({e})"
            )#20260625_kpopmodder
            return False

    def get_input_device_index(self):#20260625_kpopmodder
        if self.input_device_index_callback is None:
            device_index = self.input_device_index
        else:
            try:
                device_index = self.input_device_index_callback()
            except Exception as e:
                device_index = self.input_device_index
                log_print(
                    "[MicrophoneRecorder] input device callback error: "
                    f"{e}. fallback to last input_device_index={device_index}"
                )#20260626_kpopmodder
            else:
                if device_index is None:
                    log_print(
                        "[MicrophoneRecorder] input device callback returned "
                        "None. using sounddevice system default input device."
                    )#20260626_kpopmodder
                    self.input_device_index = None
                    return None

        if not self._input_device_exists(device_index):
            log_print(
                "[MicrophoneRecorder] falling back to system default input device."
            )#20260625_kpopmodder
            self.input_device_index = None
            return None

        if device_index is None:
            log_print(
                "[MicrophoneRecorder] input_device_index=None means "
                "sounddevice system default input device."
            )#20260626_kpopmodder
            self.input_device_index = None
            return None

        self.input_device_index = device_index
        return device_index

    def listen(self, timeout, phrase_time_limit, prefix):
        if self.vad_model is not None and self.vad_state_machine is not None:
            return self._listen_with_vad(timeout, phrase_time_limit, prefix)

        if self.vad_required:
            self._report_vad_unavailable(prefix, timeout)
            return None

        return self._listen_with_legacy_energy(
            timeout,
            phrase_time_limit,
            prefix,
        )

    def _report_vad_unavailable(self, prefix, timeout):#20260718_kpopmodder
        message = (
            "Silero VAD unavailable; microphone input blocked to avoid "
            "sending noise to Whisper."
        )
        if self.vad_error:
            message = f"{message} error={self.vad_error}"

        if not self._vad_unavailable_notice_printed:
            self.liveTextbox.print(f"{prefix} microphone VAD unavailable.")
            self._vad_unavailable_notice_printed = True

        now = time.time()
        if now - self._last_vad_unavailable_log_time >= 10.0:
            log_print(f"[MicrophoneRecorder {prefix}] {message}")
            self._last_vad_unavailable_log_time = now

        try:
            wait_seconds = float(timeout)
        except (TypeError, ValueError):
            wait_seconds = 0.25
        time.sleep(min(max(wait_seconds, 0.25), 2.0))

    def _listen_with_vad(self, timeout, phrase_time_limit, prefix):
        with self.mic_lock:
            try:
                input_device_index = self.get_input_device_index()#20260625_kpopmodder
                samplerate = 16000
                channels = 1
                block_size = int(self.vad_state_machine.frame_samples)
                frame_queue = Queue()

                self.vad_model.reset()
                self.vad_state_machine.reset()

                self.liveTextbox.print(
                    f"{prefix} microphone listen start: "
                    f"input_device_index={input_device_index}, "
                    f"samplerate={samplerate}, "
                    f"channels={channels}, "
                    f"timeout={timeout}, "
                    f"phrase_time_limit={phrase_time_limit}, "
                    "detector=silero_vad"
                )#20260718_kpopmodder
                log_print(
                    f"[MicrophoneRecorder {prefix}] listen start: "
                    f"input_device_index={input_device_index}, "
                    f"samplerate={samplerate}, "
                    f"channels={channels}, "
                    f"timeout={timeout}, "
                    f"phrase_time_limit={phrase_time_limit}, "
                    "detector=silero_vad"
                )#20260718_kpopmodder

                if prefix == "normal":
                    self.liveTextbox.print("Say something!")

                def callback(indata, frame_count, time_info, status):
                    if status:
                        log_print(
                            f"[MicrophoneRecorder {prefix} sounddevice status] {status}"
                        )

                    audio_block = indata.copy()
                    if getattr(audio_block, "ndim", 1) > 1:
                        audio_block = audio_block.mean(axis=1)
                    frame_queue.put(
                        np.asarray(audio_block, dtype=np.float32).reshape(-1)
                    )

                start_wait_time = time.time()
                record_start_time = None
                speech_detection_logged = False
                segment_audio = None

                with sd.InputStream(
                    device=input_device_index,
                    samplerate=samplerate,
                    channels=channels,
                    dtype="float32",
                    blocksize=block_size,
                    callback=callback
                ):
                    while True:
                        now = time.time()

                        if (
                            not self.vad_state_machine.recording
                            and now - start_wait_time > timeout
                        ):
                            self.liveTextbox.print(
                                f"{prefix} microphone timeout: no speech detected."
                            )#20260625_kpopmodder
                            log_print(
                                f"[MicrophoneRecorder {prefix}] "
                                "timeout: no speech detected."
                            )#20260625_kpopmodder
                            return None

                        try:
                            frame = frame_queue.get(timeout=0.05)
                        except Empty:
                            if (
                                self.vad_state_machine.recording
                                and record_start_time is not None
                                and now - record_start_time >= phrase_time_limit
                            ):
                                segment_audio = self.vad_state_machine.flush()
                                break
                            continue

                        was_recording = self.vad_state_machine.recording
                        speech_probability = self.vad_model.predict(frame)
                        segment_audio = self.vad_state_machine.process_frame(
                            frame,
                            speech_probability,
                        )

                        if (
                            not was_recording
                            and self.vad_state_machine.recording
                        ):
                            record_start_time = time.time()
                            if not speech_detection_logged:
                                self.liveTextbox.print(
                                    f"{prefix} microphone speech detected: "
                                    f"vad_probability={speech_probability:.3f}"
                                )#20260718_kpopmodder
                                log_print(
                                    f"[MicrophoneRecorder {prefix}] "
                                    f"speech detected: "
                                    f"vad_probability={speech_probability:.3f}"
                                )#20260718_kpopmodder
                                speech_detection_logged = True

                        if (
                            self.vad_state_machine.recording
                            and record_start_time is not None
                            and time.time() - record_start_time >= phrase_time_limit
                        ):
                            segment_audio = self.vad_state_machine.flush()
                            break

                        if segment_audio is not None:
                            break

                if segment_audio is None or not len(segment_audio):
                    self.liveTextbox.print(
                        f"{prefix} microphone frames empty. Whisper skipped."
                    )#20260625_kpopmodder
                    log_print(
                        f"[MicrophoneRecorder {prefix}] "
                        "frames empty. Whisper skipped."
                    )#20260625_kpopmodder
                    return None

                return self._to_recorded_audio(segment_audio, samplerate)

            except Exception as e:
                self.vad_model.reset()
                self.vad_state_machine.reset()
                log_print(f"[MicrophoneRecorder {prefix} mic error] {e}")
                self.liveTextbox.print(f"{prefix} microphone error: {e}")#20260625_kpopmodder
                log_print(
                    f"[MicrophoneRecorder {prefix}] microphone error: {e}"
                )#20260625_kpopmodder
                time.sleep(0.5)
                return None

    def _listen_with_legacy_energy(self, timeout, phrase_time_limit, prefix):
        with self.mic_lock:
            try:
                input_device_index = self.get_input_device_index()#20260625_kpopmodder
                samplerate = 16000
                channels = 1
                block_duration = 0.1
                block_size = int(samplerate * block_duration)

                silence_limit = 0.8 if prefix == "normal" else 0.45
                energy_threshold = 0.015

                self.liveTextbox.print(
                    f"{prefix} microphone listen start: "
                    f"input_device_index={input_device_index}, "
                    f"samplerate={samplerate}, "
                    f"channels={channels}, "
                    f"timeout={timeout}, "
                    f"phrase_time_limit={phrase_time_limit}, "
                    f"energy_threshold={energy_threshold:.4f}"
                )#20260625_kpopmodder
                log_print(
                    f"[MicrophoneRecorder {prefix}] listen start: "
                    f"input_device_index={input_device_index}, "
                    f"samplerate={samplerate}, "
                    f"channels={channels}, "
                    f"timeout={timeout}, "
                    f"phrase_time_limit={phrase_time_limit}, "
                    f"energy_threshold={energy_threshold:.4f}"
                )#20260625_kpopmodder

                started = False
                start_wait_time = time.time()
                last_voice_time = time.time()
                record_start_time = None
                frames = []
                speech_detected_energy = None#20260625_kpopmodder
                speech_detection_logged = False#20260625_kpopmodder

                if prefix == "normal":
                    self.liveTextbox.print("Say something!")

                def callback(indata, frame_count, time_info, status):
                    nonlocal started
                    nonlocal last_voice_time
                    nonlocal record_start_time
                    nonlocal speech_detected_energy#20260625_kpopmodder

                    if status:
                        log_print(
                            f"[MicrophoneRecorder {prefix} sounddevice status] {status}"
                        )

                    audio_block = indata.copy()
                    energy = float(np.sqrt(np.mean(audio_block ** 2)))
                    now = time.time()

                    if not started:
                        if energy >= energy_threshold:
                            started = True
                            speech_detected_energy = energy#20260625_kpopmodder
                            record_start_time = now
                            last_voice_time = now
                            frames.append(audio_block)
                    else:
                        frames.append(audio_block)

                        if energy >= energy_threshold:
                            last_voice_time = now

                with sd.InputStream(
                    device=input_device_index,
                    samplerate=samplerate,
                    channels=channels,
                    dtype="float32",
                    blocksize=block_size,
                    callback=callback
                ):
                    while True:
                        now = time.time()

                        if not started and now - start_wait_time > timeout:
                            self.liveTextbox.print(
                                f"{prefix} microphone timeout: no speech detected."
                            )#20260625_kpopmodder
                            log_print(
                                f"[MicrophoneRecorder {prefix}] "
                                f"timeout: no speech detected."
                            )#20260625_kpopmodder
                            return None

                        if started:
                            if not speech_detection_logged:
                                energy_text = (
                                    "unknown"
                                    if speech_detected_energy is None
                                    else f"{speech_detected_energy:.4f}"
                                )#20260625_kpopmodder
                                self.liveTextbox.print(
                                    f"{prefix} microphone speech detected: "
                                    f"energy={energy_text}"
                                )#20260625_kpopmodder
                                log_print(
                                    f"[MicrophoneRecorder {prefix}] "
                                    f"speech detected: energy={energy_text}"
                                )#20260625_kpopmodder
                                speech_detection_logged = True

                            if record_start_time is not None and now - record_start_time >= phrase_time_limit:
                                break

                            if now - last_voice_time >= silence_limit:
                                break

                        time.sleep(0.03)

                if not frames:
                    self.liveTextbox.print(
                        f"{prefix} microphone frames empty. Whisper skipped."
                    )#20260625_kpopmodder
                    log_print(
                        f"[MicrophoneRecorder {prefix}] "
                        f"frames empty. Whisper skipped."
                    )#20260625_kpopmodder
                    return None

                audio_np = np.concatenate(frames, axis=0)

                return self._to_recorded_audio(audio_np, samplerate)

            except Exception as e:
                log_print(f"[MicrophoneRecorder {prefix} mic error] {e}")
                self.liveTextbox.print(f"{prefix} microphone error: {e}")#20260625_kpopmodder
                log_print(
                    f"[MicrophoneRecorder {prefix}] microphone error: {e}"
                )#20260625_kpopmodder
                time.sleep(0.5)
                return None

    def _to_recorded_audio(self, audio_np, samplerate):
        with io.BytesIO() as audio_buffer:
            sf.write(
                audio_buffer,
                audio_np,
                samplerate,
                format="WAV"
            )
            wav_bytes = audio_buffer.getvalue()

        return RecordedAudio(wav_bytes)
