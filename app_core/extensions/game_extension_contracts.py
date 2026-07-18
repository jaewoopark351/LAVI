#20260717_kpopmodder: Compatibility facade for game extension DTO contracts.
from app_core.extensions.contracts_core.game_command_dto import GameCommandDTO
from app_core.extensions.contracts_core.game_result_dto import GameResultDTO
from app_core.extensions.contracts_core.game_start_result_dto import (
    GameStartResultDTO,
)
from app_core.extensions.contracts_core.game_status_dto import GameStatusDTO
from app_core.extensions.contracts_core.game_stop_result_dto import GameStopResultDTO

__all__ = [
    "GameCommandDTO",
    "GameResultDTO",
    "GameStartResultDTO",
    "GameStatusDTO",
    "GameStopResultDTO",
]
