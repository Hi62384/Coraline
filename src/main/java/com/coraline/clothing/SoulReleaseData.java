package com.coraline.clothing;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Release requests survive even when a captured mob's chunk is unloaded. */
public final class SoulReleaseData extends SavedData {
    private final Set<UUID> released=new HashSet<>();
    public static SoulReleaseData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(SoulReleaseData::load,SoulReleaseData::new,"coraline_soul_releases");}
    private static SoulReleaseData load(CompoundTag tag){
        SoulReleaseData data=new SoulReleaseData();
        for(Tag entry:tag.getList("Released",Tag.TAG_STRING))try{data.released.add(UUID.fromString(entry.getAsString()));}catch(IllegalArgumentException ignored){}
        return data;
    }
    public void release(UUID id){if(released.add(id))setDirty();}
    public boolean consume(UUID id){if(released.remove(id)){setDirty();return true;}return false;}
    @Override public CompoundTag save(CompoundTag tag){
        ListTag list=new ListTag();for(UUID id:released)list.add(StringTag.valueOf(id.toString()));tag.put("Released",list);return tag;
    }
}
