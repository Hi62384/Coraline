package com.coraline.otherworld;

import com.coraline.rebirth.StoryRunStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

/** A door can disappear before the chunk containing its passage is saved. */
final class TunnelPassageJournal extends SavedData {
    private static final String NAME="coraline_tunnel_passages";
    final CompoundTag rows;
    int orphanCursor;
    private final boolean valid;

    private TunnelPassageJournal(CompoundTag saved,boolean fresh){
        valid=fresh||saved.getInt("Version")==1&&saved.contains("Passages",Tag.TAG_COMPOUND);
        rows=saved.getCompound("Passages");
    }
    static TunnelPassageJournal get(ServerLevel level){
        var server=level.getServer();
        var path=server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");
        return server.overworld().getDataStorage().computeIfAbsent(n->new TunnelPassageJournal(n,false),
                ()->new TunnelPassageJournal(new CompoundTag(),Files.notExists(path,LinkOption.NOFOLLOW_LINKS)),NAME);
    }
    static String key(ServerLevel level,BlockPos at){return level.dimension().location()+"/"+at.asLong();}
    boolean usable(){return valid;}
    boolean checkpoint(ServerLevel level){
        if(!valid)return false;
        var server=level.getServer();
        try{
            var path=server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");
            if(Files.isSymbolicLink(path)||Files.isSymbolicLink(path.getParent()))return false;
            setDirty();server.overworld().getDataStorage().save();
            return save(new CompoundTag()).equals(StoryRunStore.read(path).getCompound("data"));
        }catch(IOException|RuntimeException failed){return false;}
    }
    @Override public CompoundTag save(CompoundTag out){
        out.putInt("Version",1);out.put("Passages",rows.copy());return out;
    }
}
