#20260801_kpopmodder: Guard Minecraft backend separation before runtime wiring exists.
import ast
import unittest
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
MINECRAFT_ROOT = PROJECT_ROOT / "plugins" / "Minecraft"
COMMON_ROOT = MINECRAFT_ROOT / "common"
FABRIC_ROOT = MINECRAFT_ROOT / "fabric"


def iter_python_files(root):
    return sorted(path for path in root.rglob("*.py") if path.is_file())


def imported_modules(path):
    tree = ast.parse(path.read_text(encoding="utf-8"))
    modules = []
    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            modules.extend(alias.name for alias in node.names)
        elif isinstance(node, ast.ImportFrom):
            modules.append(node.module or "")
    return modules


class MinecraftBackendSeparationContractTests(unittest.TestCase):
    def test_forge_backend_path_is_not_created_in_phase_1(self):
        self.assertFalse((MINECRAFT_ROOT / "forge").exists())

    def test_common_package_does_not_import_concrete_backends(self):
        banned = (
            "plugins.Minecraft.fabric",
            "plugins.Minecraft.forge",
            "fabric",
            "forge",
            "minemind",
        )

        for path in iter_python_files(COMMON_ROOT):
            for module in imported_modules(path):
                self.assertFalse(
                    module.startswith(banned),
                    f"{path} imports concrete backend module {module}",
                )

    def test_fabric_python_does_not_import_forge_or_minemind(self):
        banned_fragments = ("forge", "minemind")

        for path in iter_python_files(FABRIC_ROOT):
            for module in imported_modules(path):
                lowered = module.lower()
                self.assertFalse(
                    any(fragment in lowered for fragment in banned_fragments),
                    f"{path} imports future backend module {module}",
                )

    def test_common_package_has_no_runtime_transport_implementation(self):
        banned_imports = ("socket", "websocket", "websockets", "threading", "asyncio")

        for path in iter_python_files(COMMON_ROOT):
            for module in imported_modules(path):
                self.assertFalse(
                    module.startswith(banned_imports),
                    f"{path} imports runtime implementation module {module}",
                )


if __name__ == "__main__":
    unittest.main()
