#20260915_kpopmodder: Verify deferred response authority through the actual Chat/final-voice ingress graph.
from dataclasses import replace
from types import SimpleNamespace
import threading
import logging

import pytest

from input_core.input_event.provenance.trusted_user_ingress.routed_response_emission_capability import RoutedResponseEmissionCapability
from input_core.input_event.provenance.trusted_user_ingress.routed_response_deferred_selection import RoutedResponseDeferredSelection
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.response.trusted_korean_response_authorizer import TrustedKoreanResponseAuthorizer
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.response.coalesced.trusted_korean_coalesced_response_binding import TrustedKoreanCoalescedResponseBinding
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle.publication.command_feedback_ready_decision_acknowledgement import CommandFeedbackReadyDecisionAcknowledgement
from tests.minecraft_chatclef.command_lifecycle.list_output.test_trusted_destination_list_delivery import PHRASE, setup


def consume(f, decision, **changes):
    cap = decision.response_emission_capability
    arguments = dict(event=cap._event, text=decision.response_text,
                     source=decision.response_source, response_kind=decision.response_kind,
                     speech_text=decision.response_speech_text)
    arguments.update(changes)
    return f.route.llm.trusted_user_input_ingress_claim_registry.consume_routed_response_emission_capability(cap, **arguments)


@pytest.mark.parametrize("voice", (False, True))
@pytest.mark.parametrize("immediate", (False, True))
def test_original_and_selected_capabilities_are_once_only(monkeypatch, voice, immediate):
    f = setup(immediate=immediate)
    saved = {}
    real = CommandFeedbackReadyDecisionAcknowledgement.resolve_ready_decision

    def intercept(ack, decision):
        saved["original"] = decision
        saved["selected"] = real(ack, decision)
        saved["ack"] = ack
        return saved["selected"]

    monkeypatch.setattr(CommandFeedbackReadyDecisionAcknowledgement, "resolve_ready_decision", intercept)
    f.route.dispatch(PHRASE, voice=voice)
    old, selected, ack = saved["original"], saved["selected"], saved["ack"]
    assert old.response_emission_capability.spent
    assert selected.response_emission_capability.spent
    assert len(f.outputs) == 1
    assert selected.response_kind == ("command_coalesced" if immediate else "command_start")
    assert consume(f, old) is False and consume(f, selected) is False
    assert RoutedResponseEmissionCapability._replace_from_selection(
        old.response_emission_capability, ack._trusted_response_binding) is None
    assert RoutedResponseEmissionCapability._replace_from_selection(
        selected.response_emission_capability, ack._trusted_response_binding) is None
    assert real(ack, old) is None
    assert ack.acknowledge(published=True) is False


@pytest.mark.parametrize("mutation", ("cancel_before", "cancel_after", "ack_before", "ack_after",
                                      "token_before", "token_after", "foreign_decision", "selector", "factory"))
def test_changed_or_cancelled_publication_cannot_emit(monkeypatch, mutation):
    f = setup(immediate=True)
    real = CommandFeedbackReadyDecisionAcknowledgement.resolve_ready_decision

    def intercept(ready, decision):
        ack = ready._acknowledgement
        if mutation == "cancel_before":
            ack._permit._cancel_turn()
        elif mutation == "ack_before":
            ready.acknowledge(published=False)
        elif mutation == "token_before":
            ack._permit.lifecycle_token = object()
        elif mutation == "selector":
            ack._coalesced_terminal_selector = lambda _: None
        elif mutation == "factory":
            ready._coalesced_response_factory = SimpleNamespace(build=lambda _: None)
        selected = real(ready, replace(decision) if mutation == "foreign_decision" else decision)
        if mutation == "cancel_after":
            ack._permit._cancel_turn()
        elif mutation == "ack_after":
            ready.acknowledge(published=False)
        elif mutation == "token_after":
            ack._permit.lifecycle_token = object()
        if selected is not None:
            assert consume(f, selected) is False
        assert consume(f, decision) is False
        return selected

    monkeypatch.setattr(CommandFeedbackReadyDecisionAcknowledgement, "resolve_ready_decision", intercept)
    f.route.dispatch(PHRASE)
    assert not f.outputs


@pytest.mark.parametrize("field", ("text", "speech_text", "source", "response_kind", "event"))
def test_selected_text_and_speech_cannot_be_replaced(monkeypatch, field):
    f = setup(immediate=True)
    real = CommandFeedbackReadyDecisionAcknowledgement.resolve_ready_decision

    def intercept(ready, decision):
        selected = real(ready, decision)
        assert consume(f, selected, **{field: object() if field == "event" else "forged"}) is False
        return selected

    monkeypatch.setattr(CommandFeedbackReadyDecisionAcknowledgement, "resolve_ready_decision", intercept)
    f.route.dispatch(PHRASE)
    assert len(f.outputs) == 1


