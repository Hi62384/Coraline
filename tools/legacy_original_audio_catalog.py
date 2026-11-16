"""Supplied earlier dialogue masters, including the two garden-flight lines.

This is an explicit speech inventory, not a wildcard over voice-clone masters,
normal parents (deliberately uncaptioned), music, or reference samples.
"""
from pathlib import Path
import hashlib
import json
from import_numbered_original_audio import ogg_measure


def sources():
    result={f'wybie_line_{letter.lower()}':('voicelines/'+letter+'.wav','Wybie') for letter in 'ABC'}
    for name,file,speaker in (
        ('other_mother_intro','OtherMotherIntro.wav','Other Mother'),
        ('other_father_enter_scene','OtherFatherEnterScene.wav','Other Father'),
        ('other_father_intro','OtherFatherIntro.wav','Other Father'),
        ('other_father_dinners_ready','OtherFatherDinnersReady.wav','Other Father'),
        ('other_father_golden_chicken_breast','OtherFatherGoldenChickenBreast.wav','Other Father'),
        ('other_parents_waiting','OtherParentsWeveBeenWaitingForYou.wav','Other Parents'),
        ('other_mother_visit_again','OtherMotherVisitAgain.mp3','Other Mother'),
        ('see_you_soon','SeeYouSoon.wav','Other Mother'),
    ):result[name]=('voicelines/'+file,speaker)
    for index in range(3):result[f'other_mother_supper_{index}']=(f'voicelines/OtherMotherSupper{index}.wav','Other Mother')
    for character,folder,letters,speaker in (
        ('wybie','Wybie','ABCDEFGHIJKL','Wybie'),
        ('bobinsky','bobinsky','ABCDEFGHIJK','Mr. Bobinsky'),
        ('tea','MissSpinkAndForcible','ABCDEFGHIJ','Miss Spink & Miss Forcible'),
    ):
        for letter in letters:result[f'{character}_visit_{letter.lower()}']=(f'voicelines/{folder}/{letter}.wav',speaker)
    result['magic_garden_hop_on']=('voicelines/otherfather/activities/magic_garden/HopOnKiddo.mp3','Other Father')
    result['magic_garden_mother_said']=('voicelines/otherfather/activities/magic_garden/MotherSaidYoudLikeIt.mp3','Other Father')
    return result


def snapshot(root):
    """Read-only initial pin builder; publishing its JSON uses normal file edits."""
    root=Path(root);assets=root/'src/main/resources/assets/coraline'
    sounds=json.loads((assets/'sounds.json').read_text(encoding='utf-8'));result={}
    for name,(source,speaker) in sources().items():
        entries=sounds[name]['sounds']
        if len(entries)!=1:raise RuntimeError('Ambiguous supplied legacy recording: '+name)
        entry=entries[0];resource=entry if isinstance(entry,str) else entry['name']
        if not resource.startswith('coraline:'):raise RuntimeError('Nonlocal supplied recording: '+name)
        resource=resource.removeprefix('coraline:');payload=(assets/'sounds'/str(resource+'.ogg')).read_bytes()
        decoded=ogg_measure(payload)
        with (root/source).open('rb') as stream:digest=hashlib.file_digest(stream,'sha256').hexdigest()
        result[name]={'source':source,'source_sha256':digest,'ogg_sha256':hashlib.sha256(payload).hexdigest(),
            'resource':resource,'source_seconds':decoded['seconds'],'music':False,'speaker':speaker}
    return result
