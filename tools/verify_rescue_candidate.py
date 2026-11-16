"""Packaged asset/recording audit only; never a story/playthrough completion certificate."""
from pathlib import Path
import argparse, hashlib, io, json, zipfile
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('--jar',required=True);args=parser.parse_args()
jar=(ROOT/args.jar).resolve();prior=ROOT/'build-character-cast/libs/coraline-1.15.73-placeholder-audio.jar'
if not jar.is_relative_to(ROOT/'build-character-cast/libs') or jar==prior:raise RuntimeError('Audit an explicit new candidate, not the preserved baseline')
sha=lambda data:hashlib.sha256(data).hexdigest()
with zipfile.ZipFile(jar) as archive,zipfile.ZipFile(prior) as old:
    assert archive.testzip() is None,'JAR integrity'
    names=set(archive.namelist());prefix='assets/coraline/'
    assert not any('GameTests' in name or 'VisualCheck' in name or 'FixtureClock' in name or '/qa/' in name or name.startswith(('artwork/','voicelines/')) or name.endswith('.blend') for name in names),'Development/master artifacts leaked'
    for owner in ('RescueChapter','RescueEncounters','RescueBosses','BeldamFinale','RescueEpilogue','RescueWebSite','RescuePassage','RescueSiteCleanup','StoryItemCustody','RescueWebHearing','StoryPlaybackClock'):
        assert 'com/coraline/feast/'+owner+'.class' in names,'Missing integrated controller '+owner
    assert 'com/coraline/client/renderer/ParentReunionLayer.class' in names,'Missing actual parent frost/melt geometry'
    for observer in ('PlayedAudioFrames','ContinuousAudioStream','StoryAudioMixerCursor'):
        assert 'com/coraline/client/audio/'+observer+'.class' in names,'Missing actual mixer observer '+observer
    assert 'StoryAudioChannelMixin' in json.loads(archive.read('coraline.mixins.json'))['client'],'Mixer observer hook not registered'
    assert b'SimpleChannel' in archive.read('com/coraline/feast/ShowPlaybackNetwork.class'),'Test-only transport replaced production network'
    assert 'net/minecraft/client/Minecraft.class' not in names,'Unit-test Minecraft double leaked into mod'
    assert b'ReunionFrost' in archive.read('com/coraline/parents/ParentEntity.class'),'Missing saved/synchronized real-parent frost state'
    assert 'RescueWebMovementMixin' in json.loads(archive.read('coraline.mixins.json'))['mixins'] and 'com/coraline/mixin/RescueWebMovementMixin.class' in names,'Accepted-movement hearing hook missing'
    for name in ('story_subtitles.json','numbered_event_dialogue.json'):
        assert archive.read(prefix+name)==old.read(prefix+name),'Deferred existing captions changed'
    sounds=json.loads(archive.read(prefix+'sounds.json'));lengths=json.loads(archive.read(prefix+'visit_audio_lengths.json'));envelopes=json.loads(archive.read(prefix+'visit_voice_envelopes.json'));receipts=json.loads(archive.read(prefix+'rescue_chapters_audio.json'))
    assert len(receipts)==33,'Incomplete supplied-recording set'
    for event,receipt in receipts.items():
        master=(ROOT/receipt['source']).resolve();assert master.is_relative_to(ROOT) and sha(master.read_bytes())==receipt['source_sha256'],'Original master changed '+event
        assert lengths[event]==receipt['ticks'],'Missing or altered packaged duration '+event
        path=sounds[event]['sounds'][0]['name'].removeprefix('coraline:');raw=archive.read(prefix+'sounds/'+path+'.ogg')
        if event!='rescue_theatre_music':assert sha(raw)==receipt['ogg_sha256'],'Original export changed '+event
        if event!='rescue_theatre_music':assert event in envelopes and envelopes[event],'Missing merged speech RMS envelope '+event
    assert sounds['rescue_theatre_music']['sounds']==sounds['theatre_music']['sounds'],'Theatre recording was duplicated or playback changed'
    old_audio={name for name in old.namelist() if name.startswith(prefix+'sounds/') and name.endswith('.ogg')}
    assert all(name in names and archive.read(name)==old.read(name) for name in old_audio),'A preserved packaged recording changed or disappeared'
    pngs=0;jsons=0
    for name in names:
        if name.endswith('.json'):json.loads(archive.read(name));jsons+=1
        if name.endswith('.png'):
            with Image.open(io.BytesIO(archive.read(name))) as picture:picture.load();pngs+=1
    sky=prefix+'textures/sky/celestial_panorama_v1.png'
    with Image.open(io.BytesIO(archive.read(sky))) as picture:
        assert picture.size==(1774,887),'Panorama lost its native environment resolution'
        assert len(picture.getcolors(picture.width*picture.height) or [])>1000,'Sky is blank/flat'
    web=json.loads(archive.read(prefix+'blockstates/story_web.json'))['variants'];assert all('height='+str(height) in web for height in range(1,17)),'Missing web collision-height render variant'
    largest=sorted(archive.infolist(),key=lambda entry:entry.compress_size,reverse=True)[:10]
    compressed_assets=sum(entry.compress_size for entry in archive.infolist() if entry.filename.startswith('assets/'))
    report={'scope':'Optimized packaged assets/recordings only; NOT complete gameplay, game-camera, audible or FPS acceptance','jar':str(jar),'sha256':sha(jar.read_bytes()),'bytes':jar.stat().st_size,'baseline_bytes':prior.stat().st_size,'delta_bytes':jar.stat().st_size-prior.stat().st_size,'delta_percent':round((jar.stat().st_size/prior.stat().st_size-1)*100,3),'compressed_runtime_assets':compressed_assets,'size_warning_reviewed':compressed_assets>64*1024*1024,'preserved_prior_packaged_oggs':len(old_audio),'new_original_events':len(receipts),'pngs_decoded':pngs,'jsons_parsed':jsons,'sky_runtime_size':[1774,887],'sky_master_sha256':sha((ROOT/'artwork/celestial_sky_v1/master.png').read_bytes()),'existing_captions_preserved':True,'largest_entries':[{'path':entry.filename,'compressed_bytes':entry.compress_size,'unchanged_from_baseline':entry.filename in old.namelist() and archive.read(entry.filename)==old.read(entry.filename)} for entry in largest]}
target=ROOT/'build-character-cast/reports'/('rescue-candidate-'+jar.stem+'.json');target.write_text(json.dumps(report,indent=2)+'\n',encoding='utf8');print(json.dumps(report,indent=2));print('Report: '+str(target))
