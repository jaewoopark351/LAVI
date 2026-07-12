# #20260701_kpopmodder: Pins the SAIDA mock BWEM second-choke surface against regressions.
# import subprocess
# import unittest
# from pathlib import Path


# PROJECT_ROOT = Path(__file__).resolve().parents[1]
# SHIM_ROOT = PROJECT_ROOT / "plugins" / "StarCraftRemastered" / "bwapi_shim"
# SAIDA_RUNTIME = SHIM_ROOT / "build-saida" / "Debug" / "saida_mock_runtime.exe"


# class BWAPIShimSecondChokeRegressionTests(unittest.TestCase):
#     @classmethod
#     def setUpClass(cls):
#         if not SAIDA_RUNTIME.is_file():
#             raise unittest.SkipTest(
#                 f"saida_mock_runtime.exe is not built: {SAIDA_RUNTIME}"
#             )

#     def run_saida_runtime(self, *args):
#         completed = subprocess.run(
#             [str(SAIDA_RUNTIME), *args],
#             cwd=str(SHIM_ROOT),
#             stdout=subprocess.PIPE,
#             stderr=subprocess.STDOUT,
#             timeout=300,
#             check=False,
#         )
#         output = completed.stdout.decode("utf-8", errors="replace")
#         if completed.returncode != 0:
#             self.fail(
#                 f"saida_mock_runtime exited with {completed.returncode}\n"
#                 f"{self.output_tail(output)}"
#             )
#         return output

#     def output_tail(self, output, line_count=80):
#         lines = output.splitlines()
#         return "\n".join(lines[-line_count:])

#     def test_info_update_exposes_second_choke_surface(self):
#         output = self.run_saida_runtime("--mode=info-update", "--frames=1", "--summary")

#         self.assertIn("areas=3 chokes=2", output)
#         self.assertRegex(output, r"bases=[4-9]\d*")
#         self.assertIn("secondChoke=true", output)
#         self.assertIn("secondExpansion=true/true", output)
#         self.assertIn("reserveFailed=false", output)
#         self.assertIn("ok=true", output)

#     def test_combat_commander_runs_active_managers_with_second_choke(self):
#         output = self.run_saida_runtime("--mode=combat-commander", "--frames=304", "--summary")

#         self.assertIn("ok=true", output)
#         self.assertIn("secondExpansion=true/true", output)
#         self.assertIn("reserveFailed=false", output)
#         self.assertIn("todo=false", output)
#         self.assertIn("exception=false", output)
#         self.assertIn("refinery=0", output)
#         self.assertIn("factoryMachineShop=0", output)
#         self.assertRegex(
#             output,
#             r"manager TrainManager originalCalled=true",
#             self.output_tail(output),
#         )
#         for manager in (
#             "TankManager",
#             "VultureManager",
#             "GoliathManager",
#             "WraithManager",
#             "VessleManager",
#             "DropshipManager",
#         ):
#             with self.subTest(manager=manager):
#                 self.assertRegex(
#                     output,
#                     rf"manager {manager} originalCalled=true",
#                     self.output_tail(output),
#                 )

#     def test_tech_surface_exposes_refinery_addon_and_tech_buildings(self):
#         output = self.run_saida_runtime("--mode=tech-surface", "--frames=1", "--summary")

#         self.assertIn("ok=true", output)
#         self.assertIn("secondExpansion=true/true", output)
#         self.assertIn("reserveFailed=false", output)
#         self.assertIn("todo=false", output)
#         self.assertIn("exception=false", output)
#         for tech_surface in (
#             "refinery=2",
#             "barracks=1",
#             "engineeringBay=1",
#             "academy=1",
#             "factory=1",
#             "machineShop=1",
#             "armory=1",
#             "starport=1",
#             "controlTower=1",
#             "scienceFacility=1",
#             "comsat=1",
#             "commandCenterComsat=1",
#             "factoryMachineShop=1",
#             "starportControlTower=1",
#         ):
#             with self.subTest(tech_surface=tech_surface):
#                 self.assertIn(tech_surface, output)

#     def test_train_manager_tech_surface_uses_addons_and_production_commands(self):
#         output = self.run_saida_runtime(
#             "--mode=train-manager",
#             "--scenario=tech",
#             "--summary",
#         )

#         self.assertIn("ok=true", output)
#         self.assertIn("reserveFailed=false", output)
#         self.assertIn("todo=false", output)
#         self.assertIn("exception=false", output)
#         self.assertIn("factoryMachineShop=1", output)
#         self.assertIn("starportControlTower=1", output)
#         self.assertRegex(
#             output,
#             r"manager TrainManager originalCalled=true",
#             self.output_tail(output),
#         )
#         for command_surface in (
#             r"train=[1-9]\d*",
#             r"trainSCV=[1-9]\d*",
#             r"trainVulture=[1-9]\d*",
#             r"trainScienceVessel=[1-9]\d*",
#             r"addonBuild=[1-9]\d*",
#             r"addonComsat=[1-9]\d*",
#         ):
#             with self.subTest(command_surface=command_surface):
#                 self.assertRegex(output, command_surface, self.output_tail(output))

#     def test_research_upgrade_surface_exposes_player_state_and_capabilities(self):
#         output = self.run_saida_runtime(
#             "--mode=research-surface",
#             "--frames=1",
#             "--summary",
#         )

#         self.assertIn("ok=true", output)
#         self.assertIn("reserveFailed=false", output)
#         self.assertIn("todo=false", output)
#         self.assertIn("exception=false", output)
#         for research_surface in (
#             "siegeMode=true",
#             "spiderMines=true",
#             "irradiate=true",
#             "emp=true",
#             "researchingCloaking=true",
#             "canResearchCloaking=false",
#             "canResearchDefensiveMatrix=true",
#             "vehicleWeapons=2",
#             "vehicleWeaponsMax=3",
#             "vehiclePlating=1",
#             "ionThrusters=1",
#             "charonBoosters=1",
#             "upgradingVehiclePlating=true",
#             "canUpgradeVehicleWeapons=true",
#             "canUpgradeVehiclePlating=false",
#         ):
#             with self.subTest(research_surface=research_surface):
#                 self.assertIn(research_surface, output)


# if __name__ == "__main__":
#     unittest.main()
