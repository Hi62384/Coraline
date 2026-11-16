package com.coraline.postgame;

import java.util.*;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** A real, independently seeded level is created only after canonical unlock.
 * Epoch-specific dimension paths leave retirement/recovery possible without
 * deleting old chunks or swapping the generator beneath a live world. */
@Mod.EventBusSubscriber(modid="coraline")
public final class InfiniteRealm {
    public enum Entrance { WELL, PALACE }
    public static final ResourceLocation TYPE=ResourceLocation.fromNamespaceAndPath("coraline","infinite_other_world");
    private static final String PREFIX="infinite_other_world/";
    private static final Map<MinecraftServer,String> PROBLEMS=new WeakHashMap<>();
    private static final Map<MinecraftServer,Map<UUID,Long>> COOLDOWNS=new WeakHashMap<>();
    private static final Map<MinecraftServer,Map<UUID,Long>> MESSAGES=new WeakHashMap<>();
    private static final Set<MinecraftServer> CLOSING=Collections.newSetFromMap(new WeakHashMap<>());
    static ResourceKey<Level> key(UUID id){return ResourceKey.create(Registries.DIMENSION,ResourceLocation.fromNamespaceAndPath("coraline",PREFIX+id));}
    static Optional<UUID> identity(Level level){
        var id=level.dimension().location();if(!id.getNamespace().equals("coraline")||!id.getPath().startsWith(PREFIX))return Optional.empty();
        try{String value=id.getPath().substring(PREFIX.length());var uuid=UUID.fromString(value);return uuid.toString().equals(value)?Optional.of(uuid):Optional.empty();}catch(IllegalArgumentException malformed){return Optional.empty();}
    }
    public static boolean isRealm(Level level){return identity(level).isPresent();}
    static Optional<ServerLevel> ensure(MinecraftServer server){
        if(!server.isSameThread()||CLOSING.contains(server))return Optional.empty();
        var saved=InfiniteWorldData.get(server).state();if(!saved.active()||saved.generation().isEmpty())return Optional.empty();var id=saved.generation().orElseThrow();
        var existing=server.getLevel(key(id));
        if(existing!=null){
            if(existing.getChunkSource().getGenerator() instanceof InfiniteChunkGenerator generator){try{var bound=generator.structureGeneration();if(bound.id().equals(id)&&bound.seed()==saved.seed().orElseThrow())return Optional.of(existing);}catch(IllegalStateException unbound){}}
            PROBLEMS.put(server,"The saved Infinite Other World does not match its generation");return Optional.empty();
        }
        if(PROBLEMS.containsKey(server))return Optional.empty();ServerLevel created=null;
        try{
            var binding=InfiniteGeneration.checkpoint(server).orElseThrow(()->new IllegalStateException("No durable independent seed"));
            var ops=RegistryOps.create(JsonOps.INSTANCE,server.registryAccess());InfiniteChunkGenerator generator;
            try(var reader=server.getResourceManager().getResourceOrThrow(ResourceLocation.fromNamespaceAndPath("coraline","postgame/terrain.json")).openAsReader()){
                generator=InfiniteChunkGenerator.CODEC.parse(ops,JsonParser.parseReader(reader)).getOrThrow(false,s->{});
            }
            if(!generator.bind(binding))throw new IllegalStateException("Generation binding was refused");
            var type=server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DIMENSION_TYPE,TYPE));
            var access=(com.coraline.mixin.InfiniteServerAccess)server;
            var progress=new net.minecraft.server.level.progress.ChunkProgressListener(){public void updateSpawnPos(net.minecraft.world.level.ChunkPos p){}public void onStatusChange(net.minecraft.world.level.ChunkPos p,net.minecraft.world.level.chunk.ChunkStatus status){}public void start(){}public void stop(){}};
            created=new ServerLevel(server,access.coraline$infiniteExecutor(),access.coraline$infiniteStorage(),new DerivedLevelData(server.getWorldData(),(net.minecraft.world.level.storage.ServerLevelData)server.overworld().getLevelData()),key(id),new LevelStem(type,generator),progress,false,binding.seed(),List.of(),false,null);
            server.forgeGetWorldMap().put(key(id),created);server.markWorldsDirty();
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.level.LevelEvent.Load(created));return Optional.of(created);
        }catch(Exception failure){
            PROBLEMS.put(server,"The Infinite Other World could not be opened safely");
            if(created!=null){server.forgeGetWorldMap().remove(key(id),created);server.markWorldsDirty();try{created.close();}catch(java.io.IOException ignored){}}
            com.mojang.logging.LogUtils.getLogger().error("Infinite Other World creation stopped without choosing a replacement seed",failure);return Optional.empty();
        }
    }
    static boolean authorized(ServerPlayer p){return p.isAlive()&&!p.isSpectator()&&InfiniteWorldData.get(p.server).accessible();}
    static Optional<BlockPos> well(MinecraftServer server){var sites=com.coraline.palace.PalaceSiteData.get(server);return sites.ready()&&sites.generated(com.coraline.palace.PalaceSitePlan.Kind.WELL)?sites.position(com.coraline.palace.PalaceSitePlan.Kind.WELL):Optional.empty();}
    static Optional<BlockPos> palaceDoor(MinecraftServer server){var sites=com.coraline.palace.PalaceSiteData.get(server);return sites.ready()&&sites.generated(com.coraline.palace.PalaceSitePlan.Kind.PALACE)?sites.position(com.coraline.palace.PalaceSitePlan.Kind.PALACE).map(com.coraline.otherworld.RosewoodHouse::palaceLittleDoor):Optional.empty();}
    static void message(ServerPlayer p,String key){
        long now=p.server.overworld().getGameTime();var times=MESSAGES.computeIfAbsent(p.server,s->new HashMap<>());long before=times.getOrDefault(p.getUUID(),Long.MIN_VALUE/2);
        if(now-before<40&&now>=before)return;times.put(p.getUUID(),now);p.displayClientMessage(Component.translatable("message.coraline.infinite."+key),true);
    }
    private static boolean cooling(ServerPlayer p){return p.server.overworld().getGameTime()<COOLDOWNS.computeIfAbsent(p.server,s->new HashMap<>()).getOrDefault(p.getUUID(),Long.MIN_VALUE);}
    private static void cooldown(ServerPlayer p){COOLDOWNS.computeIfAbsent(p.server,s->new HashMap<>()).put(p.getUUID(),p.server.overworld().getGameTime()+100);}
    static void enter(ServerPlayer p,Entrance entrance){
        if(cooling(p)||p.level()!=p.server.overworld()||!authorized(p)||p.isPassenger()||p.getCamera()!=p)return;
        var source=entrance==Entrance.WELL?well(p.server):palaceDoor(p.server);if(source.isEmpty()||p.distanceToSqr(source.orElseThrow().getCenter())>25)return;
        var target=ensure(p.server);if(target.isEmpty()){message(p,"unavailable");return;}
        var level=target.orElseThrow();var id=identity(level).orElseThrow();var data=InfiniteTransitData.get(p.server,id);
        InfiniteArrival.request(level);
        if(!data.problem().isEmpty()){message(p,"save_paused");return;}
        if(!InfiniteArrival.ready(level,data)||!BreachWorldData.get(p.server).view().active()){message(p,"preparing");return;}
        var arrival=data.anchor().orElseThrow().above().south();
        if(!com.coraline.tycoon.TycoonWorld.safeFeet(level,arrival)){message(p,"arrival_blocked");return;}
        var back=entrance==Entrance.PALACE&&!p.blockPosition().equals(source.orElseThrow())&&com.coraline.tycoon.TycoonWorld.safeFeet(p.server.overworld(),p.blockPosition())?p.blockPosition():com.coraline.tycoon.TycoonWorld.landing(p.server.overworld(),entrance==Entrance.WELL?source.orElseThrow().above(2):source.orElseThrow().east(2));
        if(back==null){message(p,"return_blocked");return;}
        if(!data.enter(p.server,p.getUUID(),back)){message(p,"save_paused");return;}
        cooldown(p);p.closeContainer();p.stopRiding();p.fallDistance=0;p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        p.teleportTo(level,arrival.getX()+.5,arrival.getY(),arrival.getZ()+.5,180,0);p.playNotifySound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.AMBIENT,.8F,.6F);
    }
    public static void leave(ServerPlayer p,boolean emergency){
        var id=identity(p.level());if(id.isEmpty()||!p.isAlive()||!emergency&&cooling(p))return;
        returnFrom(p,id.orElseThrow());
    }
    private static void returnFrom(ServerPlayer p,UUID generation){
        var data=InfiniteTransitData.get(p.server,generation);var overworld=p.server.overworld();BlockPos at=null;
        var back=data.back(p.getUUID());if(back.isPresent()){overworld.getChunkAt(back.orElseThrow());at=com.coraline.tycoon.TycoonWorld.safeFeet(overworld,back.orElseThrow())?back.orElseThrow():com.coraline.tycoon.TycoonWorld.landing(overworld,back.orElseThrow());}
        if(at==null&&p.getRespawnDimension().equals(Level.OVERWORLD)&&p.getRespawnPosition()!=null)at=com.coraline.tycoon.TycoonWorld.landing(overworld,p.getRespawnPosition());
        if(at==null)at=com.coraline.tycoon.TycoonWorld.landing(overworld,overworld.getSharedSpawnPos());if(at==null)at=com.coraline.tycoon.TycoonWorld.emergencyReturn(overworld);
        if(at==null){message(p,"return_blocked");return;}
        WybieRescue.portalReturn(p,overworld,at);
        cooldown(p);p.closeContainer();p.stopRiding();p.fallDistance=0;p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);p.teleportTo(overworld,at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot(),0);
        if(back.isPresent())data.leave(p.server,p.getUUID());
    }
    public static InteractionResult doorUse(Level level,BlockPos at,Player player){
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(!(player instanceof ServerPlayer p)||!(level.getBlockEntity(at) instanceof com.coraline.otherworld.DoorBlockEntity door))return InteractionResult.CONSUME;
        var state=InfiniteWorldData.get(p.server).state();var id=door.postgameGeneration();
        if(level!=p.server.overworld()||id==null||!state.active()||!state.generation().orElseThrow().equals(id)||!palaceDoor(p.server).filter(at::equals).isPresent()||!InfiniteTransitData.get(p.server,id).ownsPalaceDoor(at)){message(p,"unavailable");return InteractionResult.CONSUME;}
        if(!authorized(p)){message(p,"ending_required");return InteractionResult.CONSUME;}
        var block=level.getBlockState(at);boolean opened=!block.getValue(com.coraline.block.TinyDoorBlock.OPEN);level.setBlock(at,block.setValue(com.coraline.block.TinyDoorBlock.PORTAL,true).setValue(com.coraline.block.TinyDoorBlock.OPEN,opened),3);
        level.playSound(null,at,opened?net.minecraft.sounds.SoundEvents.WOODEN_DOOR_OPEN:net.minecraft.sounds.SoundEvents.WOODEN_DOOR_CLOSE,net.minecraft.sounds.SoundSource.BLOCKS,.8F,1);return InteractionResult.CONSUME;
    }
    public static void doorContact(Player player,BlockPos at){if(player instanceof ServerPlayer p&&palaceDoor(p.server).filter(at::equals).isPresent()&&p.level().getBlockEntity(at) instanceof com.coraline.otherworld.DoorBlockEntity door&&Objects.equals(door.postgameGeneration(),InfiniteWorldData.get(p.server).state().generation().orElse(null)))enter(p,Entrance.PALACE);}
    @SubscribeEvent public static void started(ServerStartedEvent event){ensure(event.getServer());}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||event.getServer().getTickCount()%20!=0)return;var server=event.getServer();var level=ensure(server);level.ifPresent(InfiniteArrival::tick);
        if(!InfiniteWorldData.get(server).active())return;var well=well(server);if(well.isPresent()&&server.overworld().hasChunkAt(well.orElseThrow())){
            var at=well.orElseThrow();server.overworld().sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,at.getX()+.5,at.getY()+.7,at.getZ()+.5,7,.7,.3,.7,.015);
        }
    }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p)||!p.isAlive())return;
        if(isRealm(p.level())){if(!authorized(p))leave(p,true);return;}
        if(p.level()!=p.server.overworld()||!authorized(p)||cooling(p)||p.isPassenger()||p.getCamera()!=p)return;var well=well(p.server);if(well.isEmpty())return;var at=well.orElseThrow();
        double dx=p.getX()-at.getX()-.5,dz=p.getZ()-at.getZ()-.5;if(dx*dx+dz*dz>2.7||p.getY()>at.getY()+1.2||p.getY()<at.getY()-5)return;
        // Hold an entrant at the well mouth while a bounded asynchronous chunk
        // request completes. Walking out remains possible; no control lock.
        p.fallDistance=0;if(p.getY()<at.getY()+.9)p.connection.teleport(p.getX(),at.getY()+1,p.getZ(),p.getYRot(),p.getXRot());p.setDeltaMovement(p.getDeltaMovement().multiply(1,0,1));enter(p,Entrance.WELL);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer p){cooldown(p);if(isRealm(p.level())&&!authorized(p))leave(p,true);else if(p.level()==p.server.overworld()){var state=InfiniteWorldData.get(p.server).state();state.generation().ifPresent(id->{if(InfiniteTransitData.get(p.server,id).inside(p.getUUID()))returnFrom(p,id);});}}}
    @SubscribeEvent public static void stopping(ServerStoppingEvent event){CLOSING.add(event.getServer());InfiniteArrival.stopping(event.getServer());}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){var s=event.getServer();PROBLEMS.remove(s);COOLDOWNS.remove(s);MESSAGES.remove(s);CLOSING.remove(s);}
    private InfiniteRealm(){}
}
