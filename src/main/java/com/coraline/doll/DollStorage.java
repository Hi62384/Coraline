package com.coraline.doll;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Bounded scans of loaded storage only. Older generations are inert and removed on reload. */
@Mod.EventBusSubscriber(modid="coraline")
public final class DollStorage {
    private static final Map<ServerLevel,LinkedHashSet<Long>> chunks=new WeakHashMap<>();
    private static final Map<ServerLevel,Set<UUID>> containers=new WeakHashMap<>();
    public static boolean cleanStack(ItemStack stack,ServerLevel level){
        return cleanStack(stack,level,0);
    }
    private static boolean cleanStack(ItemStack stack,ServerLevel level,int depth){
        if(depth>8)return false;
        if(com.coraline.otherworld.DoorData.get(level.getServer()).revoked(stack)){stack.setCount(0);return true;}
        if(stack.is(com.coraline.registry.ModItems.TINY_DOOR.get())&&com.coraline.otherworld.DoorData.id(stack)!=null)com.coraline.otherworld.DoorData.get(level.getServer()).observe(stack,null);
        var owner=DollBondData.owner(stack);
        if(owner!=null){
            var data=DollBondData.get(level.getServer());
            if(!data.current(stack)){stack.setCount(0);return true;}
            var b=data.bonds.get(owner);
            if(level.getServer().getPlayerList().getPlayer(owner)!=null&&data.overdue(b,level.getServer().overworld().getGameTime())){
                return recallStored(stack,level);
            }
        }
        return cleanNested(stack,level,depth);
    }
    static boolean recallStored(ItemStack stack,ServerLevel level){
        var data=DollBondData.get(level.getServer());if(!data.current(stack)){stack.setCount(0);return true;}
        var bond=data.bonds.get(DollBondData.owner(stack));
        if(!data.overdue(bond,level.getServer().overworld().getGameTime()))return false;
        data.recall(bond,level);stack.setCount(0);return true;
    }
    public static boolean cleanNested(ItemStack stack,ServerLevel level,int depth){
        if(depth>8||!stack.hasTag())return false;
        boolean changed=cleanList(stack.getTag(),"Items",level,depth);
        var block=stack.getTagElement("BlockEntityTag");
        if(block!=null)changed|=cleanList(block,"Items",level,depth);
        return changed;
    }
    private static boolean cleanList(CompoundTag tag,String key,ServerLevel level,int depth){
        if(!tag.contains(key,Tag.TAG_LIST))return false;
        ListTag old=tag.getList(key,Tag.TAG_COMPOUND),next=new ListTag();boolean changed=false;
        for(Tag entry:old){
            var original=(CompoundTag)entry;var nested=ItemStack.of(original);
            if(DollBondData.owner(nested)!=null||nested.is(com.coraline.registry.ModItems.TINY_DOOR.get())){
                if(cleanStack(nested,level,depth+1)){changed=true;if(nested.isEmpty())continue;}
            }else if(cleanNested(nested,level,depth+1))changed=true;
            // Preserve slot numbers and any container-specific metadata.
            var saved=original.copy();nested.save(saved);next.add(saved);
        }
        if(changed)tag.put(key,next);return changed;
    }
    public static void clean(Container container,ServerLevel level){
        boolean changed=false;
        for(int slot=0;slot<container.getContainerSize();slot++){
            var stack=container.getItem(slot);
            if(container instanceof net.minecraft.world.level.block.entity.BlockEntity block)com.coraline.otherworld.DoorData.get(level.getServer()).track(stack,level,"container",block.getBlockPos(),null);
            else if(container instanceof net.minecraft.world.entity.Entity entity)com.coraline.otherworld.DoorData.get(level.getServer()).track(stack,level,"entity",entity.blockPosition(),entity.getUUID());
            if(cleanStack(stack,level)){container.setItem(slot,stack.isEmpty()?ItemStack.EMPTY:stack);changed=true;}
        }
        if(changed)container.setChanged();
    }
    private static void scan(LevelChunk chunk,ServerLevel level){
        for(var block:List.copyOf(chunk.getBlockEntities().values())){
            if(block instanceof Container container){clean(container,level);continue;}
            block.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).ifPresent(handler->{
                for(int i=0;i<handler.getSlots();i++){
                    var original=handler.getStackInSlot(i);var copy=original.copy();
                    com.coraline.otherworld.DoorData.get(level.getServer()).track(copy,level,"container",block.getBlockPos(),null);
                    if(cleanStack(copy,level)){
                        if(handler instanceof net.minecraftforge.items.IItemHandlerModifiable modifiable)modifiable.setStackInSlot(i,copy);
                        else if(copy.isEmpty())handler.extractItem(i,original.getCount(),false);
                        block.setChanged();
                    }
                }
            });
        }
    }
    @SubscribeEvent public static synchronized void loaded(ChunkEvent.Load event){
        if(event.getLevel() instanceof ServerLevel level&&event.getChunk() instanceof LevelChunk chunk){
            chunks.computeIfAbsent(level,k->new LinkedHashSet<>()).add(chunk.getPos().toLong());
            // Defer scanning until the level tick: chunk load events may be emitted off-thread.
        }
    }
    @SubscribeEvent public static synchronized void unloaded(ChunkEvent.Unload event){
        if(event.getLevel() instanceof ServerLevel level){var set=chunks.get(level);if(set!=null)set.remove(event.getChunk().getPos().toLong());}
    }
    @SubscribeEvent public static synchronized void joined(EntityJoinLevelEvent event){
        if(event.getLevel() instanceof ServerLevel level&&event.getEntity() instanceof Container)
            containers.computeIfAbsent(level,k->new HashSet<>()).add(event.getEntity().getUUID());
    }
    @SubscribeEvent public static synchronized void tick(TickEvent.LevelTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.level instanceof ServerLevel level)||level.getGameTime()%20!=0)return;
        var set=chunks.get(level);
        if(set!=null){
            var batch=set.stream().limit(32).toList();
            for(long key:batch){set.remove(key);set.add(key);var pos=new ChunkPos(key);
                var chunk=level.getChunkSource().getChunkNow(pos.x,pos.z);if(chunk!=null)scan(chunk,level);
            }
        }
        var ids=containers.get(level);
        if(ids!=null)ids.removeIf(id->{var entity=level.getEntity(id);if(entity instanceof Container container){clean(container,level);return false;}return true;});
    }
}
