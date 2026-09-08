#20260905_kpopmodder: Assemble focused trusted Korean route collaborators.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.admission import (
    TrustedKoreanAdmissionRejectionFactory,
    TrustedKoreanInputAdmission,
)
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.trusted_korean_feature_admission_observer import TrustedKoreanFeatureAdmissionObserver
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.trusted_korean_proof_lifecycle_closer import TrustedKoreanProofLifecycleCloser
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.response import (
    TrustedKoreanFeedbackRenderer,
    TrustedKoreanResponseAuthorizer,
)
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.trusted_korean_route_invoker import TrustedKoreanRouteInvoker
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.trusted_korean_proof_validator import TrustedKoreanProofValidator
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.publication.status import (
    CommandStatusPublicationCustodyGuard,
)


class TrustedKoreanInputRouteComponentGraph:
    def __init__(
        self,
        *,
        owner: object,
        input_event_normalizer,
        eligibility_admission,
        feedback_facade,
        feature_admission_logger,
        feature_admission_projector,
        route_callback,
        close_feature_dispatch_callback,
        status_publication_custody_policy=None,
        status_publication_emergency_decision=None,
    ):
        self.admission = TrustedKoreanInputAdmission(
            owner=owner,
            input_event_normalizer=input_event_normalizer,
            admission=eligibility_admission,
        )
        self.rejection_factory = TrustedKoreanAdmissionRejectionFactory()
        self.route_invoker = TrustedKoreanRouteInvoker(route_callback)
        self.feedback_renderer = TrustedKoreanFeedbackRenderer(feedback_facade)
        self.response_authorizer = TrustedKoreanResponseAuthorizer(owner=owner)
        self.status_publication_custody_guard = (
            CommandStatusPublicationCustodyGuard(
                custody_policy=status_publication_custody_policy,
                emergency_decision=status_publication_emergency_decision,
            )
        )
        self.proof_lifecycle_closer = TrustedKoreanProofLifecycleCloser(
            close_feature_dispatch_callback
        )
        self.proof_validator = TrustedKoreanProofValidator(owner=owner)
        self.diagnostics_observer = TrustedKoreanFeatureAdmissionObserver(
            projector=feature_admission_projector,
            logger=feature_admission_logger,
        )


__all__ = ("TrustedKoreanInputRouteComponentGraph",)
