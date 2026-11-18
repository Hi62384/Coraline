package com.coraline.postgame;

import com.coraline.feast.VisitData;
import com.coraline.palace.PalaceWellProgress;
import com.coraline.rebirth.StoryRunStore;
import java.nio.file.*;
import java.util.UUID;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Exclusive access to the shared well arena. Each scene retains its own
 * authenticated Hand, progression and key journal when its access expires. */
@Mod.EventBusSubscriber(modid="coraline")
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
    static final long ABANDONMENT_TICKS=6_000;
    static boolean canClaim(CompoundTag lease,UUID owner,long now,boolean holderPresent){
        if(lease.isEmpty())return true;
        if(!valid(lease))return false;
        if(owner.equals(lease.getUUID("Owner")))return true;
        return !holderPresent&&lease.contains("AbsentSince",Tag.TAG_LONG)
                &&lease.getLong("AbsentSince")>=0&&now>=lease.getLong("AbsentSince")
                &&now-lease.getLong("AbsentSince")>=ABANDONMENT_TICKS;
    }
    private static boolean valid(CompoundTag lease){
        return !lease.getBoolean("Blocked")&&lease.contains("Version",Tag.TAG_INT)&&lease.getInt("Version")==1
                &&lease.hasUUID("Owner")&&lease.hasUUID("Door")&&lease.hasUUID("Session")&&lease.hasUUID("Hand");
    }
    static boolean observePresence(CompoundTag lease,long now,boolean present){
        if(!valid(lease))return false;
        if(present){
            if(!lease.contains("AbsentSince"))return false;
            lease.remove("AbsentSince");return true;
        }
        if(lease.contains("AbsentSince",Tag.TAG_LONG)&&lease.getLong("AbsentSince")>=0&&lease.getLong("AbsentSince")<=now)return false;
        lease.putLong("AbsentSince",now);return true;
    }
    private static boolean holderPresent(MinecraftServer server,CompoundTag lease){
        var owner=server.getPlayerList().getPlayer(lease.getUUID("Owner"));
        if(owner==null)return false;
        var scene=VisitData.get(server).scenes.get(owner.getUUID());
        if(scene==null||!scene.door.equals(lease.getUUID("Door")))return false;
        if(owner.serverLevel()!=server.overworld())return false;
        return com.coraline.palace.PalaceSiteData.get(server).position(com.coraline.palace.PalaceSitePlan.Kind.WELL)
                .map(well->owner.distanceToSqr(well.getCenter())<=48*48).orElse(true);
    }
    public static boolean available(MinecraftServer server,UUID owner){
        if(TrueHeartBattle.resetting(server))return false;
        var data=get(server);
        if(data.row.isEmpty())return true;
        if(!valid(data.row))return false;
        long now=server.overworld().getGameTime();
        boolean present=holderPresent(server,data.row);
        if(observePresence(data.row,now,present)){data.durable=false;data.setDirty();}
        return canClaim(data.row,owner,now,present);
    }
    static boolean replaceLease(CompoundTag lease,UUID owner,UUID door,UUID session,UUID hand,long now,boolean holderPresent){
        if(owner==null||door==null||session==null||hand==null||!canClaim(lease,owner,now,holderPresent))return false;
        if(!lease.isEmpty()&&owner.equals(lease.getUUID("Owner"))
                &&(!door.equals(lease.getUUID("Door"))||!hand.equals(lease.getUUID("Hand"))))return false;
        lease.putInt("Version",1);lease.putUUID("Owner",owner);lease.putUUID("Door",door);
        lease.putUUID("Session",session);lease.putUUID("Hand",hand);lease.remove("AbsentSince");
        return true;
    }
    public static boolean claim(MinecraftServer server,VisitData.Scene scene,PalaceWellProgress progress){
        if(scene==null||!progress.blocker().isEmpty()||!available(server,scene.owner))return false;
        var data=get(server);
        if(data.matches(scene,progress)){
            if(!data.row.getUUID("Session").equals(progress.session())){data.row.putUUID("Session",progress.session());data.durable=false;}
            return data.durable||data.checkpoint(server);
        }
        // The incoming owner's native journal must already be durable. Taking
        // the arena never modifies the previous owner's journal or actors.
        try{
            var nativeFile=server.getWorldPath(LevelResource.ROOT).resolve("data/coraline_first_visits.dat");
            var nativeScene=StoryRunStore.read(nativeFile);
            if(!com.coraline.palace.PalaceKeyCheckpoint.story(nativeScene,scene.owner,scene.door,scene.loop.story.getCompound("PinkPalaceJourney")))return false;
        }catch(java.io.IOException|RuntimeException unverified){return false;}
        if(!replaceLease(data.row,scene.owner,scene.door,progress.session(),progress.originalHand(),
                server.overworld().getGameTime(),!data.row.isEmpty()&&holderPresent(server,data.row)))return false;
        data.durable=false;
        return data.checkpoint(server);
    }
    public static boolean holds(MinecraftServer server,VisitData.Scene scene,PalaceWellProgress progress){
        if(scene==null||!progress.blocker().isEmpty()||TrueHeartBattle.resetting(server))return false;
        var data=get(server);
        return data.durable&&data.matches(scene,progress)&&progress.session().equals(data.row.getUUID("Session"));
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||event.getServer().getTickCount()%40!=0)return;
        var server=event.getServer();
        var data=get(server);
        if(valid(data.row)&&observePresence(data.row,server.overworld().getGameTime(),holderPresent(server,data.row)))
            data.setDirty();
    }
    public static String status(MinecraftServer server,java.util.UUID viewer){
        var row=get(server).row;
        if(row.isEmpty())return "The well arena is available.";
        if(row.getBoolean("Blocked")||!row.hasUUID("Owner")||!row.hasUUID("Door")||!row.hasUUID("Hand"))
            return "The original Hand reservation needs recovery. Its identity and custody records were preserved.";
        var owner=row.getUUID("Owner");
        var scene=VisitData.get(server).scenes.get(owner);
        if(scene==null||!scene.door.equals(row.getUUID("Door")))
            return "The original owner's well checkpoint is unavailable. The reservation is preserved for recovery.";
        var root=scene.loop.story.getCompound("PinkPalaceJourney");
        var well=PalaceWellProgress.load(owner,scene.door,root.getCompound("Well"));
        if(!well.blocker().isEmpty())return "The original Hand checkpoint needs recovery: "+well.blocker();
        var player=server.getPlayerList().getPlayer(owner);
        if(owner.equals(viewer))return "Your original Hand encounter is at "+well.phase()+". Use /coralinepalace well-resume at the well.";
        String who=player==null?"the offline original owner":player.getGameProfile().getName();
        return "The well arena is reserved for "+who+" at "+well.phase()+". After five minutes away, another story can use the arena without changing this encounter.";
    }
    public static boolean reservedFor(MinecraftServer server,VisitData.Scene scene,PalaceWellProgress progress){
        return get(server).matches(scene,progress);
    }
    public static boolean permits(MinecraftServer server,VisitData.Scene scene,PalaceWellProgress progress){
        if(progress.phase()==PalaceWellProgress.Phase.COMPLETE){
            var witness=InfiniteWorldData.get(server).completionWitness(scene.owner);
            if(witness.hasUUID("Door")&&scene.door.equals(witness.getUUID("Door"))
                    &&witness.hasUUID("WellSession")&&progress.session().equals(witness.getUUID("WellSession")))return true;
        }
        return claim(server,scene,progress);
    }
    static void releaseCompleted(MinecraftServer server,VisitData.Scene scene,PalaceWellProgress progress){
        var data=get(server);
        if(progress.phase()!=PalaceWellProgress.Phase.COMPLETE||!data.matches(scene,progress)
                ||!progress.session().equals(data.row.getUUID("Session")))return;
        data.row.getAllKeys().stream().toList().forEach(data.row::remove);
        data.durable=false;data.checkpoint(server);
    }
    static boolean retire(MinecraftServer server,java.util.UUID generation,java.util.UUID reset){
        if(!TrueHeartBattle.authorizes(server,generation,reset))return false;
        var data=get(server);data.durable=false;
        for(String key:java.util.Set.copyOf(data.row.getAllKeys()))data.row.remove(key);
        return data.checkpoint(server);
    }
    @Override public CompoundTag save(CompoundTag target){target.merge(row);return target;}
}
