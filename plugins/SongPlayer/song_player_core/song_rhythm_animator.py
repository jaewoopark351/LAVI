import threading
import time
import wave

from pydub.utils import audioop

from core.logger import log_print
from plugins.SongPlayer.song_player_core.song_mouth_animator import (
    SongMouthAnimator,
)


class SongRhythmAnimator:#20260629_kpopmodder
    #20260629_kpopmodder: Smooth rhythm tilt by easing FaceAngleZ out and back.
    RHYTHM_WAVE_MULTIPLIERS = (
        0.0,
        0.125,
        0.25,
        0.375,
        0.5,
        0.625,
        0.75,
        0.875,
        1.0,
        0.875,
        0.75,
        0.625,
        0.5,
        0.375,
        0.25,
        0.125,
        0.0,
    )
    RHYTHM_WAVE_STEP_SEC = 0.05

    def __init__(
        self,
        output_callback,
        stop_event,
        frame_ms=50,
        join_timeout=0.3,
    ):
        self.output_callback = output_callback
        self.stop_event = stop_event
        self.frame_ms = frame_ms
        self.join_timeout = join_timeout
        self.thread = None
        self.local_stop_event = threading.Event()

    def start(self, audio_path, song, mouth_path=None):
        self.stop()
        if song is None or not getattr(song, "rhythm_enabled", True):
            return

        self.local_stop_event.clear()
        self.thread = threading.Thread(
            target=self._animation_loop,
            args=(audio_path, song, mouth_path),
            daemon=True,
        )
        self.thread.start()

    def stop(self):
        try:
            self.local_stop_event.set()
            if (
                self.thread
                and self.thread.is_alive()
                and threading.current_thread() != self.thread
            ):
                try:
                    self.thread.join(timeout=self.join_timeout)
                except KeyboardInterrupt:
                    log_print("[SongPlayer rhythm] join skipped during Ctrl+C shutdown.")#20260630_kpopmodder
        except Exception as e:
            log_print(f"[SongPlayer rhythm] stop error: {e}")
        finally:
            self.send_reset()

    def _animation_loop(self, audio_path, song, mouth_path=None):
        try:
            beat_events = self.extract_beat_events(
                audio_path=audio_path,
                threshold=getattr(song, "rhythm_threshold", 0.35),
                min_interval_ms=getattr(song, "rhythm_min_interval_ms", 280),
            )
            if not beat_events:
                return

            vocal_volumes, vocal_fps = self.extract_vocal_volumes(
                mouth_path=mouth_path,
                mouth_gain=getattr(song, "mouth_gain", 1.0),
                mouth_floor=getattr(song, "mouth_floor", 0.05),
            )
            if not vocal_volumes:
                return

            angle_z = float(getattr(song, "rhythm_face_angle_z", 10.0) or 0.0)
            pulse_sec = float(getattr(song, "rhythm_pulse_ms", 160) or 160) / 1000.0
            wave_step_sec = max(
                self.RHYTHM_WAVE_STEP_SEC,
                pulse_sec / max(1, len(self.RHYTHM_WAVE_MULTIPLIERS) - 1),
            )
            start_time = time.perf_counter()
            direction = 1.0

            for beat_sec, strength in beat_events:
                if self.should_stop():
                    break

                elapsed_sec = time.perf_counter() - start_time
                wait_sec = max(0.0, beat_sec - elapsed_sec)
                if self.wait_or_stopped(wait_sec):
                    break

                if not self.is_vocal_volume_active(
                    volumes=vocal_volumes,
                    beat_sec=beat_sec,
                    target_fps=vocal_fps,
                    offset_ms=getattr(song, "offset_ms", 0),
                ):
                    continue

                #20260629_kpopmodder: Keep rhythm on FaceAngleZ so FaceAngleX/Y remain owned by loud-note expression.
                strength = max(0.0, min(1.0, float(strength or 0.0)))
                scale = direction * (0.65 + 0.35 * strength)
                if not self.send_rhythm_wave(
                    face_angle_z=angle_z * scale,
                    step_sec=wave_step_sec,
                ):
                    break
                direction *= -1.0

        except Exception as e:
            log_print(f"[SongPlayer rhythm] animation error: {e}")
        finally:
            self.send_reset()

    def extract_vocal_volumes(
        self,
        mouth_path,
        mouth_gain=1.0,
        mouth_floor=0.05,
    ):
        if not mouth_path:
            return [], 0.0

        try:
            with open(mouth_path, "rb") as file:
                audio_data = file.read()

            extractor = SongMouthAnimator(
                output_callback=lambda value: None,
                stop_event=None,
            )
            return (
                extractor.extract_volumes(
                    audio_data=audio_data,
                    mouth_gain=mouth_gain,
                    mouth_floor=mouth_floor,
                ),
                extractor.normalized_target_fps(),
            )

        except Exception as e:
            log_print(f"[SongPlayer rhythm] vocal gate extract error: {e}")
            return [], 0.0

    def is_vocal_volume_active(
        self,
        volumes,
        beat_sec,
        target_fps,
        offset_ms=0,
    ):
        #20260629_kpopmodder: Rhythm tilt only follows beats while Vocal_only mouth volume is active.
        if not volumes:
            return False

        try:
            beat_sec = max(0.0, float(beat_sec))
            target_fps = max(1.0, float(target_fps))
            offset_sec = float(offset_ms or 0) / 1000.0
        except Exception:
            return False

        if offset_sec > 0:
            vocal_sec = beat_sec - offset_sec
            if vocal_sec < 0:
                return False
            index = int(vocal_sec * target_fps)
        else:
            start_index = max(0, int(abs(offset_sec) * target_fps))
            index = int(beat_sec * target_fps) + start_index

        if index < 0 or index >= len(volumes):
            return False

        try:
            return float(volumes[index]) > 0.0
        except Exception:
            return False

    def extract_beat_events(
        self,
        audio_path,
        threshold=0.35,
        min_interval_ms=280,
    ):
        try:
            with wave.open(audio_path, "rb") as wav_file:
                channels = wav_file.getnchannels()
                sample_width = wav_file.getsampwidth()
                frame_rate = wav_file.getframerate()
                frame_count = wav_file.getnframes()

                if frame_rate <= 0 or frame_count <= 0:
                    return []

                frames_per_chunk = max(
                    1,
                    int(frame_rate * self.normalized_frame_ms() / 1000.0),
                )
                bytes_per_frame = channels * sample_width
                bytes_per_chunk = frames_per_chunk * bytes_per_frame
                raw_data = wav_file.readframes(frame_count)

            rms_values = []
            for i in range(0, len(raw_data), bytes_per_chunk):
                chunk_data = raw_data[i:i + bytes_per_chunk]
                if not chunk_data:
                    continue
                rms_values.append(audioop.rms(chunk_data, sample_width))

            if not rms_values:
                return []

            max_rms = max(rms_values)
            if max_rms <= 0:
                return []

            energies = [rms / max_rms for rms in rms_values]
            smooth = self.smooth_values(energies)
            novelty = self.novelty_values(smooth)
            max_novelty = max(novelty) if novelty else 0.0
            if max_novelty <= 0:
                return []

            threshold = max(0.0, min(1.0, float(threshold or 0.0)))
            min_interval_sec = max(0.12, float(min_interval_ms or 280) / 1000.0)
            frame_sec = self.normalized_frame_ms() / 1000.0

            events = []
            for index, value in enumerate(novelty):
                strength = value / max_novelty
                if strength < threshold:
                    continue
                if smooth[index] < 0.10:
                    continue
                if not self.is_local_peak(novelty, index):
                    continue

                beat_sec = index * frame_sec
                if events and beat_sec - events[-1][0] < min_interval_sec:
                    if strength > events[-1][1]:
                        events[-1] = (beat_sec, strength)
                    continue

                events.append((beat_sec, strength))

            if len(events) < 4:
                events = self.energy_peak_fallback(
                    energies=smooth,
                    min_interval_sec=min_interval_sec,
                    frame_sec=frame_sec,
                )

            log_print(
                "[SongPlayer rhythm] detected "
                f"beats={len(events)} threshold={threshold:.2f}"
            )
            return events

        except Exception as e:
            log_print(f"[SongPlayer rhythm] beat extract error: {e}")
            return []

    def smooth_values(self, values):
        result = []
        for index in range(len(values)):
            start = max(0, index - 1)
            end = min(len(values), index + 2)
            window = values[start:end]
            result.append(sum(window) / max(1, len(window)))
        return result

    def novelty_values(self, values):
        novelty = []
        for index, value in enumerate(values):
            start = max(0, index - 4)
            previous = values[start:index]
            baseline = sum(previous) / len(previous) if previous else value
            novelty.append(max(0.0, value - baseline))
        return novelty

    def energy_peak_fallback(self, energies, min_interval_sec, frame_sec):
        max_energy = max(energies) if energies else 0.0
        if max_energy <= 0:
            return []

        threshold = max_energy * 0.65
        events = []
        for index, value in enumerate(energies):
            if value < threshold:
                continue
            if not self.is_local_peak(energies, index):
                continue

            beat_sec = index * frame_sec
            strength = value / max_energy
            if events and beat_sec - events[-1][0] < min_interval_sec:
                if strength > events[-1][1]:
                    events[-1] = (beat_sec, strength)
                continue
            events.append((beat_sec, strength))
        return events

    def is_local_peak(self, values, index):
        previous_value = values[index - 1] if index > 0 else values[index]
        next_value = values[index + 1] if index + 1 < len(values) else values[index]
        return values[index] >= previous_value and values[index] >= next_value

    def normalized_frame_ms(self):
        try:
            return max(20.0, float(self.frame_ms))
        except Exception:
            return 50.0

    def should_stop(self):
        external_stop = False
        if self.stop_event is not None:
            external_stop = self.stop_event.is_set()
        return external_stop or self.local_stop_event.is_set()

    def wait_or_stopped(self, seconds):
        try:
            seconds = max(0.0, float(seconds))
        except Exception:
            seconds = 0.0

        end_time = time.time() + seconds
        while True:
            if self.should_stop():
                return True

            remaining = end_time - time.time()
            if remaining <= 0:
                break

            time.sleep(min(0.02, remaining))
        return self.should_stop()

    def send_rhythm_wave(self, face_angle_z, step_sec=RHYTHM_WAVE_STEP_SEC):
        #20260629_kpopmodder: Ease FaceAngleZ out and back so beat motion is not a single sharp snap.
        for index, multiplier in enumerate(self.RHYTHM_WAVE_MULTIPLIERS):
            self.send_rhythm(face_angle_z=face_angle_z * multiplier)
            if index == len(self.RHYTHM_WAVE_MULTIPLIERS) - 1:
                return True
            if self.wait_or_stopped(step_sec):
                return False
        return True

    def send_rhythm(self, face_angle_z):
        self.send_output({
            "rhythm_active": True,
            "face_angle_z": face_angle_z,
        })

    def send_reset(self):
        self.send_output({
            "rhythm_active": False,
            "face_angle_z": 0.0,
        })

    def send_output(self, payload):
        try:
            self.output_callback(payload)
        except Exception as e:
            log_print(f"[SongPlayer rhythm] output callback error: {e}")
