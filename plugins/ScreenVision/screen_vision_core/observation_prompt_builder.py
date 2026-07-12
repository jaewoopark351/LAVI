#20260705_kpopmodder: Added this helper to isolate ScreenVision observation prompt assembly.


class ScreenObservationPromptBuilder:
    #20260709_kpopmodder: Restore the default Korean prompt after earlier mojibake got persisted.
    def build_screen_question(self, detail_hint=""):
        hint = str(detail_hint or "").strip()

        if hint:
            hint = hint + " "

        return (
            "당신은 PC 화면 관찰 기록 도우미입니다. "#20260622_kpopmodder
            "사용자에게 바로 말할 문장이 아니라, 나중에 사용자가 질문했을 때 참고할 화면 관찰 기록을 작성하세요. "
            f"{hint}"
            "화면에 실제로 보이는 내용만 근거로 기록하세요. "
            "열려 있는 프로그램, 창 제목, 읽을 수 있는 텍스트, 코드, 문서, 오류 메시지, 버튼, 메뉴, 게임 상태, 눈에 띄는 변화를 가능한 자세히 기록하세요. "
            "불확실한 내용은 추측하지 말고 '확실하지 않음'이라고 쓰세요. "
            "'현재 화면에는', 'PC 화면에는', '화면에는' 같은 시작 표현은 쓰지 마세요."
        )
