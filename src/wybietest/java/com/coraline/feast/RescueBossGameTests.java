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
    private static ServerPlayer player(GameTestHelper h){return player(h,ActivityWorld.KEY);}
    private static ServerPlayer player(GameTestHelper h,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension){var l=h.getLevel().getServer().getLevel(dimension);if(l==null)throw new IllegalStateException("Original boss fixture dimension missing: "+dimension.location());var p=new ServerPlayer(l.getServer(),l,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"BossDirectorQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);var s=new VisitData.Scene();s.owner=p.getUUID();s.door=java.util.UUID.randomUUID();s.stage=VisitData.Stage.DONE;VisitData.get(p.server).scenes.put(s.owner,s);ParentRescueStory.data(s).putBoolean("Started",true);ParentRescueStory.data(s).put("Scenery",new net.minecraft.nbt.CompoundTag());RescueChapter.data(s).putBoolean("Started",true);RescueChapter.data(s).putLong("Garden",TheatreSite.center(p,s).asLong());return p;}
    @GameTest(template="empty") public static void bossRetryUsesOriginalArenaDimensionEvenWhenPlayerWasStrandedElsewhere(GameTestHelper h){
        var p=player(h);
        try{
            h.assertTrue(RescueBosses.arena(p,RescueChapter.Phase.MANTIS_FIGHT)==p.server.getLevel(com.coraline.otherworld.OtherWorld.KEY),
                    "Mantis retry must return to the original Other World arena");
            h.assertTrue(RescueBosses.arena(p,RescueChapter.Phase.THEATRE_FIGHT)==p.server.getLevel(ActivityWorld.KEY),
                    "Other actresses retry must return to their actual theatre dimension");
            h.assertTrue(RescueBosses.arena(p,RescueChapter.Phase.SWARM_FIGHT)==p.server.getLevel(ActivityWorld.KEY),
                    "Ratmaster retry must return to its actual circus dimension");
        }finally{clean(p);}
        h.succeed();
    }
    private static void clean(ServerPlayer p){var s=OtherMotherLoop.scene(p);RescueBosses.hideBar(p);for(var l:p.server.getAllLevels())for(var e:l.getAllEntities())if(e instanceof CinematicPropEntity prop&&prop.owned(s))prop.discard();for(String id:java.util.List.of("evil_other_father","other_miss_spink","other_miss_forcible")){var actor=ShowActors.actor(p,s,id);if(actor!=null)actor.discard();}VisitData.get(p.server).scenes.remove(s.owner);}
    @GameTest(template="empty") public static void everyBossPatternAndHealthTierStartsWithItsOwnTelegraphAndSurvivesReload(GameTestHelper h){var p=player(h);var s=OtherMotherLoop.scene(p);p.setInvulnerable(true);var changed=new java.util.ArrayList<BlockPos>();
        var states=java.util.List.of(RescueChapter.Phase.MANTIS_FIGHT,RescueChapter.Phase.THEATRE_FIGHT,RescueChapter.Phase.SWARM_FIGHT);
        for(var state:states){RescueChapter.data(s).putString("Phase",state.name());var center=RescueEncounters.center(p,s);for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++){var at=BlockPos.containing(center).offset(x,-1,z);p.serverLevel().getChunkAt(at);p.serverLevel().setBlock(at,Blocks.STONE.defaultBlockState(),2);changed.add(at);}LoopProps.hold(p.serverLevel(),BlockPos.containing(center));}
        h.runAfterDelay(25,()->{try{for(var state:states){int epoch=0;
            RescueBosses.begin(p,s,state);var center=RescueEncounters.center(p,s);p.setPos(center.add(0,0,8));var n=RescueChapter.data(s);float maximum=n.getFloat("BossMaximum");
            for(int tier=0;tier<3;tier++){epoch+=state==RescueChapter.Phase.MANTIS_FIGHT?3840:3600;n.putFloat("BossHealth",maximum*(tier==0?.9F:tier==1?.5F:.2F));if(tier==0)n.putInt("BossCycleStart",epoch);RescueBosses.tick(p,s,epoch);h.assertTrue(n.getInt("BossTier")==tier&&n.getInt("BossBeat")==0&&!n.getBoolean("Vulnerable"),"Tier change began mid-attack: "+state+" tier "+tier);int patterns=state==RescueChapter.Phase.MANTIS_FIGHT?4:5,period=state==RescueChapter.Phase.MANTIS_FIGHT?240:state==RescueChapter.Phase.THEATRE_FIGHT?300:180-tier*20;
                for(int pattern=0;pattern<patterns;pattern++){int time=epoch+pattern*period;RescueBosses.tick(p,s,time);h.assertTrue(n.getInt("BossPattern")== (state==RescueChapter.Phase.SWARM_FIGHT?RescueBosses.ratPattern(tier,pattern):pattern)&&n.getInt("BossBeat")==0&&!n.getBoolean("Vulnerable"),"Pattern skipped its warning: "+state+" / "+tier+" / "+pattern);RescueBosses.tick(p,s,time+(state==RescueChapter.Phase.THEATRE_FIGHT?200:100));h.assertTrue((state==RescueChapter.Phase.MANTIS_FIGHT?n.getInt("BossBeat")>=100:n.getBoolean("Vulnerable"))&&RescueProps.find(p,s,"WeakPoint")!=null,"Pattern never reached its reveal opportunity");}
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
    @GameTest(template="empty") public static void onlyCurrentOwnersRevealedExposedWeakPointAcceptsDamageAndCooldown(GameTestHelper h){var p=player(h,com.coraline.otherworld.OtherWorld.KEY);var foreign=player(h,com.coraline.otherworld.OtherWorld.KEY);var s=OtherMotherLoop.scene(p);var forced=warmActorChunks(p,s);h.runAfterDelay(25,()->{try{RescueBosses.begin(p,s,RescueChapter.Phase.MANTIS_FIGHT);h.assertTrue(RescueBosses.inArena(p,RescueChapter.Phase.MANTIS_FIGHT),"Native strike fixture must use the real Other World Mantis arena");var at=Vec3.atBottomCenterOf(TheatreSite.center(p,s)).add(0,1.5,0);p.setPos(at.add(0,-1.5,3));p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,at);var point=RescueProps.ensure(p,s,"WeakPoint",RescueProps.CONTROL_POINT,0,at);var body=RescueProps.ensure(p,s,"Mantis",RescueProps.MANTIS,0,at);var n=RescueChapter.data(s);n.putDouble("WeakX",at.x);n.putDouble("WeakY",at.y);n.putDouble("WeakZ",at.z);float health=n.getFloat("BossHealth");
            h.assertTrue(RescueEncounters.gaze(p,at,5,.975)&&p.hasLineOfSight(point),"Weak-point fixture lacks real server aim/line of sight");
            h.assertTrue(RescueProps.find(p,s,"WeakPoint")==point,"Weak-point fixture entity was not registered under its actual UUID");
            h.assertTrue(point.owned(s)&&RescueChapter.owns(p,s)&&RescueBosses.fighting(RescueChapter.phase(s))&&p.distanceToSqr(point)<=30,"Weak-point fixture lacks actual owner/phase/reach");
            h.assertFalse(com.coraline.multiplayer.StoryCoop.canAct(p,s),"Unregistered fixture owner bypassed native input authority");ownerLookup(p,true);h.assertTrue(com.coraline.multiplayer.StorySessionData.get(p.server).observe(p,s)&&com.coraline.multiplayer.StoryCoop.canAct(p,s),"Registered owner phase not authorized");
            n.putBoolean("Vulnerable",false);n.putLong("WeakUntil",p.serverLevel().getGameTime()+100);RescueBosses.hit(p,point);h.assertTrue(n.getFloat("BossHealth")==health,"Closed control point took damage");n.putBoolean("Vulnerable",true);n.putLong("WeakUntil",p.serverLevel().getGameTime()-1);RescueBosses.hit(p,point);h.assertTrue(n.getFloat("BossHealth")==health,"Unrevealed weakness took damage");n.putLong("WeakUntil",p.serverLevel().getGameTime()+100);RescueBosses.hit(p,point);h.assertTrue(n.getFloat("BossHealth")==health,"A strike without attack cooldown damaged the control point");p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(7);p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED).setBaseValue(100);p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,at.add(2,0,0));RescueBosses.hit(p,body);RescueBosses.hit(foreign,point);h.assertTrue(n.getFloat("BossHealth")==health,"Boss body/foreign owner bypassed the exact weak point");h.assertFalse(n.getBoolean("Vulnerable"),"A missed body strike left the opening active");
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,at);RescueBosses.hit(p,point);h.assertTrue(n.getFloat("BossHealth")==health,"Miss could be corrected by spamming in the same opening");
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
            n.putBoolean("Vulnerable",true);n.putLong("WeakUntil",p.serverLevel().getGameTime()+80);
            RescueBosses.hit(p,point);
            float after=n.getFloat("BossHealth");
            h.assertTrue(after==health-2,"A cooled unarmed strike did not damage the revealed weak point");
            n.putBoolean("Vulnerable",true);n.putLong("WeakUntil",p.serverLevel().getGameTime()+80);
            RescueBosses.hit(p,point);
            h.assertTrue(n.getFloat("BossHealth")==after,"Same-tick unarmed attack bypassed cooldown");
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
            n.putLong("HitAt",p.serverLevel().getGameTime()-15);
            RescueBosses.hit(p,point);
            h.assertTrue(after-n.getFloat("BossHealth")>2,"A cooled weapon strike did not deal more than an unarmed strike");
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
    @GameTest(template="empty") public static void ratmasterWarningsAndHitboxesUseTheSameEdges(GameTestHelper h){
        double[][] inside={{0,8},{10,0},{3,0},{-3,0},{5,0}};
        double[][] outside={{1.7,8},{10,4},{3,2.2},{3,0},{8,0}};
        for(int tier=0;tier<3;tier++)for(int pattern=0;pattern<5;pattern++){
            var region=RatmasterRegion.attack(pattern,tier,Vec3.ZERO,Vec3.ZERO,0);
            h.assertTrue(region.hits(body(inside[pattern],0)),"Inside attack region was marked safe: "+tier+"/"+pattern);
            h.assertFalse(region.hits(body(outside[pattern],0)),"Outside boundary was hit: "+tier+"/"+pattern);
            h.assertFalse(region.hits(body(inside[pattern],3)),"Jump outside vertical danger extent was hit");
            h.assertFalse(region.hits(body(inside[pattern],-3)),"Player below the arena was hit");
            h.assertTrue(region.warning().size()>10,"Shared attack model produced no readable contour");
        }
        var wave=RatmasterRegion.attack(0,0,Vec3.ZERO,Vec3.ZERO,0);
        h.assertTrue(wave.hits(new net.minecraft.world.phys.AABB(1.5,0,15.8,1.9,1.8,16.2)),"Player body overlap at strip corner was missed");
        h.assertFalse(wave.hits(new net.minecraft.world.phys.AABB(1.7,0,15.8,2.1,1.8,16.2)),"Dodging beyond the marked corner still took damage");
        h.succeed();
    }
    private static net.minecraft.world.phys.AABB body(double[] at,double y){
        return new net.minecraft.world.phys.AABB(at[0]-.03,y,at[1]-.03,at[0]+.03,y+1.8,at[1]+.03);
    }
    @GameTest(template="empty") public static void finalRoomDoorsAreLockedAcrossConfrontationButReleaseInTheWeb(GameTestHelper h){
        var p=player(h);
        var s=OtherMotherLoop.scene(p);
        ParentRescueStory.scenery(s).putLong("LittleDoor",new BlockPos(-17,110,18).asLong());
        var n=RescueChapter.data(s);
        n.putBoolean("RoomLocked",true);
        try{
            var entrance=FirstEscapeScene.entrance(s);
            var little=BlockPos.of(ParentRescueStory.scenery(s).getLong("LittleDoor"));
            for(var phase:RescueChapter.Phase.values()){
                n.putString("Phase",phase.name());
                boolean locked=phase.ordinal()>=RescueChapter.Phase.ROOM_LOCKED.ordinal()&&phase.ordinal()<=RescueChapter.Phase.FINAL_O.ordinal();
                for(var at:java.util.List.of(entrance,entrance.above(),entrance.south(),entrance.south().above(),little,little.above()))
                    h.assertTrue(BeldamFinale.lockedDoor(s,at)==locked,"Wrong protection in "+phase+" for "+at);
                h.assertFalse(BeldamFinale.lockedDoor(s,entrance.north()),"Unrelated door was locked");
            }
        }finally{clean(p);}
        h.succeed();
    }
    @GameTest(template="empty") public static void falseFloorReceiptIsClearedAndObstructingBlockEntityIsPreserved(GameTestHelper h){
        var p=player(h);
        var s=OtherMotherLoop.scene(p);
        var level=p.server.getLevel(com.coraline.otherworld.OtherWorld.KEY);
        var house=new BlockPos(200,130,200);
        ParentRescueStory.scenery(s).putLong("House",house.asLong());
        var original=FirstEscapeScene.at(s,23,5,44);
        var obstruction=FirstEscapeScene.at(s,20,5,42);
        var expected=com.coraline.otherworld.RosewoodHouse.palaceCells().stream()
                .filter(cell->cell.offset().equals(original.subtract(house))).findFirst().orElseThrow().state();
        level.getChunkAt(obstruction);
        level.getChunkAt(original);
        level.setBlock(obstruction,Blocks.BARREL.defaultBlockState(),2);
        level.setBlock(original,expected,2);
        try{
            var n=RescueChapter.data(s);
            n.putBoolean("FloorOpened",true);
            h.assertFalse(BeldamFinale.prepareFall(p,s),"Obstructed floor trusted a legacy success receipt");
            h.assertTrue(!n.getBoolean("FloorOpened")&&level.getBlockEntity(obstruction)!=null
                    &&level.getBlockState(original).equals(expected),"Validation modified the floor before checking the whole footprint");
            level.removeBlock(obstruction,false);
            h.assertTrue(BeldamFinale.prepareFall(p,s)&&n.getBoolean("FloorOpened")&&level.isEmptyBlock(original),
                    "Cleared floor could not commit actual falling clearance");
        }finally{clean(p);level.removeBlock(obstruction,false);level.removeBlock(original,false);}
        h.succeed();
    }
    @GameTest(template="empty") public static void legacyWebEntryCannotCommitWithoutTheOriginalMother(GameTestHelper h){
        var p=player(h);
        var s=OtherMotherLoop.scene(p);
        var n=RescueChapter.data(s);
        n.putString("Phase","FINAL_P");
        n.putBoolean("WebEntered",true);
        try{
            h.assertFalse(BeldamFinale.enterWeb(p,s,null),"Missing Mother allowed the web confrontation to start");
            h.assertTrue(!n.getBoolean("WebEntered")&&!n.getBoolean("WebEntryReady")&&n.contains("Blocker"),
                    "Legacy partial transfer retained a misleading ready receipt");
        }finally{clean(p);}
        h.succeed();
    }
    @GameTest(template="empty") public static void lostCatPausesCollapseInsteadOfConsumingTheEscapeClock(GameTestHelper h){
        var p=player(h);
        var s=OtherMotherLoop.scene(p);
        var other=p.server.getLevel(com.coraline.otherworld.OtherWorld.KEY);
        var body=new ServerPlayer(p.server,other,p.getGameProfile());
        body.connection=p.connection;
        var n=RescueChapter.data(s);
        n.putString("Phase","COLLAPSE");
        n.putInt("CollapseElapsed",123);
        n.putInt("CollapseDuration",1500);
        n.putDouble("CollapseRadius",82);
        try{
            RescueEncounters.tick(body,s,RescueChapter.Phase.COLLAPSE,1600,false);
            h.assertTrue(n.getInt("CollapseElapsed")==123&&n.getDouble("CollapseRadius")==82
                    &&!n.getBoolean("CatRescued")&&n.contains("Blocker"),"Missing Cat spent the clock or granted a rescue");
        }finally{clean(p);}
        h.succeed();
    }
    @GameTest(template="empty") public static void bossCannotAdvanceAcrossMatchingCoordinatesInWrongDimension(GameTestHelper h){
        var p=player(h);var s=OtherMotherLoop.scene(p);var n=RescueChapter.data(s);
        try{
            n.putString("Phase",RescueChapter.Phase.MANTIS_FIGHT.name());
            n.putInt("Ticks",41);
            p.setPos(RescueEncounters.center(p,s));
            h.assertFalse(RescueBosses.inArena(p,RescueChapter.Phase.MANTIS_FIGHT),"Offworld director was accepted into the Mantis arena");
            h.assertFalse(RescueBosses.nearArena(p,s),"Matching coordinates in the Activity World became a Mantis boss location");
            RescueChapter.tick(p,s);
            h.assertTrue(n.getBoolean("Recover")&&n.getInt("Ticks")==41,"Offworld Mantis fight spent its attack clock instead of requesting recovery");
            h.assertTrue(RescueBosses.inArena(p,RescueChapter.Phase.THEATRE_FIGHT)
                    &&RescueBosses.inArena(p,RescueChapter.Phase.SWARM_FIGHT),"Theatre and Ratmaster arenas lost their Activity World identity");
        }finally{clean(p);}
        h.succeed();
    }
    @GameTest(template="empty") public static void finaleSwatWaitsForOriginalActorsAndRestartsInterruptedVoice(GameTestHelper h){
        var p=player(h);var s=OtherMotherLoop.scene(p);var n=RescueChapter.data(s);
        var other=p.server.getLevel(com.coraline.otherworld.OtherWorld.KEY);
        var viewer=new ServerPlayer(p.server,other,p.getGameProfile());viewer.connection=p.connection;
        try{
            h.assertFalse(BeldamFinale.finaleCastReady(RescueChapter.Phase.FINAL_N,true,false,false),"Missing Cat allowed the swat");
            h.assertTrue(BeldamFinale.finaleCastReady(RescueChapter.Phase.FINAL_N,true,false,true),"Already-escaped Cat blocked a genuine saved completion");
            h.assertFalse(BeldamFinale.finaleCastReady(RescueChapter.Phase.FINAL_N,false,true,false),"Missing canonical Mother allowed the finale");
            h.assertTrue(BeldamFinale.finaleCastReady(RescueChapter.Phase.FINAL_O,false,false,false),"Web entrance inherited the unrelated finale-cast gate");
            n.putString("Phase",RescueChapter.Phase.FINAL_N.name());
            n.putBoolean("Audio",true);
            n.putInt("Ticks",360);
            h.assertFalse(BeldamFinale.originalFinaleCastReady(viewer,s,RescueChapter.Phase.FINAL_N),"Absent original finale cast was treated as present");
            h.assertTrue(!n.getBoolean("Audio")&&n.getInt("Ticks")==0&&!n.getString("Blocker").isEmpty(),
                    "Missing original cast silently spent the recording rather than preserving the cutscene");
            h.assertFalse(n.getBoolean("CatEscaped"),"A missing Cat was awarded an escape receipt");
        }finally{clean(p);}
        h.succeed();
    }

}
