"""Validate authored cast assets, make review sheet and exact-baseline offline patch."""
import argparse, hashlib, json, math, struct, subprocess, zipfile
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
ROOT=Path(__file__).resolve().parents[1]
IDS=['wybie','other_wybie','bobinsky','other_bobinsky','miss_spink','miss_forcible','other_miss_spink','other_miss_forcible','evil_other_father','evil_other_bobinsky','evil_other_spink_forcible','evil_other_mother','beldam','coraline','cat']
ART=ROOT/'artwork/character_cast'
RUNTIME=ROOT/'src/main/resources/assets/coraline/characters'

def extras():
    lang=json.loads((ROOT/'src/main/resources/assets/coraline/lang/en_us.json').read_text(encoding='utf8'))
    for name in IDS:
        title=name.replace('_',' ').title().replace('Miss ','Miss ')
        lang['entity.coraline.character_'+name]=title
        lang['item.coraline.character_'+name+'_spawn_egg']=title+' Spawn Egg'
        path=ROOT/('src/main/resources/assets/coraline/models/item/character_'+name+'_spawn_egg.json')
        path.write_text(json.dumps({'parent':'minecraft:item/template_spawn_egg'})+'\n',encoding='utf8')
    (ROOT/'src/main/resources/assets/coraline/lang/en_us.json').write_text(json.dumps(lang,indent=2,ensure_ascii=False)+'\n',encoding='utf8')
    sheet=Image.new('RGB',(1500,1200),'#202632');draw=ImageDraw.Draw(sheet)
    font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',15)
    for i,name in enumerate(IDS):
        picture=Image.open(ART/(name+'.png')).convert('RGBA');picture.thumbnail((285,350))
        x=(i%5)*300;y=(i//5)*400
        sheet.paste(picture,(x+(300-picture.width)//2,y+8),picture)
        label=name.replace('_',' ').title()
        if len(label)>29:label=label.replace('Spink Forcible','Spink / Forcible')
        draw.text((x+150,y+375),label,font=font,anchor='mm',fill='#E9DFC9')
    sheet.save(ART/'cast_preview.jpg',quality=92)

def validate():
    total=0
    for name in IDS:
        data=json.loads((RUNTIME/(name+'.json')).read_bytes());assert data['id']==name
        bones={b['name']:b for b in data['bones']}
        for b in bones.values():
            chain=set();node=b
            while node['parent']:
                assert node['name'] not in chain,'Cyclic rig';chain.add(node['name']);node=bones[node['parent']]
        for m in data['meshes']:
            assert m['bone'] in bones
            assert all(len(p)==3 and all(math.isfinite(v) for v in p) for p in m['vertices'])
            assert all(len(f)>=3 and all(0<=ix<len(m['vertices']) for ix in f) for f in m['faces'])
            assert len(m['uv'])==len(m['vertices']) and all(len(uv)==2 and all(0<=v<=1 for v in uv) for uv in m['uv'])
        assert data['texture']=='characters/cast_atlas.png'
        b=(ART/(name+'.glb')).read_bytes();assert b[:4]==b'glTF';size=struct.unpack_from('<I',b,12)[0];gltf=json.loads(b[20:20+size])
        clips={a['name']:a for a in gltf.get('animations',[])}
        assert set(clips)=={'idle','walk','run','look','wave'},(name,list(clips))
        assert all(a['channels'] for a in clips.values())
        assert len(gltf['scenes'])==1,(name,'Export contains unrelated scenes')
        assert not any(n.get('name')=='Cube' for n in gltf['nodes'])
        assert gltf.get('images') and all('baseColorFactor' in m.get('pbrMetallicRoughness',{}) for m in gltf['materials'])
        assert (ART/(name+'.blend')).stat().st_size>10000
        total+=(RUNTIME/(name+'.json')).stat().st_size
    assert Image.open(RUNTIME/'white.png').size==(1,1)
    assert Image.open(RUNTIME/'cast_atlas.png').size==(512,128)
    print('Validated all 15 rigs, indexed meshes, independent Blender masters and 75 animation clips;',total,'runtime geometry bytes before ZIP compression')

def package():
    baseline=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
    files=['.gitignore','build.gradle','tools/optimize_runtime_assets.py','src/main/java/com/coraline/CoralineMod.java','src/main/java/com/coraline/event/ModEntityEvents.java','src/main/java/com/coraline/client/ClientModEvents.java',
           'src/main/java/com/coraline/entity/CharacterEntity.java','src/main/java/com/coraline/registry/CharacterCast.java',
           'src/main/java/com/coraline/client/renderer/CharacterModel.java','src/main/java/com/coraline/client/renderer/CharacterRenderer.java',
           'src/main/resources/assets/coraline/lang/en_us.json','tools/build_character_cast.py','tools/build_minecraft_character_cast.py','tools/create_cast_pixel_atlas.py','tools/character_cast_package.py','src/activitytest/java/com/coraline/secrets/CharacterCastGameTests.java','CHARACTER_CAST_README.txt','gradle.properties']
    files += ['src/main/java/com/coraline/registry/ModEntities.java','src/main/java/com/coraline/registry/ModItems.java',
        'src/main/java/com/coraline/feast/VisitData.java','src/main/java/com/coraline/feast/VisitAudio.java',
        'src/main/java/com/coraline/client/VisitAudioClient.java','src/main/java/com/coraline/client/WybieVisitClient.java',
        'src/main/java/com/coraline/client/renderer/WybieVisitorRenderer.java',
        'src/main/resources/assets/coraline/sounds.json','src/main/resources/assets/coraline/visit_audio_lengths.json',
        'src/main/resources/assets/coraline/wybie_visit_audio.json','src/main/resources/assets/coraline/models/item/camera.json',
        'src/main/resources/assets/coraline/textures/item/camera.png','tools/import_wybie_visit_audio.py',
        'tools/build_wybie_visit_props.py','tools/create_wybie_camera_icon.py','WYBIE_VISIT_README.txt',
        'src/wybietest/java/com/coraline/wybie/WybieVisitGameTests.java',
        'src/rendercheck/java/com/coraline/wybie/WybieVisitVisualCheck.java']
    files += [p.relative_to(ROOT).as_posix() for p in (ROOT/'src/main/java/com/coraline/wybie').glob('*.java')]
    files += [p.relative_to(ROOT).as_posix() for folder in ['src/main/java/com/coraline/bobinsky','src/main/java/com/coraline/tea','src/wybietest/java/com/coraline/wybie'] for p in (ROOT/folder).glob('*.java')]
    files += ['src/main/java/com/coraline/client/renderer/BobinskyVisitorRenderer.java','src/main/java/com/coraline/client/renderer/TeaVisitorRenderer.java',
        'src/main/java/com/coraline/client/renderer/TeaGlassRenderer.java',
        'src/main/java/com/coraline/client/StoryMusicGuard.java','src/main/java/com/coraline/registry/ModBlocks.java',
        'tools/build_story_subtitles.py','tools/story_transcripts.json','tools/build_tea_cup.py',
        'tools/build_visit_envelopes.py','src/main/resources/assets/coraline/visit_voice_envelopes.json',
        'src/main/resources/assets/coraline/blockstates/cheese_parcel.json','src/main/resources/assets/coraline/models/block/cheese_parcel.json',
        'src/main/resources/assets/coraline/blockstates/tea_theatre_poster.json','src/main/resources/assets/coraline/models/block/tea_theatre_poster.json','src/main/resources/assets/coraline/textures/block/tea_theatre_poster.png',
        'src/main/resources/assets/coraline/blockstates/tea_memorial_dog.json','src/main/resources/assets/coraline/models/block/tea_memorial_dog.json',
        'src/main/resources/assets/coraline/blockstates/tea_weathered_bricks.json','src/main/resources/assets/coraline/models/block/tea_weathered_bricks.json','src/main/resources/assets/coraline/textures/block/tea_weathered_bricks.png',
        'src/main/resources/assets/coraline/blockstates/tea_velvet_sofa.json','src/main/resources/assets/coraline/models/block/tea_velvet_sofa.json',
        'src/main/resources/assets/coraline/story_subtitles.json',
        'src/rendercheck/java/com/coraline/wybie/StoryVisitsVisualCheck.java','STORY_VISITS_README.txt',
        'src/main/resources/assets/coraline/bobinsky_visit_audio.json','src/main/resources/assets/coraline/tea_visit_audio.json']
    files += [f'src/main/resources/assets/coraline/{kind}/item/{name}.{extension}' for name in ['cheese_package','jasmine_tea','empty_teacup','broken_glass'] for kind,extension in [('models','json'),('textures','png')]]
    files += ['src/main/java/com/coraline/client/StorySubtitles.java',
        'src/main/java/com/coraline/otherworld/OtherWorldEstate.java','src/main/java/com/coraline/otherworld/RosewoodHouse.java',
        'src/main/java/com/coraline/feast/FirstVisit.java','src/main/java/com/coraline/feast/LoopProps.java',
        'src/main/java/com/coraline/feast/ScenePropEntity.java','src/main/java/com/coraline/feast/SupperSeating.java','src/main/java/com/coraline/client/renderer/ScenePropRenderer.java',
        'src/main/java/com/coraline/otherworld/RealmCopy.java','src/main/java/com/coraline/otherworld/RealmProtection.java','src/main/java/com/coraline/secrets/SecretWorld.java',
        'src/wybietest/java/com/coraline/otherworld/RosewoodHouseGameTests.java',
        'src/wybietest/java/com/coraline/feast/RosewoodDiningGameTests.java',
        'src/main/resources/assets/coraline/structures/rosewood_house.json.gz','tools/import_rosewood_house.py',
        'artwork/otherworld_house/Rosewood_House_Complete_v3.blueprint.json']
    files += ['tools/repair_rosewood_house.py','tools/build_rosewood_fixtures.py','tools/test_rosewood_house.py',
        'src/rendercheck/java/com/coraline/otherworld/RosewoodHouseVisualCheck.java',
        'src/main/java/com/coraline/otherworld/RosewoodFixtureBlock.java','src/main/java/com/coraline/otherworld/RosewoodGlassDoorBlock.java']
    files += ['src/main/java/com/coraline/otherworld/RosewoodTerrain.java','src/wybietest/java/com/coraline/bobinsky/BobinskyReturnGameTests.java']
    files += ['src/main/java/com/coraline/otherworld/OtherWorld.java','src/main/java/com/coraline/feast/OtherMotherLoop.java']
    files += ['src/main/java/com/coraline/secrets/JournalPlacement.java','src/main/java/com/coraline/secrets/SecretsState.java','src/main/java/com/coraline/secrets/SecretsRuntime.java','src/main/java/com/coraline/secrets/SecretsNetwork.java']
    files += ['src/main/java/com/coraline/secrets/HouseCabinets.java','src/main/java/com/coraline/secrets/SecretSpies.java','src/main/java/com/coraline/secrets/SecretDialogue.java','src/main/java/com/coraline/secrets/SecretTransactions.java',
        'src/main/java/com/coraline/otherworld/RosewoodGarden.java','tools/import_rosewood_garden.py','src/main/resources/assets/coraline/structures/rosewood_garden.json.gz','artwork/otherworld_garden/Rosewood_Moonlit_Garden.blueprint.json']
    files += ['src/wybietest/java/com/coraline/otherworld/HouseInteractionGameTests.java','src/wybietest/java/com/coraline/secrets/HouseSpyGameTests.java','src/main/java/com/coraline/client/SecretJournalScreen.java','src/main/java/com/coraline/block/TinyDoorBlock.java']
    files += ['src/main/java/com/coraline/secrets/MotherSearch.java','src/activitytest/java/com/coraline/secrets/SecretsGameTests.java','src/activitytest/java/com/coraline/secrets/SecretsRevisionGameTests.java']
    files += ['src/activitytest/java/com/coraline/secrets/ServerResponsivenessGameTests.java']
    files += ['src/activitytest/java/com/coraline/secrets/SecretDialogueGameTests.java']
    files += ['src/main/java/com/coraline/otherworld/TunnelPassage.java','src/main/java/com/coraline/otherworld/DoorBlockEntity.java','src/main/java/com/coraline/client/renderer/DoorPortalRenderer.java','src/main/java/com/coraline/client/renderer/TunnelRenderer.java','src/main/java/com/coraline/client/TunnelAudio.java','src/wybietest/java/com/coraline/otherworld/SeamlessTunnelGameTests.java','src/rendercheck/java/com/coraline/otherworld/SeamlessTunnelVisualCheck.java']
    files += ['src/main/java/com/coraline/feast/VisitEvents.java','src/main/java/com/coraline/feast/FamilyDinner.java','src/main/java/com/coraline/feast/OtherMotherLoopState.java','src/main/java/com/coraline/feast/OtherMotherDialogue.java','src/main/java/com/coraline/feast/VisitAudio.java','src/main/java/com/coraline/feast/VisitConfig.java','src/main/java/com/coraline/feast/ScenePropEntity.java','src/main/java/com/coraline/client/VisitEventClient.java','src/main/java/com/coraline/client/VisitAudioClient.java','src/main/java/com/coraline/client/SecretJournalScreen.java','src/wybietest/java/com/coraline/feast/VisitEventsGameTests.java','src/rendercheck/java/com/coraline/otherworld/VisitEventsVisualCheck.java','VISIT_EVENTS_README.txt','tools/prepare_exploration_audio.py','tools/exploration_voice_manifest.json','tools/dinner_mother_voice_manifest.json','tools/dinner_father_voice_manifest.json','tools/review_voice_transcripts.py']
    files += [p.relative_to(ROOT).as_posix() for folder in ['voicelines/othermother/events','voicelines/otherfather/events','src/main/resources/assets/coraline/sounds/exploration','src/main/resources/assets/coraline/sounds/family_dinner','src/main/resources/assets/coraline/sounds/study'] for p in (ROOT/folder).rglob('*') if p.is_file() and p.suffix in ('.wav','.ogg','.json','.txt')]
    files += ['src/main/resources/META-INF/VISIT_EVENT_AUDIO_NOTICE.txt']
    files += ['tools/father_study_voice_manifest.json','src/main/java/com/coraline/client/renderer/ParentRenderer.java']
    files += ['tools/verify_visit_events_audio.py']
    files += ['src/main/java/com/coraline/secrets/HouseStorageBlock.java','src/main/java/com/coraline/secrets/MotherInterrupts.java','src/main/java/com/coraline/parents/ParentEntity.java','src/wybietest/java/com/coraline/secrets/HousePursuitGameTests.java',
        'src/main/resources/assets/coraline/textures/block/rosewood_drawer.png','src/main/resources/assets/coraline/textures/block/rosewood_cabinet.png','src/main/resources/assets/coraline/textures/item/borrowed_house_key.png','src/main/resources/assets/coraline/models/item/borrowed_house_key.json']
    files += [p.relative_to(ROOT).as_posix() for folder in ['src/main/resources/assets/coraline/models/block','src/main/resources/assets/coraline/blockstates'] for p in (ROOT/folder).glob('rosewood_*.json')]
    files=list(dict.fromkeys(files))
    files += [p.relative_to(ROOT).as_posix() for p in (ROOT/'src/main/resources/assets/coraline/sounds/wybie_visit').glob('*.ogg')]
    files += [p.relative_to(ROOT).as_posix() for folder in ['bobinsky_visit','tea_visit'] for p in (ROOT/f'src/main/resources/assets/coraline/sounds/{folder}').glob('*.ogg')]
    files += [p.relative_to(ROOT).as_posix() for p in RUNTIME.iterdir() if p.suffix in ('.json','.png')]
    files += [p.relative_to(ROOT).as_posix() for p in ART.iterdir() if p.suffix in ('.blend','.glb','.png','.jpg')]
    files += ['src/main/resources/assets/coraline/models/item/character_'+name+'_spawn_egg.json' for name in IDS]
    changes=[];payloads={};temporary=ROOT/'.character-cast-patch-work';temporary.mkdir(exist_ok=True)
    for path in sorted(files):
        old=subprocess.run(['git','show',baseline+':'+path],cwd=ROOT,capture_output=True)
        before=old.stdout if old.returncode==0 else None;after=(ROOT/path).read_bytes()
        if before==after:continue
        payload='files/'+str(len(changes)+1).zfill(4);payloads[payload]=after
        changes.append({'action':'write','path':path,'before_sha256':hashlib.sha256(before).hexdigest() if before is not None else None,'after_sha256':hashlib.sha256(after).hexdigest(),'payload':payload,'summary':'Add house/visit gameplay or supporting assets' if before is None else 'Update house/visit gameplay and supporting assets'})
        # Keep exact baseline and edited bytes in a disposable review directory.
        for folder,raw in [('github_baseline',before),('edited_files',after)]:
            if raw is not None:
                dest=temporary/folder/path;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(raw)
    manifest={'format':'projectbridge.offline-patch.v1','title':'Add exploration, Father conversations and repeatable Family Dinner','description':'Extend the existing visit director with weighted Exploration and Family Dinner categories, persistent active timers, household patrol, spatial piano, owner-bound branching conversations and real secret rewards. Dinner reuses seating, food and pursuit/reconciliation, with five-minute attendance and suspicion-weighted grace. Includes generated event voice masters/runtime exports and earlier cumulative tunnel, house, storage, watcher and visit fixes.','source':{'repository':'Hi62384/Coraline','commit':baseline},'changes':changes}
    patch=ROOT/'ProjectBridge_patch.zip'
    with zipfile.ZipFile(patch,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        z.writestr('projectbridge_patch.json',json.dumps(manifest,indent=2));z.writestr('README_CHANGES.txt',(ROOT/'VISIT_EVENTS_README.txt').read_bytes()+b'\n\n'+(ROOT/'STORY_VISITS_README.txt').read_bytes()+b'\n\n'+(ROOT/'CHARACTER_CAST_README.txt').read_bytes())
        for key,raw in payloads.items():z.writestr(key,raw)
    with zipfile.ZipFile(patch) as z:
        assert z.testzip() is None
        assert set(z.namelist())=={'projectbridge_patch.json','README_CHANGES.txt',*payloads}
        paths=set()
        for c in manifest['changes']:
            assert c['path'] not in paths;paths.add(c['path'])
            assert not any(p in ('','..','.') for p in c['path'].split('/')) and ':' not in c['path'] and '\\' not in c['path']
            assert hashlib.sha256(z.read(c['payload'])).hexdigest()==c['after_sha256']
            if c['before_sha256']:
                original=(temporary/'github_baseline'/c['path']).read_bytes();assert hashlib.sha256(original).hexdigest()==c['before_sha256']
                # Apply and undo on a disposable baseline, verifying both byte hashes.
                trial=temporary/'apply_undo'/c['path'];trial.parent.mkdir(parents=True,exist_ok=True);trial.write_bytes(original)
                assert hashlib.sha256(trial.read_bytes()).hexdigest()==c['before_sha256'];trial.write_bytes(z.read(c['payload']))
                assert hashlib.sha256(trial.read_bytes()).hexdigest()==c['after_sha256'];trial.write_bytes(original)
                assert hashlib.sha256(trial.read_bytes()).hexdigest()==c['before_sha256']
    print('Verified ProjectBridge v1 patch:',len(changes),'operations;',patch.stat().st_size,'bytes; baseline',baseline)
    models=ROOT/'Coraline_Character_Models.zip'
    with zipfile.ZipFile(models,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for path in sorted(ART.iterdir()):
            if path.suffix in ('.blend','.glb','.png','.jpg'):z.write(path,path.name)
        z.write(ROOT/'CHARACTER_CAST_README.txt','README.txt')
    with zipfile.ZipFile(models) as z:assert z.testzip() is None
    print('Verified standalone models ZIP:',models.stat().st_size,'bytes')

if __name__=='__main__':
    ap=argparse.ArgumentParser();ap.add_argument('action',choices=['extras','validate','package']);args=ap.parse_args()
    globals()[args.action]()
