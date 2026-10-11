from pathlib import Path
import subprocess,json,math
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/coraline'
out=assets/'sounds/visit';out.mkdir(parents=True,exist_ok=True)
files={'other_mother_intro':'voicelines/OtherMotherIntro.wav','other_father_enter_scene':'voicelines/OtherFatherEnterScene.wav','other_father_intro':'voicelines/OtherFatherIntro.wav','other_father_song':'voicelines/OtherFatherSong.mp3','other_father_dinners_ready':'voicelines/OtherFatherDinnersReady.wav','other_father_golden_chicken_breast':'voicelines/OtherFatherGoldenChickenBreast.wav','other_parents_waiting':'voicelines/OtherParentsWeveBeenWaitingForYou.wav','other_mother_visit_again':'voicelines/OtherMotherVisitAgain.mp3','mysterious_supper':'music/eventbased/MysteriousSupper.mp3',
       'magic_garden_fly':'music/eventbased/GardenFly.mp3',
       'coraline_garden':'music/CoralineGarden.mp3',
       'cake_baking':'music/eventbased/CocoaBeetles.mp3',
       'magic_garden_hop_on':'voicelines/otherfather/activities/magic_garden/HopOnKiddo.mp3',
       'magic_garden_mother_said':'voicelines/otherfather/activities/magic_garden/MotherSaidYoudLikeIt.mp3'}
sounds=json.loads((assets/'sounds.json').read_text())
# Other importers contribute activity intros, arrivals and dialogue to this shared file.
# Importing music must never erase their duration entries.
lengths_path=assets/'visit_audio_lengths.json'
lengths=json.loads(lengths_path.read_text()) if lengths_path.exists() else {}
files.update({'ambient_carnival':'music/eventbased/Bobinsky.mp3','ambient_theatre':'music/eventbased/Theatre.mp3',
              'ambient_circus':'music/eventbased/MiceCircus.mp3','ambient_race':'music/eventbased/FirstEscape.mp3',
              'ambient_archery':'music/eventbased/FirstEscape.mp3','ambient_garden':'music/CoralineGarden.mp3',
              'ambient_feast':'music/eventbased/MysteriousSupper.mp3','ambient_dream':'music/TheDreamsArentDangerous.mp3',
              'ambient_village':'music/eventbased/Wybie.mp3'})
files.update({f'other_mother_supper_{i}':f'voicelines/OtherMotherSupper{i}.wav' for i in range(3)})
files['see_you_soon']='voicelines/SeeYouSoon.wav'
for name,path in files.items():
    target=out/(name+'.ogg')
    # Normalize dialogue recordings, not songs: volume >1 is clamped by Minecraft's mixer.
    # GardenFly's original 23-second master ends before the 28-second flight.
    # Extend it without changing pitch, so one playback spans landing instead
    # of looping its opening motif during descent.
    filters=['-af','atempo=0.76'] if name=='magic_garden_fly' else [] if name in ('other_father_song','mysterious_supper','coraline_garden') else ['-af','loudnorm=I=-16:TP=-1.5:LRA=7'] if name.startswith('magic_garden_') else ['-af','loudnorm=I=-14:TP=-1.5:LRA=7']
    subprocess.run(['ffmpeg','-v','error','-y','-i',str(root/path),'-vn',*filters,'-c:a','libvorbis','-q:a','5',str(target)],check=True)
    lengths[name]=math.ceil(float(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','csv=p=0',str(target)],text=True))*20)
    sounds[name]={'sounds':[{'name':'coraline:visit/'+name,'stream':True}]}
# A separate one-shot event reuses the complete circus master; no loop or truncation.
sounds['circus_race']={'sounds':[{'name':'coraline:visit/ambient_circus','stream':True}]}
lengths['circus_race']=lengths['ambient_circus']
(assets/'sounds.json').write_text(json.dumps(sounds,indent=2)+'\n')
(assets/'visit_audio_lengths.json').write_text(json.dumps(lengths,indent=2)+'\n')
print('First-visit audio ticks:',lengths)
