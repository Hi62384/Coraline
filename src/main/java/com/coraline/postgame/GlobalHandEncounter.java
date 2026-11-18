package com.coraline.postgame;

import com.coraline.feast.VisitData;
import com.coraline.palace.PalaceWellProgress;
import com.coraline.rebirth.StoryRunStore;
import java.nio.file.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

/** One durable physical encounter reservation. Disconnects and failures retain
 * the original actors and encounter; only the authorized world reset retires it.
 * Completion remains the independently validated native well/key witness. */
public final class GlobalHandEncounter extends SavedData {
    private static final String NAME="coraline_global_hand_encounter";
    private final CompoundTag row;
    private boolean durable;
    private GlobalHandEncounter(CompoundTag row){this.row=row.copy();}
    private static Path file(MinecraftServer server){return server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");}
    private static GlobalHandEncounter get(MinecraftServer server){
        return server.overworld().getDataStorage().computeIfAbsent(GlobalHandEncounter::new,()->{
            var n=new CompoundTag();if(!Files.notExists(file(server),LinkOption.NOFOLLOW_LINKS))n.putBoolean("Blocked",true);
            return new GlobalHandEncounter(n);
        },NAME);
    }
    private boolean matches(VisitData.Scene scene,PalaceWellProgress progress){
        return !row.getBoolean("Blocked")&&row.getInt("Version")==1&&row.hasUUID("Owner")&&scene.owner.equals(row.getUUID("Owner"))
            &&row.hasUUID("Door")&&scene.door.equals(row.getUUID("Door"))&&row.hasUUID("Session")
            &&row.hasUUID("Hand")&&progress.originalHand().equals(row.getUUID("Hand"));
    }
    private boolean checkpoint(MinecraftServer server){
        try{
            if(Files.isSymbolicLink(file(server))||Files.isSymbolicLink(file(server).getParent()))return false;
            setDirty();server.overworld().getDataStorage().save();
            durable=row.equals(StoryRunStore.read(file(server)).getCompound("data"));return durable;
        }catch(java.io.IOException|RuntimeException failed){return false;}
    }
    public static boolean claim(MinecraftServer server,VisitData.Scene scene,PalaceWellProgress progress){
        if(scene==null||!progress.blocker().isEmpty()||TrueHeartBattle.resetting(server))return false;
        var data=get(server);if(!data.row.isEmpty()){
            if(!data.matches(scene,progress))return false;
            // Session rotates on pause/reconnect to reject stale inputs. The
            // global reservation is encounter identity, so rebind its input
            // epoch only after owner, door and original Hand all match.
            if(!data.row.getUUID("Session").equals(progress.session())){data.row.putUUID("Session",progress.session());data.durable=false;}
            return data.durable||data.checkpoint(server);
        }
        if(InfiniteWorldData.get(server).active())return false;
        // A pre-upgrade native encounter must be resumed, never raced by a fresh owner.
        for(var other:VisitData.get(server).scenes.values()){
            var well=other.loop.story.getCompound("PinkPalaceJourney").getCompound("Well");
            if(well.isEmpty())continue;
            var old=PalaceWellProgress.load(other.owner,other.door,well);
            if(!old.blocker().isEmpty()||!other.owner.equals(scene.owner)||!other.door.equals(scene.door)||!old.session().equals(progress.session()))return false;
        }
        try{
            var nativeFile=server.getWorldPath(LevelResource.ROOT).resolve("data/coraline_first_visits.dat");
            var nativeScene=StoryRunStore.read(nativeFile);
            if(!com.coraline.palace.PalaceKeyCheckpoint.story(nativeScene,scene.owner,scene.door,scene.loop.story.getCompound("PinkPalaceJourney")))return false;
        }catch(java.io.IOException|RuntimeException unverified){return false;}
        data.row.putInt("Version",1);data.row.putUUID("Owner",scene.owner);data.row.putUUID("Door",scene.door);
        data.row.putUUID("Session",progress.session());data.row.putUUID("Hand",progress.originalHand());
        return data.checkpoint(server);
    }
    public static boolean reservedFor(MinecraftServer server,VisitData.Scene scene,PalaceWellProgress progress){
        return get(server).matches(scene,progress);
    }
    public static boolean permits(MinecraftServer server,VisitData.Scene scene,PalaceWellProgress progress){
        var world=InfiniteWorldData.get(server);
        if(world.active()){
            var canonical=world.canonicalCompletion();
            return canonical.hasUUID("Owner")&&scene.owner.equals(canonical.getUUID("Owner"))
                &&canonical.hasUUID("Door")&&scene.door.equals(canonical.getUUID("Door"))
                &&canonical.hasUUID("WellSession")&&progress.session().equals(canonical.getUUID("WellSession"));
        }
        return claim(server,scene,progress);
    }
    static boolean retire(MinecraftServer server,java.util.UUID generation,java.util.UUID reset){
        if(!TrueHeartBattle.authorizes(server,generation,reset))return false;
        var data=get(server);data.durable=false;
        for(String key:java.util.Set.copyOf(data.row.getAllKeys()))data.row.remove(key);
        return data.checkpoint(server);
    }
    @Override public CompoundTag save(CompoundTag target){target.merge(row);return target;}
}
