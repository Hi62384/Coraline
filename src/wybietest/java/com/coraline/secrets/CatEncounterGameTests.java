package com.coraline.secrets;

import com.coraline.feast.*;
import com.coraline.otherworld.*;
import com.coraline.registry.*;
import com.coraline.entity.CharacterEntity;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class CatEncounterGameTests {
    @GameTest(template="empty") public static void exceptionalRewardSelection(GameTestHelper h){
        var state=new SecretsState();h.assertTrue(CatEncounter.rewardSecret(state).equals("soul_prisons"),"Cat didn't choose the most valuable secret");
        state.journal.add("soul_prisons");state.issued.put(UUID.randomUUID(),"soulglass");state.blackmail.put(UUID.randomUUID(),List.of("mother_route"));
        String next=CatEncounter.rewardSecret(state);h.assertTrue(SecretRegistry.ALL.get(next).tier()==2&&!Set.of("soul_prisons","soulglass","mother_route").contains(next),"Reward duplicates known/carried/sealed knowledge");
        state.journal.addAll(SecretRegistry.ALL.keySet());h.assertTrue(CatEncounter.rewardSecret(state).isEmpty(),"Exhausted exceptional pool downgraded or duplicated rewards");h.succeed();
    }
    @GameTest(template="empty") public static void nineRecordingsAndThirtyBlockMargin(GameTestHelper h){int voices=0;for(var phase:CatEncounter.Phase.values())if(CatEncounter.voiced(phase)){voices++;String sound="cat_encounter_"+phase.name().toLowerCase(Locale.ROOT);h.assertTrue(VisitAudio.SOUNDS.containsKey(sound)&&VisitAudio.length(sound)>20,"Missing cat voice/timing: "+sound);}h.assertTrue(voices==9&&CatEncounter.marginRadius(160)==130,"Voice ordering/margin changed");h.succeed();}
    @GameTest(template="empty",timeoutTicks=200) public static void ownedActorProtectionBurrowAndCleanup(GameTestHelper h){
        var l=h.getLevel().getServer().getLevel(OtherWorld.KEY);var base=new BlockPos(94000,180,94000);l.getChunkAt(base);
        var p=new ServerPlayer(l.getServer(),l,new GameProfile(UUID.randomUUID(),"CatEncounterQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.loop.phase=OtherMotherLoopState.Phase.FREE_TIME;s.loop.visits=1;s.loop.secrets.visit=1;VisitData.get(p.server).scenes.put(s.owner,s);p.getPersistentData().putUUID("CoralineVisitDoor",s.door);
        var r=new RealmData.Realm();r.ready=true;r.house=base.west(100);r.porch=r.house.north(44);r.sourceDoor=base;r.doorPos=base;r.door=s.door;r.radius=160;RealmData.get(p.server).realms.put(s.door,r);
        s.table=base;s.piano=base;
        for(int x=-14;x<=14;x++)for(int z=-14;z<=14;z++){var at=base.offset(x,-1,z);l.getChunkAt(at);l.setBlock(at,Blocks.STONE.defaultBlockState(),2);}p.setPos(base.getX()+.5,base.getY(),base.getZ()+.5);
        var chunk=new net.minecraft.world.level.ChunkPos(base);for(int x=chunk.x-1;x<=chunk.x+1;x++)for(int z=chunk.z-1;z<=chunk.z+1;z++)l.setChunkForced(x,z,true);
        var home=r.porch.offset(24,0,-12);var homeChunks=new HashSet<net.minecraft.world.level.ChunkPos>();for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){var at=home.offset(x,-1,z);var cp=new net.minecraft.world.level.ChunkPos(at);homeChunks.add(cp);l.setChunkForced(cp.x,cp.z,true);l.getChunkAt(at);l.setBlock(at,Blocks.STONE.defaultBlockState(),2);}
        h.runAtTickTime(30,()->{try{
            SecretSpies.rat(p,base.east(2));h.assertTrue(CatEncounter.begin(p),"Safe outdoor encounter failed to begin");var n=CatEncounter.data(p);var cat=l.getEntity(n.getUUID("Actor"));h.assertTrue(cat instanceof CharacterEntity c&&c.characterId().equals("cat"),"Used a vanilla cat, not the authored model");
            h.assertTrue(s.loop.secrets.world.getList("Rats",11).isEmpty()&&MotherSearch.mother(p)==null,"Spies/mother remain active");SecretSpies.rat(p,base);h.assertTrue(s.loop.secrets.world.getList("Rats",11).isEmpty(),"Rats respawn during protected scene");
            n.putString("Phase","B");n.putInt("Ticks",VisitAudio.length("cat_encounter_b")-21);CatEncounter.tick(p);h.assertTrue(cat.isInvisible(),"B burrow did not hide the cat one second before the end");
            n.putInt("Ticks",VisitAudio.length("cat_encounter_b")-7);CatEncounter.tick(p);h.assertFalse(cat.isInvisible(),"Cat did not emerge before C");
            n.putString("Phase","E");h.assertTrue(CatEncounter.guidedVoid(p),"Normal border return wasn't suppressed");
            p.setPos(base.getX()+180.5,base.getY(),base.getZ()+.5);n.putBoolean("Finished",true);CatEncounter.tick(p);
            h.assertTrue(n.getString("Phase").equals("F")&&p.blockPosition().distSqr(home)<4&&p.blockPosition().distSqr(r.porch)>400,"E didn't return beside the house and start F");
            n.putBoolean("Finished",true);CatEncounter.tick(p);h.assertTrue(n.getString("Phase").equals("G")&&n.hasUUID("Rat"),"F didn't introduce the circus rat");
            var rat=(MouseEntity)l.getEntity(n.getUUID("Rat"));h.assertTrue(rat!=null&&rat.catPose()==1&&rat.circusStyle()==4&&!rat.spying(),"Trumpet rat wasn't a separate costumed scene actor");
            n.putBoolean("Finished",true);CatEncounter.tick(p);h.assertTrue(n.getString("Phase").equals("H")&&rat.catPose()==2,"G didn't immediately start the catch/H");
            n.putInt("Ticks",75);CatEncounter.tick(p);h.assertTrue(rat.catPose()==3,"Caught rat never went limp");
            n.putBoolean("Finished",true);CatEncounter.tick(p);h.assertTrue(n.getString("Phase").equals("I"),"H didn't advance to I");
            n.putInt("Ticks",VisitAudio.length("cat_encounter_i"));CatEncounter.tick(p);h.assertTrue(rat.ugly()==1,"Circus rat didn't transform");
            n.putBoolean("Finished",true);CatEncounter.tick(p);n.putInt("Ticks",119);CatEncounter.tick(p);
            h.assertTrue(!CatEncounter.active(p)&&cat.isRemoved()&&rat.isRemoved()&&!CatEncounter.guidedVoid(p),"Protection/actors survived departure");
            h.assertTrue(s.loop.secrets.issued.size()==1&&s.loop.secrets.issued.containsValue("soul_prisons"),"Finished cat encounter didn't award the best secret");
            boolean received=false;for(var item:p.getInventory().items)if(SecretTransactions.evidence(p,item).equals("soul_prisons"))received=true;
            h.assertTrue(received,"Secret evidence wasn't delivered to inventory");CatEncounter.cancel(p,true);
            h.assertTrue(s.loop.secrets.issued.size()==1,"Repeated cleanup duplicated the reward");
            // An interrupted encounter must not pay out, even at the final departure phase.
            n.putBoolean("Active",true);CatEncounter.cancel(p,false);h.assertTrue(s.loop.secrets.issued.size()==1,"Aborted encounter awarded a secret");
            p.getInventory().clearContent();
            p.setGameMode(GameType.SURVIVAL);h.assertFalse(com.coraline.item.SkipEventDebugItem.request(p),"Survival player can skip events");
            p.setGameMode(GameType.CREATIVE);p.tickCount=1;float trust=s.loop.trust,suspicion=s.loop.suspicion;
            h.assertTrue(com.coraline.item.SkipEventDebugItem.request(p)&&p.getPersistentData().getUUID("DebugSkipDoor").equals(s.door),"Creative skip wasn't bound to this visit");
            s.loop.event.putInt("Serial",s.loop.event.getInt("Serial")+1);h.assertFalse(com.coraline.item.SkipEventDebugItem.retry(p)||p.getPersistentData().hasUUID("DebugSkipDoor"),"Stale skip affected a different event");
            h.assertTrue(s.loop.trust==trust&&s.loop.suspicion==suspicion&&p.getInventory().isEmpty(),"Debug skip granted rewards or changed relationships");
            h.assertTrue(s.loop.secrets.issued.size()==1,"Debug skip granted additional secret evidence");
        }finally{CatEncounter.cancel(p,false);VisitData.get(p.server).scenes.remove(s.owner);RealmData.get(p.server).realms.remove(s.door);for(int x=chunk.x-1;x<=chunk.x+1;x++)for(int z=chunk.z-1;z<=chunk.z+1;z++)l.setChunkForced(x,z,false);for(var cp:homeChunks)l.setChunkForced(cp.x,cp.z,false);}h.succeed();});
    }
}
