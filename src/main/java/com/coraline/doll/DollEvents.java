package com.coraline.doll;

import com.coraline.registry.ModEntities;
import com.coraline.config.CoralineConfig;
import com.coraline.block.entity.DollBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="coraline")
public final class DollEvents {
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player)||player.tickCount%20!=0)return;
        var data=DollBondData.get(player.server);long now=player.server.overworld().getGameTime();
        data.validateFamily(player.getUUID());
        data.watchdog(player);
        boolean held=false;
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++){
            var stack=player.getInventory().getItem(slot);
            com.coraline.otherworld.DoorData.get(player.server).track(stack,player.serverLevel(),"inventory",player.blockPosition(),player.getUUID());
            if(player.getUUID().equals(DollBondData.owner(stack))){
                if(!data.current(stack)||held){stack.setCount(0);continue;}
                stack.setCount(1);held=true;data.satisfied(stack,now,true);
            }
            DollStorage.cleanNested(stack,player.serverLevel(),0);
        }
        var cursor=player.containerMenu.getCarried();
        if(player.getUUID().equals(DollBondData.owner(cursor))){
            if(!data.current(cursor)||held)player.containerMenu.setCarried(ItemStack.EMPTY);
            else{held=true;data.satisfied(cursor,now,true);}
        }
        DollStorage.clean(player.getEnderChestInventory(),player.serverLevel());
        for(var slot:player.containerMenu.slots){
            if(slot.container!=player.getInventory()){
                var stack=slot.getItem();if(DollStorage.cleanStack(stack,player.serverLevel()))slot.set(stack.isEmpty()?ItemStack.EMPTY:stack);
            }
        }
        var b=data.bonds.get(player.getUUID());if(b==null)return;
        boolean home=false;
        if(!held&&b.placed){
            var level=com.coraline.parents.FamilyData.level(player.server,b.dimension);
            if(level!=null&&DollHome.nearBed(level,b.position,player.getUUID())){
                // Verify the recorded block, even if its chunk unloaded. Never renew a bond on an assumption.
                level.getChunkAt(b.position);
                home=level.getBlockEntity(b.position) instanceof DollBlockEntity doll&&data.current(doll.createDollStack())&&player.getUUID().equals(doll.getOwnerId());
                if(home)data.satisfied(b.template,now,false);
            }
        }
        if(!held&&!home&&data.overdue(b,now))data.recall(b,player.serverLevel());
        // Creative deletion sends no item-destruction event. Reconcile the real
        // inventory/cursor/block state with a shorter, bounded confirmation window.
        if(player.isCreative()&&!held&&!home&&!b.lost&&b.lastSatisfiedAt>=0
            &&now-b.lastSatisfiedAt>=CoralineConfig.DOLL_CREATIVE_MISSING.get())data.recall(b,player.serverLevel());
        if(player.isAlive()&&!player.isSpectator()&&(held||home))rollDoor(player,b,data,now,home);
        if(!b.lost||held||home||!player.isAlive()||player.isSpectator()||now<b.returnAt)return;
        var level=player.serverLevel();
        // Fall beside the owner, below roofs if necessary, rather than stranding it on a roof.
        for(int attempt=0;attempt<16;attempt++){
            int x=player.getBlockX()+level.random.nextInt(5)-2,z=player.getBlockZ()+level.random.nextInt(5)-2;
            for(int height=10;height>=1;height--){
                BlockPos drop=new BlockPos(x,player.getBlockY()+height,z);
                if(!level.hasChunkAt(drop)||!level.getWorldBorder().isWithinBounds(drop)||drop.getY()>=level.getMaxBuildHeight()-1||!level.isEmptyBlock(drop))continue;
                boolean clear=true;
                for(int y=player.getBlockY()+1;y<drop.getY();y++)if(!level.isEmptyBlock(new BlockPos(x,y,z))){clear=false;break;}
                if(!clear)continue;
                var doll=ModEntities.BOUND_DOLL.get().create(level);if(doll==null)return;
                doll.setPos(x+.5,drop.getY(),z+.5);var stack=data.replacement(b);doll.setItem(stack);doll.setDefaultPickUpDelay();
                if(level.addFreshEntity(doll)){
                    data.returned(b,stack,now);
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("Something familiar falls from above..."),true);return;
                }
            }
        }
        var stack=data.replacement(b);var saved=stack.copy();
        if(player.getInventory().add(stack))data.returned(b,saved,now);
    }
    static void rollDoor(ServerPlayer player,DollBondData.Bond bond,DollBondData data,long now,boolean home){
        if(!home||!bond.placed)return;
        if(bond.nextDoorRoll==0){bond.nextDoorRoll=now+CoralineConfig.DOLL_DOOR_INTERVAL.get();data.setDirty();return;}
        if(now<bond.nextDoorRoll)return;
        bond.nextDoorRoll=now+CoralineConfig.DOLL_DOOR_INTERVAL.get();data.setDirty();
        if(player.getRandom().nextDouble()>=CoralineConfig.DOLL_DOOR_CHANCE.get())return;
        var level=com.coraline.parents.FamilyData.level(player.server,bond.dimension);
        var pos=bond.position;if(level==null||!level.hasChunkAt(pos))return;
        if(!(level.getBlockEntity(pos) instanceof DollBlockEntity doll)||!player.getUUID().equals(doll.getOwnerId())||!data.current(doll.createDollStack()))return;
        BlockPos drop=null;
        for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL){var candidate=pos.relative(direction);
            if(level.isEmptyBlock(candidate)&&!level.getBlockState(candidate.below()).getCollisionShape(level,candidate.below()).isEmpty()){drop=candidate;break;}}
        if(drop==null)return;
        var stack=com.coraline.otherworld.DoorData.get(player.server).gift(player.getUUID());if(stack.isEmpty())return;
        var item=new com.coraline.otherworld.DoorItemEntity(com.coraline.registry.ModEntities.TRACKED_DOOR.get(),level);item.setPos(drop.getX()+.5,drop.getY()+.2,drop.getZ()+.5);item.setItem(stack);item.setDefaultPickUpDelay();
        if(!level.addFreshEntity(item))com.coraline.otherworld.DoorData.get(player.server).destroyed(com.coraline.otherworld.DoorData.id(stack));
    }
    @SubscribeEvent public static void pickedUp(net.minecraftforge.event.entity.player.PlayerEvent.ItemPickupEvent event){
        var stack=event.getStack();
        if(stack.is(com.coraline.registry.ModItems.TINY_DOOR.get())&&stack.hasTag()&&stack.getTag().getBoolean("DollDoorGift"))
            event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.literal("Where did this strange little door come from? I should attach it to my wall somewhere"),false);
    }
}
