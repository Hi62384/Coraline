package com.coraline.feast;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Allocated sites cannot collide through UUID hash truncation. They remain owned after cleanup. */
public final class ShowSiteData extends SavedData {
    private int next;
    private final Map<UUID,CompoundTag> owners=new HashMap<>();
    public static ShowSiteData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(ShowSiteData::load,ShowSiteData::new,"coraline_show_sites");}
    public CompoundTag owner(UUID owner){return owners.computeIfAbsent(owner,id->{var n=new CompoundTag();n.putInt("Slot",next++);setDirty();return n;});}
    public BlockPos center(UUID owner,boolean circus){int slot=owner(owner).getInt("Slot");return new BlockPos((slot%1024)*512,100,-8000000+(slot/1024)*512+(circus?200:0));}
    public static ShowSiteData load(CompoundTag root){var result=new ShowSiteData();result.next=root.getInt("Next");for(var tag:root.getList("Owners",Tag.TAG_COMPOUND)){var n=(CompoundTag)tag;if(n.hasUUID("Owner"))result.owners.put(n.getUUID("Owner"),n);}return result;}
    @Override public CompoundTag save(CompoundTag root){root.putInt("Next",next);var list=new ListTag();for(var e:owners.entrySet()){var n=e.getValue().copy();n.putUUID("Owner",e.getKey());list.add(n);}root.put("Owners",list);return root;}
}
