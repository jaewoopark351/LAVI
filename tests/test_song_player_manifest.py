import io
import json
import os
import struct
import tempfile
import threading
import time
import unittest
import wave
import winsound
from unittest import mock

from plugins.SongPlayer.SongPlayer import SongPlayer
from plugins.SongPlayer.song_player_core.song_manifest import SongManifest
from plugins.SongPlayer.song_player_core.song_manifest import SongEntry
from plugins.SongPlayer.song_player_core.song_mouth_animator import (
    SongMouthAnimator,
)
from plugins.SongPlayer.song_player_core.song_playback_controller import (
    SongPlaybackController,
)
from plugins.SongPlayer.song_player_core.song_rhythm_animator import (
    SongRhythmAnimator,
)


#20260629_kpopmodder: Test helper for SongPlayer listener lock boundaries.
class _RecordingLock:
    def __init__(self):
        self.locked = False
        self.enter_count = 0

    def __enter__(self):
        self.locked = True
        self.enter_count += 1
        return self

    def __exit__(self, exc_type, exc, traceback):
        self.locked = False


#20260630_kpopmodder: Test helper for Ctrl+C during shutdown join waits.
class _JoinInterruptedThread:
    def __init__(self):
        self.join_count = 0

    def is_alive(self):
        return True

    def join(self, timeout=None):
        self.join_count += 1
        raise KeyboardInterrupt()


class SongPlayerListenerTests(unittest.TestCase):#20260629_kpopmodder
    def make_player(self):
        player = SongPlayer.__new__(SongPlayer)
        player.output_event_listeners = []
        player.expression_event_listeners = []
        player.output_lock = _RecordingLock()
        player.expression_lock = _RecordingLock()
        player.playback_controller = mock.Mock()
        player._shutdown = False
        return player

    def test_output_listener_callbacks_run_outside_lock(self):
        player = self.make_player()
        calls = []

        def listener(value):
            calls.append((value, player.output_lock.locked))

        player.add_output_event_listener(listener)
        player.add_output_event_listener(listener)

        player.send_output("1.5")

        self.assertEqual([(1.5, False)], calls)
        self.assertEqual([listener], player.output_event_listeners)
        self.assertGreaterEqual(player.output_lock.enter_count, 3)

    def test_expression_listener_callbacks_run_outside_lock(self):
        player = self.make_player()
        payload = {"active": True}
        calls = []

        def listener(expression):
            calls.append((expression, player.expression_lock.locked))

        player.add_expression_event_listener(listener)
        player.add_expression_event_listener(listener)

        player.send_expression(payload)

        self.assertEqual([(payload, False)], calls)
        self.assertEqual([listener], player.expression_event_listeners)
        self.assertGreaterEqual(player.expression_lock.enter_count, 3)

    def test_remove_listeners_uses_lock_and_removes_duplicates(self):
        player = self.make_player()

        def listener(value):
            return value

        player.output_event_listeners = [listener, listener]
        player.expression_event_listeners = [listener, listener]

        self.assertTrue(player.remove_output_event_listener(listener))
        self.assertTrue(player.remove_expression_event_listener(listener))

        self.assertEqual([], player.output_event_listeners)
        self.assertEqual([], player.expression_event_listeners)
        self.assertEqual(1, player.output_lock.enter_count)
        self.assertEqual(1, player.expression_lock.enter_count)

    def test_shutdown_stops_playback_before_clearing_listeners(self):
        player = self.make_player()
        player.output_event_listeners = [lambda value: value]
        player.expression_event_listeners = [lambda value: value]

        player.shutdown()

        player.playback_controller.stop.assert_called_once_with(join=True)
        self.assertEqual([], player.output_event_listeners)
        self.assertEqual([], player.expression_event_listeners)
        self.assertTrue(player._shutdown)
        self.assertEqual(1, player.output_lock.enter_count)
        self.assertEqual(1, player.expression_lock.enter_count)


