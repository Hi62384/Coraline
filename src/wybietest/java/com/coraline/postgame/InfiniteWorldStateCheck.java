package com.coraline.postgame;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.nbt.*;
import com.coraline.palace.PalaceWellProgress.WybieLife;
import static com.coraline.postgame.InfiniteWorldState.Activation.*;

/** Actual NBT lifecycle assertions; no claim of terrain or native acceptance. */
public final class InfiniteWorldStateCheck {
    private static int checks;
    private static void require(boolean value,String why){if(!value)throw new AssertionError(why);checks++;}
    private static InfiniteWorldState.Witness witness(){return new InfiniteWorldState.Witness(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),WybieLife.ALIVE);}
    public static void main(String[] args)throws Exception{
        if(Arrays.asList(args).contains("--ore-only")){oreChecks();System.out.println("INFINITE_WORLD_NBT_OK checks="+checks+"; ore-field component only, no native/mining acceptance");return;}
        var state=InfiniteWorldState.fresh();var original=witness();var entropyCalls=new AtomicInteger();
        java.util.function.LongSupplier entropy=()->{entropyCalls.incrementAndGet();return 572491L;};
        require(!state.active()&&state.seed().isEmpty()&&state.generation().isEmpty(),"Unfinished story allocated a generation");
        require(state.activate(null,10,5,entropy)==REFUSED&&entropyCalls.get()==0,"Missing completion witness consumed entropy");
        for(int missing=0;missing<4;missing++){
            UUID[] ids={original.owner(),original.door(),original.wellSession(),original.keyReceipt()};ids[missing]=null;
            require(state.activate(new InfiniteWorldState.Witness(ids[0],ids[1],ids[2],ids[3],original.wybie(),original.wybieLife()),10,5,entropy)==REFUSED,"Incomplete canonical receipt activated postgame");
        }
        require(state.activate(original,-1,5,entropy)==REFUSED&&state.activate(original,10,5,null)==REFUSED&&entropyCalls.get()==0,"Invalid clock or entropy partially activated state");
        require(state.activate(original,10,5,entropy)==CREATED&&state.active()&&state.completedBy(original.owner())&&entropyCalls.get()==1,"First canonical ending failed one-time activation");
        long seed=state.seed().orElseThrow();var generation=state.generation().orElseThrow();var saved=state.save();
        saved.putLong("Seed",999);require(state.seed().orElseThrow()==seed,"External snapshot changed the live generation");
        require(state.activate(original,11,5,entropy)==ALREADY_RECORDED&&entropyCalls.get()==1,"Repeated ending rerolled entropy");
        var replay=new InfiniteWorldState.Witness(original.owner(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),WybieLife.DEAD_AT_WELL);
        require(state.activate(replay,12,5,entropy)==ALREADY_RECORDED&&state.save().getCompound("Owners").getCompound(original.owner().toString()).getUUID("Door").equals(original.door()),"Ordinary story replay rewrote the original unlock receipt");
        var originalRow=state.save().getCompound("Owners").getCompound(original.owner().toString());require(originalRow.getUUID("Wybie").equals(original.wybie())&&originalRow.getString("WybieLife").equals("ALIVE"),"Main-story replay changed the original postgame Wybie identity/fate");
        state=InfiniteWorldState.load(TagParser.parseTag(state.save().toString()));
        require(state.blocker().isEmpty()&&state.generation().orElseThrow().equals(generation)&&state.seed().orElseThrow()==seed,"Server restart lost independent seed/epoch");
        var copiedReceipt=new InfiniteWorldState.Witness(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),original.keyReceipt(),UUID.randomUUID(),WybieLife.ALIVE);
        require(state.activate(copiedReceipt,15,5,entropy)==REFUSED&&!state.completedBy(copiedReceipt.owner()),"Copied disposal receipt unlocked another player");
        var copiedSession=new InfiniteWorldState.Witness(UUID.randomUUID(),UUID.randomUUID(),original.wellSession(),UUID.randomUUID(),UUID.randomUUID(),WybieLife.ALIVE);
        require(state.activate(copiedSession,15,5,entropy)==REFUSED,"Copied well session unlocked another player");
        var second=witness();require(state.activate(second,20,5,entropy)==OWNER_RECORDED&&state.completedBy(second.owner())&&entropyCalls.get()==1&&state.generation().orElseThrow().equals(generation)&&state.seed().orElseThrow()==seed,"A second legitimate ending created a second world");
        require(state.activate(witness(),9,5,entropy)==REFUSED&&entropyCalls.get()==1,"Rewound clock altered the generation");
        var collision=InfiniteWorldState.fresh();require(collision.activate(witness(),0,572491L,entropy)==CREATED&&collision.seed().orElseThrow()!=572491L,"Independent seed copied the Overworld seed");
        var fresh=InfiniteWorldState.fresh();var old=fresh.save();require(fresh.activate(original,0,0,()->{throw new IllegalStateException("entropy unavailable");})==REFUSED&&fresh.save().equals(old),"Entropy failure partially activated the generation");
        var canonical=state.save();
        for(String key:List.of("Version","Activated","Owners","Generation","Seed","ActivatedAt","TerrainVersion")){
            var malformed=canonical.copy();malformed.remove(key);var blocked=InfiniteWorldState.load(malformed);
            require(!blocked.blocker().isEmpty()&&!blocked.active()&&blocked.seed().isEmpty()&&blocked.activate(witness(),30,5,entropy)==REFUSED&&blocked.save().equals(malformed),"Missing "+key+" reset or repaired a potentially live world");
        }
        for(String key:List.of("Owner","Door","WellSession","KeyReceipt","RecordedAt","Wybie","WybieLife")){
            var malformed=canonical.copy();malformed.getCompound("Owners").getCompound(original.owner().toString()).putString(key,"broken");var blocked=InfiniteWorldState.load(malformed);
            require(!blocked.blocker().isEmpty()&&blocked.activate(witness(),30,5,entropy)==REFUSED&&blocked.save().equals(malformed),"Damaged witness "+key+" authorized another generation");
        }
        var wrongOwner=canonical.copy();wrongOwner.getCompound("Owners").getCompound(original.owner().toString()).putUUID("Owner",UUID.randomUUID());
        require(!InfiniteWorldState.load(wrongOwner).blocker().isEmpty(),"Foreign owner acquired an unlock receipt");
        var unsupported=canonical.copy();unsupported.putInt("Version",2);var future=InfiniteWorldState.load(unsupported);
        require(!future.blocker().isEmpty()&&future.save().equals(unsupported),"Unsupported schema was erased");
        var noWitness=canonical.copy();noWitness.put("Owners",new CompoundTag());require(!InfiniteWorldState.load(noWitness).blocker().isEmpty(),"Bare activation flag replaced canonical completion");
        var inactive=canonical.copy();inactive.putBoolean("Activated",false);require(!InfiniteWorldState.load(inactive).blocker().isEmpty(),"Inactive flag discarded a live generation");
        var badBoolean=canonical.copy();badBoolean.putByte("Activated",(byte)2);require(!InfiniteWorldState.load(badBoolean).blocker().isEmpty(),"Malformed boolean authorized world activation");
        require(InfiniteWorldState.load(InfiniteWorldState.fresh().save()).blocker().isEmpty(),"Fresh inactive journal could not reload");
        var disk=new CompoundTag();disk.put("data",canonical.copy());
        require(InfiniteWorldState.checkpointMatches(canonical,disk),"Exact persisted activation could not bind its generator");
        var wrongDisk=disk.copy();wrongDisk.getCompound("data").putLong("Seed",seed+1);require(!InfiniteWorldState.checkpointMatches(canonical,wrongDisk),"Different on-disk seed authorized terrain generation");
        wrongDisk=disk.copy();wrongDisk.getCompound("data").putUUID("Generation",UUID.randomUUID());require(!InfiniteWorldState.checkpointMatches(canonical,wrongDisk),"Different disk generation authorized old terrain");
        wrongDisk=disk.copy();wrongDisk.getCompound("data").getCompound("Owners").remove(second.owner().toString());require(!InfiniteWorldState.checkpointMatches(canonical,wrongDisk),"Incomplete owner checkpoint was accepted");
        require(!InfiniteWorldState.checkpointMatches(canonical,new CompoundTag())&&!InfiniteWorldState.checkpointMatches(InfiniteWorldState.fresh().save(),disk),"Absent or inactive disk journal exposed a seed");
        for(var life:List.of(WybieLife.ALIVE,WybieLife.DEAD_AT_WELL)){
            var missingActor=new InfiniteWorldState.Witness(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),null,life);
            require(InfiniteWorldState.fresh().activate(missingActor,10,5,entropy)==REFUSED,"Absent original actor was converted to "+life);
        }
        var knownDead=new InfiniteWorldState.Witness(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),null,WybieLife.DEAD_BEFORE_WELL);var deadState=InfiniteWorldState.fresh();
        require(deadState.activate(knownDead,10,5,entropy)==CREATED&&InfiniteWorldState.load(deadState.save()).active()&&!deadState.save().getCompound("Owners").getCompound(knownDead.owner().toString()).contains("Wybie"),"Explicit previously-dead outcome minted a replacement Wybie");
        if(!Arrays.asList(args).contains("--lifecycle-only"))terrainChecks();
        System.out.println("INFINITE_WORLD_NBT_OK checks="+checks+"; lifecycle component only, not terrain/native/portal acceptance");
    }
    private static void terrainChecks(){
        var terrain=new InfiniteTerrain(124119);var same=new InfiniteTerrain(124119);var other=new InfiniteTerrain(-923573);
        var points=new java.util.ArrayList<int[]>();var expected=new java.util.HashMap<String,InfiniteTerrain.Column>();
        for(int x=-192;x<=192;x+=7)for(int z=-192;z<=192;z+=11){points.add(new int[]{x,z});expected.put(x+":"+z,terrain.column(x,z));}
        java.util.Collections.shuffle(points,new java.util.Random(19));int changed=0,largestStep=0;
        for(var point:points){int x=point[0],z=point[1];var column=same.column(x,z);
            require(column.equals(expected.get(x+":"+z)),"Chunk visitation order changed terrain");
            if(!column.equals(other.column(x,z)))changed++;
            largestStep=Math.max(largestStep,Math.abs(column.surfaceY()-same.column(x+1,z).surfaceY()));
            require(column.surfaceY()>InfiniteTerrain.MIN_Y+5&&column.surfaceY()<InfiniteTerrain.MIN_Y+InfiniteTerrain.HEIGHT,"Terrain exceeded dimension bounds");
            for(int y:new int[]{-63,-58,0,27,63,120,319})require(terrain.cave(x,y,z,column)==same.cave(x,y,z,column),"Cave density changed after sampler recreation");
            require(!terrain.cave(x,InfiniteTerrain.MIN_Y+5,z,column)&&!terrain.cave(x,column.surfaceY()+1,z,column),"Cave field carved the floor or empty sky");
        }
        require(changed==points.size(),"Independent seeds did not change sampled terrain");
        require(largestStep<=8,"Adjacent terrain columns have a major discontinuity: "+largestStep);
        for(int x:new int[]{-30000000,-1601,-17,-16,-1,0,15,16,1601,30000000}){
            var column=terrain.column(x,x);require(column.equals(new InfiniteTerrain(124119).column(x,x)),"Negative/far coordinate sampling is unstable");
        }
    }
    private static void oreChecks(){
        var veins=new InfiniteOreVeins(124119);var reloaded=new InfiniteOreVeins(124119);var other=new InfiniteOreVeins(124120);var counts=new EnumMap<InfiniteOreVeins.Ore,Integer>(InfiniteOreVeins.Ore.class);int changed=0,nonempty=0;
        for(int x=-96;x<96;x++)for(int z=-96;z<96;z+=2)for(int y=-60;y<=116;y+=3){
            var ore=veins.sample(x,y,z,InfiniteTerrain.Biome.STITCHED_MEADOWS);
            if(ore!=reloaded.sample(x,y,z,InfiniteTerrain.Biome.STITCHED_MEADOWS))throw new AssertionError("Ore field changed on reload");
            if(ore!=other.sample(x,y,z,InfiniteTerrain.Biome.STITCHED_MEADOWS))changed++;
            if(ore!=InfiniteOreVeins.Ore.NONE){nonempty++;counts.merge(ore,1,Integer::sum);if(y<ore.minY||y>ore.maxY)throw new AssertionError("Ore exceeded its depth band");}
        }
        require(changed>0,"Changing independent seed retained identical ore placement");
        for(var ore:java.util.List.of(InfiniteOreVeins.Ore.OTHER_IRON,InfiniteOreVeins.Ore.OTHER_DIAMOND,InfiniteOreVeins.Ore.SOUL_GLASS))require(counts.getOrDefault(ore,0)>0,"No natural "+ore+" appeared in the sampled underground volume");
        require(counts.get(InfiniteOreVeins.Ore.OTHER_DIAMOND)<counts.get(InfiniteOreVeins.Ore.OTHER_IRON)&&counts.get(InfiniteOreVeins.Ore.SOUL_GLASS)<counts.get(InfiniteOreVeins.Ore.OTHER_IRON),"Rare ores became more abundant than Other Iron");
        require(nonempty<192*96*59/50,"Ore placement overwhelms normal underground rock");
        require(veins.sample(-1,200,-1,InfiniteTerrain.Biome.MIRROR_MARSH)==InfiniteOreVeins.Ore.NONE&&veins.sample(0,-64,0,InfiniteTerrain.Biome.MIRROR_MARSH)==InfiniteOreVeins.Ore.NONE,"Ores appeared in sky/foundation");
        var selected=new InfiniteOreVeins.Point(-34,-20,-34);var expected=veins.sample(selected.x(),selected.y(),selected.z(),InfiniteTerrain.Biome.MIRROR_MARSH);
        for(int i=0;i<100;i++)veins.sample(i*17,-30,i*-31,InfiniteTerrain.Biome.PORCELAIN_PLAINS);
        require(expected==veins.sample(selected.x(),selected.y(),selected.z(),InfiniteTerrain.Biome.MIRROR_MARSH),"Visiting another chunk changed ore placement");
        System.out.println("Sampled existing registered ore field counts="+counts+"; no actual mining/native acceptance");
    }
    private InfiniteWorldStateCheck(){}
}