@pytest.mark.parametrize("mutation", ("event", "render_failure", "fake_owner", "subclass_owner", "instance_override"))
def test_selected_owner_and_failed_selection_are_closed(monkeypatch, mutation):
    f = setup(immediate=True)
    real = CommandFeedbackReadyDecisionAcknowledgement.resolve_ready_decision

    def intercept(ready, decision):
        binding = ready._trusted_response_binding
        if mutation in {"fake_owner", "subclass_owner"}:
            class OtherBinding(TrustedKoreanCoalescedResponseBinding):
                pass
            owner = SimpleNamespace() if mutation == "fake_owner" else object.__new__(OtherBinding)
            assert TrustedKoreanCoalescedResponseBinding.commit_selected(owner, lambda *args: True) is None
            assert RoutedResponseEmissionCapability._replace_from_selection(decision.response_emission_capability, owner) is None
            assert consume(f, decision) is False
        elif mutation == "instance_override":
            binding.commit_selected = lambda callback: callback("forged", "minecraft_chatclef", "command_coalesced", "forged")
        elif mutation == "event":
            owner = binding._selector.__self__._connection_ownership
            select = owner.select_command_feedback_coalesced_terminal
            monkeypatch.setattr(owner, "select_command_feedback_coalesced_terminal",
                                lambda permit: replace(select(permit), event_id="f" * 32))
        else:
            renderer = binding._factory._response_renderer
            monkeypatch.setattr(renderer, "render_terminal", lambda _: (_ for _ in ()).throw(ValueError("renderer")))
        selected = real(ready, decision)
        if mutation != "instance_override":
            assert selected is None
            assert consume(f, decision) is False
        return selected

    monkeypatch.setattr(CommandFeedbackReadyDecisionAcknowledgement, "resolve_ready_decision", intercept)
    f.route.dispatch(PHRASE)
    assert len(f.outputs) == (1 if mutation == "instance_override" else 0)


@pytest.mark.parametrize("cancel_first", (False, True))
def test_cancel_and_emission_commit_have_one_ordered_owner_lock(monkeypatch, cancel_first):
    f = setup(immediate=True)
    real = CommandFeedbackReadyDecisionAcknowledgement.resolve_ready_decision

    def intercept(ready, decision):
        selected = real(ready, decision)
        permit = ready._acknowledgement._permit
        entered, release, cancelled = threading.Event(), threading.Event(), threading.Event()
        outcomes = []
        run = RoutedResponseDeferredSelection.run

        def pause(selection, callback):
            def committed(*args):
                entered.set()
                assert release.wait(3)
                return callback(*args)
            return run(selection, committed)

        if cancel_first:
            with permit._condition:
                worker = threading.Thread(target=lambda: outcomes.append(consume(f, selected)), daemon=True)
                worker.start()
                permit._cancel_turn()
            worker.join(3)
            assert not worker.is_alive() and outcomes == [False]
        else:
            monkeypatch.setattr(RoutedResponseDeferredSelection, "run", pause)
            worker = threading.Thread(target=lambda: outcomes.append(consume(f, selected)), daemon=True)
            worker.start()
            assert entered.wait(3)
            canceller = threading.Thread(target=lambda: (permit._cancel_turn(), cancelled.set()), daemon=True)
            canceller.start()
            assert not cancelled.wait(0.05)
            release.set()
            worker.join(3)
            canceller.join(3)
            assert not worker.is_alive() and not canceller.is_alive()
            assert outcomes == [True] and cancelled.is_set()
        return selected

    monkeypatch.setattr(CommandFeedbackReadyDecisionAcknowledgement, "resolve_ready_decision", intercept)
    f.route.dispatch(PHRASE)
    assert not f.outputs


def test_closed_proof_cannot_reissue_even_the_original_deferred_binding(monkeypatch):
    f = setup(immediate=True)
    saved = {}
    authorize = TrustedKoreanResponseAuthorizer.authorize

    def capture(authorizer, *, decision, event, proof):
        result = authorize(authorizer, decision=decision, event=event, proof=proof)
        saved.update(proof=proof, event=event, owner=authorizer._owner, decision=result)
        return result

    monkeypatch.setattr(TrustedKoreanResponseAuthorizer, "authorize", capture)
    f.route.dispatch(PHRASE)
    assert not saved["proof"].is_open
    decision = saved["decision"]
    binding = decision.response_publication_acknowledgement._trusted_response_binding
    assert saved["proof"].issue_response_emission_capability(
        saved["event"], saved["owner"], text="forged", source="minecraft_chatclef",
        response_kind="command_coalesced", deferred_selection=binding.selection()) is None
    assert len(f.outputs) == 1


def test_companion_logger_failure_cannot_change_selected_output(monkeypatch, caplog):
    import core.logger
    log = core.logger.log_print

    def fail_only_companion(message, *args, **kwargs):
        if "event=command_coalesced_output_authority" in message:
            raise RuntimeError("fixture output unavailable")
        return log(message, *args, **kwargs)

    monkeypatch.setattr(core.logger, "log_print", fail_only_companion)
    f = setup(immediate=True)
    f.route.dispatch(PHRASE)
    assert len(f.outputs) == 1
    assert "command_coalesced" == f.outputs[0]["response_kind"]


def test_required_coalesced_decision_is_physically_emitted_once(tmp_path):
    destination = tmp_path / "coalesced_decision.log"
    logger = logging.getLogger("LAV")
    handler = logging.FileHandler(destination, encoding="utf-8")
    handler.setFormatter(logging.Formatter("%(asctime)s [%(levelname)s] %(message)s"))
    logger.addHandler(handler)
    try:
        f = setup(immediate=True)
        f.route.dispatch(PHRASE)
        handler.flush()
        actual = destination.read_text(encoding="utf-8")
        assert actual.count("event=command_coalesced_output_authority") == 1
        assert "old_kind=command_start new_kind=command_coalesced accepted=true reason=selected" in actual
        assert f.outputs[0]["event_id"] in actual
        assert len(f.outputs) == 1
    finally:
        logger.removeHandler(handler)
        handler.close()
