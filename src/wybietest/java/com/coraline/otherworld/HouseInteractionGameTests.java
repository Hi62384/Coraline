package com.coraline.otherworld;

import com.coraline.feast.*;
import com.coraline.secrets.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class HouseInteractionGameTests {
    @GameTest(template="empty",batch="storage-visuals",timeoutTicks=160)
    public static void drawersAndCabinetsExposeRealContentsAndClose(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(OtherWorld.KEY);var house=new BlockPos(19000,90,19000);var realm=new RealmData.Realm();realm.door=UUID.randomUUID();realm.ready=true;realm.house=house;
        var door=new DoorData.Door();door.id=realm.door;RosewoodHouse.build(level,house,door,realm);RealmData.get(level.getServer()).realms.put(realm.door,realm);
        var p=player(level);var scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=realm.door;scene.table=realm.table;scene.piano=realm.piano;scene.stage=VisitData.Stage.DONE;scene.loop.phase=OtherMotherLoopState.Phase.EXPLORING;scene.loop.secrets.visit=1;VisitData.get(p.server).scenes.put(p.getUUID(),scene);p.getPersistentData().putUUID("CoralineVisitDoor",realm.door);
        HouseCabinets.prepare(p);var cabinetTags=scene.loop.secrets.world.getCompound("HouseCabinets").getList("Cabinets",10);var cupboard=BlockPos.of(cabinetTags.getCompound(0).getLong("Pos"));var drawer=BlockPos.of(cabinetTags.getCompound(2).getLong("Pos"));var chunks=new HashSet<net.minecraft.world.level.ChunkPos>();
        for(var pos:List.of(cupboard,drawer)){var chunk=new net.minecraft.world.level.ChunkPos(pos);for(int x=chunk.x-1;x<=chunk.x+1;x++)for(int z=chunk.z-1;z<=chunk.z+1;z++){var c=new net.minecraft.world.level.ChunkPos(x,z);chunks.add(c);level.setChunkForced(x,z,true);level.getChunk(x,z);}}
        h.runAtTickTime(30,()->{p.setPos(Vec3.atBottomCenterOf(cupboard.south()));HouseCabinets.interact(p,cupboard);});
        h.runAtTickTime(35,()->{
            var displayed=level.getEntitiesOfClass(net.minecraft.world.entity.Display.ItemDisplay.class,new net.minecraft.world.phys.AABB(cupboard).inflate(2),e->e.getPersistentData().contains("CabinetContents"));
            h.assertTrue(displayed.size()>=3&&displayed.stream().anyMatch(e->ItemStack.of(e.saveWithoutId(new CompoundTag()).getCompound("item")).is(SecretsItems.KEY.get())),"Open cupboard doesn't visibly contain its real key/loot/secret");
            for(var item:displayed)h.assertTrue(Math.abs(item.saveWithoutId(new CompoundTag()).getCompound("transformation").getList("scale",5).getFloat(0)-.22F)<.0001,"Visible contents didn't retain their compact scale");
            int keySlot=-1;for(int slot=0;slot<27;slot++)if(p.containerMenu.getSlot(slot).getItem().is(SecretsItems.KEY.get()))keySlot=slot;
            p.containerMenu.clicked(keySlot,0,ClickType.PICKUP,p);p.getInventory().setItem(0,p.containerMenu.getCarried().copy());p.containerMenu.setCarried(ItemStack.EMPTY);
        });
        h.runAtTickTime(40,()->{var displayed=level.getEntitiesOfClass(net.minecraft.world.entity.Display.ItemDisplay.class,new net.minecraft.world.phys.AABB(cupboard).inflate(2),e->e.getPersistentData().contains("CabinetContents"));h.assertFalse(displayed.stream().anyMatch(e->ItemStack.of(e.saveWithoutId(new CompoundTag()).getCompound("item")).is(SecretsItems.KEY.get())),"Taken key remains visually duplicated");p.closeContainer();HouseCabinets.interact(p,cupboard);p.setPos(Vec3.atBottomCenterOf(drawer.south()));HouseCabinets.interact(p,drawer);});
        h.runAtTickTime(45,()->{h.assertTrue(level.getBlockState(drawer).getBlock() instanceof HouseStorageBlock block&&block.drawer&&level.getBlockState(drawer).getValue(HouseStorageBlock.OPEN),"Drawer doesn't physically open as a distinct drawer block");h.assertTrue(!level.getEntitiesOfClass(net.minecraft.world.entity.Display.ItemDisplay.class,new net.minecraft.world.phys.AABB(drawer).inflate(2),e->e.getPersistentData().contains("CabinetContents")).isEmpty(),"Drawer contents invisible");HouseCabinets.interact(p,drawer);});
        h.runAtTickTime(50,()->{for(var pos:List.of(cupboard,drawer))h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.Display.ItemDisplay.class,new net.minecraft.world.phys.AABB(pos).inflate(2),e->e.getPersistentData().contains("CabinetContents")).isEmpty(),"Closed fixture retained visible items");VisitData.get(p.server).scenes.remove(p.getUUID());RealmData.get(p.server).realms.remove(realm.door);for(var chunk:chunks)level.setChunkForced(chunk.x,chunk.z,false);h.succeed();});
    }
    @GameTest(template="empty",batch="house-interactions",timeoutTicks=400)
    public static void cabinetsKeysJournalAndStairway(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(OtherWorld.KEY);var house=new BlockPos(12000,90,12000);var realm=new RealmData.Realm();realm.door=UUID.randomUUID();realm.ready=true;realm.house=house;
        var door=new DoorData.Door();door.id=realm.door;RosewoodHouse.build(level,house,door,realm);RealmData.get(level.getServer()).realms.put(realm.door,realm);
        var p=player(level);var scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=realm.door;scene.table=realm.table;scene.piano=realm.piano;scene.stage=VisitData.Stage.DONE;scene.loop.phase=OtherMotherLoopState.Phase.EXPLORING;scene.loop.visits=1;scene.loop.secrets.visit=1;VisitData.get(p.server).scenes.put(p.getUUID(),scene);p.getPersistentData().putUUID("CoralineVisitDoor",realm.door);
        HouseCabinets.prepare(p);var ledger=scene.loop.secrets.world.getCompound("HouseCabinets");var cabinets=ledger.getList("Cabinets",10);var keys=ledger.getCompound("Keys");
        for(int id=0;id<HouseCabinets.CABINETS.length;id++){var pos=BlockPos.of(cabinets.getCompound(id).getLong("Pos"));h.assertTrue(level.getBlockState(pos).is(HouseCabinets.storageBlock(id))&&level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP),"Cabinet is floating or embedded in a wall: "+id);if(HouseCabinets.CABINETS[id].room().isEmpty())h.assertFalse(HouseCabinets.forbidden(scene.loop.secrets,pos),"Public key source moved into a locked room");}
        h.assertTrue(keys.getCompound("storage").getInt("Source")==0&&keys.getCompound("attic").getInt("Source")==4&&keys.getCompound("basement").getInt("Source")==5&&keys.getCompound("archive").getInt("Source")==6,"Key route has a cycle or wrong source");
        for(int id:new int[]{4,5,6,7}){var pos=BlockPos.of(cabinets.getCompound(id).getLong("Pos"));String room=HouseCabinets.CABINETS[id].room();var definition=Arrays.stream(HouseCabinets.ROOMS).filter(r->r.id().equals(room)).findFirst().orElseThrow();h.assertTrue(HouseCabinets.contains(house,definition,pos),"Key cabinet migrated outside its intended room: "+room);}
        var cabinet=BlockPos.of(cabinets.getCompound(0).getLong("Pos"));p.setPos(Vec3.atBottomCenterOf(cabinet.south()));
        h.assertTrue(HouseCabinets.interact(p,cabinet)&&cabinets.getCompound(0).getBoolean("Open"),"Cabinet failed to open");
        h.assertTrue(level.getBlockState(cabinet).getValue(HouseStorageBlock.OPEN),"Physical cabinet didn't open");
        int keySlot=-1;for(int i=0;i<27;i++)if(p.containerMenu.getSlot(i).getItem().is(SecretsItems.KEY.get()))keySlot=i;
        h.assertTrue(keySlot>=0,"Starting key is inaccessible");p.containerMenu.clicked(keySlot,0,ClickType.PICKUP,p);var borrowed=p.containerMenu.getCarried().copy();p.containerMenu.setCarried(ItemStack.EMPTY);p.getInventory().setItem(0,borrowed);
        h.assertTrue(keys.getCompound("storage").getBoolean("Missing"),"Taking the key didn't update missing state");
        h.assertTrue(HouseCabinets.canReturn(p,0,borrowed)&&!HouseCabinets.canReturn(p,1,borrowed),"Key can be returned to the wrong cabinet");
        p.closeContainer();HouseCabinets.interact(p,cabinet);h.assertFalse(cabinets.getCompound(0).getBoolean("Open"),"Cabinet failed to close");
        var storage=HouseCabinets.at(house,36,15,53);p.setPos(Vec3.atBottomCenterOf(storage.west()));HouseCabinets.interact(p,storage);
        h.assertFalse(level.getBlockState(storage).getValue(DoorBlock.OPEN),"Key bypassed the timed unlocking interaction");
        p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,storage.getCenter().add(0,.3,0));
        // A synchronous fixture has not run Entity.baseTick after teleport/look.
        // The real held-key ray uses partial tick zero (previous pose).
        p.setOldPosAndRot();
        var keyHit=p.pick(4,0,false);
        h.assertTrue(keyHit instanceof net.minecraft.world.phys.BlockHitResult hit&&(hit.getBlockPos().equals(storage)||hit.getBlockPos().equals(storage.above())),"Held-key fixture isn't actually looking at its locked door: "+keyHit);
        for(int remaining=48;remaining>=1;remaining--)HouseCabinets.keyTick(p,p.getMainHandItem(),remaining);
        HouseCabinets.finishUnlock(p,p.getMainHandItem());p.stopUsingItem();
        h.assertTrue(level.getBlockState(storage).getValue(DoorBlock.OPEN),"Matching room key didn't open its door");
        p.setPos(Vec3.atBottomCenterOf(cabinet.south()));HouseCabinets.interact(p,cabinet);h.assertTrue(p.containerMenu.slots.stream().anyMatch(slot->"key_drawer".equals(SecretTransactions.evidence(p,slot.getItem()))),"Untaken cabinet secret disappeared when reopened");p.getInventory().setItem(0,ItemStack.EMPTY);p.containerMenu.setCarried(borrowed);p.containerMenu.clicked(26,0,ClickType.PICKUP,p);
        h.assertFalse(keys.getCompound("storage").getBoolean("Missing"),"Returning the key didn't restore its source");p.closeContainer();
        h.assertFalse(level.getBlockState(storage).getValue(DoorBlock.OPEN),"Returned key left its room unlocked");
        p.setPos(Vec3.atBottomCenterOf(storage.west()));HouseCabinets.interact(p,storage);h.assertFalse(level.getBlockState(storage).getValue(DoorBlock.OPEN),"Closed room opened without its key");
        p.setPos(Vec3.atBottomCenterOf(HouseCabinets.at(house,37,15,53)));HouseCabinets.interact(p,storage);h.assertTrue(level.getBlockState(storage).getValue(DoorBlock.OPEN),"Relocking trapped someone inside a room");
        p.setPos(Vec3.atBottomCenterOf(cabinet.south()));HouseCabinets.interact(p,cabinet);HouseCabinets.interact(p,cabinet);p.containerMenu.clicked(keySlot,0,ClickType.PICKUP,p);p.getInventory().setItem(0,p.containerMenu.getCarried().copy());p.containerMenu.setCarried(ItemStack.EMPTY);p.closeContainer();
        cabinets.getCompound(0).putLong("CheckAt",OtherMotherLoop.now(p));p.tickCount=20;float beforeCheck=scene.loop.suspicion;HouseCabinets.tick(p);
        h.assertTrue(scene.loop.suspicion>beforeCheck&&!HouseCabinets.keyConfrontation(p),"Missing-key inspection scanned the hidden inventory instead of just noticing the cabinet loss");
        HouseCabinets.confronted(p);SecretTransactions.confiscate(p);h.assertTrue(!keys.getCompound("storage").getBoolean("Missing")&&keys.getCompound("storage").getInt("Source")!=0&&!p.getInventory().contains(new ItemStack(SecretsItems.KEY.get())),"Confiscated key wasn't moved to a new source");scene.loop.secrets.search=SecretsState.Search.NORMAL;
        int movedSource=keys.getCompound("storage").getInt("Source");var movedCabinet=BlockPos.of(cabinets.getCompound(movedSource).getLong("Pos"));p.setPos(Vec3.atBottomCenterOf(movedCabinet.south()));HouseCabinets.interact(p,movedCabinet);
        int movedSlot=-1;for(int i=0;i<27;i++)if(p.containerMenu.getSlot(i).getItem().is(SecretsItems.KEY.get())&&"storage".equals(p.containerMenu.getSlot(i).getItem().getTag().getString("HouseKey")))movedSlot=i;
        h.assertTrue(movedSlot>=0,"Relocated key wasn't available at its new source");p.containerMenu.clicked(movedSlot,0,ClickType.PICKUP,p);SecretTransactions.confiscate(p);
        h.assertTrue(p.containerMenu.getCarried().isEmpty()&&keys.getCompound("storage").getInt("Source")!=movedSource&&!keys.getCompound("storage").getBoolean("Missing"),"Key held on the menu cursor escaped confiscation");
        HouseCabinets.relocate(p,"attic");h.assertTrue(Set.of(0,1,2,3,8).contains(keys.getCompound("attic").getInt("Source")),"Relocated key became inaccessible");
        float low=0,high=1;scene.loop.suspicion=low;long slow=HouseCabinets.checkInterval(p);scene.loop.suspicion=high;h.assertTrue(HouseCabinets.checkInterval(p)<slow,"Suspicion doesn't shorten cabinet checks");scene.loop.suspicion=.2F;
        var spots=new HashSet<BlockPos>();for(int i=0;i<SecretWorld.HIDING.length;i++){var spot=SecretWorld.hiding(p,i);h.assertTrue(spot!=null&&spots.add(spot),"Missing/duplicate floor hiding spot");var display=SecretWorld.journalDisplay(p,spot);var ground=SecretWorld.journalGround(p,spot);h.assertTrue(Math.abs(display.y-7.0/16-ground.y)<.0001,"Journal model floats above ground");}
        h.assertTrue(spots.size()==12,"Journal hiding spot expansion lost");
        for(int x=23;x<=25;x++)for(int z=27;z<=30;z++){int y=11+z-27;for(int head=1;head<=3;head++)h.assertTrue(level.isEmptyBlock(RosewoodHouse.at(house,x,y+head,z)),"Interior stairway headroom blocked");}
        var saved=SecretsState.load(scene.loop.secrets.save());h.assertTrue(saved.world.getCompound("HouseCabinets").getCompound("Keys").getCompound("attic").getInt("Source")==keys.getCompound("attic").getInt("Source"),"Relocated key source lost on save");
        VisitData.get(p.server).scenes.remove(p.getUUID());RealmData.get(p.server).realms.remove(realm.door);h.succeed();
    }
    @GameTest(template="empty",batch="house-entry",timeoutTicks=200)
    public static void entryTrustIsDeferredConsumedOnceAndCapped(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(OtherWorld.KEY);var p=player(level);var realm=new RealmData.Realm();realm.door=UUID.randomUUID();
        OtherMotherLoop.rewardEntry(p,realm);h.assertTrue(p.getPersistentData().getInt("CoralineEntryTrustPoints")==8,"First entry bonus lost before scene exists");
        var scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=realm.door;scene.loop.trust=.5F;scene.loop.suspicion=.3F;VisitData.get(p.server).scenes.put(p.getUUID(),scene);
        OtherMotherLoop.applyEntryTrust(p);OtherMotherLoop.applyEntryTrust(p);h.assertTrue(Math.abs(scene.loop.trust-.58F)<.0001&&scene.loop.suspicion==.3F,"Deferred bonus duplicated or changed suspicion");
        p.getPersistentData().putBoolean("CoralineReturning",true);OtherMotherLoop.rewardEntry(p,realm);h.assertTrue(Math.abs(scene.loop.trust-.58F)<.0001,"Return trip awarded trust");p.getPersistentData().remove("CoralineReturning");
        scene.loop.trust=.98F;OtherMotherLoop.rewardEntry(p,realm);h.assertTrue(scene.loop.trust==1,"Entry trust exceeds 100");VisitData.get(p.server).scenes.remove(p.getUUID());h.succeed();
    }
    private static ServerPlayer player(net.minecraft.server.level.ServerLevel level){var p=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"HouseQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);return p;}
    @GameTest(template="empty",batch="house-return",timeoutTicks=200)
    public static void returnTunnelStaysOutboundAndReachesHome(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(OtherWorld.KEY);var p=player(level);var door=new DoorData.Door();door.owner=p.getUUID();door.pos=h.absolutePos(new BlockPos(10,2,10));door.dimension="minecraft:overworld";DoorData.get(p.server).doors.put(door.id,door);
        var realm=new RealmData.Realm();realm.door=door.id;realm.sourceDoor=door.pos;realm.doorPos=new BlockPos(16000,90,16000);realm.ready=true;RealmData.get(p.server).realms.put(door.id,realm);
        OtherWorld.startTunnel(p,door,true);var tunnel=realm.tunnel();OtherWorld.playerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
        h.assertTrue(p.getPersistentData().getBoolean("CoralineInTunnel")&&p.getPersistentData().getBoolean("CoralineReturning"),"Returning was mistaken for another house entry");
        var normal=tunnel.offset(0,0,-1);var state=level.getBlockState(normal);level.setBlock(normal,state.setValue(com.coraline.block.TinyDoorBlock.OPEN,true),3);p.getPersistentData().putLong("CoralinePortalCooldown",0);p.setPos(tunnel.getX()+.5,tunnel.getY(),tunnel.getZ()-.4);
        OtherWorld.playerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
        h.assertTrue(p.serverLevel()==h.getLevel()&&!p.getPersistentData().getBoolean("CoralineInTunnel"),"Home exit reset player before allowing departure");
        RealmData.get(p.server).realms.remove(door.id);DoorData.get(p.server).doors.remove(door.id);h.succeed();
    }
}
