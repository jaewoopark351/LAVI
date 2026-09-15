#20260915_kpopmodder: Verify Python-owned vocabulary independently of GET's task whitelist and Java gameplay.
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
from pathlib import Path
import shutil
import unicodedata

import pytest

from plugins.Minecraft.fabric.chatclef.intent.chatclef_natural_language_service import ChatClefNaturalLanguageService
from plugins.Minecraft.fabric.chatclef.intent.navigation.find import FindRequest, KoreanFindRuleParser
from plugins.Minecraft.fabric.chatclef.intent.navigation.find.find_name_repository import FindNameRepository
from plugins.Minecraft.fabric.chatclef.intent.navigation.find.find_target_resolver import FindTargetResolver, FindTargetResolutionError

RESOURCES = Path(__file__).resolve().parents[3] / 'plugins/Minecraft/fabric/chatclef/intent/resources'
ENTITY_FILE = json.loads((RESOURCES / 'korean_find_entity_names.json').read_text(encoding='utf-8'))


@pytest.fixture(scope='module')
def resolver():
    return FindTargetResolver(FindNameRepository(asset_index_paths=()))


def copy_resources(tmp_path):
    directory = tmp_path / 'names'
    directory.mkdir()
    for name in ('korean_find_entity_names.json', 'korean_find_aliases.json', 'chatclef_item_command_target_policy.json'):
        shutil.copyfile(RESOURCES / name, directory / name)
    return directory


def write_aliases(directory, mutate):
    path = directory / 'korean_find_aliases.json'
    value = json.loads(path.read_text(encoding='utf-8'))
    mutate(value)
    path.write_text(json.dumps(value, ensure_ascii=False), encoding='utf-8')


def write_asset(tmp_path, language):
    root = tmp_path / 'assets'
    raw = json.dumps(language, ensure_ascii=False).encode('utf-8')
    digest = hashlib.sha1(raw).hexdigest()
    asset = root / 'objects' / digest[:2] / digest
    asset.parent.mkdir(parents=True, exist_ok=True)
    asset.write_bytes(raw)
    index = root / 'indexes/5.json'
    index.parent.mkdir(parents=True, exist_ok=True)
    index.write_text(json.dumps({'objects': {'minecraft/lang/ko_kr.json': {'hash': digest, 'size': len(raw)}}}), encoding='utf-8')
    return index, asset


def test_bundled_entity_ids_match_the_pinned_complete_1201_registry_blob():
    names = ENTITY_FILE['entity']
    assert len(names) == 124
    raw = (json.dumps(sorted(key.split(':', 1)[1] for key in names), indent=2) + '\n').encode()
    git_blob = b'blob ' + str(len(raw)).encode() + b'\0' + raw
    assert hashlib.sha1(git_blob).hexdigest() == 'fef0b3bb2820bfabc26825426ae867e499e8cabd'
    assert 'minecraft:killer_bunny' not in names  # A display-only locale key, not an entity registry type.


@pytest.mark.parametrize('identifier,label', tuple(ENTITY_FILE['entity'].items()))
def test_every_vanilla_entity_korean_name_resolves_with_explicit_kind(resolver, identifier, label):
    result = resolver.resolve(FindRequest('entity', label))
    assert result.request == FindRequest('entity', identifier)
    assert result.label == label


@pytest.mark.parametrize('text,expected', (
    ('철골램 찾아줘', 'find entity minecraft:iron_golem approach'),
    ('철 골램 찾아줘', 'find entity minecraft:iron_golem approach'),
    ('철골렘 찾아줘', 'find entity minecraft:iron_golem approach'),
    ('아이언 골렘 위치만 알려줘', 'find entity minecraft:iron_golem report'),
    ('소 찾아줘', 'find entity minecraft:cow approach'),
    ('양 찾아줘', 'find entity minecraft:sheep approach'),
    ('돼지 찾아줘', 'find entity minecraft:pig approach'),
    ('눈골램 찾아줘', 'find entity minecraft:snow_golem approach'),
    ('떨어진 다야 찾아줘', 'find item minecraft:diamond approach'),
    ('떨어진 상자 찾아줘', 'find item minecraft:chest approach'),
    ('상자 블록 찾아줘', 'find block minecraft:chest approach'),
    ('다이아 블록 찾아줘', 'find block minecraft:diamond_block approach'),
    ('다이아 광석 찾아줘', 'find block minecraft:diamond_ore approach'),
    ('주민 찾아서 알려줘', 'find entity minecraft:villager report'),
    ('플레이어 Steve 찾아줘', 'find player Steve approach'),
    ('엔티티 mod:unlisted_monster 찾아줘', 'find entity mod:unlisted_monster approach'),
    ('아이템 mod:new_gem 찾아줘', 'find item mod:new_gem approach'),
    ('블록 mod:new_block 찾아줘', 'find block mod:new_block approach'),
))
def test_actual_service_compiles_names_to_only_canonical_ids(text, expected):
    translated = ChatClefNaturalLanguageService().translate(text)
    assert translated.executable, translated.to_dict()
    assert translated.command == expected
    assert translated.intent.original_text == text
    assert translated.intent.slots == KoreanFindRuleParser().parse(text).slots
    assert translated.data['find_resolution']['vocabulary_sha256']
    assert all(ord(c) < 128 for c in translated.command)


