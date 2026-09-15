#20260913_kpopmodder: Share immutable coordinate grammar clues and complete command forms.
import re


MOVE_VERBS = ("이동해주세요", "이동해줘", "가주세요", "이동해", "가줘", "가자", "이동", "가")
#20260915_kpopmodder: Final microphone spacing may separate the request ending.
MOVE_VERBS = MOVE_VERBS + ("이동해 주세요", "이동해 줘", "가 주세요", "가 줘")
NEGATED_FAVOR_MOVE = r"(?:이동해|가)[ ]*주지[ ]*(?:마|말)"
MOVE_CONTEXT_RE = re.compile(
    r"이동|가(?:주세요|줘|자|면|고|지|나요|니|도|야|려|는|서|겠|라고)|"
    r"갈(?:까|래|수)|(?:으로|로)[ ]*가(?:[ .!?]|$)|(?:^|[ ])가[ .!]*$|"
    + NEGATED_FAVOR_MOVE
)
AXIS_CLUE_RE = re.compile(r"(?<![a-z])(?:[xyz](?![a-z])|엑스|와이|제트)", re.IGNORECASE)
NUMERIC_CLUE_RE = re.compile(r"[0-9]+|[~^]")
HANGUL_NUMBER_RE = re.compile(r"(?<![가-힣])[영공일이삼사오육칠팔구십백천만억]+(?![가-힣])")
NONCOMMAND_RE = re.compile(
    r"[?？\"'“”‘’「」『』]|가지[ ]*(?:마|말)|가[ ]*지[ ]*마|"
    r"(?:이동|가)[ ]*(?:하지[ ]*(?:마|말|않)|금지)|안[ ]*(?:가|이동)|"
    r"가면|간다면|이동하면|가도[ ]*(?:돼|되)|가나요|가니(?:[ .!?]|$)|"
    r"갈까|갈래|가줄래|어떻게|만약|가정|예시|설명|라고|라는|라며|말했|"
    r"(?:이동|가)[ ]*.*(?:말아|않아|않을|않는)|맞[나요아]|"
    + NEGATED_FAVOR_MOVE
)
SIGNED_INTEGER = r"(?:[+-]?[0-9]+|(?:플러스|마이너스)[ ]*[0-9]+)"
SEPARATOR = r"(?:[ ]*,[ ]*|[ ]+)"
UNLABELLED_XYZ = (
    rf"(?P<x>{SIGNED_INTEGER}){SEPARATOR}"
    rf"(?P<y>{SIGNED_INTEGER}){SEPARATOR}(?P<z>{SIGNED_INTEGER})"
)
LABELLED_XYZ = (
    rf"(?:x|엑스)[ ]*(?:=[ ]*)?(?P<x>{SIGNED_INTEGER}){SEPARATOR}"
    rf"(?:y|와이)[ ]*(?:=[ ]*)?(?P<y>{SIGNED_INTEGER}){SEPARATOR}"
    rf"(?:z|제트)[ ]*(?:=[ ]*)?(?P<z>{SIGNED_INTEGER})"
)
COMMAND_PREFIX = r"[ ]*(?:좌표[ ]+)?(?P<opening>\()?[ ]*"
COMMAND_SUFFIX = (
    r"[ ]*(?(opening)\))[ ]*(?:좌표[ ]*)?(?:으로|로)?[ ]*(?:"
    + "|".join(MOVE_VERBS)
    + r")[ .!]*"
)
COORDINATE_COMMAND_PATTERNS = (
    re.compile(COMMAND_PREFIX + UNLABELLED_XYZ + COMMAND_SUFFIX, re.IGNORECASE),
    re.compile(COMMAND_PREFIX + LABELLED_XYZ + COMMAND_SUFFIX, re.IGNORECASE),
)
