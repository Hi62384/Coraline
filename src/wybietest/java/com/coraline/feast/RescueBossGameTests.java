package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

/** Real server directors/input guards in bounded arenas; not visual or whole-fight acceptance. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class RescueBossGameTests {
    /** Register only this bounded native input fixture's lookup. It has no TCP
     * connection and is never reported as connected multiplayer acceptance. */
    @SuppressWarnings("unchecked") private static void ownerLookup(ServerPlayer p,boolean present){
        try{var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);var map=(java.util.Map<java.util.UUID,ServerPlayer>)field.get(p.server.getPlayerList());
            if(present){if(map.putIfAbsent(p.getUUID(),p)!=null)throw new IllegalStateException("Fixture owner already registered");}else map.remove(p.getUUID(),p);
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Native owner lookup fixture unavailable",error);}
    }
    private static ServerPlayer player(GameTestHelper h){var l=h.getLevel().getServer().getLevel(ActivityWorld.KEY);var p=new ServerPlayer(l.getServer(),l,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"BossDirectorQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);var s=new VisitData.Scene();s.owner=p.getUUID();s.door=java.util.UUID.randomUUID();s.stage=VisitData.Stage.DONE;VisitData.get(p.server).scenes.put(s.owner,s);ParentRescueStory.data(s).putBoolean("Started",true);ParentRescueStory.data(s).put("Scenery",new net.minecraft.nbt.CompoundTag());RescueChapter.data(s).putBoolean("Started",true);RescueChapter.data(s).putLong("Garden",TheatreSite.center(p,s).asLong());return p;}
    private static void clean(ServerPlayer p){var s=OtherMotherLoop.scene(p);RescueBosses.hideBar(p);for(var l:p.server.getAllLevels())for(var e:l.getAllEntities())if(e instanceof CinematicPropEntity prop&&prop.owned(s))prop.discard();for(String id:java.util.List.of("evil_other_father","other_miss_spink","other_miss_forcible")){var actor=ShowActors.actor(p,s,id);if(actor!=null)actor.discard();}VisitData.get(p.server).scenes.remove(s.owner);}
    @GameTest(template="empty") public static void everyBossPatternAndHealthTierStartsWithItsOwnTelegraphAndSurvivesReload(GameTestHelper h){var p=player(h);var s=OtherMotherLoop.scene(p);p.setInvulnerable(true);var changed=new java.util.ArrayList<BlockPos>();
        var states=java.util.List.of(RescueChapter.Phase.MANTIS_FIGHT,RescueChapter.Phase.THEATRE_FIGHT,RescueChapter.Phase.SWARM_FIGHT);
        for(var state:states){RescueChapter.data(s).putString("Phase",state.name());var center=RescueEncounters.center(p,s);for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++){var at=BlockPos.containing(center).offset(x,-1,z);p.serverLevel().getChunkAt(at);p.serverLevel().setBlock(at,Blocks.STONE.defaultBlockState(),2);changed.add(at);}LoopProps.hold(p.serverLevel(),BlockPos.containing(center));}
        h.runAfterDelay(25,()->{try{for(var state:states){int epoch=0;
            RescueBosses.begin(p,s,state);var center=RescueEncounters.center(p,s);p.setPos(center.add(0,0,8));var n=RescueChapter.data(s);float maximum=n.getFloat("BossMaximum");
            for(int tier=0;tier<3;tier++){epoch+=state==RescueChapter.Phase.MANTIS_FIGHT?3840:3600;n.putFloat("BossHealth",maximum*(tier==0?.9F:tier==1?.5F:.2F));if(tier==0)n.putInt("BossCycleStart",epoch);RescueBosses.tick(p,s,epoch);h.assertTrue(n.getInt("BossTier")==tier&&n.getInt("BossBeat")==0&&!n.getBoolean("Vulnerable"),"Tier change began mid-attack: "+state+" tier "+tier);int patterns=state==RescueChapter.Phase.MANTIS_FIGHT?4:5,period=state==RescueChapter.Phase.MANTIS_FIGHT?240:state==RescueChapter.Phase.THEATRE_FIGHT?300:180-tier*20;
                for(int pattern=0;pattern<patterns;pattern++){int time=epoch+pattern*period;RescueBosses.tick(p,s,time);h.assertTrue(n.getInt("BossPattern")==pattern&&n.getInt("BossBeat")==0&&!n.getBoolean("Vulnerable"),"Pattern skipped its warning: "+state+" / "+tier+" / "+pattern);RescueBosses.tick(p,s,time+(state==RescueChapter.Phase.THEATRE_FIGHT?200:100));h.assertTrue((state==RescueChapter.Phase.MANTIS_FIGHT?n.getInt("BossBeat")>=100:n.getBoolean("Vulnerable"))&&RescueProps.find(p,s,"WeakPoint")!=null,"Pattern never reached its reveal opportunity");}
                var phase=RescueChapter.phase(s);float health=n.getFloat("BossHealth");s.loop=OtherMotherLoopState.load(s.loop.save());n=RescueChapter.data(s);h.assertTrue(RescueChapter.phase(s)==phase&&n.getFloat("BossHealth")==health&&n.getInt("BossLastTier")==tier,"Saved director lost fight/tier state");
            }
        }}finally{for(var at:changed)p.serverLevel().removeBlock(at,false);clean(p);}h.succeed();});
    }
    @GameTest(template="empty") public static void invisibleSocketUsesNormalSurfaceReach(GameTestHelper h){
        var weak=new Vec3(0,1.5,0);
        h.assertTrue(RescueBosses.weakContact(new Vec3(0,1.5,3.3),new Vec3(0,0,-1),weak).isPresent(),"Socket requires entering its volume rather than ordinary surface reach");
        h.assertTrue(RescueBosses.weakContact(new Vec3(0,1.5,3.6),new Vec3(0,0,-1),weak).isEmpty(),"Socket accepts an out-of-reach swing");
        h.assertTrue(RescueBosses.weakContact(new Vec3(0,1.5,3),new Vec3(1,0,0),weak).isEmpty(),"Swing away from socket accepted");h.succeed();
    }
    private static java.util.Set<net.minecraft.world.level.ChunkPos> warmActorChunks(ServerPlayer p,VisitData.Scene s){
        // A fake player is not a player-list chunk ticket. Warm the entity-
        // ticking neighborhood, preserving any preexisting forced chunks.
        var forced=new java.util.HashSet<net.minecraft.world.level.ChunkPos>();var center=new net.minecraft.world.level.ChunkPos(TheatreSite.center(p,s));
        for(int x=center.x-1;x<=center.x+1;x++)for(int z=center.z-1;z<=center.z+1;z++){var chunk=new net.minecraft.world.level.ChunkPos(x,z);if(!p.serverLevel().getForcedChunks().contains(chunk.toLong())){forced.add(chunk);p.serverLevel().setChunkForced(x,z,true);}p.serverLevel().getChunk(x,z);}
        return forced;
    }
    @GameTest(template="empty") public static void onlyCurrentOwnersRevealedExposedWeakPointAcceptsDamageAndCooldown(GameTestHelper h){var p=player(h);var foreign=player(h);var s=OtherMotherLoop.scene(p);var forced=warmActorChunks(p,s);h.runAfterDelay(25,()->{try{RescueBosses.begin(p,s,RescueChapter.Phase.MANTIS_FIGHT);var at=Vec3.atBottomCenterOf(TheatreSite.center(p,s)).add(0,1.5,0);p.setPos(at.add(0,-1.5,3));p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,at);var point=RescueProps.ensure(p,s,"WeakPoint",RescueProps.CONTROL_POINT,0,at);var body=RescueProps.ensure(p,s,"Mantis",RescueProps.MANTIS,0,at);var n=RescueChapter.data(s);n.putDouble("WeakX",at.x);n.putDouble("WeakY",at.y);n.putDouble("WeakZ",at.z);float health=n.getFloat("BossHealth");
            h.assertTrue(RescueEncounters.gaze(p,at,5,.975)&&p.hasLineOfSight(point),"Weak-point fixture lacks real server aim/line of sight");
            h.assertTrue(RescueProps.find(p,s,"WeakPoint")==point,"Weak-point fixture entity was not registered under its actual UUID");
            h.assertTrue(point.owned(s)&&RescueChapter.owns(p,s)&&RescueBosses.fighting(RescueChapter.phase(s))&&p.distanceToSqr(point)<=30,"Weak-point fixture lacks actual owner/phase/reach");
            h.assertFalse(com.coraline.multiplayer.StoryCoop.canAct(p,s),"Unregistered fixture owner bypassed native input authority");ownerLookup(p,true);h.assertTrue(com.coraline.multiplayer.StorySessionData.get(p.server).observe(p,s)&&com.coraline.multiplayer.StoryCoop.canAct(p,s),"Registered owner phase not authorized");
            n.putBoolean("Vulnerable",false);n.putLong("WeakUntil",p.serverLevel().getGameTime()+100);RescueBosses.hit(p,point);h.assertTrue(n.getFloat("BossHealth")==health,"Closed control point took damage");n.putBoolean("Vulnerable",true);n.putLong("WeakUntil",p.serverLevel().getGameTime()-1);RescueBosses.hit(p,point);h.assertTrue(n.getFloat("BossHealth")==health,"Unrevealed weakness took damage");n.putLong("WeakUntil",p.serverLevel().getGameTime()+100);RescueBosses.hit(p,point);h.assertTrue(n.getFloat("BossHealth")==health,"Empty fist damaged the control point");p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(7);p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED).setBaseValue(100);p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,at.add(2,0,0));RescueBosses.hit(p,body);RescueBosses.hit(foreign,point);h.assertTrue(n.getFloat("BossHealth")==health,"Boss body/foreign owner bypassed the exact weak point");h.assertFalse(n.getBoolean("Vulnerable"),"A missed body strike left the opening active");
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,at);RescueBosses.hit(p,point);h.assertTrue(n.getFloat("BossHealth")==health,"Miss could be corrected by spamming in the same opening");
            n.putBoolean("Vulnerable",true);n.putLong("WeakUntil",p.serverLevel().getGameTime()+80);RescueBosses.hit(p,point);float after=n.getFloat("BossHealth");h.assertTrue(after<health,"Owner could not strike the actual revealed weak point");RescueBosses.hit(p,point);h.assertTrue(n.getFloat("BossHealth")==after,"Same-tick attack flood bypassed cooldown");
        }finally{ownerLookup(p,false);clean(p);clean(foreign);for(var chunk:forced)p.serverLevel().setChunkForced(chunk.x,chunk.z,false);}h.succeed();});}
    @GameTest(template="empty") public static void offeredEyeCanBeCollectedFromAnOldHandoffWithoutRepeatingOrDuplicating(GameTestHelper h){
        var p=player(h);var s=OtherMotherLoop.scene(p);var n=RescueChapter.data(s);var at=Vec3.atBottomCenterOf(TheatreSite.center(p,s)).add(0,1.6,0);var forced=warmActorChunks(p,s);
        h.runAfterDelay(25,()->{try{n.putString("Phase","MANTIS_SINK");n.putInt("Ticks",10000);var eye=RescueProps.ensure(p,s,"GardenEye",RescueProps.EYE,0,at);p.setPos(at.add(0,-1.6,2));
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,at);h.assertTrue(eye!=null&&RescueEncounters.takeOfferedEye(p,s),"Offered eye did not handle the use ray through the mantis pick box");
            h.assertTrue(RescueEyes.eye(s,0).getBoolean("Claimed")&&eye.isInvisible(),"Saved handoff cannot deliver its authentic eye");
            int count=0;for(var stack:p.getInventory().items)if(RescueEyes.authentic(stack,s,0))count+=stack.getCount();
            RescueEncounters.interact(p,s,eye);int repeated=0;for(var stack:p.getInventory().items)if(RescueEyes.authentic(stack,s,0))repeated+=stack.getCount();
            h.assertTrue(count==1&&repeated==1,"Eye handoff duplicated its unique item");h.assertTrue(RescueChapter.phase(s)==RescueChapter.Phase.MANTIS_SINK,"Eye handoff restarted its scene");
        }finally{clean(p);for(var chunk:forced)p.serverLevel().setChunkForced(chunk.x,chunk.z,false);}h.succeed();});
    }
    private RescueBossGameTests(){}
}