@pytest.mark.parametrize('query,identifier', (
    ('iron_golem','minecraft:iron_golem'),('minecraft:iron_golem','minecraft:iron_golem'),
    ('mod:foo_bar','mod:foo_bar'),('mod:foobar','mod:foobar'),
    ('mod:folder/item','mod:folder/item'),('unlisted_vanilla_id','minecraft:unlisted_vanilla_id'),
))
def test_ids_never_use_get_whitelist_or_remove_underscores(resolver, query, identifier):
    assert resolver.resolve(FindRequest('item', query)).request.query == identifier


@pytest.mark.parametrize('query', ('골렘','철골','돼','모르는 동물','철골랭','다이아몬드 아닌 것'))
def test_unknown_names_are_not_guessed(resolver, query):
    with pytest.raises(FindTargetResolutionError, match='find_unknown_name'):
        resolver.resolve(FindRequest('auto', query))


def test_unicode_and_whitespace_normalization_is_only_for_names(resolver):
    for text in ('철 골램', '철골램', unicodedata.normalize('NFD', '철 골램'), '아이언골렘'):
        assert resolver.resolve(FindRequest('entity', text)).request.query == 'minecraft:iron_golem'
    with pytest.raises(FindTargetResolutionError, match='find_invalid_registry_id'):
        resolver.resolve(FindRequest('entity', 'Minecraft:iron_golem'))


def test_mod_aliases_and_collisions_are_data_only_and_instance_isolated(tmp_path):
    directory=copy_resources(tmp_path)
    write_aliases(directory, lambda data: data['entity'].update({'mod:giant_cow':['거대 소'], 'mod:iron_golem':['철 골렘']}))
    first=FindTargetResolver(FindNameRepository(directory, asset_index_paths=()))
    assert first.resolve(FindRequest('entity','거대 소')).request.query == 'mod:giant_cow'
    with pytest.raises(FindTargetResolutionError) as error:
        first.resolve(FindRequest('entity','철 골렘'))
    assert error.value.code == 'find_ambiguous_name'
    assert set(error.value.suggestions) == {'entity mod:iron_golem','entity minecraft:iron_golem'}
    write_aliases(directory, lambda data: data['entity'].update({'mod:giant_cow':['큰 소']}))
    second=FindTargetResolver(FindNameRepository(directory, asset_index_paths=()))
    assert first.resolve(FindRequest('entity','거대 소')).request.query == 'mod:giant_cow'
    assert second.resolve(FindRequest('entity','큰 소')).request.query == 'mod:giant_cow'
    with pytest.raises(FindTargetResolutionError): second.resolve(FindRequest('entity','거대 소'))
    with pytest.raises(TypeError): first.repository.names[('entity','minecraft:cow')]='변조'
    with ThreadPoolExecutor(max_workers=4) as pool:
        results=list(pool.map(lambda _: first.resolve(FindRequest('entity','거대 소')).request.query, range(40)))
    assert set(results)=={'mod:giant_cow'}
    assert first.repository.diagnostics['vocabulary_sha256'] != second.repository.diagnostics['vocabulary_sha256']


