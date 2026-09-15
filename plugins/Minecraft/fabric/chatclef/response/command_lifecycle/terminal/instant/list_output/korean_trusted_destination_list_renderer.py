#20260915_kpopmodder: Render one validated list snapshot for display or numbered speech.
class KoreanTrustedDestinationListRenderer:
    _DIMENSIONS = {"overworld": "오버월드", "nether": "네더", "end": "엔드"}
    _STATUSES = {
        "KNOWN_AVAILABLE": "사용 가능으로 기록됨", "KNOWN_FULL": "가득 참으로 기록됨",
        "MISSING": "대상 없음", "KNOWN_UNREACHABLE": "도달 불가로 기록됨",
        "UNKNOWN_OR_STALE": "현재 상태 미확인",
    }

    @classmethod
    def render(cls, values, *, speech=False):
        if not values["total"]:
            return "등록된 자동 보관 장소가 없어."
        lines = [f"등록된 자동 보관 장소는 {values['total']}곳이야."]
        spoken = 0
        for index, entry in enumerate(values["destinations"], 1):
            label = f"{index}번" if speech else entry["destination_id"]
            line = (
                f"{label}: {cls._DIMENSIONS[entry['dimension']]} "
                f"{entry['x']}, {entry['y']}, {entry['z']}, {cls._STATUSES[entry['status']]}."
            )
            # Retain complete rows and a truthful omission notice below the normal
            # 2,000-character SafetyFilter limit; never alter that user policy.
            if speech and len("\n".join(lines)) + len(line) + 100 > 1800:
                break
            lines.append(line)
            spoken = index
        if speech and spoken < values["listed"]:
            lines.append(f"음성은 전체 {values['total']}곳 중 앞의 {spoken}곳까지 안내했어. 나머지는 화면 목록에서 확인해 줘.")
        if values["truncated"]:
            lines.append(f"화면에는 앞의 {values['listed']}곳을 표시했어." if speech else f"앞의 {values['listed']}곳을 표시했어.")
        return "\n".join(lines)
