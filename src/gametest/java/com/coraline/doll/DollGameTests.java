package com.coraline.doll;

import com.coraline.registry.ModEntities;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import java.util.UUID;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class DollGameTests {
    @GameTest(template="empty") public static void creativeDeletionSchedulesReplacementWithoutUnbinding(GameTestHelper h){
        var p=new net.minecraftforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"CreativeRecall"));family(h,p.getUUID());p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.tickCount=20;
        var data=DollBondData.get(p.server);var original=data.claim(p);var b=data.bonds.get(p.getUUID());
        b.lastSatisfiedAt=p.server.overworld().getGameTime()-com.coraline.config.CoralineConfig.DOLL_CREATIVE_MISSING.get();
        // Empty inventory models deleting the stack through the Creative trash slot.
        DollEvents.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
        h.assertTrue(b.lost&&b.recallRequested,"Creative deletion did not schedule a replacement");
        h.assertTrue(data.bonds.containsKey(p.getUUID()),"Deleting item incorrectly removed its haunting bond");
        data.returned(b,data.replacement(b),p.server.overworld().getGameTime());
        h.assertFalse(data.current(original),"Deleted generation can be duplicated after return");h.succeed();
    }
    @GameTest(template="empty") public static void debugDollBindsOnceAndRequiresCreative(GameTestHelper h){
        var player=new net.minecraftforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DebugDollTester"));
        family(h,player.getUUID());var data=DollBondData.get(player.server);
        h.assertTrue(com.coraline.item.DollDebugItem.bind(player).contains("Creative-only"),"Survival used debug item");
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        com.coraline.item.DollDebugItem.bind(player);
        h.assertTrue(data.bonds.containsKey(player.getUUID()),"Debug did not establish real doll bond");
        h.assertTrue(player.getInventory().countItem(com.coraline.registry.ModItems.DOLL.get())==1,"Debug doll missing");
        com.coraline.item.DollDebugItem.bind(player);
        h.assertTrue(player.getInventory().countItem(com.coraline.registry.ModItems.DOLL.get())==1,"Debug duplicated doll");
        data.endFamily(player.getUUID());com.coraline.parents.ParentAccess.family(player.server,player.getUUID()).dead=true;
        h.assertTrue(com.coraline.item.DollDebugItem.bind(player).contains("parents first"),"Debug ignored missing parents");h.succeed();
    }
    @GameTest(template="empty") public static void lostRegistryRebindsFromPlayerBackup(GameTestHelper h){
        var player=player(h);var data=DollBondData.get(player.server);var old=data.claim(player);
        data.bonds.remove(player.getUUID());data.watchdog(player);
        var rebound=data.bonds.get(player.getUUID());h.assertTrue(rebound!=null&&rebound.lost&&rebound.recallRequested,"Lost record wasn't rebound for sky delivery");
        h.assertFalse(data.current(old),"Previous generation revived after emergency rebinding");
        var family=com.coraline.parents.ParentAccess.family(player.server,player.getUUID());family.dead=true;data.endFamily(player.getUUID());data.watchdog(player);
        h.assertFalse(data.bonds.containsKey(player.getUUID()),"Watchdog ignored intentional parent death");h.succeed();
    }
    @GameTest(template="empty") public static void containerRecallInvalidatesOldCopies(GameTestHelper h){
        var player=player(h);var data=DollBondData.get(player.server);var stack=data.claim(player);var stale=stack.copy();var bond=data.bonds.get(player.getUUID());
        long now=player.server.overworld().getGameTime();bond.lastSatisfiedAt=now;
        h.assertFalse(DollStorage.recallStored(stack,h.getLevel()),"Freshly stored doll vanished immediately");
        bond.lastSatisfiedAt=now-com.coraline.config.CoralineConfig.DOLL_UNSATISFIED_TICKS.get();
        h.assertTrue(DollStorage.recallStored(stack,h.getLevel())&&stack.isEmpty()&&bond.lost,"Stored doll did not vanish and schedule return");
        var replacement=data.replacement(bond);data.returned(bond,replacement,now);
        h.assertTrue(data.current(replacement)&&!data.current(stale),"Old generation remained active");
        var loaded=DollBondData.load(data.save(new CompoundTag()));h.assertFalse(loaded.current(stale),"Stale copy revived after save/load");
        var box=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHULKER_BOX);
        var contents=new net.minecraft.nbt.ListTag();var dollTag=stale.save(new CompoundTag());dollTag.putByte("Slot",(byte)0);contents.add(dollTag);
        var food=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.APPLE,5).save(new CompoundTag());food.putByte("Slot",(byte)1);contents.add(food);
        box.getOrCreateTagElement("BlockEntityTag").put("Items",contents);
        h.assertTrue(DollStorage.cleanNested(box,h.getLevel(),0),"Nested stale doll wasn't cleaned");
        var remaining=box.getTagElement("BlockEntityTag").getList("Items",10);
        h.assertTrue(remaining.size()==1&&net.minecraft.world.item.ItemStack.of(remaining.getCompound(0)).getCount()==5,"Cleanup deleted unrelated shulker contents");h.succeed();
    }
    @GameTest(template="empty") public static void bedBoundsRejectMissingFarAndWrongHeight(GameTestHelper h){
        var bed=h.absolutePos(new net.minecraft.core.BlockPos(7,1,7));
        h.getLevel().setBlock(bed,net.minecraft.world.level.block.Blocks.RED_BED.defaultBlockState(),2);
        h.assertTrue(DollHome.nearBed(h.getLevel(),bed.offset(3,0,0),bed),"Nearby doll rejected");
        h.assertFalse(DollHome.nearBed(h.getLevel(),bed.offset(13,0,0),bed),"Distant doll accepted");
        h.assertFalse(DollHome.nearBed(h.getLevel(),bed.offset(0,5,0),bed),"Wrong-floor doll accepted");
        h.getLevel().removeBlock(bed,false);
        h.assertFalse(DollHome.nearBed(h.getLevel(),bed.offset(3,0,0),bed),"Missing bed accepted");h.succeed();
    }
    @GameTest(template="empty") public static void dollCanDropTaggedDoor(GameTestHelper h){
        var player=player(h);var pos=h.absolutePos(new net.minecraft.core.BlockPos(7,1,7));player.setPos(pos.getX(),pos.getY(),pos.getZ());
        var data=DollBondData.get(player.server);data.claim(player);var bond=data.bonds.get(player.getUUID());
        long now=player.server.overworld().getGameTime();bond.nextDoorRoll=now-1;
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)h.getLevel().setBlock(pos.offset(x,-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
        h.getLevel().setBlock(pos,com.coraline.registry.ModBlocks.DOLL.get().defaultBlockState(),3);
        var placed=(com.coraline.block.entity.DollBlockEntity)h.getLevel().getBlockEntity(pos);placed.load(bond.template.getTagElement("BlockEntityTag"));data.placed(placed.createDollStack(),h.getLevel(),pos);
        double before=com.coraline.config.CoralineConfig.DOLL_DOOR_CHANCE.get();
        try{
            com.coraline.config.CoralineConfig.DOLL_DOOR_CHANCE.set(1.0);
            DollEvents.rollDoor(player,bond,data,now,true);
            var gifts=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2),item->item.getItem().is(com.coraline.registry.ModItems.TINY_DOOR.get())&&item.getItem().hasTag()&&item.getItem().getTag().getBoolean("DollDoorGift"));
            h.assertTrue(!gifts.isEmpty(),"No tagged door gift spawned");
        }finally{com.coraline.config.CoralineConfig.DOLL_DOOR_CHANCE.set(before);}
        h.succeed();
    }
    @GameTest(template="empty") public static void unseenDollMovesWithoutLosingBond(GameTestHelper h){
        for(int x=1;x<14;x++)for(int z=1;z<14;z++){h.setBlock(x,0,z,net.minecraft.world.level.block.Blocks.STONE);h.setBlock(x,4,z,net.minecraft.world.level.block.Blocks.STONE);}
        var origin=h.absolutePos(new net.minecraft.core.BlockPos(7,1,7));
        h.getLevel().setBlock(origin,com.coraline.registry.ModBlocks.DOLL.get().defaultBlockState(),3);
        var doll=(com.coraline.block.entity.DollBlockEntity)h.getLevel().getBlockEntity(origin);
        var id=UUID.randomUUID();doll.setOwner(id,"MovingDoll");
        family(h,id);
        DollBondData.get(h.getLevel().getServer()).placed(doll.createDollStack(),h.getLevel(),origin);
        DollMovement.tryMove(h.getLevel(),origin,doll,pos->pos.distSqr(origin)<=16);
        var bond=DollBondData.get(h.getLevel().getServer()).bonds.get(id);
        h.assertTrue(!origin.equals(bond.position)&&bond.placed&&!bond.lost,"Unseen doll did not relocate with intact bond");
        h.assertTrue(h.getLevel().isEmptyBlock(origin),"Old doll block left behind");
        var moved=(com.coraline.block.entity.DollBlockEntity)h.getLevel().getBlockEntity(bond.position);
        h.assertTrue(id.equals(moved.getOwnerId()),"Movement lost doll owner");
        for(int attempt=0;attempt<20;attempt++){
            var current=(com.coraline.block.entity.DollBlockEntity)h.getLevel().getBlockEntity(bond.position);
            DollMovement.tryMove(h.getLevel(),bond.position,current,pos->pos.distSqr(origin)<=16);
            h.assertTrue(bond.position.distSqr(origin)<=16,"Repeated movement drifted away from fixed home");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void visibleDollIsWatched(GameTestHelper h){
        var observer=player(h);var origin=h.absolutePos(new net.minecraft.core.BlockPos(7,1,7));
        observer.setPos(origin.getX()+.5,origin.getY(),origin.getZ()-3);observer.setYRot(0);observer.setXRot(18);
        h.getLevel().players().add(observer);
        try{h.assertTrue(DollMovement.watched(h.getLevel(),origin),"Visible doll was considered unobserved");}
        finally{h.getLevel().players().remove(observer);}
        h.succeed();
    }
    private static void family(GameTestHelper h,UUID owner){
        var f=new com.coraline.parents.FamilyData.Family();f.owner=owner;f.mom=UUID.randomUUID();f.dad=UUID.randomUUID();
        com.coraline.parents.FamilyData.get(h.getLevel().getServer()).families.put(f.id,f);
    }
    private static ServerPlayer player(GameTestHelper h){var player=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DollTester")){
        @Override public void displayClientMessage(net.minecraft.network.chat.Component message,boolean actionBar){}
    };family(h,player.getUUID());return player;}
    @GameTest(template="empty") public static void parentsRequiredAndDeathResetsDoll(GameTestHelper h){
        var player=player(h);var data=DollBondData.get(player.server);
        var family=com.coraline.parents.ParentAccess.family(player.server,player.getUUID());
        var stack=data.claim(player);player.getInventory().add(stack.copy());
        var pos=h.absolutePos(new net.minecraft.core.BlockPos(4,1,4));
        h.getLevel().setBlock(pos,com.coraline.registry.ModBlocks.DOLL.get().defaultBlockState(),3);
        var block=(com.coraline.block.entity.DollBlockEntity)h.getLevel().getBlockEntity(pos);
        block.load(stack.getTagElement("BlockEntityTag"));data.placed(stack,h.getLevel(),pos);
        var mom=com.coraline.registry.ModEntities.MOM.get().create(h.getLevel());mom.familyId=family.id;family.mom=mom.getUUID();
        var littleDoor=com.coraline.otherworld.DoorData.get(player.server).gift(player.getUUID());
        mom.kill();
        h.assertTrue(com.coraline.otherworld.DoorData.get(player.server).revoked(littleDoor),"Parent death did not revoke bound door");
        h.assertTrue(family.dead,"Parent death hook wasn't invoked");
        h.assertTrue(h.getLevel().isEmptyBlock(pos),"Dead parents left a placed doll");
        h.assertFalse(data.bonds.containsKey(player.getUUID()),"Received flag wasn't reset");
        h.assertFalse(data.current(stack),"Retired doll became valid again");
        DollStorage.clean(player.getInventory(),h.getLevel());h.assertTrue(player.getInventory().isEmpty(),"Inventory doll survived");
        var rod=com.coraline.registry.ModItems.DOWSING_ROD.get();player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(rod));
        h.assertTrue(rod.use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND).getResult()==net.minecraft.world.InteractionResult.FAIL,"Rod worked without parents");
        h.assertTrue(data.claim(player).isEmpty(),"Wybie gave a doll without parents");
        family(h,player.getUUID());var next=data.claim(player);
        h.assertTrue(!next.isEmpty()&&data.current(next)&&!data.current(stack),"New family could not start a fresh doll lifetime");
        var loaded=DollBondData.load(data.save(new CompoundTag()));
        h.assertFalse(loaded.current(stack),"Save/load revived old family's doll");h.succeed();
    }
    @GameTest(template="empty") public static void accessTooltipsHaveRequestedColors(GameTestHelper h){
        var rod=com.coraline.registry.ModItems.DOWSING_ROD.get();
        var lines=new java.util.ArrayList<net.minecraft.network.chat.Component>();
        boolean previous=com.coraline.parents.ParentStatus.clientHasParents;
        try{
            com.coraline.parents.ParentStatus.clientHasParents=false;
            rod.appendHoverText(rod.getDefaultInstance(),h.getLevel(),lines,net.minecraft.world.item.TooltipFlag.Default.NORMAL);
            h.assertTrue(lines.get(0).getString().equals("Parents are needed to use this item. Claimable parents should be out there somewhere...")&&lines.get(0).getStyle().getColor().equals(net.minecraft.network.chat.TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.YELLOW)),"Missing yellow parent warning");
            lines.clear();com.coraline.parents.ParentStatus.clientHasParents=true;
            rod.appendHoverText(rod.getDefaultInstance(),h.getLevel(),lines,net.minecraft.world.item.TooltipFlag.Default.NORMAL);
            h.assertTrue(lines.get(0).getStyle().getColor().equals(net.minecraft.network.chat.TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.GREEN)),"Normal rod text is not green");
            lines.clear();var doll=com.coraline.registry.ModItems.DOLL.get();
            doll.appendHoverText(doll.getDefaultInstance(),h.getLevel(),lines,net.minecraft.world.item.TooltipFlag.Default.NORMAL);
            h.assertTrue(lines.stream().anyMatch(c->c.getString().equals("this item will only exist as long as your parents are alive")&&c.getStyle().getColor().equals(net.minecraft.network.chat.TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.YELLOW))),"Doll lifetime warning missing");
        }finally{com.coraline.parents.ParentStatus.clientHasParents=previous;}
        h.succeed();
    }
    @GameTest(template="empty") public static void rodDisabledAfterDollEvenWhenLost(GameTestHelper h){
        var player=player(h);var data=DollBondData.get(player.server);var doll=data.claim(player);
        var rod=com.coraline.registry.ModItems.DOWSING_ROD.get();player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(rod));
        h.assertTrue(rod.use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND).getResult()==net.minecraft.world.InteractionResult.FAIL,"Rod worked after gift");
        data.lost(doll,h.getLevel());
        h.assertTrue(rod.use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND).getResult()==net.minecraft.world.InteractionResult.FAIL,"Losing doll re-enabled rod");h.succeed();
    }
    @GameTest(template="empty") public static void oneDollPerOwnerPersists(GameTestHelper h){
        var player=player(h);var data=DollBondData.get(player.server);
        h.assertTrue(!data.claim(player).isEmpty(),"First doll missing");
        h.assertTrue(data.claim(player).isEmpty(),"Wybie can give a second doll");
        var loaded=DollBondData.load(data.save(new CompoundTag()));
        h.assertTrue(loaded.claim(player).isEmpty(),"Received flag did not survive save");h.succeed();
    }
    @GameTest(template="empty") public static void destroyedDollSchedulesReturn(GameTestHelper h){
        var player=player(h);var data=DollBondData.get(player.server);var stack=data.claim(player);
        var entity=new BoundDollEntity(ModEntities.BOUND_DOLL.get(),h.getLevel());entity.setItem(stack);
        entity.hurt(h.getLevel().damageSources().generic(),100);
        var bond=data.bonds.get(player.getUUID());
        h.assertTrue(bond.lost&&!bond.placed&&bond.returnAt>0,"Destroyed doll not scheduled");
        data.present(stack);h.assertFalse(bond.lost,"Recovered item still scheduled");
        data.placed(stack,h.getLevel(),h.absolutePos(new net.minecraft.core.BlockPos(4,1,4)));
        h.assertTrue(bond.placed&&!bond.lost,"Placed doll must not recover from sky");h.succeed();
    }
    @GameTest(template="empty") public static void pickupAndUnloadAreNotDestruction(GameTestHelper h){
        var player=player(h);var data=DollBondData.get(player.server);var stack=data.claim(player);
        var entity=new BoundDollEntity(ModEntities.BOUND_DOLL.get(),h.getLevel());entity.setItem(stack.copy());
        entity.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        h.assertFalse(data.bonds.get(player.getUUID()).lost,"Chunk unload caused false recovery");
        var picked=new BoundDollEntity(ModEntities.BOUND_DOLL.get(),h.getLevel());picked.setItem(net.minecraft.world.item.ItemStack.EMPTY);
        picked.remove(Entity.RemovalReason.DISCARDED);
        h.assertFalse(data.bonds.get(player.getUUID()).lost,"Pickup caused false recovery");
        var hopperSource=new BoundDollEntity(ModEntities.BOUND_DOLL.get(),h.getLevel());hopperSource.setItem(stack.copy());
        hopperSource.remove(Entity.RemovalReason.DISCARDED);
        h.assertFalse(data.bonds.get(player.getUUID()).lost,"Hopper transfer caused duplicate recovery");h.succeed();
    }
}
