package com.coraline.rebirth;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import net.minecraft.Util;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Proves absence from native entity storage before upgrading a legacy admission. */
@Mod.EventBusSubscriber(modid="coraline")
public final class LegacyActorRecovery {
    public enum Result { WAITING, PRESENT, ABSENT, UNVERIFIED }
    private static final Pattern REGION=Pattern.compile("r[.](-?\\d+)[.](-?\\d+)[.]mca");
    private static final Map<MinecraftServer,Search> SEARCHES=new WeakHashMap<>();

    static final class Observation {
        private final UUID target;
        private volatile boolean seen;
        Observation(UUID target){this.target=target;}
        void joined(UUID actor){if(target.equals(actor))seen=true;}
        Result decide(Result disk){return seen?Result.PRESENT:disk;}
    }

    private record Search(UUID actor,Observation observation,CompletableFuture<Result> disk,long retryAfter){}

    public static Result check(MinecraftServer server,UUID actor){
        for(var level:server.getAllLevels())if(level.getEntity(actor)!=null)return Result.PRESENT;
        long now=server.overworld().getGameTime();
        var search=SEARCHES.get(server);
        if(search!=null){
            if(!search.disk.isDone())return Result.WAITING;
            if(search.actor.equals(actor)){
                var result=search.observation.decide(search.disk.join());
                if(result==Result.ABSENT){
                    SEARCHES.remove(server);
                    return result;
                }
                if(now<search.retryAfter)return result;
            }else if(now<search.retryAfter-400)return Result.WAITING;
        }
        if(!server.saveEverything(true,true,true))return Result.UNVERIFIED;
        var root=server.getWorldPath(LevelResource.ROOT);
        var observation=new Observation(actor);
        var disk=CompletableFuture.supplyAsync(()->inspect(root,actor),Util.backgroundExecutor());
        SEARCHES.put(server,new Search(actor,observation,disk,now+1200));
        return Result.WAITING;
    }

