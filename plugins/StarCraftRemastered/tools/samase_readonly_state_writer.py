# #20260701_kpopmodder: Dev writer for the Samase read-only state file contract.
# import argparse
# import json
# import time

# from plugins.StarCraftRemastered.core.game_state import (
#     StarCraftGameState,
#     StarCraftPlayer,
# )
# from plugins.StarCraftRemastered.core.unit import StarCraftUnit
# from plugins.StarCraftRemastered.lav_bridge.samase_readonly_state_writer import (
#     SamaseReadonlyStateWriter,
# )
# from plugins.StarCraftRemastered.starcraft_config import StarCraftConfig


# def build_sample_state(frame):
#     minerals = 50 + max(0, int(frame) - 1) * 8
#     state = StarCraftGameState(
#         is_connected=True,
#         is_in_game=True,
#         is_single_player=True,
#         is_battlenet_screen=False,
#         is_multiplayer_screen=False,
#         player_race="Terran",
#         enemy_race="Zerg",
#         minerals=minerals,
#         gas=0,
#         supply_used=8,
#         supply_total=20,
#         frame_count=int(frame),
#         map_name="Samase Readonly Probe",
#         map_width=64,
#         map_height=64,
#         my_start_location=(9, 15),
#         enemy_start_location=(24, 7),
#         last_screen_observation="sample Samase read-only state",
#     )
#     state.self_player = StarCraftPlayer(
#         player_id=1,
#         name="SAIDA",
#         race="Terran",
#         minerals=minerals,
#         gas=0,
#         supply_used=8,
#         supply_total=20,
#     )
#     state.enemy_player = StarCraftPlayer(
#         player_id=2,
#         name="Enemy",
#         race="Zerg",
#         supply_used=4,
#         supply_total=18,
#     )
#     state.my_units = [
#         StarCraftUnit(
#             unit_id=1,
#             unit_type="Terran Command Center",
#             owner="self",
#             owner_id=1,
#             x=288,
#             y=480,
#             hp=1500,
#             is_completed=True,
#         ),
#         StarCraftUnit(
#             unit_id=2,
#             unit_type="Terran SCV",
#             owner="self",
#             owner_id=1,
#             x=320,
#             y=512,
#             hp=60,
#             is_completed=True,
#             is_idle=True,
#         ),
#     ]
#     state.enemy_units = [
#         StarCraftUnit(
#             unit_id=100,
#             unit_type="Zerg Hatchery",
#             owner="enemy",
#             owner_id=2,
#             x=768,
#             y=224,
#             hp=1250,
#             is_completed=True,
#         )
#     ]
#     state.neutral_units = [
#         StarCraftUnit(
#             unit_id=200,
#             unit_type="Resource Mineral Field",
#             owner="neutral",
#             x=352,
#             y=544,
#             resources=1500,
#             is_completed=True,
#         )
#     ]
#     return state


# def load_state_payload(path):
#     with open(path, "r", encoding="utf-8") as file:
#         return json.load(file)


# def parse_args(argv=None):
#     config = StarCraftConfig()
#     parser = argparse.ArgumentParser(
#         description=(
#             "Write the Samase read-only state JSON contract. This tool is a "
#             "safe file writer/probe; it does not attach to StarCraft."
#         )
#     )
#     parser.add_argument(
#         "--output",
#         default=config.resolve_samase_state_path(),
#         help="Samase read-only state JSON path.",
#     )
#     parser.add_argument(
#         "--bwapi-snapshot",
#         default=config.resolve_bwapi_snapshot_path(),
#         help="Optional BWAPI-RM snapshot mirror path.",
#     )
#     parser.add_argument(
#         "--no-bwapi-mirror",
#         action="store_true",
#         help="Only write the Samase state file.",
#     )
#     parser.add_argument(
#         "--from-json",
#         default="",
#         help="Copy an already built read-only state JSON payload once.",
#     )
#     parser.add_argument(
#         "--frames",
#         type=int,
#         default=1,
#         help="Number of sample frames to write. Use 0 to run until Ctrl+C.",
#     )
#     parser.add_argument(
#         "--interval",
#         type=float,
#         default=0.25,
#         help="Delay between sample frames.",
#     )
#     return parser.parse_args(argv)


# def main(argv=None):
#     args = parse_args(argv)
#     writer = SamaseReadonlyStateWriter(
#         state_path=args.output,
#         bwapi_snapshot_path=None if args.no_bwapi_mirror else args.bwapi_snapshot,
#     )

#     if args.from_json:
#         payload = writer.write_payload(load_state_payload(args.from_json))
#         print(
#             "[Samase readonly writer] wrote payload "
#             f"schema={payload.get('schema')} path={args.output}"
#         )
#         return 0

#     frame = 1
#     remaining = args.frames
#     try:
#         while remaining != 0:
#             state = build_sample_state(frame)
#             writer.write_state(state, source="samase_readonly_state_writer")
#             print(
#                 "[Samase readonly writer] "
#                 f"frame={frame} minerals={state.minerals} "
#                 f"state={args.output} bwapi={writer.bwapi_snapshot_path or 'disabled'}"
#             )
#             frame += 1
#             if remaining > 0:
#                 remaining -= 1
#             if remaining != 0:
#                 time.sleep(max(0.0, args.interval))
#     except KeyboardInterrupt:
#         print("[Samase readonly writer] stopped")
#         return 130
#     return 0


# if __name__ == "__main__":
#     raise SystemExit(main())