def test_local_language_asset_supplies_names_outside_get_and_is_read_only(tmp_path):
    index, asset=write_asset(tmp_path, {'item.minecraft.test_only_relic':'검증 유물',
        'block.minecraft.test_only_block':'검증 블록','entity.minecraft.cow':'자원 소',
        'entity.minecraft.killer_bunny':'살인 토끼','entity.minecraft.cow.variant':'변종',
        'item.minecraft.lodestone_compass':'자석석 나침반', 'gui.some.button':'가짜 버튼'})
    before={p: p.read_bytes() for p in index.parent.parent.rglob('*') if p.is_file()}
    names=FindNameRepository(asset_index_paths=(index,)); resolved=FindTargetResolver(names)
    assert resolved.resolve(FindRequest('item','검증 유물')).request.query=='minecraft:test_only_relic'
    assert resolved.resolve(FindRequest('block','검증 블록')).request.query=='minecraft:test_only_block'
    assert resolved.resolve(FindRequest('entity','자원 소')).request.query=='minecraft:cow'
    assert names.diagnostics['asset_status']=='local_asset_loaded'
    assert names.diagnostics['asset_warnings']==0
    assert ('entity','minecraft:killer_bunny') not in names.names
    assert ('item','minecraft:lodestone_compass') not in names.names
    after={p: p.read_bytes() for p in index.parent.parent.rglob('*') if p.is_file()}
    assert before==after  # Vocabulary is NOT proof these test-only IDs exist in a live registry.


def test_configured_asset_path_loads_instead_of_guessing_an_installation(tmp_path):
    directory=copy_resources(tmp_path)
    index,_=write_asset(tmp_path, {'item.minecraft.test_only_relic':'검증 유물'})
    write_aliases(directory, lambda data: data.update(asset_index_paths=[str(index)]))
    names=FindNameRepository(directory)
    assert names.diagnostics['asset_status']=='local_asset_loaded'
    assert names.names[('item','minecraft:test_only_relic')]=='검증 유물'


@pytest.mark.parametrize('failure', ('missing','corrupt','traversal','duplicate','oversize','bad_root'))
def test_invalid_asset_is_bounded_and_falls_back_without_aborting_known_names(tmp_path,failure):
    index,asset=write_asset(tmp_path, {'item.minecraft.test_only_relic':'검증 유물'})
    if failure=='missing': asset.unlink()
    elif failure=='corrupt': asset.write_bytes(b'wrong content')
    elif failure=='traversal': index.write_text(json.dumps({'objects':{'minecraft/lang/ko_kr.json':{'hash':'../../sensitive'}}}),encoding='utf-8')
    elif failure=='duplicate': index.write_text('{"objects":{},"objects":{}}',encoding='utf-8')
    elif failure=='oversize': index.write_bytes(b' '*(FindNameRepository._LIMIT+1))
    elif failure=='bad_root': index.write_text('[]',encoding='utf-8')
    names=FindNameRepository(asset_index_paths=(index,))
    assert names.diagnostics['asset_status']=='bundled_names_only'
    assert names.diagnostics['asset_warnings']==1
    assert FindTargetResolver(names).resolve(FindRequest('entity','철골램')).request.query=='minecraft:iron_golem'


@pytest.mark.parametrize('mutate', (
    lambda d:d.update(schema_version=True), lambda d:d.update(minecraft_version='1.21'),
    lambda d:d.update(unknown=True), lambda d:d.update(asset_index_paths=[1]),
    lambda d:d['entity'].update({'minecraft:cow':['소\n공격']}),
    lambda d:d['entity'].update({'minecraft:cow':['소\u200b']}),
    lambda d:d['entity'].update({'minecraft:cow':[' ']}),
    lambda d:d['entity'].update({'minecraft:cow':[]}),
    lambda d:d['entity'].update({'minecraft:cow':['a'*129]}),
    lambda d:d['entity'].update({'minecraft:cow;stop':['소']}),
    lambda d:d['entity'].update({'minecraft:cow':'소'}),
))
def test_alias_json_is_strict_and_does_not_silently_skip_invalid_entries(tmp_path,mutate):
    directory=copy_resources(tmp_path);write_aliases(directory,mutate)
    with pytest.raises(ValueError):FindNameRepository(directory,asset_index_paths=())


def test_get_does_not_load_find_json_even_when_find_loader_fails(monkeypatch):
    def unavailable(*args,**kwargs): raise OSError('name resource intentionally missing')
    monkeypatch.setattr(FindNameRepository,'__init__',unavailable)
    result=ChatClefNaturalLanguageService().translate('다이아몬드 곡괭이 1개 만들어줘')
    assert result.executable and result.command=='get diamond_pickaxe 1'
    result=ChatClefNaturalLanguageService().translate('철골램 찾아줘')
    assert not result.executable and result.command is None
