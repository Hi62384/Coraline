package com.coraline.otherworld;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/** Durable identities: an unloaded item/container is not evidence that a door was destroyed. */
public final class DoorData extends SavedData {
    public final Map<UUID,Door> doors=new HashMap<>();
    private MinecraftServer server;
    public static final class Door {
        public UUID id=UUID.randomUUID(),owner;
        public boolean exists=true,placed,revoked;
        public String dimension="minecraft:overworld";
        public BlockPos pos=BlockPos.ZERO;
        public long night=-1,activeUntil,absentUntil;
        public long closedNight=Long.MIN_VALUE;
        public String holderKind="",holderDimension="";
        public BlockPos holderPos=BlockPos.ZERO;
        public UUID holder;
        public long missingSince=-1;
    }
    public static DoorData get(MinecraftServer server){var data=server.overworld().getDataStorage().computeIfAbsent(DoorData::load,DoorData::new,"coraline_doors");data.server=server;return data;}
    public boolean hasDoor(UUID owner){return doors.values().stream().anyMatch(d->!d.revoked&&(d.exists||(server!=null&&server.overworld().getGameTime()<d.absentUntil))&&owner.equals(d.owner));}
    public ItemStack gift(UUID owner){
        if(hasDoor(owner))return ItemStack.EMPTY;
        Door d=new Door();d.owner=owner;doors.put(d.id,d);setDirty();return stack(d);
    }
    public static UUID id(ItemStack stack){return stack.hasTag()&&stack.getTag().hasUUID("CoralineDoor")?stack.getTag().getUUID("CoralineDoor"):null;}
    public static ItemStack stack(Door d){var s=new ItemStack(com.coraline.registry.ModItems.TINY_DOOR.get());s.getOrCreateTag().putUUID("CoralineDoor",d.id);s.getTag().putUUID("DoorOwner",d.owner);s.getTag().putBoolean("DollDoorGift",true);return s;}
    public Door observe(ItemStack stack,UUID fallback){
        UUID id=id(stack);if(id==null){id=UUID.randomUUID();stack.getOrCreateTag().putUUID("CoralineDoor",id);}
        Door d=doors.get(id);if(d==null){d=new Door();d.id=id;d.owner=stack.getTag().hasUUID("DoorOwner")?stack.getTag().getUUID("DoorOwner"):fallback;if(d.owner==null)return null;doors.put(id,d);}
        if(d.revoked)return null;
        d.exists=true;stack.getTag().putUUID("DoorOwner",d.owner);setDirty();return d;
    }
    public boolean revoked(ItemStack stack){var d=doors.get(id(stack));return d!=null&&d.revoked;}
    public void endFamily(UUID owner){
        if(owner==null)return;
        boolean changed=false;
        for(var d:List.copyOf(doors.values()))if(owner.equals(d.owner)&&!d.revoked){
            changed=true;
            d.revoked=true;d.exists=false;d.activeUntil=0;d.absentUntil=0;
            var level=com.coraline.parents.FamilyData.level(server,d.dimension);
            if(d.placed&&level!=null){
                level.getChunkAt(d.pos);
                if(level.getBlockEntity(d.pos) instanceof DoorBlockEntity block&&d.id.equals(block.doorId))level.removeBlock(d.pos,false);
            }
            d.placed=false;d.absentUntil=0;
            var stored=com.coraline.parents.FamilyData.level(server,d.holderDimension);
            if(stored!=null&&stored.hasChunkAt(d.holderPos)&&stored.getBlockEntity(d.holderPos) instanceof net.minecraft.world.Container container)
                cleanDoors(container);
        }
        if(!changed)return;
        for(var player:server.getPlayerList().getPlayers()){
            cleanDoors(player.getInventory());cleanDoors(player.getEnderChestInventory());
            var cursor=player.containerMenu.getCarried();
            if(cleanDoors(cursor,0))player.containerMenu.setCarried(cursor.isEmpty()?ItemStack.EMPTY:cursor);
            player.containerMenu.broadcastChanges();
        }
        setDirty();
    }
    private void cleanDoors(net.minecraft.world.Container container){
        for(int i=0;i<container.getContainerSize();i++)if(cleanDoors(container.getItem(i),0))container.setChanged();
    }
    private boolean cleanDoors(ItemStack stack,int depth){
        if(depth>8||stack.isEmpty())return false;
        if(revoked(stack)){stack.setCount(0);return true;}
        if(!stack.hasTag())return false;boolean changed=false;
        for(var tag:new CompoundTag[]{stack.getTag(),stack.getTagElement("BlockEntityTag")})if(tag!=null){
            var items=tag.getList("Items",10);
            for(int i=items.size()-1;i>=0;i--){var n=items.getCompound(i);var nested=ItemStack.of(n);
                if(cleanDoors(nested,depth+1)){changed=true;if(nested.isEmpty())items.remove(i);else nested.save(n);}
            }
        }return changed;
    }
    public void destroyed(UUID id){var d=doors.get(id);if(d!=null){d.exists=false;d.placed=false;d.activeUntil=0;d.absentUntil=server==null?0:server.overworld().getGameTime()+40;setDirty();}}
    public void track(ItemStack stack,net.minecraft.server.level.ServerLevel level,String kind,BlockPos pos,UUID holder){track(stack,level,kind,pos,holder,0);}
    private void track(ItemStack stack,net.minecraft.server.level.ServerLevel level,String kind,BlockPos pos,UUID holder,int depth){
        if(stack.isEmpty()||depth>8)return;
        if(stack.is(com.coraline.registry.ModItems.TINY_DOOR.get())){
            var d=observe(stack,"inventory".equals(kind)?holder:null);
            if(d!=null){d.placed=false;d.holderKind=kind;d.holderDimension=level.dimension().location().toString();d.holderPos=pos.immutable();d.holder=holder;d.missingSince=-1;setDirty();}
        }
        if(!stack.hasTag())return;
        for(var tag:new CompoundTag[]{stack.getTag(),stack.getTagElement("BlockEntityTag")})if(tag!=null)
            for(Tag entry:tag.getList("Items",10)){var n=(CompoundTag)entry;var nested=ItemStack.of(n);track(nested,level,kind,pos,holder,depth+1);nested.save(n);}
    }
    public void audit(Door d){
        if(!d.exists||d.placed||d.holderKind.isEmpty())return;
        var level=com.coraline.parents.FamilyData.level(server,d.holderDimension);if(level==null)return;
        boolean found=false;
        if(d.holderKind.equals("inventory")){
            var player=d.holder==null?null:server.getPlayerList().getPlayer(d.holder);if(player==null)return;
            found=contains(player.getInventory(),d.id)||contains(player.getEnderChestInventory(),d.id)||contains(player.containerMenu.getCarried(),d.id,0);
        }else{
            if(!level.hasChunkAt(d.holderPos))return;
            if(d.holderKind.equals("entity")){
                var entity=d.holder==null?null:level.getEntity(d.holder);found=entity instanceof net.minecraft.world.entity.item.ItemEntity item&&contains(item.getItem(),d.id,0);
                if(entity instanceof net.minecraft.world.Container container)found=contains(container,d.id);
            }else if(level.getBlockEntity(d.holderPos) instanceof net.minecraft.world.Container container)found=contains(container,d.id);
            else if(level.getBlockEntity(d.holderPos)!=null){
                var handler=level.getBlockEntity(d.holderPos).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).orElse(null);
                if(handler!=null)for(int i=0;i<handler.getSlots();i++)found|=contains(handler.getStackInSlot(i),d.id,0);
            }
        }
        if(found){d.missingSince=-1;return;}
        long now=server.overworld().getGameTime();if(d.missingSince<0)d.missingSince=now;
        // Give transfers and bounded storage scans time to report the new holder.
        if(now-d.missingSince>=com.coraline.config.CoralineConfig.DOOR_MISSING_TICKS.get())destroyed(d.id);
    }
    private static boolean contains(net.minecraft.world.Container container,UUID id){for(int i=0;i<container.getContainerSize();i++)if(contains(container.getItem(i),id,0))return true;return false;}
    private static boolean contains(ItemStack stack,UUID id,int depth){
        if(stack.isEmpty()||depth>8)return false;if(id.equals(id(stack)))return true;if(!stack.hasTag())return false;
        for(var tag:new CompoundTag[]{stack.getTag(),stack.getTagElement("BlockEntityTag")})if(tag!=null)for(Tag entry:tag.getList("Items",10))if(contains(ItemStack.of((CompoundTag)entry),id,depth+1))return true;return false;
    }
    public static DoorData load(CompoundTag root){var data=new DoorData();for(Tag entry:root.getList("Doors",10)){
        var n=(CompoundTag)entry;Door d=new Door();d.id=n.getUUID("Id");d.owner=n.getUUID("Owner");d.exists=n.getBoolean("Exists");d.placed=n.getBoolean("Placed");d.dimension=n.getString("Dimension");d.pos=BlockPos.of(n.getLong("Position"));d.night=n.getLong("Night");d.activeUntil=n.getLong("ActiveUntil");data.doors.put(d.id,d);
        d.revoked=n.getBoolean("Revoked");d.holderKind=n.getString("HolderKind");d.holderDimension=n.getString("HolderDimension");d.holderPos=BlockPos.of(n.getLong("HolderPosition"));d.holder=n.hasUUID("Holder")?n.getUUID("Holder"):null;
        d.closedNight=n.contains("ClosedNight")?n.getLong("ClosedNight"):Long.MIN_VALUE;
    }return data;}
    @Override public CompoundTag save(CompoundTag root){ListTag list=new ListTag();for(Door d:doors.values()){
        var n=new CompoundTag();n.putUUID("Id",d.id);n.putUUID("Owner",d.owner);n.putBoolean("Exists",d.exists);n.putBoolean("Placed",d.placed);n.putString("Dimension",d.dimension);n.putLong("Position",d.pos.asLong());n.putLong("Night",d.night);n.putLong("ActiveUntil",d.activeUntil);
        n.putBoolean("Revoked",d.revoked);n.putString("HolderKind",d.holderKind);n.putString("HolderDimension",d.holderDimension);n.putLong("HolderPosition",d.holderPos.asLong());if(d.holder!=null)n.putUUID("Holder",d.holder);list.add(n);
        n.putLong("ClosedNight",d.closedNight);
    }root.put("Doors",list);return root;}
}
