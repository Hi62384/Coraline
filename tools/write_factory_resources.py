"""Author the factory's physical press/clutches using existing block textures.
No audio synthesis, test execution or packaged-master modifications.
"""
from pathlib import Path
import json

RES=Path(__file__).resolve().parents[1]/'src/main/resources'

def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def box(a,b,texture):
    return {'from':a,'to':b,'faces':{side:{'texture':'#'+texture} for side in ('north','south','east','west','up','down')}}

def main():
    variants={}
    for part in range(4):
        for active in (False,True):
            for pulse in (False,True):
                name=f'factory_{part}_{int(active)}_{int(pulse) if part==0 else 0}'
                textures={'particle':'minecraft:block/iron_block','metal':'minecraft:block/iron_block','dark':'minecraft:block/polished_deepslate','lamp':'minecraft:block/redstone_block' if active else 'minecraft:block/black_concrete'}
                elements=[box([0,0,0],[16,3,16],'dark'),box([2,3,2],[14,5,14],'metal')]
                if part==0:
                    for x in (1,13):elements.append(box([x,3,5],[x+2,24,11],'metal'))
                    elements += [box([1,22,4],[15,26,12],'dark'),box([5,11 if pulse and active else 17,6],[11,23,10],'metal'),box([3,7 if pulse and active else 13,4],[13,11 if pulse and active else 17,12],'metal'),box([5,4,5],[11,6,11],'lamp')]
                else:
                    textures['color']='minecraft:block/'+{1:'red',2:'blue',3:'green'}[part]+'_concrete'
                    elements += [box([3,5,3],[13,9,13],'color'),box([6,9,6],[10,16 if active else 11,10],'metal'),box([3,15 if active else 10,5],[13,18 if active else 13,11],'dark'),box([4,5,2],[12,8,3],'lamp')]
                write(RES/f'assets/coraline/models/block/{name}.json',{'textures':textures,'elements':elements})
                variants[f'active={str(active).lower()},part={part},pulse={str(pulse).lower()}']={'model':'coraline:block/'+name}
    write(RES/'assets/coraline/blockstates/factory_control.json',{'variants':variants})
    write(RES/'data/coraline/loot_tables/blocks/factory_control.json',{'type':'minecraft:block','pools':[]})
    path=RES/'assets/coraline/lang/en_us.json';lang=json.loads(path.read_text(encoding='utf-8-sig'))
    lang['block.coraline.factory_control']='Clockwork Production Control'
    messages={
        'factory_instructions':'This press assembles a batch of three artificial workers. Disengage the RED clutch in the workroom, then the BLUE clutch in the main hall, then the GREEN clutch in the control room. Return here to switch off the press and release the archive lock.',
        'factory_order':'The governor holds that clutch shut. Disengage red, then blue, then green.',
        'factory_clutch_off':'The clutch disengages. The production press still needs to be switched off.',
        'factory_return_press':'All three clutches are disengaged. Return to the central press and switch it off.',
        'factory_stopped':'The production line is shut down. Its control-room archive is accessible.',
        'factory_save_paused':'The factory record cannot be saved safely. Its machinery and worker identities have been preserved; resolve the world save problem before continuing.',
        'factory_archive_sealed':'The production line holds this archive shut. Read the central press and operate its three shutdown clutches first.'}
    for key,value in messages.items():lang['message.coraline.'+key]=value
    write(path,lang)

if __name__=='__main__':main()
