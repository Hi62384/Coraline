package com.coraline.wybie;
import com.coraline.bobinsky.*;
import com.coraline.feast.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import java.util.UUID;
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class BobinskyVisitGameTests {
    @GameTest(template="empty",timeoutTicks=200) public static void cheesePickupSurvivesMissingHouseSiteAndRetries(GameTestHelper h){
        var level=h.getLevel();var center=h.absolutePos(new BlockPos(500,200,500));
        for(int x=-48;x<=48;x+=16)for(int z=-48;z<=48;z+=16){var at=center.offset(x,0,z);level.getChunkAt(at);level.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.PORTAL,new net.minecraft.world.level.ChunkPos(at),2,center);}
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)level.setBlock(center.offset(x,0,z),Blocks.STONE.defaultBlockState(),2);
        var p=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"CheesePickupQA"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);p.setPos(center.getX()+.5,center.getY()+1,center.getZ()+.5);
        var scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=UUID.randomUUID();scene.table=scene.piano=center;scene.stage=VisitData.Stage.DONE;VisitData.get(p.server).scenes.put(scene.owner,scene);
        BobinskyVisit.offer(p,scene);var data=BobinskyVisitData.get(p.server);var visit=data.visits.get(p.getUUID());h.assertTrue(visit!=null,"No pickup fixture parcel");var pos=BlockPos.of(visit.getLong("Parcel"));
        BobinskyVisit.takeParcel(p,pos);
        h.assertTrue(visit.getBoolean("Active")&&visit.getInt("Phase")==BobinskyVisit.CHEESE,"No-site pickup cancelled the visit");
        var cheese=p.getInventory().items.stream().filter(stack->BobinskyVisit.matches(stack,visit)).findFirst().orElseThrow(()->new IllegalStateException("Pickup deleted the cheese"));
        h.assertTrue(cheese.getCount()==1&&visit.getBoolean("SiteHintSent"),"Pickup or outdoor hint missing");
        h.assertFalse(level.getBlockState(pos).is(com.coraline.registry.ModBlocks.CHEESE_PARCEL.get()),"Collected ground parcel remains");
        // Protected blocks must remain untouched while waiting for a usable site.
        var protectedBlock=center.offset(20,1,0);level.setBlock(protectedBlock,Blocks.CHEST.defaultBlockState(),2);
        BobinskyVisit.pickedUp(p,cheese);h.assertTrue(level.getBlockState(protectedBlock).is(Blocks.CHEST),"Retry damaged a protected container");
        for(int x=-46;x<=46;x++)for(int z=-46;z<=46;z++)level.setBlock(center.offset(x,0,z),Blocks.STONE.defaultBlockState(),2);
        h.runAfterDelay(5,()->{
            visit.putLong("HouseSearchAt",0);BobinskyVisit.pickedUp(p,cheese);
            h.assertTrue(visit.getBoolean("Active")&&visit.getInt("Phase")==BobinskyVisit.KNOCK&&visit.contains("Originals"),"Carried cheese didn't start the house: phase="+visit.getInt("Phase")+" active="+visit.getBoolean("Active")+" scene="+VisitData.get(p.server).scenes.containsKey(p.getUUID())+" height="+level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,center.getX()+30,center.getZ())+" playerY="+p.getY()+" loaded="+level.hasChunkAt(center.offset(30,0,0)));
            h.assertTrue(cheese.getCount()==1,"Cheese consumed before the handoff");
            var front=BobinskyHouse.front(visit);p.setPos(front.getX()+.5,front.getY(),front.getZ()+.5);BobinskyVisit.knock(p,visit);
            h.assertTrue(visit.getInt("Phase")==BobinskyVisit.A&&BobinskyVisit.actor(p.server,visit)!=null,"Knock didn't begin Bobinsky dialogue");
            BobinskyVisit.cancel(p.server,visit,null);data.visits.remove(p.getUUID());VisitData.get(p.server).scenes.remove(scene.owner);
            level.setBlock(protectedBlock,Blocks.AIR.defaultBlockState(),2);for(int x=-46;x<=46;x++)for(int z=-46;z<=46;z++)level.setBlock(center.offset(x,0,z),Blocks.AIR.defaultBlockState(),2);h.succeed();
        });
    }
    @GameTest(template="empty") public static void fullInventoryLeavesCheeseParcelAvailable(GameTestHelper h){
        var level=h.getLevel();var center=h.absolutePos(new BlockPos(700,200,700));for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)level.setBlock(center.offset(x,0,z),Blocks.STONE.defaultBlockState(),2);
        var p=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"FullCheeseQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);p.setPos(center.getX()+.5,center.getY()+1,center.getZ()+.5);
        var scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=UUID.randomUUID();scene.table=scene.piano=center;scene.stage=VisitData.Stage.DONE;VisitData.get(p.server).scenes.put(scene.owner,scene);BobinskyVisit.offer(p,scene);
        var data=BobinskyVisitData.get(p.server);var visit=data.visits.get(p.getUUID());h.assertTrue(visit!=null,"No full-inventory fixture parcel");var pos=BlockPos.of(visit.getLong("Parcel"));
        for(int slot=0;slot<p.getInventory().items.size();slot++)p.getInventory().items.set(slot,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE,64));
        // Creative inventory.add can discard an uninsertable item; use survival.
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);BobinskyVisit.takeParcel(p,pos);
        h.assertTrue(level.getBlockState(pos).is(com.coraline.registry.ModBlocks.CHEESE_PARCEL.get())&&visit.contains("Parcel")&&visit.getInt("Phase")==BobinskyVisit.CHEESE,"Full inventory consumed the offer");
        p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);BobinskyVisit.takeParcel(p,pos);h.assertTrue(level.getBlockState(pos).is(com.coraline.registry.ModBlocks.CHEESE_PARCEL.get()),"Creative full inventory silently discarded the cheese");
        BobinskyVisit.cancel(p.server,visit,null);data.visits.remove(p.getUUID());VisitData.get(p.server).scenes.remove(scene.owner);for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)level.setBlock(center.offset(x,0,z),Blocks.AIR.defaultBlockState(),2);h.succeed();
    }
    @GameTest(template="empty") public static void openedBobinskyDoorHasFullActorClearance(GameTestHelper h){var l=h.getLevel();var c=h.absolutePos(new BlockPos(65,190,0));var v=new CompoundTag();v.putLong("House",c.asLong());v.put("Originals",BobinskyHouse.snapshot(l,c));BobinskyHouse.build(l,v);var e=com.coraline.registry.ModEntities.BOBINSKY_VISITOR.get().create(l);e.setPos(c.getX()+1,c.getY(),c.getZ()-3.5);BobinskyHouse.open(l,v,true);h.assertTrue(l.noCollision(e,e.getBoundingBox()),"Bobinsky cannot fit through the opened double doorway");BobinskyHouse.open(l,v,false);h.assertFalse(l.noCollision(e,e.getBoundingBox()),"Closed doorway has no collision");e.discard();BobinskyHouse.restore(l,v);h.succeed();}
    @GameTest(template="empty") public static void cheeseOfferUsesSolidGroundOutsideThePlayerPickupBox(GameTestHelper h){
        var level=h.getLevel();var center=h.absolutePos(new BlockPos(0,160,0));for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)level.setBlock(center.offset(x,0,z),Blocks.STONE.defaultBlockState(),2);
        var p=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"CheeseQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);p.setPos(center.getX()+.5,center.getY()+1,center.getZ()+.5);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.table=s.piano=center;s.stage=VisitData.Stage.DONE;VisitData.get(p.server).scenes.put(s.owner,s);BobinskyVisit.offer(p,s);
        var d=BobinskyVisitData.get(p.server);var v=d.visits.get(p.getUUID());h.assertTrue(v!=null,"No ground cheese offered");var pos=BlockPos.of(v.getLong("Parcel"));h.assertTrue(level.getBlockState(pos).is(com.coraline.registry.ModBlocks.CHEESE_PARCEL.get())&&level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP),"Package is not a solid-ground parcel block");h.assertFalse(v.hasUUID("Cheese"),"Package still uses auto-pickup entity");h.assertTrue(Math.max(Math.abs(pos.getX()+.5-p.getX()),Math.abs(pos.getZ()+.5-p.getZ()))>=1.7,"Package appeared under the player");BobinskyVisit.cancel(p.server,v,null);h.assertTrue(level.getBlockState(pos).isAir(),"Cancelled parcel not removed");d.visits.remove(p.getUUID());VisitData.get(p.server).scenes.remove(s.owner);for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)level.setBlock(center.offset(x,0,z),Blocks.AIR.defaultBlockState(),2);h.succeed();
    }
    @GameTest(template="empty") public static void houseRestoresOriginalStatesAndContainerContents(GameTestHelper h){
        var level=h.getLevel();var center=h.absolutePos(new BlockPos(0,40,0));var chest=center.offset(1,0,1);
        level.setBlock(center,Blocks.DIRT.defaultBlockState(),2);level.setBlock(chest,Blocks.CHEST.defaultBlockState(),2);
        ((net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(chest)).setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,7));
        var v=new CompoundTag();v.putLong("House",center.asLong());v.put("Originals",BobinskyHouse.snapshot(level,center));
        level.setBlock(center,Blocks.SPRUCE_PLANKS.defaultBlockState(),2);level.removeBlockEntity(chest);level.setBlock(chest,Blocks.AIR.defaultBlockState(),2);
        BobinskyHouse.restore(level,v);
        h.assertTrue(level.getBlockState(center).is(Blocks.DIRT),"Original terrain lost");
        var restored=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(chest);h.assertTrue(restored!=null&&restored.getItem(0).is(net.minecraft.world.item.Items.DIAMOND)&&restored.getItem(0).getCount()==7,"Container contents lost");h.assertFalse(v.contains("Originals"),"Recovery journal retained after persisted restore");level.setBlock(center,Blocks.AIR.defaultBlockState(),2);level.setBlock(chest,Blocks.AIR.defaultBlockState(),2);h.succeed();
    }
    @GameTest(template="empty") public static void recoveryJournalAndPlacementHistorySurviveSave(GameTestHelper h){
        var d=new BobinskyVisitData();var owner=UUID.randomUUID();var v=new CompoundTag();v.putUUID("Owner",owner);v.putUUID("Token",UUID.randomUUID());v.putLong("House",new BlockPos(4,90,4).asLong());v.putBoolean("Active",true);var originals=new ListTag();var n=new CompoundTag();n.putLong("Pos",44);n.put("State",NbtUtils.writeBlockState(Blocks.DIRT.defaultBlockState()));originals.add(n);v.put("Originals",originals);d.visits.put(owner,v);d.playerPlaced.add(123L);
        var copy=BobinskyVisitData.load(d.save(new CompoundTag()));h.assertTrue(copy.visits.get(owner).getList("Originals",10).size()==1&&copy.playerPlaced.contains(123L),"Recovery snapshot or placed-block history lost");h.succeed();
    }
    @GameTest(template="empty") public static void cancelledVisitRestoresHouseAndRemainsRetryable(GameTestHelper h){
        var level=h.getLevel();var center=h.absolutePos(new BlockPos(0,70,0));level.setBlock(center,Blocks.GRAVEL.defaultBlockState(),2);
        var v=new CompoundTag();v.putUUID("Owner",UUID.randomUUID());v.putUUID("Token",UUID.randomUUID());v.putInt("Phase",BobinskyVisit.KNOCK);v.putLong("House",center.asLong());v.put("Originals",BobinskyHouse.snapshot(level,center));v.putBoolean("Active",true);level.setBlock(center,Blocks.SPRUCE_PLANKS.defaultBlockState(),2);
        BobinskyVisit.cancel(level.getServer(),v,null);h.assertFalse(v.getBoolean("Active"),"Cancellation retained active house");h.assertTrue(level.getBlockState(center).is(Blocks.GRAVEL),"Cancelled house damaged terrain");h.assertTrue(v.getLong("RetryAt")>level.getGameTime(),"Cancellation did not schedule retry");level.setBlock(center,Blocks.AIR.defaultBlockState(),2);h.succeed();
    }
    @GameTest(template="empty") public static void protectedHouseAndOutsideAreDistinguished(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(0,100,0));var owner=UUID.randomUUID();var v=new CompoundTag();v.putUUID("Owner",owner);v.putLong("House",center.asLong());v.put("Originals",new ListTag());v.putBoolean("Active",true);var data=BobinskyVisitData.get(h.getLevel().getServer());data.visits.put(owner,v);
        h.assertTrue(BobinskyHouseProtection.house(h.getLevel(),center)!=null,"House mining not protected");h.assertTrue(BobinskyHouseProtection.house(h.getLevel(),center.offset(12,0,0))==null,"Protection leaked beyond house");data.visits.remove(owner);h.succeed();
    }
    @GameTest(template="empty") public static void elevenBobinskyLinesAndInvulnerableActorAreRegistered(GameTestHelper h){
        StringBuilder lines=new StringBuilder();for(int phase=0;phase<=14;phase++){String line=BobinskyVisit.line(phase);if(!line.isEmpty()){lines.append(line);h.assertTrue(VisitAudio.length("bobinsky_visit_"+line)>0,"Missing Bobinsky recording");}}h.assertTrue(lines.toString().equals("abcdefghijk"),"Bobinsky dialogue order changed");
        var actor=com.coraline.registry.ModEntities.BOBINSKY_VISITOR.get().create(h.getLevel());h.assertFalse(actor.hurt(h.getLevel().damageSources().generic(),10000),"Bobinsky can die during visit");actor.kill();h.assertTrue(actor.isAlive()&&actor.characterId().equals("bobinsky"),"Wrong model or kill protection");actor.discard();h.succeed();
    }
}