    @SubscribeEvent public static void joining(EntityJoinLevelEvent event){
        if(event.getLevel() instanceof ServerLevel level){
            var search=SEARCHES.get(level.getServer());
            if(search!=null)search.observation.joined(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent public static void stopping(ServerStoppingEvent event){
        SEARCHES.remove(event.getServer());
    }

    static Result inspect(Path root,UUID actor){
        try{
            if(Files.isSymbolicLink(root))return Result.UNVERIFIED;
            int directories=0,regions=0;
            long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
            if(playerVehicles(root.resolve("playerdata"),actor,deadline))return Result.PRESENT;
            try(var paths=Files.walk(root)){
                var iterator=paths.iterator();
                int visited=0;
                while(iterator.hasNext()){
                    if(++visited>65536||System.nanoTime()>=deadline)return Result.UNVERIFIED;
                    var folder=iterator.next();
                    if(Files.isSymbolicLink(folder))return Result.UNVERIFIED;
                    if(!"entities".equals(folder.getFileName().toString()))continue;
                    if(!Files.isDirectory(folder))return Result.UNVERIFIED;
                    if(++directories>256)return Result.UNVERIFIED;
                    try(var files=Files.newDirectoryStream(folder,"*.mca")){
                        for(var region:files){
                            if(++regions>4096||!Files.isRegularFile(region,LinkOption.NOFOLLOW_LINKS))return Result.UNVERIFIED;
                            if(contains(region,actor,deadline))return Result.PRESENT;
                        }
                    }
                }
            }
            return Result.ABSENT;
        }catch(IOException|RuntimeException unreadable){
            return Result.UNVERIFIED;
        }
    }

    private static boolean playerVehicles(Path folder,UUID actor,long deadline)throws IOException{
        if(Files.notExists(folder,LinkOption.NOFOLLOW_LINKS))return false;
        if(Files.isSymbolicLink(folder))throw new IOException("Unverified player storage");
        int players=0;
        try(var saves=Files.newDirectoryStream(folder,"*.dat")){
            for(var save:saves){
                if(++players>4096||System.nanoTime()>=deadline||Files.isSymbolicLink(save))throw new IOException("Unverified player save");
                var player=StoryRunStore.read(save);
                if(!player.contains("RootVehicle"))continue;
                if(!player.contains("RootVehicle",Tag.TAG_COMPOUND))throw new IOException("Unknown saved vehicle");
                var vehicle=player.getCompound("RootVehicle").get("Entity");
                if(!(vehicle instanceof CompoundTag entity))throw new IOException("Missing saved vehicle entity");
                if(containsActor(entity,actor))return true;
            }
        }
        return false;
    }

    private static boolean contains(Path region,UUID actor,long deadline)throws IOException{
        var name=REGION.matcher(region.getFileName().toString());
        if(!name.matches())throw new IOException("Invalid entity region path");
        int regionX=Integer.parseInt(name.group(1)),regionZ=Integer.parseInt(name.group(2));
        try(var file=new RandomAccessFile(region.toFile(),"r")){
            if(file.length()<8192)throw new IOException("Incomplete entity region header");
            var header=new int[1024];
            for(int i=0;i<header.length;i++)header[i]=file.readInt();
            for(int i=0;i<header.length;i++){
                if(System.nanoTime()>=deadline)throw new IOException("Entity scan exceeded its time budget");
                int address=header[i];
                if(address==0)continue;
                long offset=(address>>>8)*4096L;
                int capacity=(address&255)*4096;
                if(offset<8192||capacity<5||offset+capacity>file.length())throw new IOException("Invalid entity sector");
                file.seek(offset);
                int length=file.readInt();
                int compression=file.readUnsignedByte();
                if(length<1||length>capacity-4)throw new IOException("Invalid entity payload");
                InputStream payload;
                if((compression&128)!=0){
                    var external=region.resolveSibling("c."+(regionX*32+i%32)+"."+(regionZ*32+i/32)+".mcc");
                    if(Files.isSymbolicLink(external)||Files.size(external)>32L*1024*1024)throw new IOException("Unverified external payload");
                    payload=Files.newInputStream(external);
                }else{
                    var bytes=new byte[length-1];
                    file.readFully(bytes);
                    payload=new ByteArrayInputStream(bytes);
                }
                try(payload){
                    var decoded=switch(compression&127){
                        case 1->new GZIPInputStream(payload);
                        case 2->new InflaterInputStream(payload);
                        case 3->payload;
                        default->throw new IOException("Unknown entity compression");
                    };
                    try(var input=new DataInputStream(decoded)){
                        var chunk=NbtIo.read(input,new NbtAccounter(32L*1024*1024));
                        if(chunk==null||!chunk.contains("Entities",Tag.TAG_LIST))throw new IOException("Missing entity list");
                        if(containsActors((ListTag)chunk.get("Entities"),actor))return true;
                    }
                }
            }
            file.seek(0);
            for(int address:header)if(file.readInt()!=address)throw new IOException("Entity region changed during recovery");
        }
        return false;
    }

    private static boolean containsActors(ListTag entries,UUID actor)throws IOException{
        for(var entry:entries){
            if(!(entry instanceof CompoundTag entity))throw new IOException("Unknown entity entry");
            if(containsActor(entity,actor))return true;
        }
        return false;
    }

    private static boolean containsActor(CompoundTag entity,UUID actor)throws IOException{
        if(!entity.hasUUID("UUID"))throw new IOException("Unknown entity identity");
        if(actor.equals(entity.getUUID("UUID")))return true;
        if(!entity.contains("Passengers"))return false;
        if(!(entity.get("Passengers") instanceof ListTag passengers))throw new IOException("Unknown passenger data");
        return containsActors(passengers,actor);
    }

    private LegacyActorRecovery(){}
}
