#20260710_kpopmodder: Added explicit launch profiles for the different StarCraft2 bot runtimes.
from __future__ import annotations

from dataclasses import dataclass
import hashlib
import os


@dataclass(frozen=True)
class BotLaunchProfile:
    name: str
    bot_type: str
    file_name: str
    required_runtime: str
    config_file: str = ""
    runtime_sha256: tuple[tuple[str, str], ...] = ()

    def validate(self, bot_root: str) -> dict:
        bot_dir = os.path.join(bot_root, self.name)
        target = os.path.join(bot_dir, self.file_name)
        result = {
            "ok": os.path.isfile(target),
            "bot": self.name,
            "type": self.bot_type,
            "required_runtime": self.required_runtime,
            "bot_dir": bot_dir,
            "target": target,
            "config_file": os.path.join(bot_dir, self.config_file) if self.config_file else "",
            "error": "bot_runtime_missing" if not os.path.isfile(target) else "",
            "strict_runtime": bool(self.runtime_sha256),
        }
        if not result["ok"] or not self.runtime_sha256:
            return result

        checks = []
        for file_name, expected_sha256 in self.runtime_sha256:
            path = os.path.join(bot_dir, file_name)
            exists = os.path.isfile(path)
            actual_sha256 = _sha256_file(path) if exists else ""
            checks.append(
                {
                    "file": file_name,
                    "path": path,
                    "exists": exists,
                    "expected_sha256": expected_sha256,
                    "actual_sha256": actual_sha256,
                    "ok": exists and actual_sha256.upper() == expected_sha256.upper(),
                }
            )
        result["runtime_checks"] = checks
        failed_checks = [item for item in checks if not item["ok"]]
        if failed_checks:
            result["ok"] = False
            result["error"] = (
                "bot_runtime_dependency_missing"
                if any(not item["exists"] for item in failed_checks)
                else "bot_runtime_checksum_mismatch"
            )
        return result


def _sha256_file(path: str) -> str:
    digest = hashlib.sha256()
    with open(path, "rb") as file_handle:
        for chunk in iter(lambda: file_handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


BOT_LAUNCH_PROFILES = {
    "BenBotBC": BotLaunchProfile(
        name="BenBotBC",
        bot_type="Java",
        file_name="BenBotBC.jar",
        required_runtime="jre/bin/java.exe",
    ),
    "changeling": BotLaunchProfile(
        name="changeling",
        bot_type="BinaryCpp",
        file_name="changeling.exe",
        required_runtime="native executable",
        config_file="config.yml",
    ),
    "sharkbot": BotLaunchProfile(
        name="sharkbot",
        bot_type="dotnetcore",
        file_name="sharkbot.dll",
        required_runtime="dotnet plus pinned Sharky DLLs",
        #20260710_kpopmodder: Keep the legacy Sharkbot bundle coherent; newer
        # SharkyLAVBot DLLs are API-incompatible with this legacy bot package.
        #20260710_kpopmodder: Keep Sharkbot's Meatbag anti-screen-cheat camera
        # calls disabled so the local user retains manual camera control.
        runtime_sha256=(
            ("sharkbot.dll", "8AFD017FF3D43064C0F1F0BD349F09328CB1A631B666595E3FE4F3AFA28A3457"),
            ("Sharky.dll", "5BC2997DB40F80BF172E15C17F1C2601C927631FF01E8A89FAAE356919771DE5"),
            ("S2ClientProtocol.dll", "6086CCC0414FAD090DB34780C338FC423A842605C2A59A91C71CB9B30668C2B7"),
            ("SharkySharkbotEnhancements.dll", "15B431D624E034F50856468FB13F8114B736519225EFD47ED948392B8B7A3CE6"),
        ),
    ),
}


def get_bot_launch_profile(bot_name: str) -> BotLaunchProfile | None:
    return BOT_LAUNCH_PROFILES.get(str(bot_name or "").strip())