class SongPlayerManifestTests(unittest.TestCase):
    def test_missing_user_config_does_not_load_example_manifest(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            config_dir = os.path.join(temp_dir, "config")
            os.makedirs(config_dir)
            example_path = os.path.join(
                config_dir,
                "song_player_songs.example.json",
            )

            with open(example_path, "w", encoding="utf-8") as file:
                json.dump(
                    {
                        "songs": [
                            {
                                "id": "song_a",
                                "title": "Song A",
                                "audio_path": "songs/a.wav",
                                "mouth_path": "songs/a_vocal.wav",
                                "mouth_gain": 1.5,
                                "mouth_floor": 0.1,
                            }
                        ]
                    },
                    file,
                )

            manifest = SongManifest(
                plugin_root=temp_dir,
                config_path=os.path.join(config_dir, "song_player_songs.json"),
                example_path=example_path,
            )

            songs = manifest.load()

            self.assertEqual([], songs)
            self.assertEqual("", manifest.loaded_path)
            self.assertIn("song_player_songs.json", manifest.status_text())
            self.assertIn(
                "song_player_songs.example.json",
                manifest.status_text(),
            )

    def test_user_config_wins_over_example_manifest(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            config_dir = os.path.join(temp_dir, "config")
            os.makedirs(config_dir)
            config_path = os.path.join(config_dir, "song_player_songs.json")
            example_path = os.path.join(
                config_dir,
                "song_player_songs.example.json",
            )

            with open(example_path, "w", encoding="utf-8") as file:
                json.dump(
                    {
                        "songs": [
                            {
                                "title": "Example",
                                "audio_path": "example.wav",
                                "mouth_path": "example_vocal.wav",
                            }
                        ]
                    },
                    file,
                )
            with open(config_path, "w", encoding="utf-8") as file:
                json.dump(
                    {
                        "songs": [
                            {
                                "title": "User Song",
                                "audio_path": "user.wav",
                                "mouth_path": "user_vocal.wav",
                                "mouth_gain": 1.5,
                                "mouth_floor": 0.1,
                            }
                        ]
                    },
                    file,
                )
            self.touch_song_file(temp_dir, "user.wav")
            self.touch_song_file(temp_dir, "user_vocal.wav")

            manifest = SongManifest(
                plugin_root=temp_dir,
                config_path=config_path,
                example_path=example_path,
            )

            songs = manifest.load()

            self.assertEqual(["User Song"], [song.title for song in songs])
            self.assertEqual(config_path, manifest.loaded_path)
            self.assertEqual(1.5, songs[0].mouth_gain)
            self.assertEqual(0.1, songs[0].mouth_floor)
            self.assertTrue(songs[0].expression_enabled)
            self.assertEqual(0.65, songs[0].expression_threshold)
            self.assertEqual(0.05, songs[0].expression_refresh_sec)
            self.assertEqual(-6.0, songs[0].expression_face_angle_x)
            self.assertEqual(-10.0, songs[0].expression_face_angle_y)
            self.assertTrue(songs[0].rhythm_enabled)
            self.assertEqual(0.35, songs[0].rhythm_threshold)
            self.assertEqual(280, songs[0].rhythm_min_interval_ms)
            self.assertEqual(160, songs[0].rhythm_pulse_ms)
            self.assertEqual(10.0, songs[0].rhythm_face_angle_z)

    def test_load_skips_songs_with_missing_audio_or_mouth(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            config_dir = os.path.join(temp_dir, "config")
            os.makedirs(config_dir)
            config_path = os.path.join(config_dir, "song_player_songs.json")
            self.touch_song_file(temp_dir, os.path.join("songs", "ok.wav"))
            self.touch_song_file(
                temp_dir,
                os.path.join("songs", "ok_vocal.wav"),
            )

            with open(config_path, "w", encoding="utf-8") as file:
                json.dump(
                    {
                        "songs": [
                            {
                                "title": "Valid",
                                "audio_path": "songs/ok.wav",
                                "mouth_path": "songs/ok_vocal.wav",
                            },
                            {
                                "title": "Missing Audio",
                                "audio_path": "songs/missing.wav",
                                "mouth_path": "songs/ok_vocal.wav",
                            },
                            {
                                "title": "Missing Mouth",
                                "audio_path": "songs/ok.wav",
                                "mouth_path": "songs/missing_vocal.wav",
                            },
                        ]
                    },
                    file,
                )

            manifest = SongManifest(
                plugin_root=temp_dir,
                config_path=config_path,
            )

            songs = manifest.load()
            status = manifest.status_text()

            self.assertEqual(["Valid"], [song.title for song in songs])
            self.assertIn(
                "Missing Audio audio_path="
                + os.path.join("songs", "missing.wav"),
                manifest.missing_files,
            )
            self.assertIn(
                "Missing Mouth mouth_path="
                + os.path.join("songs", "missing_vocal.wav"),
                manifest.missing_files,
            )
            self.assertIn("Skipped missing files", status)
            self.assertIn("Missing Audio", status)
            self.assertIn("Missing Mouth", status)

    def touch_song_file(self, root, relative_path):
        path = os.path.join(root, relative_path)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as file:
            file.write(b"")
        return path

    def test_loads_song_expression_config(self):
        manifest = SongManifest(plugin_root="unused")

        song = manifest.parse_song({
            "title": "Song With Expression",
            "audio_path": "audio.wav",
            "mouth_path": "mouth.wav",
            "expression_enabled": False,
            "expression_threshold": 0.8,
            "expression_hold_ms": 500,
            "expression_refresh_sec": 0.02,
            "expression_eye_open": 0.1,
            "expression_mouth_smile": 0.2,
            "expression_face_angle_x": -9,
            "expression_face_angle_y": -12,
        })

        self.assertFalse(song.expression_enabled)
        self.assertEqual(0.8, song.expression_threshold)
        self.assertEqual(500, song.expression_hold_ms)
        self.assertEqual(0.02, song.expression_refresh_sec)
        self.assertEqual({
            "active": True,
            "eye_open": 0.1,
            "mouth_smile": 0.2,
            "face_angle_x": -9.0,
            "face_angle_y": -12.0,
        }, song.expression_payload(True))

    def test_loads_song_rhythm_config(self):
        manifest = SongManifest(plugin_root="unused")

        song = manifest.parse_song({
            "title": "Song With Rhythm",
            "audio_path": "audio.wav",
            "mouth_path": "mouth.wav",
            "rhythm_enabled": False,
            "rhythm_threshold": 0.6,
            "rhythm_min_interval_ms": 420,
            "rhythm_pulse_ms": 90,
            "rhythm_face_angle_z": -8,
        })

        self.assertFalse(song.rhythm_enabled)
        self.assertEqual(0.6, song.rhythm_threshold)
        self.assertEqual(420, song.rhythm_min_interval_ms)
        self.assertEqual(90, song.rhythm_pulse_ms)
        self.assertEqual(-8.0, song.rhythm_face_angle_z)


class SongPlayerMouthAnimatorTests(unittest.TestCase):
    def test_extract_volumes_uses_gain_and_floor(self):
        audio_data = self.make_test_wav()
        animator = SongMouthAnimator(
            output_callback=lambda value: None,
            stop_event=None,
            target_fps=10,
        )

        volumes = animator.extract_volumes(
            audio_data=audio_data,
            mouth_gain=2.0,
            mouth_floor=0.2,
        )

        self.assertTrue(volumes)
        self.assertEqual(0.0, volumes[0])
        self.assertGreater(max(volumes), 0.9)

    def test_frame_index_uses_elapsed_time(self):
        animator = SongMouthAnimator(
            output_callback=lambda value: None,
            stop_event=None,
            target_fps=10,
        )

        self.assertEqual(0, animator.frame_index_for_elapsed(0.0))
        self.assertEqual(2, animator.frame_index_for_elapsed(0.25))
        self.assertEqual(
            7,
            animator.frame_index_for_elapsed(
                0.25,
                start_index=5,
            ),
        )

    def test_stop_swallows_keyboard_interrupt_during_join(self):
        outputs = []
        animator = SongMouthAnimator(
            output_callback=outputs.append,
            stop_event=None,
        )
        thread = _JoinInterruptedThread()
        animator.thread = thread

        animator.stop()

        self.assertEqual(1, thread.join_count)
        self.assertEqual([0], outputs)

    def make_test_wav(self):
        buffer = io.BytesIO()
        sample_rate = 1000
        samples = [0] * 100 + [10000] * 100

        with wave.open(buffer, "wb") as wav_file:
            wav_file.setnchannels(1)
            wav_file.setsampwidth(2)
            wav_file.setframerate(sample_rate)
            wav_file.writeframes(
                b"".join(struct.pack("<h", sample) for sample in samples)
            )

        return buffer.getvalue()


class SongPlayerRhythmAnimatorTests(unittest.TestCase):
    def test_extract_beat_events_uses_audio_energy_peaks(self):
        with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as file:
            path = file.name

        try:
            self.write_pulse_wav(path)
            animator = SongRhythmAnimator(
                output_callback=lambda payload: None,
                stop_event=None,
                frame_ms=50,
            )

            events = animator.extract_beat_events(
                audio_path=path,
                threshold=0.3,
                min_interval_ms=250,
            )

            self.assertGreaterEqual(len(events), 3)
            self.assertAlmostEqual(0.5, events[0][0], delta=0.15)
            self.assertAlmostEqual(1.0, events[1][0], delta=0.15)

        finally:
            try:
                os.remove(path)
            except OSError:
                pass

    def test_wait_or_stopped_ignores_negative_sleep_window(self):
        animator = SongRhythmAnimator(
            output_callback=lambda payload: None,
            stop_event=None,
            frame_ms=50,
        )

        self.assertFalse(animator.wait_or_stopped(-0.1))
        self.assertFalse(animator.wait_or_stopped(0))

    def test_send_rhythm_wave_eases_face_angle_z_out_and_back(self):
        payloads = []
        waits = []
        animator = SongRhythmAnimator(
            output_callback=payloads.append,
            stop_event=None,
            frame_ms=50,
        )

        def fake_wait(seconds):
            waits.append(seconds)
            return False

        animator.wait_or_stopped = fake_wait

        self.assertTrue(animator.send_rhythm_wave(8.0, step_sec=0.05))
        self.assertEqual(
            [
                0.0,
                1.0,
                2.0,
                3.0,
                4.0,
                5.0,
                6.0,
                7.0,
                8.0,
                7.0,
                6.0,
                5.0,
                4.0,
                3.0,
                2.0,
                1.0,
                0.0,
            ],
            [payload["face_angle_z"] for payload in payloads],
        )
        self.assertEqual([0.05] * 16, waits)

    def test_rhythm_skips_beats_when_vocal_volume_is_zero(self):
        sent_angles = []
        vocal_args = {}
        animator = SongRhythmAnimator(
            output_callback=lambda payload: None,
            stop_event=None,
            frame_ms=50,
        )
        song = SongEntry(
            song_id="song",
            title="Song",
            audio_path="audio.wav",
            mouth_path="mouth.wav",
            mouth_gain=1.2,
            mouth_floor=0.05,
            rhythm_face_angle_z=8.0,
        )

        animator.extract_beat_events = lambda **kwargs: [
            (0.0, 1.0),
            (0.1, 1.0),
            (0.2, 1.0),
        ]

        def fake_extract_vocal_volumes(mouth_path, mouth_gain, mouth_floor):
            vocal_args["mouth_path"] = mouth_path
            vocal_args["mouth_gain"] = mouth_gain
            vocal_args["mouth_floor"] = mouth_floor
            return [0.0, 0.5, 0.0], 10.0

        animator.extract_vocal_volumes = fake_extract_vocal_volumes
        animator.wait_or_stopped = lambda seconds: False
        animator.send_rhythm_wave = (
            lambda face_angle_z, step_sec: sent_angles.append(face_angle_z)
            or True
        )

        animator._animation_loop("audio.wav", song, "vocal_only.wav")

        self.assertEqual({
            "mouth_path": "vocal_only.wav",
            "mouth_gain": 1.2,
            "mouth_floor": 0.05,
        }, vocal_args)
        self.assertEqual([8.0], sent_angles)

    def test_vocal_volume_gate_honors_mouth_offset(self):
        animator = SongRhythmAnimator(
            output_callback=lambda payload: None,
            stop_event=None,
            frame_ms=50,
        )

        self.assertFalse(
            animator.is_vocal_volume_active(
                volumes=[1.0],
                beat_sec=0.04,
                target_fps=10,
                offset_ms=100,
            )
        )
        self.assertTrue(
            animator.is_vocal_volume_active(
                volumes=[1.0],
                beat_sec=0.1,
                target_fps=10,
                offset_ms=100,
            )
        )
        self.assertTrue(
            animator.is_vocal_volume_active(
                volumes=[0.0, 1.0],
                beat_sec=0.0,
                target_fps=10,
                offset_ms=-100,
            )
        )

    def test_stop_swallows_keyboard_interrupt_during_join(self):
        payloads = []
        animator = SongRhythmAnimator(
            output_callback=payloads.append,
            stop_event=None,
        )
        thread = _JoinInterruptedThread()
        animator.thread = thread

        animator.stop()

        self.assertEqual(1, thread.join_count)
        self.assertEqual(
            [{"rhythm_active": False, "face_angle_z": 0.0}],
            payloads,
        )

    def write_pulse_wav(self, path):
        sample_rate = 1000
        samples = []
        for index in range(2000):
            in_pulse = (index % 500) < 50 and index >= 500
            samples.append(12000 if in_pulse else 500)

        with wave.open(path, "wb") as wav_file:
            wav_file.setnchannels(1)
            wav_file.setsampwidth(2)
            wav_file.setframerate(sample_rate)
            wav_file.writeframes(
                b"".join(struct.pack("<h", sample) for sample in samples)
            )


class SongPlaybackControllerTests(unittest.TestCase):
    def test_song_expression_follows_loud_mouth_volume(self):
        mouth_values = []
        expression_values = []
        controller = SongPlaybackController(
            plugin_root="unused",
            output_callback=mouth_values.append,
            expression_callback=expression_values.append,
        )
        controller.current_song = SongEntry(
            song_id="song",
            title="Song",
            audio_path="audio.wav",
            mouth_path="mouth.wav",
            expression_threshold=0.5,
            expression_hold_ms=0,
        )

        controller._handle_mouth_output(0.2)
        controller._handle_mouth_output(0.6)
        controller._handle_mouth_output(0.0)

        self.assertEqual([0.2, 0.6, 0.0], mouth_values)
        self.assertEqual(
            [True, False],
            [payload["active"] for payload in expression_values],
        )

    def test_song_expression_repeats_while_active(self):
        expression_values = []
        controller = SongPlaybackController(
            plugin_root="unused",
            output_callback=lambda value: None,
            expression_callback=expression_values.append,
        )
        controller.current_song = SongEntry(
            song_id="song",
            title="Song",
            audio_path="audio.wav",
            mouth_path="mouth.wav",
            expression_threshold=0.5,
            expression_hold_ms=1000,
            expression_refresh_sec=0.01,
        )

        controller._handle_mouth_output(0.6)
        deadline = time.time() + 0.2
        while len(expression_values) < 2 and time.time() < deadline:
            time.sleep(0.005)

        controller._reset_song_expression(force=True)

        active_payloads = [
            payload for payload in expression_values
            if payload.get("active")
        ]
        self.assertGreaterEqual(len(active_payloads), 2)

    def test_audio_playback_uses_async_winsound_and_stops(self):
        with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as file:
            path = file.name

        try:
            self.write_silence_wav(path)
            controller = SongPlaybackController(
                plugin_root="unused",
                output_callback=lambda value: None,
            )

            with mock.patch("tts_core.winsound_player.winsound.PlaySound") as play_sound:
                timer = threading.Timer(0.02, controller.stop_event.set)
                timer.start()
                try:
                    completed = controller._play_audio_until_stopped(path)
                finally:
                    timer.cancel()

            self.assertFalse(completed)
            play_sound.assert_any_call(
                path,
                winsound.SND_FILENAME | winsound.SND_ASYNC,
            )
            play_sound.assert_any_call(None, 0)

        finally:
            try:
                os.remove(path)
            except OSError:
                pass

    def test_stop_swallows_keyboard_interrupt_during_join(self):
        outputs = []
        controller = SongPlaybackController(
            plugin_root="unused",
            output_callback=outputs.append,
        )
        thread = _JoinInterruptedThread()
        controller.thread = thread

        with mock.patch("tts_core.winsound_player.winsound.PlaySound"):
            controller.stop(join=True)

        self.assertEqual(1, thread.join_count)
        self.assertEqual(0, outputs[-1])

    def write_silence_wav(self, path):
        sample_rate = 1000
        samples = [0] * 1000

        with wave.open(path, "wb") as wav_file:
            wav_file.setnchannels(1)
            wav_file.setsampwidth(2)
            wav_file.setframerate(sample_rate)
            wav_file.writeframes(
                b"".join(struct.pack("<h", sample) for sample in samples)
            )


if __name__ == "__main__":
    unittest.main()
