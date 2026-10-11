package com.coraline.doll;

import com.coraline.item.DollItem;
import com.coraline.config.CoralineConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Received-doll flag lasts for a parent pair; retired generations can never revive it. */
public final class DollBondData extends SavedData {
    public final Map<UUID,Bond> bonds=new HashMap<>();
    private final Map<UUID,Integer> retired=new HashMap<>();
    private MinecraftServer server;
    public static final class Bond {
        public ItemStack template=ItemStack.EMPTY;
        public boolean lost,placed,recallRequested;
        public long returnAt;
        public int generation;
        public UUID familyId;
        public long lastSatisfiedAt=-1,nextDoorRoll;
        public String dimension="";
        public BlockPos position=BlockPos.ZERO;
    }
    public static DollBondData get(MinecraftServer server){var data=server.overworld().getDataStorage().computeIfAbsent(DollBondData::load,DollBondData::new,"coraline_doll_bonds");data.server=server;return data;}
    public static UUID owner(ItemStack stack){
        if(!stack.is(com.coraline.registry.ModItems.DOLL.get()))return null;
        var tag=stack.getTagElement("BlockEntityTag");return tag!=null&&tag.hasUUID(DollItem.OWNER_ID)?tag.getUUID(DollItem.OWNER_ID):null;
    }
    private Bond ensure(ItemStack stack){
        UUID id=owner(stack);if(id==null)return null;
        int generation=stack.getTagElement("BlockEntityTag").getInt("BondGeneration");
        if(generation<retired.getOrDefault(id,0))return null;
        if(server!=null&&!validateFamily(id))return null;
        Bond b=bonds.get(id);
        // Only migrate pre-lifecycle dolls. New generations must be issued by claim().
        if(b==null&&retired.containsKey(id))return null;
        if(b==null){b=new Bond();b.template=stack.copyWithCount(1);b.generation=generation;
            if(server!=null)b.familyId=com.coraline.parents.ParentAccess.family(server,id).id;
            bonds.put(id,b);setDirty();}
        return b;
    }
    public boolean validateFamily(UUID owner){
        var family=com.coraline.parents.ParentAccess.family(server,owner);var b=bonds.get(owner);
        if(family==null||(b!=null&&b.familyId!=null&&!b.familyId.equals(family.id))){endFamily(owner);return false;}
        if(b!=null&&b.familyId==null){b.familyId=family.id;setDirty();}
        return true;
    }
    public void endFamily(UUID owner){
        if(owner==null)return;
        Bond b=bonds.remove(owner);
        if(b==null){if(retired.putIfAbsent(owner,1)==null)setDirty();return;}
        retired.merge(owner,b.generation+1,Math::max);setDirty();
        if(server==null)return;
        var level=com.coraline.parents.FamilyData.level(server,b.dimension);
        if(b.placed&&level!=null&&level.hasChunkAt(b.position)&&level.getBlockEntity(b.position) instanceof com.coraline.block.entity.DollBlockEntity doll&&owner.equals(doll.getOwnerId())){
            doll.moving=true;level.removeBlock(b.position,false);
        }
        for(var player:server.getPlayerList().getPlayers()){
            DollStorage.clean(player.getInventory(),player.serverLevel());
            DollStorage.clean(player.getEnderChestInventory(),player.serverLevel());
            var cursor=player.containerMenu.getCarried();
            if(DollStorage.cleanStack(cursor,player.serverLevel()))player.containerMenu.setCarried(cursor.isEmpty()?ItemStack.EMPTY:cursor);
            player.containerMenu.broadcastChanges();
        }
    }
    public boolean current(ItemStack stack){
        Bond b=ensure(stack);return b!=null&&stack.getTagElement("BlockEntityTag").getInt("BondGeneration")==b.generation;
    }
    public void satisfied(ItemStack stack,long now,boolean inInventory){
        if(!current(stack))return;Bond b=ensure(stack);
        b.lastSatisfiedAt=now;b.lost=false;b.recallRequested=false;b.returnAt=0;if(inInventory)b.placed=false;setDirty();
    }
    public boolean overdue(Bond b,long now){
        if(b.lastSatisfiedAt==-1){b.lastSatisfiedAt=now;setDirty();return false;}
        return now-b.lastSatisfiedAt>=CoralineConfig.DOLL_UNSATISFIED_TICKS.get();
    }
    public void recall(Bond b,ServerLevel level){
        if(b.lost)return;b.lost=true;b.placed=false;b.recallRequested=true;
        b.returnAt=level.getServer().overworld().getGameTime()+CoralineConfig.DOLL_RECALL_DELAY.get();setDirty();
    }
    public ItemStack replacement(Bond b){var stack=b.template.copyWithCount(1);stack.getOrCreateTagElement("BlockEntityTag").putInt("BondGeneration",b.generation+1);return stack;}
    public void returned(Bond b,ItemStack stack,long now){
        b.generation++;b.template=stack.copyWithCount(1);b.lost=false;b.placed=false;b.recallRequested=false;b.returnAt=0;b.lastSatisfiedAt=now;setDirty();
    }
    public ItemStack claim(ServerPlayer player){
        server=player.server;
        if(!validateFamily(player.getUUID()))return ItemStack.EMPTY;
        if(bonds.containsKey(player.getUUID()))return ItemStack.EMPTY;
        var stack=DollItem.createFor(player);int generation=retired.getOrDefault(player.getUUID(),0);
        stack.getOrCreateTagElement("BlockEntityTag").putInt("BondGeneration",generation);
        var b=new Bond();b.template=stack.copy();b.generation=generation;b.familyId=com.coraline.parents.ParentAccess.family(server,player.getUUID()).id;
        bonds.put(player.getUUID(),b);remember(player,b);setDirty();return stack;
    }
    private static void remember(ServerPlayer player,Bond b){
        var root=player.getPersistentData();var tag=root.getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        if(b.familyId!=null)tag.putUUID("CoralineDollFamily",b.familyId);
        tag.putInt("CoralineDollGeneration",b.generation);root.put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG,tag);
    }
    public void watchdog(ServerPlayer player){
        var family=com.coraline.parents.ParentAccess.family(player.server,player.getUUID());
        var backup=player.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        if(family==null)return;
        var bond=bonds.get(player.getUUID());
        if(bond!=null){remember(player,bond);return;}
        if(!backup.hasUUID("CoralineDollFamily")||!family.id.equals(backup.getUUID("CoralineDollFamily")))return;
        retired.merge(player.getUUID(),backup.getInt("CoralineDollGeneration")+1,Math::max);
        var stack=claim(player);if(stack.isEmpty())return;
        bond=bonds.get(player.getUUID());recall(bond,player.serverLevel());
    }
    public void present(ItemStack stack){
        if(!current(stack))return;Bond b=ensure(stack);
        if(b.lost&&!b.recallRequested){b.lost=false;b.placed=false;b.returnAt=0;setDirty();}
    }
    public void placed(ItemStack stack,ServerLevel level,BlockPos pos){
        if(!current(stack))return;Bond b=ensure(stack);
        String dimension=level.dimension().location().toString();
        if(b.recallRequested)return;
        if(!b.placed||b.lost||!pos.equals(b.position)||!dimension.equals(b.dimension)){
            b.placed=true;b.lost=false;b.returnAt=0;b.position=pos.immutable();b.dimension=dimension;setDirty();
        }
    }
    public void lost(ItemStack stack,ServerLevel level){
        if(!current(stack))return;Bond b=ensure(stack);if(b.lost)return;
        b.placed=false;b.lost=true;
        int min=CoralineConfig.DOLL_RETURN_MIN.get(),max=Math.max(min,CoralineConfig.DOLL_RETURN_MAX.get());
        b.returnAt=level.getServer().overworld().getGameTime()+min+level.random.nextInt(max-min+1);setDirty();
    }
    public static DollBondData load(CompoundTag n){
        var data=new DollBondData();
        for(Tag entry:n.getList("Retired",10)){var tag=(CompoundTag)entry;if(tag.hasUUID("Owner"))data.retired.put(tag.getUUID("Owner"),tag.getInt("Generation"));}
        for(Tag entry:n.getList("Bonds",10)){
            CompoundTag tag=(CompoundTag)entry;if(!tag.hasUUID("Owner"))continue;
            Bond b=new Bond();b.template=ItemStack.of(tag.getCompound("Doll"));b.lost=tag.getBoolean("Lost");b.placed=tag.getBoolean("Placed");
            if(tag.hasUUID("Family"))b.familyId=tag.getUUID("Family");
            b.returnAt=tag.getLong("ReturnAt");b.position=BlockPos.of(tag.getLong("Position"));b.dimension=tag.getString("Dimension");
            b.generation=tag.getInt("Generation");b.recallRequested=tag.getBoolean("RecallRequested");b.lastSatisfiedAt=tag.contains("LastSatisfiedAt")?tag.getLong("LastSatisfiedAt"):-1;b.nextDoorRoll=tag.getLong("NextDoorRoll");data.bonds.put(tag.getUUID("Owner"),b);
        }return data;
    }
    @Override public CompoundTag save(CompoundTag n){
        ListTag list=new ListTag();bonds.forEach((id,b)->{
            CompoundTag tag=new CompoundTag();tag.putUUID("Owner",id);tag.put("Doll",b.template.save(new CompoundTag()));
            if(b.familyId!=null)tag.putUUID("Family",b.familyId);
            tag.putBoolean("Lost",b.lost);tag.putBoolean("Placed",b.placed);tag.putLong("ReturnAt",b.returnAt);tag.putLong("Position",b.position.asLong());tag.putString("Dimension",b.dimension);
            tag.putInt("Generation",b.generation);tag.putBoolean("RecallRequested",b.recallRequested);tag.putLong("LastSatisfiedAt",b.lastSatisfiedAt);tag.putLong("NextDoorRoll",b.nextDoorRoll);list.add(tag);
        });n.put("Bonds",list);
        ListTag tombstones=new ListTag();retired.forEach((id,g)->{var tag=new CompoundTag();tag.putUUID("Owner",id);tag.putInt("Generation",g);tombstones.add(tag);});n.put("Retired",tombstones);return n;
    }
}
