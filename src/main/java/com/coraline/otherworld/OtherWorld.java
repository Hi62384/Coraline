package com.coraline.otherworld;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.entity.Pose;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.coraline.block.TinyDoorBlock;
import com.coraline.config.CoralineConfig;
import com.coraline.registry.*;

@Mod.EventBusSubscriber(modid="coraline")
public final class OtherWorld {
    public static final ResourceKey<Level> KEY=ResourceKey.create(Registries.DIMENSION,ResourceLocation.fromNamespaceAndPath("coraline","other_world"));
    private static final Map<MinecraftServer,Map<UUID,RealmCopy>> jobs=new WeakHashMap<>();
    public static void cancelCopy(MinecraftServer server,UUID door){var queue=jobs.get(server);if(queue!=null){var job=queue.remove(door);if(job!=null)job.cancel();}}
    @SubscribeEvent public static void stopping(net.minecraftforge.event.server.ServerStoppingEvent event){
        var level=event.getServer().getLevel(KEY);if(level!=null)OtherWorldEstate.clearWhite(level);
    }
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event){jobs.remove(event.getServer());}
    public static boolean isOther(Level level){return level.dimension().equals(KEY);}
    public static boolean night(Level level){long time=Math.floorMod(level.getDayTime(),24000);return time>=13000&&time<23000;}
    public static boolean active(ServerLevel level,DoorData.Door door){return door.exists&&door.placed&&night(level)&&Math.floorDiv(level.getDayTime(),24000)!=door.closedNight&&level.getDayTime()<door.activeUntil;}
    public static void arrived(ServerLevel level,DoorData.Door door){
        if(Math.floorDiv(level.getDayTime(),24000)==door.closedNight)return;
        if(!night(level))return;
        door.activeUntil=(Math.floorDiv(level.getDayTime(),24000)+1)*24000;DoorData.get(level.getServer()).setDirty();setPortal(level,door.pos,true);
        var state=level.getBlockState(door.pos);if(state.is(ModBlocks.TINY_DOOR.get()))level.setBlock(door.pos,state.setValue(TinyDoorBlock.OPEN,false),3);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,door.pos.getX()+.5,door.pos.getY()+.5,door.pos.getZ()+.5,35,.3,.3,.3,.02);
    }
    private static void setPortal(ServerLevel level,BlockPos pos,boolean active){
        var state=level.getBlockState(pos);if(state.is(ModBlocks.TINY_DOOR.get())&&state.getValue(TinyDoorBlock.PORTAL)!=active)level.setBlock(pos,active?state.setValue(TinyDoorBlock.PORTAL,true):state.setValue(TinyDoorBlock.PORTAL,false).setValue(TinyDoorBlock.OPEN,false),3);
    }
    public static void expireNight(ServerLevel level,DoorData.Door door){
        if(door.activeUntil<=0||night(level)&&level.getDayTime()<door.activeUntil)return;
        SceneAudio.unusedNightEnded(level.getServer(),door);
        door.activeUntil=0;DoorData.get(level.getServer()).setDirty();
        if(level.hasChunkAt(door.pos))setPortal(level,door.pos,false);
    }
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return;
        var queue=jobs.get(server);
        if(queue!=null){var iterator=queue.entrySet().iterator();while(iterator.hasNext()){
            var job=iterator.next().getValue();boolean done=false;
            for(int n=0;n<CoralineConfig.OTHER_COPY_CHUNKS.get()&&!done;n++)done=job.tick();
            if(job.takeReady()&&!job.failed)finish(server,job.realm);
            if(done)iterator.remove();break;
        }}
        if(server.overworld().getGameTime()%20!=0)return;
        var other=server.getLevel(KEY);if(other!=null)OtherWorldEstate.cleanupWhite(other);
        for(var d:DoorData.get(server).doors.values()){
            DoorData.get(server).audit(d);
            if(!d.exists||!d.placed)continue;
            var level=com.coraline.parents.FamilyData.level(server,d.dimension);if(level==null||isOther(level))continue;
            expireNight(level,d);
            if(!level.hasChunkAt(d.pos))continue;
            if(!(level.getBlockEntity(d.pos) instanceof DoorBlockEntity block)||!d.id.equals(block.doorId)){d.exists=false;d.placed=false;DoorData.get(server).setDirty();continue;}
            boolean portalActive=active(level,d);
            setPortal(level,d.pos,portalActive);
            if(portalActive)doorParticles(level,d.pos,true);
            if(!night(level))continue;
            var owner=server.getPlayerList().getPlayer(d.owner);
            if(owner==null||mouseBlocker(level,owner,d)!=null)continue;
            // Eligibility sparkles remain visible even after this night's roll was used.
            if(!portalActive)doorParticles(level,d.pos,false);
            long night=Math.floorDiv(level.getDayTime(),24000);if(d.night==night)continue;
            var persisted=owner.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
            if(persisted.contains("CoralineMiceNight")&&persisted.getLong("CoralineMiceNight")==night&&persisted.getString("CoralineMiceDimension").equals(d.dimension))continue;
            persisted.putLong("CoralineMiceNight",night);persisted.putString("CoralineMiceDimension",d.dimension);owner.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG,persisted);
            d.night=night;DoorData.get(server).setDirty();
            if(level.random.nextDouble()<CoralineConfig.OTHER_NIGHT_CHANCE.get())SceneAudio.begin(owner,d);
        }
    }
    static String mouseBlocker(ServerLevel level,ServerPlayer owner,DoorData.Door door){
        if(Math.floorDiv(level.getDayTime(),24000)==door.closedNight)return "The Little Door is bricked up for the rest of this night.";
        if(!night(level))return "It must be nighttime (ticks 13000–23000).";
        if(owner.level()!=level||!owner.getRespawnDimension().equals(level.dimension()))return "Your bed and door must be in your current dimension.";
        var bed=owner.getRespawnPosition();
        if(bed==null||!(level.getBlockState(bed).getBlock() instanceof BedBlock))return "Set your respawn point at a normal Minecraft bed first.";
        if(owner.distanceToSqr(bed.getCenter())>Math.pow(CoralineConfig.OTHER_BED_RANGE.get(),2))return "Move within "+CoralineConfig.OTHER_BED_RANGE.get()+" blocks of your bed (flying is fine).";
        if(com.coraline.parents.ParentAccess.family(level.getServer(),owner.getUUID())==null)return "You need living, claimed parents.";
        var data=com.coraline.doll.DollBondData.get(level.getServer());
        var bond=data.bonds.get(owner.getUUID());
        // Inventory copies or a move can leave the cached placement stale. Validate the
        // actual block identity, then repair from loaded block entities near the bed.
        if(bond!=null&&bond.dimension.equals(door.dimension)&&com.coraline.doll.DollHome.nearBed(level,bond.position,owner.getUUID())
            &&level.getBlockEntity(bond.position) instanceof com.coraline.block.entity.DollBlockEntity doll
            &&owner.getUUID().equals(doll.getOwnerId())&&data.current(doll.createDollStack())){
            data.placed(doll.createDollStack(),level,bond.position);return null;
        }
        int radius=CoralineConfig.DOLL_HOME_RADIUS.get();
        for(int x=(bed.getX()-radius)>>4;x<=(bed.getX()+radius)>>4;x++)for(int z=(bed.getZ()-radius)>>4;z<=(bed.getZ()+radius)>>4;z++){
            var chunk=level.getChunkSource().getChunkNow(x,z);if(chunk==null)continue;
            for(var entity:chunk.getBlockEntities().values())if(entity instanceof com.coraline.block.entity.DollBlockEntity doll
                &&owner.getUUID().equals(doll.getOwnerId())&&com.coraline.doll.DollHome.nearBed(level,doll.getBlockPos(),owner.getUUID())&&data.current(doll.createDollStack())){
                data.placed(doll.createDollStack(),level,doll.getBlockPos());return null;
            }
        }
        return "No current bound doll found within "+radius+" blocks (and "+CoralineConfig.DOLL_HOME_HEIGHT.get()+" vertically) of your respawn bed at "+bed.toShortString()+". Check this is YOUR doll and the bed where you set your spawn.";
    }
    public static String debugMice(ServerPlayer player){
        if(!player.isCreative())return "Mouse Event Debug is Creative-only.";
        var level=player.serverLevel();
        var door=DoorData.get(player.server).doors.values().stream()
            .filter(d->d.exists&&d.placed&&player.getUUID().equals(d.owner)&&level.dimension().location().toString().equals(d.dimension))
            .min(Comparator.comparingDouble(d->player.distanceToSqr(d.pos.getCenter()))).orElse(null);
        if(door==null)return "Place a little door yourself in this dimension first.";
        if(!level.hasChunkAt(door.pos)||!(level.getBlockEntity(door.pos) instanceof DoorBlockEntity block)||!door.id.equals(block.doorId))return "The tracked little door is unloaded or missing. Place it nearby again.";
        String blocker=mouseBlocker(level,player,door);if(blocker!=null)return blocker;
        int count=spawnMice(level,player,door);
        if(count>0)SceneAudio.cancelPending(player);
        return count>0?"Spawned "+count+" mice near your bed. Follow them to the little door!":"No safe, reachable ground found near your bed. Clear a route to the door.";
    }
    private static void doorParticles(ServerLevel level,BlockPos pos,boolean active){
        int count=active?CoralineConfig.OTHER_ACTIVE_PARTICLES.get():CoralineConfig.OTHER_ELIGIBLE_PARTICLES.get();
        if(count==0)return;
        var blue=new net.minecraft.core.particles.DustParticleOptions(active?new org.joml.Vector3f(.15F,.65F,1F):new org.joml.Vector3f(.38F,.38F,.38F),active?1F:.6F);
        level.sendParticles(blue,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,count,.42,.42,.42,0);
        if(active)level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,count,.4,.4,.4,.015);
    }
    static int spawnMice(ServerLevel level,ServerPlayer owner,DoorData.Door d){
        BlockPos bed=owner.getRespawnPosition();if(bed==null)return 0;
        int spawned=0;
        int radius=CoralineConfig.OTHER_MOUSE_RADIUS.get(),height=CoralineConfig.OTHER_MOUSE_HEIGHT.get();
        for(int attempt=0;attempt<160&&spawned<CoralineConfig.OTHER_MICE.get();attempt++){
            var column=bed.offset(level.random.nextInt(radius*2+1)-radius,0,level.random.nextInt(radius*2+1)-radius);
            // Search actual floor surfaces near the bed, never the flying player's feet.
            for(int step=0;step<=height*2;step++){
                int dy=step==0?0:(step%2==1?(step+1)/2:-step/2);
                var floor=column.offset(0,dy-1,0);
                if(!level.hasChunkAt(floor)||floor.getY()<level.getMinBuildHeight()||floor.getY()>=level.getMaxBuildHeight()-1)continue;
                var shape=level.getBlockState(floor).getCollisionShape(level,floor);
                if(shape.isEmpty()||!level.getFluidState(floor).isEmpty())continue;
                double y=floor.getY()+shape.max(Direction.Axis.Y);
                var mouse=ModEntities.MOUSE.get().create(level);if(mouse==null)return spawned;
                mouse.setPos(floor.getX()+.5,y,floor.getZ()+.5);mouse.doorId=d.id;
                // Pathfinding must start on this floor, not use the airborne fall search
                // (which mistakes non-air grass/plants for the landing surface).
                mouse.setOnGround(true);
                if(mouse.distanceToSqr(d.pos.getCenter())<9||!level.noCollision(mouse)||level.containsAnyLiquid(mouse.getBoundingBox()))continue;
                var path=mouse.getNavigation().createPath(d.pos,1);if(path==null||!path.canReach())continue;
                if(level.addFreshEntity(mouse)){spawned++;break;}
            }
        }
        if(spawned>0)level.playSound(null,bed,ModSounds.MOUSE_SQUEAK.get(),net.minecraft.sounds.SoundSource.NEUTRAL,.8F,1);
        return spawned;
    }
    public static void enter(ServerPlayer player,DoorBlockEntity block){
        var tag=player.getPersistentData();long now=player.server.overworld().getGameTime();
        if(now<tag.getLong("CoralinePortalCooldown")||block.doorId==null||tag.getBoolean("CoralineInTunnel"))return;
        var d=DoorData.get(player.server).doors.get(block.doorId);if(d==null)return;
        if(isOther(player.level())){
            if(com.coraline.secrets.SecretsRuntime.exitBlocked(player))return;
            if(com.coraline.feast.FirstVisit.locked(player)||com.coraline.feast.OtherMotherLoop.locked(player)){player.displayClientMessage(Component.literal("Your Other Mother wants you to stay a little longer."),true);return;}
            startTunnel(player,d,true);
            return;
        }
        if(tag.hasUUID("CoralineWaitingDoor"))return;
        if(!active(player.serverLevel(),d)||!player.isShiftKeyDown())return;
        var target=player.server.getLevel(KEY);if(target==null){player.displayClientMessage(Component.literal("The Other World is unavailable. Restart the world after installing this update."),true);return;}
        var existing=RealmData.get(player.server).realms.get(d.id);
        if(existing!=null&&existing.ready&&existing.estateVersion>=OtherWorldEstate.VERSION){
            RealmData.expand(existing);RealmData.get(player.server).setDirty();
            if(!existing.islandComplete){
                var pending=jobs.computeIfAbsent(player.server,s->new HashMap<>());
                pending.computeIfAbsent(d.id,id->new RealmCopy(player.serverLevel(),target,d,existing,true));
            }
            var scene=com.coraline.feast.VisitData.get(player.server).scenes.get(d.owner);
            if(scene==null||!d.id.equals(scene.door))finish(player.server,existing);
            startTunnel(player,d,false);return;
        }
        var realm=RealmData.get(player.server).realm(d);
        // The perfected estate supplies its own upstairs story bed.
        var pending=jobs.computeIfAbsent(player.server,s->new HashMap<>());var running=pending.get(d.id);
        if(running!=null&&!running.stillCurrent()){running.cancel();pending.remove(d.id);}
        pending.computeIfAbsent(d.id,id->new RealmCopy(player.serverLevel(),target,d,realm));
        startTunnel(player,d,false);
    }
    static void finish(MinecraftServer server,RealmData.Realm r){
        var target=server.getLevel(KEY);var d=DoorData.get(server).doors.get(r.door);if(target==null||d==null)return;
        var state=target.getBlockState(r.doorPos);if(!state.is(ModBlocks.TINY_DOOR.get()))state=ModBlocks.TINY_DOOR.get().defaultBlockState();
        target.setBlock(r.doorPos,state.setValue(TinyDoorBlock.PORTAL,true).setValue(TinyDoorBlock.OPEN,true),3);
        if(target.getBlockEntity(r.doorPos) instanceof DoorBlockEntity block){block.doorId=d.id;block.setChanged();}
        buildTunnel(target,r);
        var f=com.coraline.parents.ParentAccess.family(server,d.owner);
        // Persist an incarnation number: unloaded old counterparts also retire on load.
        r.parentGeneration++;
        var obsolete=new ArrayList<com.coraline.parents.ParentEntity>();
        for(var entity:target.getAllEntities())if(entity instanceof com.coraline.parents.ParentEntity p&&p.isOtherParent()&&p.getPersistentData().hasUUID("RealmDoor")&&r.door.equals(p.getPersistentData().getUUID("RealmDoor")))obsolete.add(p);
        obsolete.forEach(net.minecraft.world.entity.Entity::discard);
        if(f!=null)for(boolean mom:new boolean[]{true,false}){
            var visit=com.coraline.feast.VisitData.get(server).scenes.get(f.owner);
            if(!mom&&visit!=null&&visit.stage==com.coraline.feast.VisitData.Stage.DONE)continue;
            var source=com.coraline.parents.FamilyData.level(server,mom?f.momDim:f.dadDim);
            if(source!=null)source.getChunkAt(mom?f.momPos:f.dadPos);
            var original=source==null?null:source.getEntity(mom?f.mom:f.dad);
            var echo=(mom?ModEntities.MOM:ModEntities.DAD).get().create(target);if(echo==null)continue;
            echo.makeOtherParent(original instanceof com.coraline.parents.ParentEntity p?p.skin():Math.max(0,mom?f.momSkin:f.dadSkin));
            echo.getPersistentData().putUUID("OriginalFamily",f.id);
            echo.getPersistentData().putUUID("RealmDoor",r.door);
            echo.getPersistentData().putInt("RealmGeneration",r.parentGeneration);
            var near=mom?r.doorPos.east(4):(r.piano!=null?r.piano:r.doorPos.north(4));
            echo.getPersistentData().putLong("OtherHome",near.asLong());
            var safe=com.coraline.parents.ParentEvents.safe(target,echo,near,8);if(safe==null)continue;
            echo.setPos(safe.getX()+.5,safe.getY(),safe.getZ()+.5);target.addFreshEntity(echo);
        }
        RealmData.get(server).setDirty();
    }
    private static void buildTunnel(ServerLevel level,RealmData.Realm r){
        var p=r.tunnel();
        // Remove the duplicate corridor left by older saves that anchored it to
        // the estate's relocated Little Door instead of the original entrance.
        var oldDoor=r.doorPos;
        var legacy=new BlockPos((oldDoor.getX()>>4)*16,300,(oldDoor.getZ()>>4)*16+r.radius+64);
        if(!legacy.equals(p)&&level.getBlockState(legacy.below()).is(ModBlocks.TUNNEL_CORE.get()))
            for(int z=-1;z<=18;z++)for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++){
                var old=legacy.offset(x,y,z);var state=level.getBlockState(old);
                if(state.is(ModBlocks.TUNNEL_FABRIC.get())||state.is(ModBlocks.TUNNEL_CORE.get())||
                    state.is(ModBlocks.TINY_DOOR.get()))level.setBlock(old,Blocks.AIR.defaultBlockState(),2);
            }
        holdTunnel(level,r);
        for(int z=-1;z<=18;z++)for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++){
            boolean wall=x!=0||y!=0||z==-1||z==18;
            level.setBlock(p.offset(x,y,z),wall?ModBlocks.TUNNEL_FABRIC.get().defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        }
        level.setBlock(p.below(),ModBlocks.TUNNEL_CORE.get().defaultBlockState(),3);
        var endDoor=ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(TinyDoorBlock.PORTAL,true);
        level.setBlock(p.offset(0,0,17),endDoor,2);
        level.setBlock(p.offset(0,0,-1),endDoor.setValue(TinyDoorBlock.FACING,Direction.SOUTH),2);
    }
    private static void holdTunnel(ServerLevel level,RealmData.Realm r){
        var p=r.tunnel();
        for(int x=(p.getX()-1)>>4;x<=(p.getX()+1)>>4;x++)for(int z=(p.getZ()-1)>>4;z<=(p.getZ()+18)>>4;z++){
            var chunk=new ChunkPos(x,z);
            level.getChunkSource().addRegionTicket(TicketType.PORTAL,chunk,2,p);
            level.getChunk(x,z);
        }
    }
    public static boolean rescueTunnel(ServerPlayer player){
        var tag=player.getPersistentData();if(!com.coraline.feast.ActivityWorld.protectedWorld(player.level())||!tag.hasUUID("CoralineVisitDoor"))return false;
        var door=DoorData.get(player.server).doors.get(tag.getUUID("CoralineVisitDoor"));
        if(door==null||!player.getUUID().equals(door.owner)||!RealmData.get(player.server).realms.containsKey(door.id)||player.server.getLevel(KEY)==null)return false;
        tag.putBoolean("CoralineRecovering",true);
        try{player.stopRiding();player.stopSleepInBed(true,false);tag.remove("CoralineWakeAt");tag.putBoolean("CoralineRecoveryTunnel",true);startTunnel(player,door,true);}
        finally{tag.remove("CoralineRecovering");}
        return true;
    }
    static void startTunnel(ServerPlayer player,DoorData.Door door,boolean returning){
        var r=RealmData.get(player.server).realms.get(door.id);var target=player.server.getLevel(KEY);if(r==null||target==null)return;
        buildTunnel(target,r);
        var tag=player.getPersistentData();tag.putUUID("CoralineVisitDoor",door.id);tag.putBoolean("CoralineInTunnel",true);tag.putBoolean("CoralineReturning",returning);tag.putLong("CoralinePortalCooldown",player.server.overworld().getGameTime()+40);
        tag.putLong("CoralineTunnelStartedAt",player.server.overworld().getGameTime());
        player.setForcedPose(Pose.SWIMMING);player.setPose(Pose.SWIMMING);var p=r.tunnel();player.teleportTo(target,p.getX()+.5,p.getY(),p.getZ()+(returning?16.5:.5),returning?180:0,0);
        player.fallDistance=0;player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        SceneAudio.tunnelEntered(player,door);
        // Push the actual corridor chunks immediately; do not wait for view-distance streaming.
        for(int x=(p.getX()-1)>>4;x<=(p.getX()+1)>>4;x++)for(int z=(p.getZ()-1)>>4;z<=(p.getZ()+18)>>4;z++)
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket(target.getChunk(x,z),target.getChunkSource().getLightEngine(),null,null));
    }
    public static void leave(ServerPlayer player,boolean bed){
        if(com.coraline.secrets.SecretsRuntime.exitBlocked(player))return;
        player.getPersistentData().remove("CoralineDebugVisitCompleted");
        com.coraline.feast.OtherMotherLoop.depart(player,bed);
        var tag=player.getPersistentData();var id=tag.hasUUID("CoralineVisitDoor")?tag.getUUID("CoralineVisitDoor"):null;
        var d=id==null?null:DoorData.get(player.server).doors.get(id);
        var source=d==null?player.server.overworld():com.coraline.parents.FamilyData.level(player.server,d.dimension);if(source==null)source=player.server.overworld();
        BlockPos pos=d==null?source.getSharedSpawnPos():d.pos;
        var r=id==null?null:RealmData.get(player.server).realms.get(id);
        if(bed&&player.getRespawnPosition()!=null&&player.getRespawnDimension().equals(source.dimension()))pos=player.getRespawnPosition();
        else if(bed&&r!=null&&r.bed!=null)pos=r.bed.offset(-r.dx,0,-r.dz);
        var safe=safePlayer(source,pos);player.setForcedPose(null);player.teleportTo(source,safe.getX()+.5,safe.getY(),safe.getZ()+.5,player.getYRot(),0);
        com.coraline.feast.ShoppingMallActivity.deliver(player);
        if(d!=null&&tag.getBoolean("CoralineReturning")){
            d.closedNight=Math.floorDiv(source.getDayTime(),24000);d.activeUntil=0;DoorData.get(player.server).setDirty();
            var state=source.getBlockState(d.pos);if(state.is(ModBlocks.TINY_DOOR.get()))source.setBlock(d.pos,state.setValue(TinyDoorBlock.PORTAL,false).setValue(TinyDoorBlock.OPEN,false),3);
        }
        tag.remove("CoralineRecoveryTunnel");tag.remove("CoralineVisitDoor");tag.remove("CoralineInTunnel");tag.remove("CoralineReturning");tag.remove("CoralineTunnelStartedAt");tag.putLong("CoralinePortalCooldown",player.server.overworld().getGameTime()+60);
        if(d!=null)setPortal(source,d.pos,active(source,d));
    }
    public static BlockPos safePlayer(ServerLevel level,BlockPos near){
        level.getChunkAt(near);
        var state=level.getBlockState(near);
        if(state.is(ModBlocks.TINY_DOOR.get())){
            var front=near.relative(state.getValue(TinyDoorBlock.FACING));
            if(level.isEmptyBlock(front)&&level.isEmptyBlock(front.above())&&level.getBlockState(front.below()).isFaceSturdy(level,front.below(),Direction.UP))return front;
        }
        for(int radius=0;radius<=8;radius++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)for(int step=0;step<=16;step++){
            int y=step==0?0:((step+1)/2)*(step%2==1?1:-1);
            var p=near.offset(x,y,z);if(level.isEmptyBlock(p)&&level.isEmptyBlock(p.above())&&level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP))return p;
        }
        return level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,near);
    }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player))return;
        var tag=player.getPersistentData();
        if(tag.hasUUID("CoralineWaitingDoor")){
            var id=tag.getUUID("CoralineWaitingDoor");var d=DoorData.get(player.server).doors.get(id);var r=RealmData.get(player.server).realms.get(id);
            if(d==null||isOther(player.level())||!active(player.serverLevel(),d)||player.distanceToSqr(d.pos.getCenter())>64){tag.remove("CoralineWaitingDoor");}
            else if(!jobs.getOrDefault(player.server,Map.of()).containsKey(id)){
                tag.remove("CoralineWaitingDoor");if(r!=null&&r.ready)startTunnel(player,d,false);
                else player.displayClientMessage(Component.literal("The tunnel could not finish weaving. You are safe here; try again."),true);
            }
            else if(player.tickCount%100==0)player.displayClientMessage(Component.literal("Weaving the Other World: "+jobs.get(player.server).get(id).progress()+"%"),true);
        }
        if(!isOther(player.level()))return;
        if(!tag.hasUUID("CoralineVisitDoor")){leave(player,false);return;}
        var r=RealmData.get(player.server).realms.get(tag.getUUID("CoralineVisitDoor"));if(r==null){leave(player,false);return;}
        if(tag.getBoolean("CoralineInTunnel")){
            player.setForcedPose(Pose.SWIMMING);var p=r.tunnel();boolean back=tag.getBoolean("CoralineReturning");
            player.fallDistance=0;
            if(player.tickCount%20==0)holdTunnel(player.serverLevel(),r);
            if(player.getY()<p.getY()-.2||player.getY()>p.getY()+1||Math.abs(player.getX()-(p.getX()+.5))>.65||player.getZ()<p.getZ()-.5||player.getZ()>p.getZ()+18){
                buildTunnel(player.serverLevel(),r);
                player.teleportTo(player.serverLevel(),p.getX()+.5,p.getY(),p.getZ()+(back?16.5:.5),player.getYRot(),0);
                player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);return;
            }
            if(!back&&!r.ready){
                var job=jobs.getOrDefault(player.server,Map.of()).get(r.door);
                if(job==null){leave(player,false);player.displayClientMessage(Component.literal("The tunnel could not finish weaving. You have returned safely."),true);return;}
                if(player.tickCount%100==0)player.displayClientMessage(Component.literal("Weaving the Other World: "+job.progress()+"%"),true);
            }
            boolean normalDoorOpen=tunnelDoorOpen(player.serverLevel(),p.offset(0,0,-1));
            boolean staged=back||!r.ready||com.coraline.feast.FirstVisit.prepareEntrance(player,r);
            // Missing or slow-loading scene actors must not strand the player in a
            // finished tunnel. The first-visit tick retries scene setup after arrival.
            boolean welcomeReady=staged||r.ready&&player.server.overworld().getGameTime()-tag.getLong("CoralineTunnelStartedAt")>=160;
            if(player.getZ()<p.getZ()-.65&&normalDoorOpen&&player.server.overworld().getGameTime()>=tag.getLong("CoralinePortalCooldown")){leave(player,false);return;}
            if(r.ready&&!welcomeReady&&player.tickCount%100==0)
                player.displayClientMessage(Component.literal("The welcome is still being prepared. Stay by the far door."),true);
            if(r.ready&&welcomeReady){
                var end=p.offset(0,0,17);var endState=player.serverLevel().getBlockState(end);
                if(endState.is(ModBlocks.TINY_DOOR.get())&&!endState.getValue(TinyDoorBlock.OPEN))
                    player.serverLevel().setBlock(end,endState.setValue(TinyDoorBlock.OPEN,true),3);
            }
            if(r.ready&&welcomeReady&&player.getZ()>p.getZ()+16.35){
                player.setForcedPose(null);var safe=safePlayer(player.serverLevel(),r.doorPos);player.teleportTo(player.serverLevel(),safe.getX()+.5,safe.getY(),safe.getZ()+.5,player.getYRot(),0);tag.remove("CoralineInTunnel");tag.putLong("CoralinePortalCooldown",player.server.overworld().getGameTime()+40);
                tag.remove("CoralineTunnelStartedAt");
                com.coraline.feast.FirstVisit.entered(player,r);
                SceneAudio.otherWorldEntered(player);
                tag.remove("CoralineDebugVisitCompleted");
                tag.remove("CoralineReturning");tag.remove("CoralineRecoveryTunnel");
                if(!back&&!com.coraline.feast.OtherMotherLoop.entered(player,r))com.coraline.feast.OtherMotherWelcome.entered(player,r);
            }
        }else{
            player.fallDistance=0;
            long now=player.server.overworld().getGameTime();double distance=Math.hypot(player.getX()-(r.sourceDoor.getX()+r.dx),player.getZ()-(r.sourceDoor.getZ()+r.dz));
            if(distance>r.radius-10)OtherWorldEstate.extendWhite(player.serverLevel(),r,player.blockPosition());
            if(player.getY()<Math.max(player.level().getMinBuildHeight()+4,(r.porch==null?r.doorPos:r.porch).getY()-24))com.coraline.feast.QuestRecovery.recover(player);
            // Only begin the return after the player is well inside the white.
            if(distance>r.radius+43&&!tag.contains("CoralineWhiteoutStart")){
                tag.putLong("CoralineWhiteoutStart",now);tag.putLong("CoralineWhiteoutAnchor",player.blockPosition().asLong());
                tag.putBoolean("CoralineWhiteoutMoved",false);
            }
            if(tag.contains("CoralineWhiteoutStart")){
                long age=now-tag.getLong("CoralineWhiteoutStart");
                if(age<200&&!tag.getBoolean("CoralineWhiteoutMoved")){
                    var anchor=BlockPos.of(tag.getLong("CoralineWhiteoutAnchor"));
                    player.teleportTo(player.serverLevel(),anchor.getX()+.5,anchor.getY()+.5,anchor.getZ()+.5,player.getYRot(),player.getXRot());
                    player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);player.fallDistance=0;
                }
                if(age>=200&&!tag.getBoolean("CoralineWhiteoutMoved")){
                    tag.putBoolean("CoralineWhiteoutMoved",true);
                    if(!com.coraline.feast.QuestRecovery.recoverToPorch(player)){
                        var safe=safePlayer(player.serverLevel(),r.porch==null?r.doorPos:r.porch);
                        player.teleportTo(player.serverLevel(),safe.getX()+.5,safe.getY(),safe.getZ()+.5,player.getYRot(),player.getXRot());
                    }
                    player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                }
                if(age>=260){tag.remove("CoralineWhiteoutStart");tag.remove("CoralineWhiteoutMoved");tag.remove("CoralineWhiteoutAnchor");}
            }
        }
        if(tag.contains("CoralineWakeAt")&&player.server.overworld().getGameTime()>=tag.getLong("CoralineWakeAt")){tag.remove("CoralineWakeAt");com.coraline.feast.FirstVisit.slept(player);player.stopSleepInBed(true,false);leave(player,true);}
    }
    private static boolean tunnelDoorOpen(ServerLevel level,BlockPos pos){
        var state=level.getBlockState(pos);return state.is(ModBlocks.TINY_DOOR.get())&&state.getValue(TinyDoorBlock.OPEN);
    }
    public static boolean lockTunnelDoor(ServerPlayer player,BlockPos pos){
        if(player.getPersistentData().getBoolean("CoralineRecoveryTunnel")&&player.getPersistentData().getBoolean("CoralineInTunnel"))return false;
        var tag=player.getPersistentData();if(!tag.getBoolean("CoralineInTunnel")||!tag.hasUUID("CoralineVisitDoor"))return false;
        var r=RealmData.get(player.server).realms.get(tag.getUUID("CoralineVisitDoor"));
        if(r==null||!pos.equals(r.tunnel().offset(0,0,17))||r.ready||tag.getBoolean("CoralineReturning"))return false;
        var job=jobs.getOrDefault(player.server,Map.of()).get(r.door);
        player.displayClientMessage(Component.literal("The door is locked. Weaving the Other World: "+(job==null?0:job.progress())+"%"),true);return true;
    }
}
