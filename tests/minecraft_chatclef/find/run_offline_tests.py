#20260914_kpopmodder: Explicit Linux import shim for listener-boundary tests; never simulate hardware playback.
"""Run: python -m tests.minecraft_chatclef.find.run_offline_tests [pytest arguments].
Windows uses real winsound imports. On non-Windows, every attempted audio playback fails.
The shim permits import of application composition only; it does not assert audio playback.
"""
import sys
import types

def main():
    if sys.platform != "win32":
        shim = types.ModuleType("winsound")
        def no_hardware(*args, **kwargs):
            raise AssertionError("Offline tests cannot perform Windows audio playback")
        shim.PlaySound = no_hardware
        shim.SND_FILENAME = 0x20000
        shim.SND_ASYNC = 1
        sys.modules["winsound"] = shim
        print("TEST-ONLY winsound import shim active. Hardware audio is NOT tested.", flush=True)
    import pytest
    return pytest.main(sys.argv[1:] or ["-q", "tests/minecraft_chatclef/find"])

if __name__ == "__main__":
    raise SystemExit(main())
