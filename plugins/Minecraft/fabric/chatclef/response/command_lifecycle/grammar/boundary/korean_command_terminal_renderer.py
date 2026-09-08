#20260907_kpopmodder: Own deterministic terminal sentences without selecting lifecycle truth.
from __future__ import annotations

from ...terminal.store_home import KoreanStoreHomeTerminalRenderer
from ..arguments.command_feedback_descriptor_phrase_semantics import get_verb


class KoreanCommandTerminalRenderer:
    def __init__(
        self,
        *,
        subject_renderer,
        location_renderer,
        hunger_renderer,
        particle_renderer,
        store_home_renderer=None,
    ) -> None:
        self._subjects = subject_renderer
        self._locations = location_renderer
        self._hunger = hunger_renderer
        self._particle = particle_renderer
        self._store_home = (
            store_home_renderer or KoreanStoreHomeTerminalRenderer()
        )

    def render(
        self,
        descriptor: object,
        profile: object,
        *,
        status: str,
        verified: bool,
        dispatch_started: bool,
        evidence_projection: object = None,
    ) -> str:
        if (
            status == "accepted_without_result_callback"
            and getattr(descriptor, "command_name", "") == "gamma"
        ):
            return (
                "감마 변경 명령은 보냈는데, "
                "실제로 바뀌었는지는 확인하지 못했어"
            )
        if status == "completed":
            return self._completed(
                descriptor,
                profile,
                verified,
                evidence_projection,
            )
        if status == "failed":
            return self._failed(descriptor, profile, dispatch_started)
        if status == "rejected":
            return self._rejected(descriptor, profile)
        if status == "cancelled":
            return self._cancelled(descriptor, profile)
        if status == "deadline_exceeded":
            return self._deadline(descriptor, profile)
        return self._unknown(descriptor, profile)

    def _completed(
        self,
        descriptor: object,
        profile: object,
        verified: bool,
        evidence_projection: object,
    ) -> str:
        family = profile.family
        subject = self._subjects.render(descriptor, profile)
        if family == "item_get":
            verb = get_verb(descriptor)
            if verified:
                action = "다 만들었어" if verb == "craft" else "다 구했어"
                return f"{subject} {action}"
            if verb == "craft":
                return (
                    f"{subject} 만드는 작업은 끝났는데,\n"
                    "다 만들어졌는지는 확인하지 못했어"
                )
            return (
                f"{subject} 구하는 작업은 끝났는데,\n"
                "다 구했는지는 확인하지 못했어"
            )
        if family == "control":
            return "중지 명령은 끝났는데, 실제로 멈췄는지는 확인하지 못했어"
        if family == "store_home" and verified:
            strong_response = self._store_home.render(evidence_projection)
            if strong_response is not None:
                return strong_response
        destination = self._particle.attach_directional(
            self._locations.render(descriptor)
        )
        cautious = {
            "item_deposit": f"{subject} 보관 작업은 끝났는데, 실제로 보관됐는지는 확인하지 못했어",
            "item_equip": f"{subject} 장착 작업은 끝났는데, 실제로 장착됐는지는 확인하지 못했어",
            "item_give": f"{subject} 건네는 작업은 끝났는데, 실제로 전달됐는지는 확인하지 못했어",
            "movement_goto": f"{destination} 가는 작업은 끝났는데, 도착했는지는 확인하지 못했어",
            "food_acquisition": f"{self._hunger.render(descriptor, family)} 모으는 작업은 끝났는데, 필요한 만큼 모였는지는 확인하지 못했어",
            "meat_acquisition": f"{self._hunger.render(descriptor, family)} 모으는 작업은 끝났는데, 필요한 만큼 모였는지는 확인하지 못했어",
            "store_home": "아이템을 집에 정리하는 작업은 끝났는데, 실제로 정리됐는지는 확인하지 못했어",
            "movement_follow": "따라가기 작업이 끝났지만 종료 이유는 확인하지 못했어",
            "persistent": f"{profile.command_label} 작업이 끝났지만 종료 이유는 확인하지 못했어",
        }
        return cautious.get(
            family,
            f"{profile.command_label} 작업은 끝났는데, 요청한 결과는 확인하지 못했어",
        )

    def _failed(self, descriptor: object, profile: object, began: bool) -> str:
        family = profile.family
        subject = self._subjects.render(descriptor, profile)
        if family == "item_get":
            if get_verb(descriptor) == "craft":
                action = "만들다가 실패했어" if began else "만들지 못했어"
            else:
                action = "구하다가 실패했어" if began else "구하지 못했어"
            return f"{subject} {action}"
        if family == "item_deposit":
            action = "보관하다가 실패했어" if began else "보관하지 못했어"
            return f"{subject} {action}"
        if family == "item_equip":
            action = "장착하다가 실패했어" if began else "장착하지 못했어"
            return f"{subject} {action}"
        if family == "item_give":
            action = "건네다가 실패했어" if began else "건네지 못했어"
            return f"{subject} {action}"
        if family == "movement_goto":
            destination = self._particle.attach_directional(
                self._locations.render(descriptor)
            )
            action = "가다가 실패했어" if began else "가지 못했어"
            return f"{destination} {action}"
        if family in {"food_acquisition", "meat_acquisition"}:
            action = "모으다가 실패했어" if began else "모으지 못했어"
            return f"{self._hunger.render(descriptor, family)} {action}"
        if family == "store_home":
            return (
                "아이템을 집에 정리하다가 실패했어"
                if began
                else "아이템을 집에 정리하지 못했어"
            )
        if family == "control":
            return (
                "중지 요청을 처리하다가 실패했어"
                if began
                else "중지 요청을 처리하지 못했어"
            )
        return f"{profile.command_label} 작업에 실패했어"

    def _rejected(self, descriptor: object, profile: object) -> str:
        if profile.family == "item_get":
            subject = self._subjects.render(descriptor, profile)
            action = "만들지" if get_verb(descriptor) == "craft" else "구하지"
            return f"{subject} {action} 못했어"
        if profile.family == "control":
            return "중지 명령은 실행하지 못했어"
        return f"{profile.command_label} 명령은 실행하지 못했어"

    def _cancelled(self, descriptor: object, profile: object) -> str:
        if profile.family == "item_get":
            subject = self._subjects.render(descriptor, profile)
            action = "만들기" if get_verb(descriptor) == "craft" else "구하기"
            return f"{subject} {action}가 중단됐어"
        if profile.family == "control":
            return "중지 요청이 중단됐어"
        return f"{profile.command_label} 작업이 중단됐어"

    def _deadline(self, descriptor: object, profile: object) -> str:
        if profile.family == "item_get":
            subject = self._subjects.render(descriptor, profile)
            action = "만들기" if get_verb(descriptor) == "craft" else "구하기"
            return f"{subject} {action}가 시간 안에 끝나지 않았어"
        if profile.family == "control":
            return "중지 요청이 시간 안에 끝나지 않았어"
        return f"{profile.command_label} 작업이 시간 안에 끝나지 않았어"

    def _unknown(self, descriptor: object, profile: object) -> str:
        if profile.family == "item_get":
            subject = self._subjects.render(descriptor, profile)
            action = "만들기" if get_verb(descriptor) == "craft" else "구하기"
            return f"{subject} {action} 결과는 확인하지 못했어"
        if profile.family == "control":
            return "중지 요청 결과는 확인하지 못했어"
        return f"{profile.command_label} 작업 결과를 확인하지 못했어"


__all__ = ("KoreanCommandTerminalRenderer",)
