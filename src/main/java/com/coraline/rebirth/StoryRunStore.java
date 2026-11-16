package com.coraline.rebirth;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Small per-owner atomic journal. Corrupt/ambiguous IO blocks mutation, never silently resets a run.
 * Minecraft player/entity files remain separate: adapters must checkpoint/read back before advancing.
 */
@Mod.EventBusSubscriber(modid="coraline")
public final class StoryRunStore {
    private static final Map<MinecraftServer,Map<UUID,StoryRun>> CACHE=new WeakHashMap<>();
    private static Path directory(MinecraftServer server)throws IOException{
        var dir=server.getWorldPath(LevelResource.ROOT).resolve("data/coraline_story_runs");
        if(Files.isSymbolicLink(dir)||Files.isSymbolicLink(dir.getParent()))throw new IOException("Symlinked story journal");
        Files.createDirectories(dir);return dir;
    }
    public static CompoundTag read(Path file)throws IOException{
        if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)||Files.size(file)>8*1024*1024)throw new IOException("Invalid checkpoint file");
        try(var in=new DataInputStream(new BufferedInputStream(new java.util.zip.GZIPInputStream(Files.newInputStream(file))))){
            return NbtIo.read(in,new NbtAccounter(32L*1024*1024));
        }
    }
    private static StoryRun current(MinecraftServer server,UUID owner)throws IOException{
        var records=CACHE.computeIfAbsent(server,s->new HashMap<>());var existing=records.get(owner);
        if(existing!=null)return existing;var path=directory(server).resolve(owner+".dat");
        try{
            if(Files.exists(path,LinkOption.NOFOLLOW_LINKS))existing=StoryRun.load(owner,read(path));
            else if(Files.notExists(path,LinkOption.NOFOLLOW_LINKS))existing=StoryRun.first(owner);
            else throw new IOException("Owner journal existence is ambiguous; no fresh run allocated");
        }
        catch(RuntimeException bad){throw new IOException("Story journal requires review; no fresh ledger allocated",bad);}
        records.put(owner,existing);return existing;
    }
    public static StoryRun get(MinecraftServer server,UUID owner)throws IOException{return current(server,owner).copy();}
    public record Status(long run,boolean active,boolean complete,boolean pending,boolean claimed){}
    public static Status status(MinecraftServer server,UUID owner)throws IOException{
        var run=current(server,owner);return new Status(run.run(),run.active(),run.complete(),run.rewardPending(),run.claimed());
    }
    public static boolean ritualParent(MinecraftServer server,UUID owner,UUID family,UUID parent,boolean mother)throws IOException{
        var run=current(server,owner);return run.active()&&family.equals(run.id("Family"))&&parent.equals(run.id(mother?"Mother":"Father"));
    }
    /** Hot parent ticks do not copy a completed chapter's NBT snapshot. */
    public static boolean tombstoned(MinecraftServer server,UUID owner,UUID parent)throws IOException{return current(server,owner).tombstoned(parent);}
    public static boolean ownsProp(MinecraftServer server,UUID owner,UUID prop)throws IOException{
        var details=current(server,owner).details();
        for(String key:new String[]{"Portrait","Circle","FirstLife","FinalLife"})if(details.hasUUID(key)&&prop.equals(details.getUUID(key)))return true;
        return false;
    }
    public static void save(MinecraftServer server,StoryRun run)throws IOException{
        StoryRun.load(run.owner(),run.save()); // revalidate before touching a durable record
        var dir=directory(server);var target=dir.resolve(run.owner()+".dat");
        if(Files.isSymbolicLink(target))throw new IOException("Symlinked owner journal");
        var temp=Files.createTempFile(dir,run.owner()+"-",".pending");
        try{
            NbtIo.writeCompressed(run.save(),temp.toFile());
            try(var channel=FileChannel.open(temp,StandardOpenOption.WRITE)){channel.force(true);}
            if(!run.save().equals(read(temp)))throw new IOException("Story journal read-back mismatch");
            // Do not fall back to truncate/copy on platforms without atomic replacement.
            Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            CACHE.computeIfAbsent(server,s->new HashMap<>()).put(run.owner(),run.copy());
        }finally{Files.deleteIfExists(temp);}
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){CACHE.remove(e.getServer());}
    private StoryRunStore(){}
}
