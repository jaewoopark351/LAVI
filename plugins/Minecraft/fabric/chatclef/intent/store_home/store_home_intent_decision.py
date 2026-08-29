#20260827_kpopmodder: Added this module to keep one project class per Python file.
from enum import Enum


class StoreHomeIntentDecision(str, Enum):
    NO_MATCH = "no_match"
    STORE_HOME = "store_home"
    AMBIGUOUS = "ambiguous"
    NEGATED = "negated"
    DEFERRED = "deferred"
    QUESTION = "question"
    AMBIGUOUS_COMPOUND = "ambiguous_compound"
