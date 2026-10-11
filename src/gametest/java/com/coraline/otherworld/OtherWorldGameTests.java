package com.coraline.otherworld;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import java.util.UUID;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class OtherWorldGameTests {
    @GameTest(template="empty") public static void firstMouseIntroSchedulesOncePerFamily(GameTestHelper h){
        var p=new net.minecraftforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"MouseIntro"));
        var family=new com.coraline.parents.FamilyData.Family();family.owner=p.getUUID();family.mom=UUID.randomUUID();family.dad=UUID.randomUUID();
        com.coraline.parents.FamilyData.get(p.server).families.put(family.id,family);
        var door=new DoorData.Door();door.owner=p.getUUID();
        long now=p.server.overworld().getGameTime();SceneAudio.begin(p,door);
        var saved=SceneAudio.get(p.server).save(new CompoundTag());
        h.assertTrue(saved.getBoolean("Intro_"+family.id),"Intro was not tied to parent pair");
        long due=saved.getCompound("Pending_"+p.getUUID()).getLong("Due");
        h.assertTrue(due-now>=565,"Mice were scheduled before recording plus five seconds finished");
        SceneAudio.begin(p,door);
        h.assertTrue(SceneAudio.get(p.server).save(new CompoundTag()).getCompound("Pending_"+p.getUUID()).getLong("Due")==due,"Repeated trigger restarted intro");
        SceneAudio.cancelPending(p);h.succeed();
    }
    @GameTest(template="empty") public static void visualUpdateKeepsRealmReadOnlyAndMobFree(GameTestHelper h){
        var source=h.getLevel();var target=source.getServer().getLevel(OtherWorld.KEY);var pos=new BlockPos(0,100,0);
        var player=new net.minecraftforge.common.util.FakePlayer(target,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"VisualProtection"));
        var breaking=new net.minecraftforge.event.level.BlockEvent.BreakEvent(target,pos,Blocks.STONE.defaultBlockState(),player);RealmProtection.breakBlock(breaking);
        h.assertTrue(breaking.isCanceled(),"Other World break protection removed");
        var placing=new net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent(net.minecraftforge.common.util.BlockSnapshot.create(target.dimension(),target,pos),Blocks.STONE.defaultBlockState(),player);RealmProtection.place(placing);
        h.assertTrue(placing.isCanceled(),"Other World place protection removed");
        var originalBreak=new net.minecraftforge.event.level.BlockEvent.BreakEvent(source,pos,Blocks.STONE.defaultBlockState(),player);RealmProtection.breakBlock(originalBreak);
        h.assertFalse(originalBreak.isCanceled(),"Other World protection leaked into Overworld");
        for(var type:new net.minecraft.world.entity.EntityType<?>[]{net.minecraft.world.entity.EntityType.COW,net.minecraft.world.entity.EntityType.ZOMBIE,net.minecraft.world.entity.EntityType.BAT}){
            var mob=type.create(target);h.assertFalse(target.addFreshEntity(mob),"Normal mob entered visual dimension: "+type);
        }
        var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("coraline","perfected_garden"));
        var biome=target.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(key).value();
        for(var category:net.minecraft.world.entity.MobCategory.values())h.assertTrue(biome.getMobSettings().getMobs(category).isEmpty(),"Visual biome added spawn entries");
        player.discard();h.succeed();
    }
    @GameTest(template="empty") public static void parentDeathRevokesPlacedAndStoredDoor(GameTestHelper h){
        var data=DoorData.get(h.getLevel().getServer());var owner=UUID.randomUUID();var item=data.gift(owner);var d=data.doors.get(DoorData.id(item));
        var pos=h.absolutePos(new BlockPos(4,1,4));h.getLevel().setBlock(pos,com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState(),3);
        ((DoorBlockEntity)h.getLevel().getBlockEntity(pos)).doorId=d.id;d.placed=true;d.pos=pos;d.dimension=h.getLevel().dimension().location().toString();
        var box=new ItemStack(Items.SHULKER_BOX);var list=new net.minecraft.nbt.ListTag();var n=item.save(new CompoundTag());n.putByte("Slot",(byte)0);list.add(n);box.getOrCreateTagElement("BlockEntityTag").put("Items",list);
        data.endFamily(owner);
        h.assertTrue(h.getLevel().isEmptyBlock(pos)&&d.revoked&&!data.hasDoor(owner),"Parent death left bound door block alive");
        h.assertTrue(data.observe(item,null)==null,"Revoked door revived");
        h.assertTrue(com.coraline.doll.DollStorage.cleanStack(box,h.getLevel()),"Nested old door not removed");
        h.assertTrue(box.is(Items.SHULKER_BOX)&&box.getTagElement("BlockEntityTag").getList("Items",10).isEmpty(),"Nested cleanup destroyed container or retained door");
        var loaded=DoorData.load(data.save(new CompoundTag()));h.assertTrue(loaded.revoked(item),"Revocation did not survive restart");
        h.assertTrue(!data.gift(owner).isEmpty(),"Retired door prevented a new door identity");h.succeed();
    }
    @GameTest(template="empty") public static void counterpartsDoNotAccumulateAcrossRefreshes(GameTestHelper h){
        var server=h.getLevel().getServer();var target=server.getLevel(OtherWorld.KEY);
        var d=new DoorData.Door();d.owner=UUID.randomUUID();d.pos=h.absolutePos(new BlockPos(8,1,8));d.dimension=h.getLevel().dimension().location().toString();
        DoorData.get(server).doors.put(d.id,d);var r=RealmData.get(server).realm(d);r.ready=true;
        var f=new com.coraline.parents.FamilyData.Family();f.owner=d.owner;f.mom=UUID.randomUUID();f.dad=UUID.randomUUID();f.momPos=d.pos;f.dadPos=d.pos;
        com.coraline.parents.FamilyData.get(server).families.put(f.id,f);
        for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)target.setBlock(r.doorPos.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
        OtherWorld.finish(server,r);
        var box=new net.minecraft.world.phys.AABB(r.doorPos).inflate(16);
        var old=target.getEntitiesOfClass(com.coraline.parents.ParentEntity.class,box,p->p.isOtherParent());
        h.assertTrue(old.size()==2,"Initial counterpart pair missing");
        var saved=new CompoundTag();old.get(0).save(saved);
        OtherWorld.finish(server,r);OtherWorld.finish(server,r);
        h.assertTrue(target.getEntitiesOfClass(com.coraline.parents.ParentEntity.class,box,p->p.isOtherParent()).size()==2,"Refresh duplicated counterpart parents");
        var stale=com.coraline.registry.ModEntities.MOM.get().create(target);stale.load(saved);
        h.assertFalse(target.addFreshEntity(stale),"Unloaded old counterpart returned after refresh");
        var reloaded=RealmData.load(RealmData.get(server).save(new CompoundTag()));
        h.assertTrue(reloaded.realms.get(r.door).parentGeneration==r.parentGeneration,"Counterpart generation not persisted");h.succeed();
    }
    @GameTest(template="empty") public static void miceSpawnOnGrassWhileOwnerFlies(GameTestHelper h){
        var level=h.getLevel();
        for(int x=1;x<24;x++)for(int z=1;z<24;z++){
            h.setBlock(x,0,z,Blocks.GRASS_BLOCK);h.setBlock(x,1,z,Blocks.GRASS);
        }
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"FlyingMouseTest"));
        var bed=h.absolutePos(new BlockPos(12,1,12));player.setRespawnPosition(level.dimension(),bed,0,true,false);
        player.setPos(bed.getX()+.5,bed.getY()+7,bed.getZ()+.5);
        var door=new DoorData.Door();door.pos=h.absolutePos(new BlockPos(12,1,8));door.owner=player.getUUID();
        level.setBlock(door.pos,com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState(),3);
        int count=OtherWorld.spawnMice(level,player,door);
        h.assertTrue(count>0,"No mice spawned on grass with flying owner");
        var mice=level.getEntitiesOfClass(MouseEntity.class,new net.minecraft.world.phys.AABB(bed).inflate(12));
        h.assertTrue(mice.stream().filter(m->door.id.equals(m.doorId)).allMatch(m->!m.chasing&&m.getNavigation().isDone()),"Mice started running before being seen");
        h.assertTrue(mice.stream().filter(m->door.id.equals(m.doorId)).allMatch(m->Math.abs(m.getY()-bed.getY())<.01),"Mice spawned at flying player height instead of ground");
        h.assertTrue(OtherWorld.debugMice(player).contains("Creative-only"),"Survival bypassed debug restriction");
        player.discard();h.succeed();
    }
    @GameTest(template="empty",batch="realm_night",timeoutTicks=200) public static void mouseActuallyRunsIntoDoorAndOpensPortal(GameTestHelper h){
        var level=h.getLevel();long time=level.getDayTime();level.setDayTime(14000);
        for(int x=1;x<16;x++)for(int z=1;z<16;z++)h.setBlock(x,0,z,Blocks.STONE);
        var d=new DoorData.Door();d.owner=UUID.randomUUID();d.placed=true;d.pos=h.absolutePos(new BlockPos(8,1,8));d.dimension=level.dimension().location().toString();DoorData.get(level.getServer()).doors.put(d.id,d);
        level.setBlock(d.pos,com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState(),3);((DoorBlockEntity)level.getBlockEntity(d.pos)).doorId=d.id;
        var mouse=h.spawn(com.coraline.registry.ModEntities.MOUSE.get(),8.5F,1,3.5F);mouse.doorId=d.id;mouse.chasing=true;
        h.onEachTick(()->{if(d.activeUntil>0){h.assertTrue(mouse.isRemoved(),"Mouse remained outside activated door");h.assertTrue(level.getBlockState(d.pos).getValue(com.coraline.block.TinyDoorBlock.PORTAL),"Mouse did not activate portal");level.setDayTime(time);h.succeed();}});
        h.runAtTickTime(180,()->{level.setDayTime(time);h.fail("Mouse could not reach the open floor's little door");});
    }
    @GameTest(template="empty") public static void onlyRealRespawnBedAllowsWakingHome(GameTestHelper h){
        var server=h.getLevel().getServer();var target=server.getLevel(OtherWorld.KEY);var source=h.getLevel();
        var p=new net.minecraftforge.common.util.FakePlayer(target,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BedVisitor")){
            @Override public void teleportTo(net.minecraft.server.level.ServerLevel dest,double x,double y,double z,float yaw,float pitch){super.teleportTo(dest,x,y,z,yaw,pitch);setPos(x,y,z);}
        };
        var d=new DoorData.Door();d.owner=p.getUUID();d.pos=h.absolutePos(new BlockPos(8,1,8));d.dimension=source.dimension().location().toString();DoorData.get(server).doors.put(d.id,d);
        var r=RealmData.get(server).realm(d);r.ready=true;var bed=h.absolutePos(new BlockPos(5,1,5));
        var state=Blocks.RED_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.HEAD);
        source.setBlock(bed,state,2);p.setRespawnPosition(source.dimension(),bed,0,true,false);r.bed=bed.offset(r.dx,0,r.dz);target.setBlock(r.bed,state,2);target.setBlock(r.bed.offset(4,0,0),state,2);
        p.getPersistentData().putUUID("CoralineVisitDoor",d.id);p.setPos(r.bed.getX()+.5,r.bed.getY(),r.bed.getZ()+.5);
        java.util.function.Function<BlockPos,net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock> click=pos->new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p,net.minecraft.world.InteractionHand.MAIN_HAND,pos,new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),net.minecraft.core.Direction.UP,pos,false));
        RealmProtection.useBlock(click.apply(r.bed.offset(4,0,0)));h.assertFalse(p.isSleeping(),"Wrong copied bed allowed sleeping");
        RealmProtection.useBlock(click.apply(r.bed));h.assertTrue(p.isSleeping(),"Matching respawn bed rejected");
        p.getPersistentData().putLong("CoralineWakeAt",server.overworld().getGameTime());OtherWorld.playerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
        h.assertTrue(p.level()==source&&!p.isSleeping()&&bed.equals(p.getRespawnPosition()),"Wake did not return home with original respawn intact");p.discard();h.succeed();
    }
    @GameTest(template="empty",batch="realm_scale",timeoutTicks=6000) public static void full224BlockRadiusSnapshot(GameTestHelper h){
        var server=h.getLevel().getServer();var target=server.getLevel(OtherWorld.KEY);
        var door=new DoorData.Door();door.owner=UUID.randomUUID();door.pos=h.absolutePos(new BlockPos(8,1,8));
        var realm=RealmData.get(server).realm(door);realm.radius=224;
        var probe=door.pos.offset(200,0,0);h.getLevel().getChunkAt(probe);
        var far=h.getLevel().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,probe);
        if(far.getY()>=h.getLevel().getMaxBuildHeight())far=far.atY(h.getLevel().getMaxBuildHeight()-1);
        h.getLevel().setBlock(far,Blocks.EMERALD_BLOCK.defaultBlockState(),3);
        final var surfaceMarker=far;
        h.assertTrue(h.getLevel().getBlockState(surfaceMarker).is(Blocks.EMERALD_BLOCK),"Source surface marker could not be placed");
        var job=new RealmCopy(h.getLevel(),target,door,realm);long started=System.nanoTime();
        h.onEachTick(()->{if(job.tick()){
            h.assertFalse(job.failed,"Full-size snapshot failed");
            h.assertTrue(target.getBlockState(surfaceMarker.offset(realm.dx,0,realm.dz)).is(Blocks.EMERALD_BLOCK),"200-block distant terrain not copied: source="+h.getLevel().getBlockState(surfaceMarker)+" pos="+surfaceMarker+" radius="+realm.radius);
            com.mojang.logging.LogUtils.getLogger().info("CORALINE_SCALE_CHECK: 224-block radius completed in {} ms",(System.nanoTime()-started)/1_000_000);h.succeed();
        }});
    }
    @GameTest(template="empty") public static void storedDoorDeletionIsDetectedWithoutDuplicatingTransfers(GameTestHelper h){
        var data=DoorData.get(h.getLevel().getServer());var owner=UUID.randomUUID();var stack=data.gift(owner);var d=data.doors.get(DoorData.id(stack));
        var pos=h.absolutePos(new BlockPos(4,1,4));h.getLevel().setBlock(pos,Blocks.CHEST.defaultBlockState(),3);
        var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(pos);chest.setItem(0,stack);
        data.track(stack,h.getLevel(),"container",pos,null);data.audit(d);h.assertTrue(d.exists,"Stored door was falsely lost");
        var loaded=DoorData.load(data.save(new CompoundTag()));h.assertTrue(loaded.doors.get(d.id).holderPos.equals(pos),"Storage location did not persist");
        chest.setItem(0,ItemStack.EMPTY);d.missingSince=h.getLevel().getServer().overworld().getGameTime()-1201;data.audit(d);
        h.assertFalse(d.exists,"Deleted container item remained alive forever");h.succeed();
    }
    @GameTest(template="empty") public static void replicaBlocksAreProtected(GameTestHelper h){
        var target=h.getLevel().getServer().getLevel(OtherWorld.KEY);var p=new net.minecraftforge.common.util.FakePlayer(target,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ProtectedVisitor"));var pos=new BlockPos(0,90,0);
        var breaking=new net.minecraftforge.event.level.BlockEvent.BreakEvent(target,pos,Blocks.STONE.defaultBlockState(),p);RealmProtection.breakBlock(breaking);h.assertTrue(breaking.isCanceled(),"Replica block break was allowed");
        var placing=new net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent(net.minecraftforge.common.util.BlockSnapshot.create(target.dimension(),target,pos),Blocks.AIR.defaultBlockState(),p);RealmProtection.place(placing);h.assertTrue(placing.isCanceled(),"Replica block placement was allowed");h.succeed();
    }
    @GameTest(template="empty") public static void daylightClosesEntryButVisitorsCanReturn(GameTestHelper h){
        var server=h.getLevel().getServer();var level=h.getLevel();long time=level.getDayTime();
        var d=new DoorData.Door();d.owner=UUID.randomUUID();d.pos=h.absolutePos(new BlockPos(8,1,8));d.dimension=level.dimension().location().toString();d.placed=true;DoorData.get(server).doors.put(d.id,d);
        level.setBlock(d.pos,com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState(),3);((DoorBlockEntity)level.getBlockEntity(d.pos)).doorId=d.id;
        var r=RealmData.get(server).realm(d);r.ready=true;var target=server.getLevel(OtherWorld.KEY);
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)target.setBlock(r.doorPos.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
        target.setBlock(r.doorPos,com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState(),3);((DoorBlockEntity)target.getBlockEntity(r.doorPos)).doorId=d.id;
        OtherWorld.finish(server,r);
        var visitor=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"RealmVisitor")){
            @Override public void teleportTo(net.minecraft.server.level.ServerLevel dest,double x,double y,double z,float yaw,float pitch){super.teleportTo(dest,x,y,z,yaw,pitch);setPos(x,y,z);}
        };
        try{
            level.setDayTime(14000);OtherWorld.arrived(level,d);h.assertTrue(OtherWorld.active(level,d),"Mice did not open night portal");
            r.ready=false;OtherWorld.startTunnel(visitor,d,false);
            h.assertTrue(OtherWorld.isOther(visitor.level())&&visitor.getPersistentData().getBoolean("CoralineInTunnel"),"Should enter safe tunnel before replica is ready");
            h.assertTrue(OtherWorld.lockTunnelDoor(visitor,r.tunnel().offset(0,0,17)),"Incomplete destination door must stay locked");r.ready=true;
            h.assertTrue(!OtherWorld.lockTunnelDoor(visitor,r.tunnel().offset(0,0,17)),"Completed destination door must unlock");
            OtherWorld.startTunnel(visitor,d,false);h.assertTrue(OtherWorld.isOther(visitor.level())&&visitor.getPersistentData().getBoolean("CoralineInTunnel"),"Visitor could not enter tunnel");
            visitor.setPos(r.tunnel().getX()+.5,-80,r.tunnel().getZ()+8);
            OtherWorld.playerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,visitor));
            h.assertTrue(visitor.getY()==r.tunnel().getY()&&visitor.fallDistance==0,"Tunnel fall did not recover safely");
            var farDoor=r.tunnel().offset(0,0,17);
            visitor.setPos(r.tunnel().getX()+.5,r.tunnel().getY(),r.tunnel().getZ()+17.4);OtherWorld.playerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,visitor));
            h.assertTrue(visitor.getPersistentData().getBoolean("CoralineInTunnel"),"Closed far door teleported visitor");
            target.setBlock(farDoor,target.getBlockState(farDoor).setValue(com.coraline.block.TinyDoorBlock.OPEN,true),3);
            OtherWorld.playerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,visitor));
            h.assertFalse(visitor.getPersistentData().getBoolean("CoralineInTunnel"),"Tunnel did not reach island");
            level.setDayTime(1000);h.assertFalse(OtherWorld.active(level,d),"Daytime still allowed entry");
            OtherWorld.startTunnel(visitor,d,true);
            visitor.setPos(r.tunnel().getX()+.5,r.tunnel().getY(),r.tunnel().getZ()+8);
            OtherWorld.playerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,visitor));
            h.assertTrue(visitor.level()==target&&visitor.getPersistentData().getBoolean("CoralineInTunnel"),"Return skipped the crawl tunnel");
            visitor.setPos(r.tunnel().getX()+.5,r.tunnel().getY(),r.tunnel().getZ()+.5);OtherWorld.playerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,visitor));
            h.assertTrue(visitor.level()==target,"Approaching closed normal-world door exited early");
            visitor.getPersistentData().putLong("CoralinePortalCooldown",0);
            var nearDoor=r.tunnel().offset(0,0,-1);target.setBlock(nearDoor,target.getBlockState(nearDoor).setValue(com.coraline.block.TinyDoorBlock.OPEN,true),3);
            OtherWorld.playerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,visitor));
            h.assertTrue(visitor.level()==target,"Open normal-world door exited before threshold");
            visitor.setPos(r.tunnel().getX()+.5,r.tunnel().getY(),r.tunnel().getZ()+.1);OtherWorld.playerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,visitor));
            h.assertTrue(visitor.level()==level&&!visitor.getPersistentData().hasUUID("CoralineVisitDoor"),"Daytime trapped visitor in other world");
            h.assertFalse(level.getBlockState(d.pos).getValue(com.coraline.block.TinyDoorBlock.PORTAL),"Return did not restore bricks");
        }finally{level.setDayTime(time);visitor.discard();}
        h.succeed();
    }
    @GameTest(template="empty") public static void oneDoorSurvivesStorageAndRestart(GameTestHelper h){
        var data=DoorData.get(h.getLevel().getServer());var owner=UUID.randomUUID();var stack=data.gift(owner);var id=DoorData.id(stack);
        h.assertTrue(!stack.isEmpty()&&data.gift(owner).isEmpty(),"Doll issued duplicate door");
        var loaded=DoorData.load(data.save(new CompoundTag()));h.assertTrue(loaded.hasDoor(owner)&&loaded.gift(owner).isEmpty(),"Restart forgot existing door");
        var entity=new DoorItemEntity(com.coraline.registry.ModEntities.TRACKED_DOOR.get(),h.getLevel());entity.setItem(stack.copy());entity.remove(net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK);
        h.assertTrue(data.hasDoor(owner),"Chunk unload lost door identity");
        var doomed=new DoorItemEntity(com.coraline.registry.ModEntities.TRACKED_DOOR.get(),h.getLevel());doomed.setItem(stack.copy());doomed.hurt(h.getLevel().damageSources().generic(),100);
        h.assertFalse(data.doors.get(id).exists,"Destroyed item still considered alive");
        data.doors.get(id).absentUntil=0;h.assertTrue(!data.gift(owner).isEmpty(),"Destroyed door blocked replacement forever");h.succeed();
    }
    @GameTest(template="empty") public static void placedDoorKeepsIdentityWhenBroken(GameTestHelper h){
        var player=h.makeMockSurvivalPlayer();player.setUUID(UUID.randomUUID());var data=DoorData.get(h.getLevel().getServer());var stack=data.gift(player.getUUID());
        var pos=h.absolutePos(new BlockPos(5,1,5));var block=(com.coraline.block.TinyDoorBlock)com.coraline.registry.ModBlocks.TINY_DOOR.get();h.getLevel().setBlock(pos,block.defaultBlockState(),3);
        block.setPlacedBy(h.getLevel(),pos,h.getLevel().getBlockState(pos),player,stack);
        var id=DoorData.id(stack);h.assertTrue(data.doors.get(id).placed&&((DoorBlockEntity)h.getLevel().getBlockEntity(pos)).doorId.equals(id),"Placed identity missing");
        var drops=net.minecraft.world.level.block.Block.getDrops(h.getLevel().getBlockState(pos),h.getLevel(),pos,h.getLevel().getBlockEntity(pos));
        h.assertTrue(drops.size()==1&&id.equals(DoorData.id(drops.get(0))),"Breaking changed door identity");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=1200) public static void snapshotCopiesAndRefreshesHouseWithoutLoot(GameTestHelper h){
        var server=h.getLevel().getServer();var target=server.getLevel(OtherWorld.KEY);h.assertTrue(target!=null,"Other dimension did not load");
        var door=new DoorData.Door();door.owner=UUID.randomUUID();door.pos=h.absolutePos(new BlockPos(8,65,8));door.dimension=h.getLevel().dimension().location().toString();
        var realm=RealmData.get(server).realm(door);realm.radius=16;
        var rim=door.pos.offset(12,0,0);h.getLevel().setBlock(rim,Blocks.GOLD_BLOCK.defaultBlockState(),3);
        var source=h.absolutePos(new BlockPos(10,65,10));h.getLevel().setBlock(source,Blocks.CHEST.defaultBlockState(),3);
        var dollPos=h.absolutePos(new BlockPos(11,65,10));h.getLevel().setBlock(dollPos,com.coraline.registry.ModBlocks.DOLL.get().defaultBlockState(),3);
        var caveFloor=source.below(20);var buried=source.below(45);
        h.getLevel().setBlock(caveFloor,Blocks.EMERALD_BLOCK.defaultBlockState(),3);h.getLevel().setBlock(buried,Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
        ((net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(source)).setItem(0,new ItemStack(Items.DIAMOND,12));
        var destination=source.offset(realm.dx,0,realm.dz);
        var sourceBiome=h.getLevel().getBiome(source);
        var signPos=source.offset(-1,0,-1);h.getLevel().setBlock(signPos.below(),Blocks.STONE.defaultBlockState(),2);h.getLevel().setBlock(signPos,Blocks.OAK_SIGN.defaultBlockState(),2);
        ((net.minecraft.world.level.block.entity.SignBlockEntity)h.getLevel().getBlockEntity(signPos)).updateText(text->text.setMessage(0,net.minecraft.network.chat.Component.literal("My unchanged home")),true);
        var machine=source.offset(-2,0,-1);var machineState=Blocks.OBSERVER.defaultBlockState().setValue(net.minecraft.world.level.block.ObserverBlock.FACING,net.minecraft.core.Direction.WEST);h.getLevel().setBlock(machine,machineState,2);
        var job=new RealmCopy[]{new RealmCopy(h.getLevel(),target,door,realm)};var stage=new int[]{0};
        h.onEachTick(()->{
            if(!job[0].tick())return;
            h.assertFalse(job[0].failed,"Chunk snapshot failed");
            if(stage[0]++==0){
                h.assertTrue(target.getBlockState(destination).is(Blocks.CHEST),"House block not copied");
                h.assertTrue(target.getBlockState(dollPos.offset(realm.dx,0,realm.dz)).isAir(),"Other World copied the doll");
                h.assertTrue(target.getBlockState(caveFloor.offset(realm.dx,0,realm.dz)).isAir(),"Fully enclosed underground voxel was copied");
                h.assertTrue(target.getBlockState(buried.offset(realm.dx,0,realm.dz)).isAir(),"Invisible deep terrain was copied");
                h.assertTrue(target.getBlockEntity(destination) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest&&chest.isEmpty(),"Snapshot duplicated storage items");
                h.assertTrue(((net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(source)).getItem(0).getCount()==12,"Visual update changed original inventory");
                h.assertTrue(h.getLevel().getBiome(source).equals(sourceBiome),"Visual palette changed source biome");
                h.assertTrue(target.getBiome(destination).is(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("coraline","perfected_garden")),"Existing copied chunks did not receive the Other World visual palette");
                h.assertTrue(target.getBlockState(machine.offset(realm.dx,0,realm.dz)).equals(machineState),"Visual update changed copied machine orientation/state");
                h.assertTrue(((net.minecraft.world.level.block.entity.SignBlockEntity)target.getBlockEntity(signPos.offset(realm.dx,0,realm.dz))).getFrontText().getMessage(0,false).getString().equals("My unchanged home"),"Visual update lost sign text");
                h.assertTrue(target.getBlockState(rim.offset(realm.dx,0,realm.dz)).isAir(),"Fully enclosed island-rim voxel was copied");
                realm.radius=8;
                h.getLevel().setBlock(source,Blocks.GOLD_BLOCK.defaultBlockState(),3);job[0]=new RealmCopy(h.getLevel(),target,door,realm);
            }else{
                h.assertTrue(target.getBlockState(destination).is(Blocks.GOLD_BLOCK),"Re-entry snapshot did not refresh source edits");
                h.assertTrue(target.getBlockState(rim.offset(realm.dx,0,realm.dz)).isAir(),"Smaller radius retained old outer terrain");
                h.assertTrue(target.getBlockState(realm.doorPos.offset(18,0,18)).isAir(),"Snapshot extended beyond circular island");h.succeed();
            }
        });
    }
}
