#20260915_kpopmodder: Exercise canonical binding, actual UTF-8 log output, and Korean listener delivery.
import json
import logging
from types import SimpleNamespace
from unittest.mock import patch

import pytest

from core.logger import log_print, logger
from plugins.Minecraft.fabric.chatclef.input.routing.find.find_translation_binding_stage import FindTranslationBindingStage
from plugins.Minecraft.fabric.chatclef.intent.chatclef_natural_language_service import ChatClefNaturalLanguageService
from plugins.Minecraft.fabric.chatclef.result.find import FindTerminalPayload
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import CommandFeedbackDescriptorFactory
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.descriptor.raw_form.grammar.command_feedback_raw_scalar_grammar import CommandFeedbackRawScalarGrammar
from . import test_find_terminal_delivery as delivery
from .fixtures import descriptor, terminal_data


def event(text='철골램 찾아줘', source='lavi_chat_ui'):
    return SimpleNamespace(text=text, source=source, provider_id='VoiceInput' if source=='voice_input_final' else source,
        event_kind='final_transcript' if source=='voice_input_final' else 'chat_submit', final=True, event_id='f'*32)


@pytest.mark.parametrize('command', (
    'find entity minecraft:cow approach', 'find item minecraft:iron_golem approach',
    'find entity minecraft:iron_golem report', 'find auto minecraft:iron_golem approach',
    'find entity other:iron_golem approach', 'find entity minecraft:irongolem approach',
))
def test_canonical_command_tampering_is_rejected_by_admission_and_feedback(command):
    e=event(); t=ChatClefNaturalLanguageService().translate(e.text).to_dict();t['command']=command
    outcome=FindTranslationBindingStage(live_proof_validator=lambda *_:True).inspect(event=e,proof=object(),translation=t)
    assert outcome.reason=='find_original_request_mismatch'
    assert CommandFeedbackDescriptorFactory().from_trusted_translation(event=e,translation=t) is None


@pytest.mark.parametrize('source', ('lavi_chat_ui','voice_input_final'))
def test_golem_terminal_uses_python_korean_label_and_delivers_once(source):
    with patch.object(delivery,'descriptor',lambda s:descriptor(s,'철골램 찾아줘')):
        f=delivery.fixture(source)
    data=terminal_data(query='minecraft:iron_golem')
    data['find'].update(registry_id='minecraft:iron_golem',label='minecraft:iron_golem')
    assert f.ack.acknowledge(published=True)
    message=delivery.envelope(f,data=data)
    f.graph.command_result_handler.handle(f.websocket,message)
    f.graph.command_result_handler.handle(f.websocket,message)
    assert f.output_payloads==f.tts_payloads and len(f.tts_payloads)==1
    assert f.tts_payloads[0]['text']=='철 골렘 근처에 도착했어. 대상 좌표 10, 64, -20.'
    assert f.ui_queue.snapshot()[1][0].content==f.tts_payloads[0]['text']


def test_returned_registry_id_must_match_requested_id():
    data=terminal_data();data['find']['registry_id']='minecraft:zombie'
    assert FindTerminalPayload.from_data(data) is None
    data=terminal_data(query='minecraft:iron_golem');data['find']['registry_id']='other:iron_golem'
    assert FindTerminalPayload.from_data(data) is None


@pytest.mark.parametrize('args,expected', (
    (('entity','minecraft:iron_golem','approach'),'find_target'),
    (('entity','iron_golem','report'),'find_target'),
    (('entity','철골램','approach'),None),
    (('entity','Iron','Golem'),None),
    (('player','Steve'),'find_target'),
))
def test_native_raw_descriptor_is_id_only(args,expected):
    assert CommandFeedbackRawScalarGrammar().decode('find_target','find',args)==expected


def test_actual_logger_utf8_formatter_and_file_output(tmp_path):
    path=tmp_path/'find_boundary.log'
    handler=logging.FileHandler(path,encoding='utf-8-sig')
    handler.setFormatter(logger.handlers[0].formatter)
    logger.addHandler(handler)
    e=event(); t=ChatClefNaturalLanguageService().translate(e.text).to_dict()
    try:
        stage=FindTranslationBindingStage(live_proof_validator=lambda *_:True,log_callback=log_print)
        assert stage.inspect(event=e,proof=object(),translation=t) is None
        handler.flush()
    finally:
        logger.removeHandler(handler);handler.close()
    lines=path.read_text(encoding='utf-8-sig').splitlines()
    assert len(lines)==1 and '[INFO]' in lines[0]
    payload=json.loads(lines[0].split('[LAVI FIND Input] ',1)[1])
    assert payload['query']=='철골램'
    assert payload['registry_id']=='minecraft:iron_golem' and payload['kind']=='entity'
    assert payload['source']=='json_alias' and payload['reason']=='original_find_matched'
    assert len(payload['vocabulary_sha256'])==64
    assert payload['asset_status'] in ('local_asset_loaded','bundled_names_only')
    assert len(lines[0])<2400


def test_diagnostic_values_are_bounded_even_for_rejected_external_data():
    lines=[]; stage=FindTranslationBindingStage(live_proof_validator=lambda *_:True,log_callback=lines.append)
    stage.inspect(event=event(),proof=object(),translation={'executable':False,'reason_code':'x'*10000,
        'data':{'find_resolution':{'query':'y'*10000,'alias_count':[1]*10000,'registry_id':'z'*10000}}})
    assert len(lines)==1 and len(lines[0])<700
    payload=json.loads(lines[0].split('[LAVI FIND Input] ',1)[1])
    assert payload['query']=='철골램' and payload['reason']=='find_original_request_mismatch'
    assert 'alias_count' not in payload and 'registry_id' not in payload
    lines.clear()
    stage._emit(event(), 'x'*10000, {'query':'y'*10000,'alias_count':[1]*10000})
    payload=json.loads(lines[0].split('[LAVI FIND Input] ',1)[1])
    assert len(payload['query'])==128 and len(payload['reason'])==64 and 'alias_count' not in payload


@pytest.mark.parametrize('text,expected', (
    ('철골램 찾아줘','철 골렘을 찾아서 가까이 갈게'),
    ('소 찾아줘','소를 찾아서 가까이 갈게'),
    ('돼지 찾아줘','돼지를 찾아서 가까이 갈게'),
    ('철골램 위치만 알려줘','철 골렘을 찾아서 위치를 알려줄게'),
))
def test_start_uses_official_korean_label_not_the_wire_id(text,expected):
    from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import CommandLifecycleResponseRenderer
    assert CommandLifecycleResponseRenderer().render_start(descriptor(text=text))==expected


@pytest.mark.parametrize('voice',(False,True))
@pytest.mark.parametrize('text',('골렘 찾아줘','모르는 몬스터 찾아줘'))
def test_unknown_korean_name_is_consumed_without_command_or_llm_repair(voice,text):
    from plugins.Minecraft.fabric.chatclef.extension import MinecraftFabricChatClefExtension
    from tests.minecraft_chatclef.lavi_input.test_trusted_korean_chat_voice_integration import (
        _RecordingMinecraftAdapter,_llm_harness,_dispatch_goto_input,
    )
    adapter=_RecordingMinecraftAdapter()
    extension=MinecraftFabricChatClefExtension(adapter=adapter,natural_language_service=ChatClefNaturalLanguageService())
    llm,pipeline,_=_llm_harness(extension)
    _dispatch_goto_input(llm,text,voice=voice)
    assert adapter.requests==[] and pipeline.provider_calls==0
