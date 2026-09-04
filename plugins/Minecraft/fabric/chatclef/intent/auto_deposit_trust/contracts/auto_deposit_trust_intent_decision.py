#20260905_kpopmodder: Define the exact deterministic H5 decision values.
from enum import Enum


class AutoDepositTrustIntentDecision(str, Enum):
    NO_MATCH = "no_match"
    AUTO_DEPOSIT_TRUST_AREA = "auto_deposit_trust_area"
    NEGATED = "negated"
    QUESTION = "question"
    DEFERRED = "deferred"
    AMBIGUOUS = "ambiguous"
    AMBIGUOUS_COMPOUND = "ambiguous_compound"
    UNSUPPORTED_SIZE = "unsupported_size"
    MALFORMED = "malformed"
