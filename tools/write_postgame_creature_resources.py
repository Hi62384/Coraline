"""Merge real biome spawn pools, creature loot and Button eligibility."""
from pathlib import Path
import json

RES=Path(__file__).resolve().parents[1]/'src/main/resources'
SPAWNS={
    'threadcrawler':(['buttonwood_forest','velvet_mushroom_forest','rootbound_wilds','threadbound_mountains'],8,1,2),
    'stitched_hound':(['buttonwood_forest','paperwood_forest','rootbound_wilds'],8,2,4),
    'porcelain_sentinel':(['porcelain_plains','clockwork_highlands','white_rift_wastes'],3,1,1),
    'paper_wraith':(['paperwood_forest','pale_wasteland'],5,1,2),
    'rootbound_horror':(['rootbound_wilds','forgotten_gardens'],5,1,1),
    'void_stalker':(['white_rift_wastes','pale_wasteland'],4,1,1),
    'false_villager':(['stitched_meadows','forgotten_gardens'],3,1,1),
    'thread_spinner':(['threadbound_mountains','buttonwood_forest','velvet_mushroom_forest'],5,1,2),
    'rift_guardian':([],0,1,1),
}
def put(path,value):
    p=RES/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
def pool(item,lo,hi,chance=1):
    p={'rolls':1,'entries':[{'type':'minecraft:item','name':item,'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':lo,'max':hi}}]}]}
    if chance<1:p['conditions']=[{'condition':'minecraft:killed_by_player'},{'condition':'minecraft:random_chance','chance':chance}]
    return p
def tag(name,ids):
    path='data/coraline/tags/entity_types/'+name+'.json';n=json.loads((RES/path).read_text())
    for item in ids:
        if item not in n['values']:n['values'].append(item)
    put(path,n)
def main():
    for mob,(biomes,weight,lo,hi) in SPAWNS.items():
        for biome in biomes:
            path='data/coraline/worldgen/biome/postgame/'+biome+'.json';n=json.loads((RES/path).read_text());entries=n.setdefault('spawners',{}).setdefault('monster',[])
            entry={'type':'coraline:'+mob,'weight':weight,'minCount':lo,'maxCount':hi}
            for i,old in enumerate(entries):
                if old['type']==entry['type']:entries[i]=entry;break
            else:entries.append(entry)
            put(path,n)
    put('data/coraline/loot_tables/entities/threadcrawler.json',{'type':'minecraft:entity','pools':[pool('minecraft:string',2,4),pool('minecraft:spider_eye',1,1,.15)]})
    put('data/coraline/loot_tables/entities/stitched_hound.json',{'type':'minecraft:entity','pools':[pool('minecraft:leather',0,2),pool('minecraft:string',1,2)]})
    put('data/coraline/loot_tables/entities/porcelain_sentinel.json',{'type':'minecraft:entity','pools':[pool('minecraft:quartz',2,4),pool('coraline:other_iron_ingot',1,2,.35),pool('coraline:moonstone',1,1,.08)]})
    loot={
        'paper_wraith':[pool('minecraft:paper',2,5),pool('minecraft:string',1,2)],
        'rootbound_horror':[pool('minecraft:stick',2,5),pool('minecraft:moss_block',1,2,.4)],
        'void_stalker':[pool('minecraft:string',1,3),pool('coraline:soulglass',1,1,.08)],
        'false_villager':[pool('minecraft:string',1,2),pool('coraline:other_iron_ingot',1,1,.12)],
        'thread_spinner':[pool('minecraft:string',3,6),pool('minecraft:cobweb',1,1,.3)],
        'rift_guardian':[pool('coraline:other_iron_ingot',3,6),pool('coraline:other_diamond',1,2,.8),pool('coraline:moonstone',1,2,.65)],
    }
    for mob,pools in loot.items():put('data/coraline/loot_tables/entities/'+mob+'.json',{'type':'minecraft:entity','pools':pools})
    tag('button_drop_themed',['coraline:threadcrawler','coraline:stitched_hound','coraline:paper_wraith','coraline:false_villager','coraline:thread_spinner']);tag('button_drop_strong',['coraline:porcelain_sentinel','coraline:rootbound_horror','coraline:void_stalker','coraline:rift_guardian'])
    path='assets/coraline/lang/en_us.json';n=json.loads((RES/path).read_text(encoding='utf-8-sig'))
    for mob in SPAWNS:n['entity.coraline.'+mob]=mob.replace('_',' ').title()
    put(path,n)

if __name__=='__main__':main()
