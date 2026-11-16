package com.coraline.feast;

import com.coraline.parents.*;
import com.coraline.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

/** Deterministic controller acceptance, not a recording or whole-scene playthrough. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class RescueReunionGameTests {
    @GameTest(template="empty",timeoutTicks=200) public static void canonicalParentsKeepTheirSkinFlagsAndIdentityWhileFrostMeltsAcrossReload(GameTestHelper h){
        var l=h.getLevel();var p=new ServerPlayer(l.getServer(),l,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ReunionQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);var at=h.absolutePos(new BlockPos(2,3,2));for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){l.setBlock(at.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);for(int y=0;y<4;y++)l.setBlock(at.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);}p.setPos(Vec3.atBottomCenterOf(at));
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=java.util.UUID.randomUUID();s.stage=VisitData.Stage.DONE;VisitData.get(p.server).scenes.put(s.owner,s);ParentRescueStory.data(s).putBoolean("Started",true);var n=RescueChapter.data(s);n.putBoolean("Started",true);n.putBoolean("PermanentlySealed",true);n.putBoolean("BangingDone",true);n.putString("Phase","BANGING");n.putLong("Garden",at.asLong());for(int i=0;i<3;i++){var eye=RescueEyes.eye(s,i);eye.putBoolean("Defeated",true);eye.putBoolean("Discovered",true);h.assertTrue(RescueEyes.claim(p,s,i),"Authentic eye fixture failed");}
        var door=new com.coraline.otherworld.DoorData.Door();door.id=s.door;door.owner=s.owner;door.pos=at;com.coraline.otherworld.DoorData.get(p.server).doors.put(door.id,door);var family=new FamilyData.Family();family.owner=s.owner;family.bed=at.east(7);family.chest=at.west(7);family.storyMissing=true;FamilyData.get(p.server).families.put(family.id,family);
        var mom=ModEntities.MOM.get().create(l);var dad=ModEntities.DAD.get().create(l);mom.familyId=dad.familyId=family.id;mom.setPos(Vec3.atBottomCenterOf(at.west(5)));dad.setPos(Vec3.atBottomCenterOf(at.east(5)));family.mom=mom.getUUID();family.dad=dad.getUUID();family.momPos=mom.blockPosition();family.dadPos=dad.blockPosition();var flags=new CompoundTag();flags.putBoolean("Invisible",false);flags.putBoolean("NoAI",true);flags.putBoolean("Silent",false);for(var actor:java.util.List.of(mom,dad)){actor.setInvisible(true);actor.setSilent(true);actor.setNoAi(true);actor.setNoGravity(true);actor.noPhysics=true;actor.getPersistentData().put("FirstEscapeHidden",flags.copy());h.assertTrue(l.addFreshEntity(actor),"Original real parent fixture rejected");}
        h.runAfterDelay(10,()->finish(h,p,s,family,mom,dad,0));
    }
    private static void finish(GameTestHelper h,ServerPlayer p,VisitData.Scene s,FamilyData.Family f,ParentEntity mom,ParentEntity dad,int wait){
        if(p.serverLevel().getEntity(mom.getUUID())!=mom||p.serverLevel().getEntity(dad.getUUID())!=dad){h.assertTrue(wait<100,"Original real parents never became visible");h.runAfterDelay(1,()->finish(h,p,s,f,mom,dad,wait+1));return;}
        try{h.assertTrue(RescueEpilogue.begin(p,s),"Canonical real-parent reunion refused its prerequisites");h.assertTrue(RescueChapter.phase(s)==RescueChapter.Phase.REUNION_A,"Reunion skipped recording A");
            for(var actor:java.util.List.of(mom,dad)){h.assertTrue(!actor.isOtherParent()&&!actor.isInvisible()&&!actor.isNoAi()&&!actor.isNoGravity()&&!actor.noPhysics&&actor.reunionFrost()==1,"Real parents were replaced, whitewashed or prevented from navigating");}
            mom.setPos(mom.position().add(1,0,0));mom.tickCount=20;RescueEpilogue.controlParent(mom,f);h.assertTrue(f.momPos.equals(mom.blockPosition()),"Following reunion parent retained a stale reload/chunk anchor");
            s.loop=OtherMotherLoopState.load(s.loop.save());RescueEpilogue.tick(p,s,RescueChapter.Phase.REUNION_A,ShowRecordings.duration("reunion_a"),true);h.assertTrue(RescueChapter.phase(s)==RescueChapter.Phase.REUNION_B,"Acknowledged A did not hand off to B");var n=RescueChapter.data(s);n.putInt("Ticks",ShowRecordings.duration("reunion_b")/2);for(var actor:java.util.List.of(mom,dad)){RescueEpilogue.controlParent(actor,f);h.assertTrue(actor.reunionFrost()>.4F&&actor.reunionFrost()<.6F,"Frost did not progressively melt during B");var saved=actor.saveWithoutId(new CompoundTag());var copy=actor.getType().create(p.serverLevel());copy.load(saved);h.assertTrue(copy instanceof ParentEntity restored&&restored.getUUID().equals(actor.getUUID())&&restored.reunionFrost()==actor.reunionFrost(),"Parent identity/frost lost on entity reload");}
            RescueEpilogue.tick(p,s,RescueChapter.Phase.REUNION_B,ShowRecordings.duration("reunion_b"),true);h.assertTrue(RescueChapter.phase(s)==RescueChapter.Phase.CAT_NOTICE&&f.rescued&&!f.storyMissing&&!f.busy,"Parents did not resume their permanently upgraded family state");for(var actor:java.util.List.of(mom,dad))h.assertTrue(actor.reunionFrost()==0&&actor.isNoAi()&&actor.isNoGravity()&&actor.noPhysics&&!actor.isSilent()&&!actor.isInvisible()&&!actor.getPersistentData().contains("ReunionFlags"),"Reunion leaked snow or failed to restore original flags");h.assertTrue(f.mom.equals(mom.getUUID())&&f.dad.equals(dad.getUUID()),"Reunion changed canonical family UUIDs");
        }finally{mom.discard();dad.discard();for(var entity:p.serverLevel().getAllEntities())if(entity instanceof CinematicPropEntity prop&&prop.owned(s))prop.discard();FamilyData.get(p.server).families.remove(f.id);VisitData.get(p.server).scenes.remove(s.owner);com.coraline.otherworld.DoorData.get(p.server).doors.remove(s.door);}h.succeed();
    }
    private RescueReunionGameTests(){}
}
