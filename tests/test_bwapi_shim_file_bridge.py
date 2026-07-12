# #20260701_kpopmodder: Verifies the native read-only snapshot probe for Samase bridge work.
# import json
# import subprocess
# import tempfile
# import unittest
# from pathlib import Path


# PROJECT_ROOT = Path(__file__).resolve().parents[1]
# SHIM_ROOT = PROJECT_ROOT / "plugins" / "StarCraftRemastered" / "bwapi_shim"
# RUNTIME_CANDIDATES = (
#     SHIM_ROOT / "build-saida" / "Debug" / "scr_readonly_runtime.exe",
#     SHIM_ROOT / "build" / "Debug" / "scr_readonly_runtime.exe",
# )


# class BWAPIShimFileBridgeTests(unittest.TestCase):
#     @classmethod
#     def setUpClass(cls):
#         cls.runtime = next(
#             (path for path in RUNTIME_CANDIDATES if path.is_file()),
#             None,
#         )
#         if cls.runtime is None:
#             raise unittest.SkipTest("scr_readonly_runtime.exe is not built")

#     def test_scr_readonly_runtime_reads_snapshot_without_commands(self):
#         temp_dir = tempfile.TemporaryDirectory()
#         self.addCleanup(temp_dir.cleanup)
#         snapshot_path = Path(temp_dir.name) / "snapshot.json"
#         snapshot_path.write_text(
#             json.dumps(
#                 {
#                     "schema": "lav_bwapi_rm_snapshot_v1",
#                     "game": {
#                         "connected": True,
#                         "in_game": True,
#                         "single_player": True,
#                         "battle_net_screen": False,
#                         "multiplayer_screen": False,
#                         "frame_count": 321,
#                         "map_name": "Readonly Bridge",
#                         "map_width": 64,
#                         "map_height": 64,
#                         "self": {
#                             "id": 1,
#                             "name": "SAIDA",
#                             "race": "Terran",
#                             "minerals": 150,
#                             "gas": 25,
#                             "supply_used": 8,
#                             "supply_total": 20,
#                             "start_location": [9, 15],
#                         },
#                         "enemy": {
#                             "id": 2,
#                             "name": "Enemy",
#                             "race": "Zerg",
#                             "supply_used": 4,
#                             "supply_total": 18,
#                             "start_location": [24, 7],
#                         },
#                     },
#                     "units": {
#                         "my": [
#                             {
#                                 "unit_id": 1,
#                                 "unit_type": "Terran Command Center",
#                                 "owner": "self",
#                                 "position": {"x": 288, "y": 480},
#                                 "hp": 1500,
#                                 "is_completed": True,
#                             },
#                             {
#                                 "unit_id": 2,
#                                 "unit_type": "Terran SCV",
#                                 "owner": "self",
#                                 "x": 320,
#                                 "y": 512,
#                                 "hp": 60,
#                                 "is_completed": True,
#                             },
#                         ],
#                         "enemy": [
#                             {
#                                 "unit_id": 100,
#                                 "unit_type": "Zerg Hatchery",
#                                 "owner": "enemy",
#                                 "x": 768,
#                                 "y": 224,
#                                 "hp": 1250,
#                                 "is_completed": True,
#                             }
#                         ],
#                         "neutral": [
#                             {
#                                 "unit_id": 200,
#                                 "unit_type": "Resource Mineral Field",
#                                 "owner": "neutral",
#                                 "x": 352,
#                                 "y": 544,
#                                 "resources": 1500,
#                                 "is_completed": True,
#                             }
#                         ],
#                     },
#                 }
#             ),
#             encoding="utf-8",
#         )

#         completed = subprocess.run(
#             [str(self.runtime), f"--snapshot={snapshot_path}"],
#             cwd=str(SHIM_ROOT),
#             stdout=subprocess.PIPE,
#             stderr=subprocess.STDOUT,
#             timeout=30,
#             check=False,
#         )
#         output = completed.stdout.decode("utf-8", errors="replace")

#         self.assertEqual(0, completed.returncode, output)
#         self.assertIn("connected=true", output)
#         self.assertIn("inGame=true", output)
#         self.assertIn("singlePlayer=true", output)
#         self.assertIn("frame=321", output)
#         self.assertIn("map=Readonly Bridge size=64x64", output)
#         self.assertIn("self name=SAIDA race=Terran minerals=150 gas=25 supply=8/20", output)
#         self.assertIn("enemy name=Enemy race=Zerg supply=4/18", output)
#         self.assertIn("units my=2 enemy=1 neutral=1", output)
#         self.assertIn("firstMyUnit id=1 type=Terran Command Center pos=288,480 hp=1500", output)
#         self.assertIn("commands=disabled", output)
#         self.assertIn("ok=true", output)


# if __name__ == "__main__":
#     unittest.main()
