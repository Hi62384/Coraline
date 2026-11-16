package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.secrets.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class PartyRevisionGameTests {
    @GameTest(template="empty") public static void journalFilingRequiresThePhysicalBookAndHeldEvidence(GameTestHelper h){
        var p=fixture(h);try{var s=OtherMotherLoop.scene(p);String id="rat_communication";var secretToken=UUID.randomUUID();var bookToken=UUID.randomUUID();s.loop.secrets.issued.put(secretToken,id);s.loop.secrets.world.putUUID("CarriedJournalToken",bookToken);
            var evidence=new ItemStack(SecretsItems.EVIDENCE.get(id).get());evidence.getOrCreateTag().putUUID("SecretOwner",p.getUUID());evidence.getTag().putUUID("SecretToken",secretToken);p.getInventory().setItem(8,evidence);
            var book=new ItemStack(SecretsItems.JOURNAL.get());book.getOrCreateTag().putUUID("SecretOwner",p.getUUID());book.getTag().putUUID("SecretToken",bookToken);p.setItemSlot(EquipmentSlot.OFFHAND,book);
            h.assertFalse(SecretTransactions.deposit(p,id),"Journal reached into hidden inventory");p.getInventory().setItem(8,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.MAINHAND,evidence);
            h.assertTrue(SecretTransactions.deposit(p,id)&&s.loop.secrets.journal.contains(id)&&p.getMainHandItem().isEmpty(),"Two-hand physical filing failed");h.assertFalse(SecretTransactions.deposit(p,id),"A filed capability was spent twice");
        }finally{cleanup(p);}h.succeed();
    }
    @GameTest(template="empty") public static void playerAdmissionOpensBothLeavesAndClosesAfterTheWave(GameTestHelper h){
        var p=fixture(h);try{var s=OtherMotherLoop.scene(p);var n=HouseParty.data(s);var level=p.serverLevel();var abs=h.absolutePos(new BlockPos(3,2,3));var at=new BlockPos(abs.getX()+74000,180,abs.getZ()+74000);
            var state=net.minecraft.world.level.block.Blocks.SPRUCE_DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING,net.minecraft.core.Direction.NORTH);
            for(int side=0;side<2;side++){var lower=at.east(side);level.setBlock(lower.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);var leaf=state.setValue(net.minecraft.world.level.block.DoorBlock.HINGE,side==0?net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT:net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT);level.setBlock(lower,leaf,18);level.setBlock(lower.above(),leaf.setValue(net.minecraft.world.level.block.DoorBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),18);}
            n.putLong("PartyFrontDoor",at.asLong());n.putString("PartyInward","south");p.setPos(Vec3.atBottomCenterOf(at.south(2)));
            PartyEntrance.opened(new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p,net.minecraft.world.InteractionHand.MAIN_HAND,at,new net.minecraft.world.phys.BlockHitResult(at.getCenter(),net.minecraft.core.Direction.SOUTH,at,false)));
            h.assertTrue(level.getBlockState(at).getValue(net.minecraft.world.level.block.DoorBlock.OPEN)&&level.getBlockState(at.east()).getValue(net.minecraft.world.level.block.DoorBlock.OPEN),"Only one door leaf opened");
            PartyEntrance.tick(p,s);n.putLong("Clock",20);PartyEntrance.tick(p,s);h.assertFalse(level.getBlockState(at).getValue(net.minecraft.world.level.block.DoorBlock.OPEN)||level.getBlockState(at.east()).getValue(net.minecraft.world.level.block.DoorBlock.OPEN),"Admission doors didn't close after clear passage");
        }finally{cleanup(p);}h.succeed();
    }
    @GameTest(template="empty") public static void arrivalGroupsAndCardTurnsVaryWithoutLongPauses(GameTestHelper h){
        var random=net.minecraft.util.RandomSource.create(65);var sizes=new HashSet<Integer>();var waits=new HashSet<Integer>();
        for(int i=0;i<200;i++){int count=PartyGuests.arrivalGroupSize(random,40);h.assertTrue(count>=1&&count<=6,"Arrival group outside 1–6");sizes.add(count);waits.add(PartyGuests.arrivalDelay(random));}
        h.assertTrue(sizes.size()==6&&waits.size()>20,"Arrivals remain clockwork/single-file");
        var game=TabletopGames.start(TabletopGames.Game.BUTTON_CARDS,1);h.assertTrue(TabletopGames.npcTurnDelay(game)>=12&&TabletopGames.npcTurnDelay(game)<=24,"Card AI still waits several seconds");h.succeed();
    }
    private static ServerPlayer fixture(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(OtherWorld.KEY);var p=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"PartyRevisionQA"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.loop.phase=OtherMotherLoopState.Phase.PARTY_SOCIAL;
        VisitData.get(p.server).scenes.put(p.getUUID(),s);p.getPersistentData().putUUID("CoralineVisitDoor",s.door);
        var r=new RealmData.Realm();r.door=s.door;r.house=new BlockPos(72000,180,72000);r.porch=r.house.north(15);r.ready=true;RealmData.get(p.server).realms.put(s.door,r);
        p.setPos(Vec3.atBottomCenterOf(PartyRegions.rooms().get(0).center(r.house)));s.table=s.piano=p.blockPosition();
        var n=new CompoundTag();HousePartyLedger.begin(n,s.owner,4,1,12);s.loop.event.put("Party",n);s.loop.event.putString("Type",VisitEvents.Type.HOUSE_PARTY.name());s.loop.event.putInt("Serial",4);s.loop.event.putBoolean("PartyPending",true);return p;
    }
    private static void cleanup(ServerPlayer p){var s=OtherMotherLoop.scene(p);RealmData.get(p.server).realms.remove(s.door);VisitData.get(p.server).scenes.remove(p.getUUID());}
    @GameTest(template="empty") public static void rosterIncludesAllCompletedNeighboursAndRealCrowd(GameTestHelper h){
        var p=fixture(h);try{var s=OtherMotherLoop.scene(p);s.bobinskyVisited=true;s.teaVisited=true;s.loop.story.putBoolean("WybieIntroductionComplete",true);PartyGuests.roster(p,s);
            var guests=PartyGuests.guests(s);var kinds=guests.stream().map(g->g.getString("Kind")).toList();
            h.assertTrue(guests.size()>=12&&kinds.containsAll(List.of("other_bobinsky","other_miss_spink","other_miss_forcible","other_wybie")),"Completed visits did not produce the named crowd");
            h.assertTrue(guests.stream().map(g->g.getUUID("Id")).distinct().count()==guests.size(),"Duplicate guest identities");
        }finally{cleanup(p);}h.succeed();
    }
    @GameTest(template="empty") public static void uncompletedVisitsAndWybieBanStillApply(GameTestHelper h){
        var p=fixture(h);try{var s=OtherMotherLoop.scene(p);s.loop.story.putBoolean("WybieIntroductionComplete",true);s.loop.story.putBoolean("WybieVisitsForbidden",true);PartyGuests.roster(p,s);
            h.assertTrue(PartyGuests.guests(s).size()>=12&&PartyGuests.guests(s).stream().noneMatch(g->g.getString("Kind").startsWith("other_")),"Unmet/banned named guests were fabricated");
        }finally{cleanup(p);}h.succeed();
    }
    @GameTest(template="empty") public static void bellRequiresSubsequentPlayerOpeningOrPhysicalHostAdmission(GameTestHelper h){
        var g=new CompoundTag();h.assertFalse(PartyEntrance.admitted(g,true,100),"Guest entered without ringing");g.putLong("RangAt",100);
        h.assertFalse(PartyEntrance.admitted(g,true,99)||PartyEntrance.admitted(g,false,101),"Stale opening or shut door admitted guest");
        h.assertTrue(PartyEntrance.admitted(g,true,101),"Player's new opening ignored");g.putBoolean("HostAdmitted",true);
        h.assertTrue(PartyEntrance.admitted(g,true,-1)&&!PartyEntrance.admitted(g,false,-1),"Mother admission bypassed actual door state");h.succeed();
    }
    @GameTest(template="empty") public static void witnessesNoticeOpenCabinetWithoutItsMenu(GameTestHelper h){
        var p=fixture(h);try{var s=OtherMotherLoop.scene(p);h.assertTrue(PartySurveillance.offense(p,s).isEmpty(),"Public-room fixture isn't permitted");
            var cabinets=new ListTag();var c=new CompoundTag();c.putLong("Pos",p.blockPosition().asLong());c.putBoolean("Open",true);cabinets.add(c);var ledger=new CompoundTag();ledger.put("Cabinets",cabinets);s.loop.secrets.world.put("HouseCabinets",ledger);
            h.assertTrue(p.containerMenu==p.inventoryMenu&&PartySurveillance.offense(p,s).equals("drawer"),"Closing inventory concealed the open cabinet");
            c.putBoolean("Open",false);h.assertTrue(PartySurveillance.offense(p,s).isEmpty(),"Closed cabinet still incriminates player");
        }finally{cleanup(p);}h.succeed();
    }
    @GameTest(template="empty") public static void witnessesSeeOffhandEvidenceButNotHiddenInventory(GameTestHelper h){
        var p=fixture(h);try{var s=OtherMotherLoop.scene(p);var id="rat_communication";var token=UUID.randomUUID();s.loop.secrets.issued.put(token,id);var item=new ItemStack(SecretsItems.EVIDENCE.get(id).get());item.getOrCreateTag().putUUID("SecretOwner",p.getUUID());item.getOrCreateTag().putUUID("SecretToken",token);
            p.getInventory().setItem(10,item);h.assertTrue(PartySurveillance.offense(p,s).isEmpty(),"Witness scanned hidden inventory");p.getInventory().setItem(10,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,item);
            h.assertTrue(PartySurveillance.offense(p,s).equals("restricted_item"),"Visible offhand evidence ignored");
        }finally{cleanup(p);}h.succeed();
    }
    @GameTest(template="empty") public static void partyDoesNotHardLockExitBeforeAPursuit(GameTestHelper h){
        var p=fixture(h);try{var s=OtherMotherLoop.scene(p);h.assertFalse(OtherMotherLoop.locked(p),"Party hard-locks the Little Door");s.loop.secrets.search=SecretsState.Search.DIRECT_CHASE;h.assertTrue(OtherMotherLoop.locked(p),"Party bypasses active pursuit");}finally{cleanup(p);}h.succeed();
    }
    @GameTest(template="empty") public static void debugCompletionGrantsNoRewardsAndPreservesBan(GameTestHelper h){
        var p=fixture(h);try{var s=OtherMotherLoop.scene(p);s.loop.story.putBoolean("WybieVisitsForbidden",true);float trust=s.loop.trust,suspicion=s.loop.suspicion;
            p.setGameMode(GameType.SURVIVAL);com.coraline.item.NeighborVisitsDebugItem.complete(p);h.assertFalse(s.bobinskyVisited,"Survival player used debug shortcut");p.setGameMode(GameType.CREATIVE);com.coraline.item.NeighborVisitsDebugItem.complete(p);
            h.assertTrue(s.bobinskyVisited&&s.teaVisited&&s.wybieVisited&&s.loop.story.getBoolean("WybieIntroductionComplete"),"Shortcut missed durable visit flags");h.assertTrue(s.loop.story.getBoolean("WybieVisitsForbidden")&&s.loop.trust==trust&&s.loop.suspicion==suspicion&&p.getInventory().isEmpty(),"Shortcut granted rewards or reset ban/relationship");
        }finally{cleanup(p);}h.succeed();
    }
    @GameTest(template="empty") public static void conversationAnswersDistinguishTopicsAndPeople(GameTestHelper h){
        var g=new CompoundTag();var replies=new HashSet<String>();for(String kind:List.of("other_bobinsky","other_miss_spink","other_miss_forcible","resident")){g.putString("Kind",kind);for(int topic=0;topic<2;topic++)h.assertTrue(replies.add(PartyConversations.socialReply(g,topic)),"Two different questions get the same canned reply");}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100) public static void createdPartyGuestPassesRealmSpawnProtection(GameTestHelper h){
        var p=fixture(h);var s=OtherMotherLoop.scene(p);var realm=SecretWorld.realm(p);var l=p.serverLevel();
        try{
            for(int x=-14;x<=14;x++)for(int z=-10;z<=-4;z++){var at=realm.porch.offset(x,-1,z);l.getChunkAt(at);l.setBlock(at,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);}
            var g=HousePartyLedger.addGuest(HouseParty.data(s),"resident",null,0);g.putString("Name","Spawn QA");
            h.assertTrue(PartyGuests.spawn(p,s,g),"Realm protection rejected the newly created party guest");
            var actor=PartyGuests.entity(p,g);h.assertTrue(actor!=null&&actor.getPersistentData().getBoolean("CoralineDecoration"),"Guest was not admitted as an owned scene actor");actor.discard();
        }finally{cleanup(p);}h.succeed();
    }
}
