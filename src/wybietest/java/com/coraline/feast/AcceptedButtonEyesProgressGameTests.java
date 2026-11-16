package com.coraline.feast;

import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class AcceptedButtonEyesProgressGameTests {
    @GameTest(template="empty") public static void completionRequiresRealGameReturnAndReaction(GameTestHelper h){
        var n=new CompoundTag();var door=UUID.randomUUID();var id=UUID.randomUUID();
        h.assertTrue(AcceptedButtonEyesProgress.begin(n,AcceptedButtonEyesProgress.Route.ACCEPTED_PROPOSAL,door,5,0),"Initial entry refused");
        var initial=n.copy();h.assertFalse(AcceptedButtonEyesProgress.begin(n,AcceptedButtonEyesProgress.Route.FINAL_BOSS_LOSS,UUID.randomUUID(),1,1),"Reentry overwrote original branch");
        h.assertTrue(n.equals(initial),"Reentry mutated existing progress");
        AcceptedButtonEyesProgress.move(n,AcceptedButtonEyesProgress.Phase.ACTIVITY_INVITATION,1);
        h.assertTrue(AcceptedButtonEyesProgress.openActivity(n,"archery_contest",id,2),"Activity entry refused");
        h.assertTrue(AcceptedButtonEyesProgress.stage(n)==0&&!AcceptedButtonEyesProgress.normalReturn(n,id,3),"Starting/aborting counted as completion");
        h.assertFalse(AcceptedButtonEyesProgress.gameCompleted(n,UUID.randomUUID()),"Foreign instance completed this activity");
        AcceptedButtonEyesProgress.gameCompleted(n,id);h.assertTrue(AcceptedButtonEyesProgress.normalReturn(n,id,4),"Normal return refused");
        var restored=n.copy();h.assertTrue(AcceptedButtonEyesProgress.stage(restored)==0,"Return advanced before normal reaction");
        h.assertTrue(AcceptedButtonEyesProgress.commitReaction(restored,5)&&AcceptedButtonEyesProgress.stage(restored)==1,"Completed return did not commit");
        h.assertFalse(AcceptedButtonEyesProgress.commitReaction(restored,6),"Repeated callback duplicated progress");h.succeed();
    }
    @GameTest(template="empty") public static void interruptedActivitiesAndTerminalSaveCannotRestartEscape(GameTestHelper h){
        var n=new CompoundTag();AcceptedButtonEyesProgress.begin(n,AcceptedButtonEyesProgress.Route.FINAL_BOSS_LOSS,UUID.randomUUID(),5,0);
        AcceptedButtonEyesProgress.move(n,AcceptedButtonEyesProgress.Phase.ACTIVITY_INVITATION,1);
        AcceptedButtonEyesProgress.openActivity(n,"fishing_contest",UUID.randomUUID(),2);AcceptedButtonEyesProgress.abortActivity(n,3);
        h.assertTrue(AcceptedButtonEyesProgress.completed(n)==0&&AcceptedButtonEyesProgress.stage(n)==0,"Abort granted deterioration");
        for(int i=0;i<5;i++){var id=UUID.randomUUID();h.assertTrue(AcceptedButtonEyesProgress.openActivity(n,"fishing_contest",id,4+i*4),"Normal activity could not start");AcceptedButtonEyesProgress.gameCompleted(n,id);AcceptedButtonEyesProgress.normalReturn(n,id,5+i*4);AcceptedButtonEyesProgress.commitReaction(n,6+i*4);}
        h.assertTrue(AcceptedButtonEyesProgress.stage(n)==5&&AcceptedButtonEyesProgress.phase(n)==AcceptedButtonEyesProgress.Phase.WORLD_COLLAPSED,"Final completed return did not collapse world");
        AcceptedButtonEyesProgress.move(n,AcceptedButtonEyesProgress.Phase.ACCEPTED_BUTTON_EYES_MIRROR_PRISON,30);
        var saved=n.copy();AcceptedButtonEyesProgress.move(saved,AcceptedButtonEyesProgress.Phase.ACTIVITY_INVITATION,40);
        h.assertTrue(AcceptedButtonEyesProgress.terminal(saved)&&saved.equals(n),"Terminal reload escaped prison or reset its playback checkpoint");h.succeed();
    }
    @GameTest(template="empty") public static void terminalBranchSuppressesPriorStoryAndEntryResets(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);var s=OtherMotherLoop.scene(p);try{
            var traditional=BlackIsTraditional.data(s);traditional.putBoolean("Started",true);traditional.putString("Phase","AWAITING_CHOICE");
            FirstEscapeStory.data(s).putBoolean("Started",true);ParentRescueStory.data(s).putBoolean("Started",true);RescueChapter.data(s).putBoolean("Started",true);MissingParentsTea.data(s).putBoolean("Active",true);
            var n=AcceptedButtonEyesProgress.data(s);AcceptedButtonEyesProgress.begin(n,AcceptedButtonEyesProgress.Route.FINAL_BOSS_LOSS,s.door,5,0);AcceptedButtonEyesProgress.move(n,AcceptedButtonEyesProgress.Phase.ACCEPTED_BUTTON_EYES_MIRROR_PRISON,1);n.putBoolean("GhostScenePlayed",true);s.loop.phase=OtherMotherLoopState.Phase.ACCEPTED_BUTTON_EYES;
            h.assertFalse(BlackIsTraditional.restoreOnEntry(p,s),"Old offer resumed over accepted branch");
            h.assertFalse(FirstEscapeStory.engaged(s)||ParentRescueStory.started(s)||RescueChapter.started(s)||MissingParentsTea.active(s)||MissingParentsCat.eligible(s),"Old rescue/aftermath track resumed");
            var realm=new com.coraline.otherworld.RealmData.Realm();realm.door=s.door;int visits=s.loop.visits;
            h.assertTrue(OtherMotherLoop.entered(p,realm)&&s.loop.visits==visits,"Entry recreated a normal visit");OtherMotherLoop.depart(p,false);
            h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.ACCEPTED_BUTTON_EYES&&AcceptedButtonEyesProgress.terminal(n)&&n.getBoolean("GhostScenePlayed"),"Entry/depart destroyed terminal checkpoint");
            var saved=OtherMotherLoopState.load(s.loop.save());h.assertTrue(AcceptedButtonEyesProgress.terminal(saved.story.getCompound(AcceptedButtonEyesProgress.KEY)),"Terminal state lost in actual loop serialization");
        }finally{AcceptedBranchPlayers.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=1200) public static void acceptedChoiceAndFinalLossShareOwnerScopedEntry(GameTestHelper h){
        var server=h.getLevel().getServer();var level=server.getLevel(com.coraline.otherworld.OtherWorld.KEY);h.assertTrue(level!=null,"Other World dimension missing");
        var players=new java.util.ArrayList<net.minecraft.server.level.ServerPlayer>();var floors=new java.util.ArrayList<net.minecraft.core.BlockPos>();
        try{for(int i=0;i<2;i++){
            var p=new net.minecraft.server.level.ServerPlayer(server,level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"AcceptedEntryQA"+i));players.add(p);
            p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
            var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.loop.phase=OtherMotherLoopState.Phase.BLACK_IS_TRADITIONAL;s.loop.event.putInt("Serial",3);VisitData.get(server).scenes.put(s.owner,s);
            var realm=new com.coraline.otherworld.RealmData.Realm();realm.door=s.door;realm.house=new net.minecraft.core.BlockPos(110000+i*32,180,110000);realm.doorPos=realm.house;realm.radius=500;realm.ready=true;realm.islandComplete=true;com.coraline.otherworld.RealmData.get(server).realms.put(s.door,realm);
            var at=com.coraline.secrets.HouseCabinets.at(realm.house,34,6,23);for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){var floor=at.offset(x,-1,z);level.setBlock(floor,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);floors.add(floor);}
            s.table=at;s.piano=at;p.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);p.getPersistentData().putUUID("CoralineVisitDoor",s.door);
            if(i==0){var n=BlackIsTraditional.data(s);var nonce=UUID.randomUUID();n.putBoolean("Started",true);n.putString("Phase","AWAITING_CHOICE");n.putUUID("Choice",nonce);
                BlackIsTraditional.choice(p,UUID.randomUUID(),"traditional:accept");h.assertFalse(AcceptedButtonEyes.active(s),"Foreign choice nonce accepted");
                h.assertTrue(BlackIsTraditional.choice(p,nonce,"traditional:accept"),"Accepted option not handled");
            }else h.assertTrue(AcceptedButtonEyes.begin(p,s,AcceptedButtonEyesProgress.Route.FINAL_BOSS_LOSS),"Final loss entry rejected");
            h.assertTrue(AcceptedButtonEyes.active(s)&&AcceptedButtonEyesProgress.phase(AcceptedButtonEyes.data(s))==AcceptedButtonEyesProgress.Phase.BASEMENT_INVITATION,"Shared basement invitation missing");
            h.assertTrue(AcceptedButtonEyes.data(s).getString("Route").equals(i==0?"ACCEPTED_PROPOSAL":"FINAL_BOSS_LOSS"),"Entry route provenance lost");
            h.assertTrue(AcceptedButtonEyesProgress.scenery(s).getLong("House")==realm.house.asLong()&&p.isAlive()&&!p.isPassenger(),"Wrong owner's house or death/mount transition");
        }}finally{for(var p:players){var s=OtherMotherLoop.scene(p);if(s!=null){ShowPlayback.stop(p,s);VisitData.get(server).scenes.remove(s.owner);com.coraline.otherworld.RealmData.get(server).realms.remove(s.door);}}for(var at:floors)level.setBlock(at,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);}h.succeed();
    }
    @GameTest(template="empty") public static void debugCollapseWaitsForCompletedReturnAndSurvivesAbort(GameTestHelper h){
        var n=new CompoundTag();AcceptedButtonEyesProgress.begin(n,AcceptedButtonEyesProgress.Route.ACCEPTED_PROPOSAL,UUID.randomUUID(),5,0);AcceptedButtonEyesProgress.move(n,AcceptedButtonEyesProgress.Phase.ACTIVITY_INVITATION,1);n.putBoolean("DebugCollapseAfterNext",true);
        AcceptedButtonEyesProgress.openActivity(n,"fishing_contest",UUID.randomUUID(),2);AcceptedButtonEyesProgress.abortActivity(n,3);
        h.assertTrue(AcceptedButtonEyesProgress.stage(n)==0&&n.getBoolean("DebugCollapseAfterNext"),"Early exit consumed debug completion");
        var id=UUID.randomUUID();AcceptedButtonEyesProgress.openActivity(n,"archery_contest",id,4);AcceptedButtonEyesProgress.gameCompleted(n,id);AcceptedButtonEyesProgress.normalReturn(n,id,5);AcceptedButtonEyesProgress.commitReaction(n,6);
        h.assertTrue(AcceptedButtonEyesProgress.stage(n)==5&&!n.getBoolean("DebugCollapseAfterNext"),"Next completed activity did not collapse once");h.succeed();
    }
    private AcceptedButtonEyesProgressGameTests(){}
}
