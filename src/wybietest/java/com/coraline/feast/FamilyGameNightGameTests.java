package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.registry.ModEntities;
import com.coraline.secrets.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class FamilyGameNightGameTests {
    @GameTest(template="empty") public static void gameNightControllerCannotMissEveryNavigationTickBySchedulerParity(GameTestHelper h){var w=com.coraline.registry.CharacterCast.ENTITIES.get("other_wybie").get().create(h.getLevel());w.getPersistentData().putUUID("GameNightOwner",UUID.randomUUID());w.getPersistentData().putUUID("WybieCompanionOwner",UUID.randomUUID());var controller=new FamilyGameNight.ControlGoal(w);h.assertTrue(controller.canUse()&&controller.requiresUpdateEveryTick(),"Half-rate game-night goal can miss all tickCount % 10 navigation updates");h.assertFalse(new WybieOptionalVisits.CompanionGoal(w).canUse(),"Generic companion goal still competes with the game-night driver");for(int parity=0;parity<2;parity++){int updates=0;for(int tick=1;tick<=100;tick++)if((controller.requiresUpdateEveryTick()||tick%2==parity)&&tick%10==0)updates++;h.assertTrue(updates==10,"Scheduler parity starved table approach updates");}h.succeed();}
    @GameTest(template="empty")
    public static void completionRequiresActuallyFinishedMatch(GameTestHelper h){
        var s=new VisitData.Scene();
        s.loop.event.put("Game",TabletopGames.start(TabletopGames.Game.BUTTON_CARDS,5));
        h.assertFalse(FamilyGameNight.completionEligible(s),"Unplayed event qualified for rewards");
        var rules=s.loop.event.getCompound("Game");rules.putBoolean("Done",true);rules.putInt("Winner",0);
        h.assertFalse(FamilyGameNight.completionEligible(s),"Uncounted match qualified for rewards");
        s.loop.event.putInt("CompletedMatches",1);
        h.assertTrue(FamilyGameNight.completionEligible(s),"Completed legitimate match lost eligibility");
        rules.putBoolean("TimedOut",true);
        h.assertFalse(FamilyGameNight.completionEligible(s),"Timed-out rematch qualified for rewards");
        h.succeed();
    }
    @GameTest(template="empty",batch="family-four-participants",timeoutTicks=120)
    public static void seatsOwnershipNonceAndInterruptedMatchRemainConsistent(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(OtherWorld.KEY);var at=new BlockPos(35000,130,35000);var chunk=new net.minecraft.world.level.ChunkPos(at);for(int x=chunk.x-1;x<=chunk.x+1;x++)for(int z=chunk.z-1;z<=chunk.z+1;z++){level.setChunkForced(x,z,true);level.getChunk(x,z);}
        var p=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"GameNightQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);p.setPos(Vec3.atBottomCenterOf(at));
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.table=at;s.piano=at.east(20);s.loop.visits=2;s.loop.secrets.visit=2;s.loop.trust=.5F;s.loop.suspicion=.1F;VisitData.get(p.server).scenes.put(p.getUUID(),s);p.getPersistentData().putUUID("CoralineVisitDoor",s.door);
        var r=new RealmData.Realm();r.door=s.door;r.doorPos=at;r.sourceDoor=at;r.house=at;r.porch=at;r.table=at;r.piano=s.piano;r.ready=true;RealmData.get(p.server).realms.put(s.door,r);
        s.loop.event.putString("Type",VisitEvents.Type.FAMILY_GAME_NIGHT.name());s.loop.event.putInt("Serial",1);
        s.loop.story.putBoolean("WybieIntroductionComplete",true);
        for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++){level.setBlock(at.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);for(int y=0;y<4;y++)level.setBlock(at.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);}
        var companion=com.coraline.registry.CharacterCast.ENTITIES.get("other_wybie").get().create(level);companion.getPersistentData().putUUID("RealmDoor",s.door);companion.getPersistentData().putBoolean("CoralineDecoration",true);companion.setNoAi(true);companion.setNoGravity(true);companion.noPhysics=true;companion.setPos(p.position());
        companion.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).setBaseValue(27);
        h.runAtTickTime(30,()->h.assertTrue(level.addFreshEntity(companion),"Protected-world fixture rejected its existing Other Wybie"));
        h.runAtTickTime(40,()->{
            var mom=ModEntities.MOM.get().create(level);var dad=ModEntities.DAD.get().create(level);for(var actor:List.of(mom,dad)){actor.makeOtherParent(0);actor.getPersistentData().putUUID("RealmDoor",s.door);actor.setPos(p.position());level.addFreshEntity(actor);}mom.getPersistentData().putUUID("LoopOwner",p.getUUID());s.mom=mom.getUUID();s.dad=dad.getUUID();FamilyGameNight.beginWindow(p,s);
        });
        h.runAtTickTime(50,()->{
            var chairs=VisitEvents.props(p,s).stream().filter(e->e.kind()==ScenePropEntity.CHAIR).toList();h.assertTrue(chairs.size()==4&&chairs.stream().map(e->e.slot).distinct().count()==4,"Game Night lacks four distinct reserved seats: count="+chairs.size()+", slots="+chairs.stream().map(e->e.slot).toList()+", optional="+s.loop.event.getBoolean("GameHasWybie")+", eligible="+WybieOptionalVisits.allowed(s)+", existing="+(level.getEntity(companion.getUUID())==companion)+", lease="+FamilyGameNight.companion(s));
            var w=FamilyGameNight.wybie(p,s);h.assertTrue(w!=null&&w.characterId().equals("other_wybie"),"Actual silent Other Wybie missing");
            h.assertTrue(w==companion&&!w.isNoAi()&&!w.isNoGravity()&&!w.noPhysics,"Acquisition replaced the borrowed actor or left its navigation/physics disabled");
            h.assertTrue(w.partyDoorDuck()&&w.getBbHeight()<2,"Companion still plans a three-node-high path through carpeted two-block house doors");
            h.assertTrue(w.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).getBaseValue()==64&&w.getPersistentData().getDouble("WybieOldFollowRange")==27,"Long house route still uses 16 m navigation range or overwrote the original attribute");
            var reloaded=com.coraline.feast.OtherMotherLoopState.load(s.loop.save());s.loop=reloaded;
            h.assertTrue(FamilyGameNight.wybie(p,s)==companion,"Reload lost the original owner/realm/serial-bound game-night lease");
            var seat=FamilyGameNight.prop(p,s,ScenePropEntity.CHAIR,0);var mom=MotherSearch.mother(p);var dad=VisitEvents.father(p,s);mom.startRiding(FamilyGameNight.prop(p,s,ScenePropEntity.CHAIR,1),true);dad.startRiding(FamilyGameNight.prop(p,s,ScenePropEntity.CHAIR,2),true);w.startRiding(FamilyGameNight.prop(p,s,ScenePropEntity.CHAIR,3),true);p.setPos(seat.position());
            s.loop.secrets.search=SecretsState.Search.DIRECT_CHASE;h.assertTrue(!FamilyGameNight.arrive(p,s,seat),"Seat cleared active pursuit");s.loop.secrets.search=SecretsState.Search.NORMAL;
            h.assertTrue(FamilyGameNight.arrive(p,s,seat)&&p.getVehicle()==seat,"Owner cannot join reserved family seat");float trust=s.loop.trust;
            h.assertTrue(FamilyGameNight.seated(p,s)&&!SecretsRuntime.active(p)&&!MotherInterrupts.check(p),"Invited seated owner still triggers house surveillance/confrontation after the greetings");
            p.stopRiding();h.assertFalse(FamilyGameNight.seated(p,s),"Leaving the assigned chair retained the gathering exemption");p.startRiding(seat,true);
            FamilyGameNight.selectGame(p,s,TabletopGames.Game.BUTTON_CARDS);s.loop.phase=OtherMotherLoopState.Phase.GAME_NIGHT_ACTIVE;var rules=s.loop.event.getCompound("Game");var nonce=s.loop.event.getUUID("GameMoveNonce");int[] hand=rules.getIntArray("Hand0");int index=-1;for(int i=0;i<hand.length;i++)if(TabletopGames.legalCard(hand[i],rules.getInt("Top"),rules.getInt("Color"))){index=i;break;}String action=index<0?"draw":"card:"+index+":0";
            FamilyGameNight.choice(p,UUID.randomUUID(),action);h.assertTrue(rules.getInt("Moves")==0,"Forged nonce altered authoritative match");FamilyGameNight.choice(p,nonce,action);int moves=rules.getInt("Moves");FamilyGameNight.choice(p,nonce,action);h.assertTrue(moves==1&&rules.getInt("Moves")==moves,"Move replay changed game twice");
            FamilyGameNight.suspend(p,s);s.loop.phase=OtherMotherLoopState.Phase.EXPLORING;h.assertTrue(FamilyGameNight.resume(p)&&s.loop.phase==OtherMotherLoopState.Phase.GAME_NIGHT_WAIT,"Reconciliation lost pending Game Night");p.stopRiding();p.setPos(seat.position());h.assertTrue(FamilyGameNight.arrive(p,s,seat)&&s.loop.phase==OtherMotherLoopState.Phase.GAME_NIGHT_ACTIVE&&s.loop.event.getCompound("Game").getInt("Moves")==moves&&s.loop.trust==trust,"Resume replayed arrival reward or reset current match");
            // A result-page interruption must resume the same result without counting the match twice.
            s.loop.event.putInt("Matches",1);s.loop.event.putInt("CompletedMatches",1);rules.putBoolean("Done",true);rules.putInt("Winner",0);
            s.loop.event.putUUID("GameChoiceNonce",UUID.randomUUID());s.loop.phase=OtherMotherLoopState.Phase.GAME_NIGHT_CHOICE;
            var choiceNonce=s.loop.event.getUUID("GameChoiceNonce");
            FamilyGameNight.choice(p,choiceNonce,"not_a_choice");
            h.assertTrue(s.loop.event.getUUID("GameChoiceNonce").equals(choiceNonce)&&s.loop.phase==OtherMotherLoopState.Phase.GAME_NIGHT_CHOICE,"Unknown choice consumed nonce or finished event");
            s.loop.event.putInt("Matches",2);FamilyGameNight.choice(p,choiceNonce,"another");
            h.assertTrue(s.loop.event.getInt("Matches")==2&&s.loop.phase==OtherMotherLoopState.Phase.GAME_NIGHT_CHOICE&&s.loop.event.getUUID("GameChoiceNonce").equals(choiceNonce),"Third match was offered or started");
            FamilyGameNight.suspend(p,s);s.loop.phase=OtherMotherLoopState.Phase.EXPLORING;
            h.assertTrue(FamilyGameNight.resume(p),"Result-page reconciliation lost pending event");
            p.stopRiding();p.setPos(seat.position());h.assertTrue(FamilyGameNight.arrive(p,s,seat),"Cannot resume result-page seat");
            h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.GAME_NIGHT_CHOICE&&s.loop.event.getInt("Matches")==2,"Result-page resume replayed finished match");
            // Temporary held props restore at actor changes, between games, and on interruption.
            var originalDad=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.APPLE);
            var originalWybie=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER);
            dad.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,originalDad.copy());w.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,originalWybie.copy());
            FamilyGameNight.selectGame(p,s,TabletopGames.Game.SILENT_CHARADES);s.loop.phase=OtherMotherLoopState.Phase.GAME_NIGHT_ACTIVE;
            var charades=s.loop.event.getCompound("Game");charades.putInt("Actor",2);charades.putInt("Topic",1);FamilyGameNight.act(p,s,charades);
            h.assertTrue(dad.getMainHandItem().is(net.minecraft.world.item.Items.FISHING_ROD),"Father did not hold charade prop");
            charades.putInt("Actor",3);charades.putInt("Topic",0);FamilyGameNight.act(p,s,charades);
            h.assertTrue(dad.getMainHandItem().is(net.minecraft.world.item.Items.APPLE)&&w.getMainHandItem().is(net.minecraft.world.item.Items.FEATHER),"Actor change did not restore previous actor prop");
            FamilyGameNight.suspend(p,s);
            h.assertTrue(w.getMainHandItem().is(net.minecraft.world.item.Items.PAPER)&&!w.getPersistentData().contains("GameOldHand"),"Interruption did not restore Wybie prop");
            FamilyGameNight.selectGame(p,s,TabletopGames.Game.SILENT_CHARADES);charades=s.loop.event.getCompound("Game");charades.putInt("Actor",2);charades.putInt("Topic",2);FamilyGameNight.act(p,s,charades);
            FamilyGameNight.selectGame(p,s,TabletopGames.Game.BUTTON_FLICK);
            h.assertTrue(dad.getMainHandItem().is(net.minecraft.world.item.Items.APPLE)&&!dad.getPersistentData().contains("GameOldHand"),"New match retained charade prop");
            // The fixture disables guest AI. Restore its required seat after
            // suspension, as the real online companion controller would do.
            h.assertTrue(w.startRiding(FamilyGameNight.prop(p,s,ScenePropEntity.CHAIR,3),true),"Fixture could not restore Wybie's reserved seat");
            s.loop.phase=OtherMotherLoopState.Phase.GAME_NIGHT_ACTIVE;s.loop.event.putInt("GameAway",1200);p.stopRiding();FamilyGameNight.tick(p,s);
            h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.GAME_NIGHT_FINISH&&!s.loop.event.getBoolean("GameCompleted"),"Abandoned game qualified for rewards");
            s.loop.event.putLong("GameShelfAfter",12345);
            mom.discard();dad.discard();VisitEvents.cleanup(p,s);h.assertTrue(!companion.isRemoved()&&companion.isNoAi()&&companion.isNoGravity()&&companion.noPhysics,"Cleanup deleted the borrowed actor or failed to restore its original AI/gravity/physics flags");h.assertTrue(companion.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).getBaseValue()==27&&!companion.getPersistentData().contains("WybieOldFollowRange"),"Cleanup did not restore the borrowed actor's exact navigation attribute");companion.discard();h.assertTrue(!s.loop.event.contains("GameShelfAfter"),"Shelf retry timer survived event cleanup");RealmData.get(p.server).realms.remove(s.door);VisitData.get(p.server).scenes.remove(p.getUUID());for(int x=chunk.x-1;x<=chunk.x+1;x++)for(int z=chunk.z-1;z<=chunk.z+1;z++)level.setChunkForced(x,z,false);h.succeed();
        });
    }
}
