package com.coraline.otherworld;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.tags.BlockTags;

/** Chunk-sized snapshot work. No blocking joins on generation futures on the server thread. */
public final class RealmCopy {
    public static final TicketType<UUID> COPY_TICKET=TicketType.create("coraline_snapshot",UUID::compareTo);
    private final UUID ticket=UUID.randomUUID();
    public static CompletableFuture<Either<ChunkAccess,ChunkHolder.ChunkLoadingFailure>> requestChunk(ServerLevel level,ChunkPos pos,UUID ticket){
        // UNKNOWN tickets expire on the next tick. Async work needs its own
        // full-chunk ticket until copying finishes, without forcing entity ticks.
        level.getChunkSource().addRegionTicket(COPY_TICKET,pos,0,ticket);
        try{return ((com.coraline.mixin.ChunkFutureAccess)level.getChunkSource()).coraline$requestChunk(pos.x,pos.z,ChunkStatus.FULL,true);}
        catch(RuntimeException e){level.getChunkSource().removeRegionTicket(COPY_TICKET,pos,0,ticket);throw e;}
    }
    private void release(ChunkPos pos){
        source.getChunkSource().removeRegionTicket(COPY_TICKET,pos,0,ticket);
        target.getChunkSource().removeRegionTicket(COPY_TICKET,new ChunkPos(pos.x+(originalDX>>4),pos.z+(originalDZ>>4)),0,ticket);
    }
    public void cancel(){for(var task:inFlight)release(task.chunk);inFlight.clear();}
    private final ServerLevel source,target;
    private final DoorData.Door door;
    private final BlockPos originalPosition;
    private final int originalDX,originalDZ;
    public final RealmData.Realm realm;
    private final ArrayDeque<ChunkPos> priority=new ArrayDeque<>(),background=new ArrayDeque<>();
    private static final int MAX_IN_FLIGHT=12,MAX_COPIES_PER_TICK=4;
    private record Pending(ChunkPos chunk,
        CompletableFuture<Either<ChunkAccess,ChunkHolder.ChunkLoadingFailure>> source,
        CompletableFuture<Either<ChunkAccess,ChunkHolder.ChunkLoadingFailure>> target,long requestedAt){}
    private final ArrayList<Pending> inFlight=new ArrayList<>();
    private int copied,priorityCopied;
    public boolean failed;
    private final int total,priorityTotal;
    private final BlockPos sourceSite;
    private boolean readyNotice;
    public int progress(){return realm.ready?100:priorityTotal==0?100:100*priorityCopied/priorityTotal;}
    public boolean takeReady(){boolean value=readyNotice;readyNotice=false;return value;}
    public int remainingChunks(){return priority.size()+background.size()+inFlight.size();}
    public RealmCopy(ServerLevel source,ServerLevel target,DoorData.Door door,RealmData.Realm realm){
        this(source,target,door,realm,false);
    }
    public RealmCopy(ServerLevel source,ServerLevel target,DoorData.Door door,RealmData.Realm realm,boolean resume){
        this.source=source;this.target=target;this.door=door;this.realm=realm;
        originalPosition=door.pos.immutable();originalDX=realm.dx;originalDZ=realm.dz;
        boolean existing=resume&&realm.ready&&realm.house!=null&&realm.estateVersion>=OtherWorldEstate.VERSION;
        if(!existing){realm.ready=false;realm.islandComplete=false;realm.copiedChunks.clear();realm.preservedRadius=0;}
        sourceSite=existing?realm.house.offset(-realm.dx,0,-realm.dz):OtherWorldEstate.chooseSite(source,door.pos);
        int r=Math.max(realm.radius,realm.previousRadius);
        ArrayList<ChunkPos> outer=new ArrayList<>();
        for(int x=(door.pos.getX()-r)>>4;x<=(door.pos.getX()+r)>>4;x++)for(int z=(door.pos.getZ()-r)>>4;z<=(door.pos.getZ()+r)>>4;z++){
            int nx=Math.max(x*16,Math.min(door.pos.getX(),x*16+15)),nz=Math.max(z*16,Math.min(door.pos.getZ(),z*16+15));
            if((long)(nx-door.pos.getX())*(nx-door.pos.getX())+(long)(nz-door.pos.getZ())*(nz-door.pos.getZ())>r*r)continue;
            var chunk=new ChunkPos(x,z);if(existing&&realm.copiedChunks.contains(chunk.toLong()))continue;
            if(existing&&preserved(x*16,z*16)&&preserved(x*16+15,z*16)&&preserved(x*16,z*16+15)&&preserved(x*16+15,z*16+15)){realm.copiedChunks.add(chunk.toLong());continue;}
            if(!existing&&x*16<=sourceSite.getX()+50&&x*16+15>=sourceSite.getX()-50&&
                z*16<=sourceSite.getZ()+99&&z*16+15>=sourceSite.getZ()-26)priority.add(chunk);
            else outer.add(chunk);
        }
        outer.sort(Comparator.comparingLong(chunk->{long x=(long)chunk.getMiddleBlockX()-sourceSite.getX(),z=(long)chunk.getMiddleBlockZ()-sourceSite.getZ();return x*x+z*z;}));
        background.addAll(outer);priorityTotal=priority.size();total=priorityTotal+background.size();
    }
    public boolean tick(){
        try{boolean done=advance();if(done){for(var task:inFlight)release(task.chunk);inFlight.clear();}return done;}
        catch(RuntimeException error){failed=true;for(var task:inFlight)release(task.chunk);inFlight.clear();com.mojang.logging.LogUtils.getLogger().error("Coraline realm snapshot failed safely",error);return true;}
    }
    public boolean stillCurrent(){return door.exists&&door.pos.equals(originalPosition)&&door.dimension.equals(source.dimension().location().toString())&&realm.dx==originalDX&&realm.dz==originalDZ;}
    private boolean advance(){
        if(!stillCurrent()){failed=true;return true;}
        long now=source.getServer().overworld().getGameTime();
        // Queue several asynchronous chunk generations at once. The old single
        // pending future imposed a one-chunk-per-tick ceiling on a 300-block island.
        var queue=realm.ready?background:priority;
        while(inFlight.size()<(realm.ready?2:MAX_IN_FLIGHT)&&!queue.isEmpty()){
            var chunk=queue.remove();
            try{inFlight.add(new Pending(chunk,
                requestChunk(source,chunk,ticket),
                requestChunk(target,new ChunkPos(chunk.x+(realm.dx>>4),chunk.z+(realm.dz>>4)),ticket),now));}
            catch(RuntimeException e){release(chunk);throw e;}
        }
        int processed=0;long budgetStart=System.nanoTime();
        for(var iterator=inFlight.iterator();iterator.hasNext()&&processed<(realm.ready?1:MAX_COPIES_PER_TICK);){
            var task=iterator.next();
            if(!task.source.isDone()||!task.target.isDone()){
                if(now-task.requestedAt>1200){failed=true;return true;}continue;
            }
            var a=task.source.getNow(null);var b=task.target.getNow(null);
            if(a==null||b==null||a.left().isEmpty()||b.left().isEmpty()){failed=true;return true;}
            copy((LevelChunk)a.left().get(),(LevelChunk)b.left().get());release(task.chunk);iterator.remove();copied++;processed++;
            realm.copiedChunks.add(task.chunk.toLong());if(!realm.ready)priorityCopied++;
            RealmData.get(source.getServer()).setDirty();
            // Leave time for player movement and networking on a busy server.
            if(System.nanoTime()-budgetStart>18_000_000L)break;
        }
        if(!realm.ready&&priority.isEmpty()&&inFlight.isEmpty()){
            // Configured realms are always >=32; tiny radii are useful only for
            // bounded fixtures and cannot physically contain the estate.
            if(realm.radius>=32)OtherWorldEstate.build(source,target,door,realm,sourceSite);
            realm.ready=true;readyNotice=true;RealmData.get(source.getServer()).setDirty();
        }
        if(realm.ready&&background.isEmpty()&&inFlight.isEmpty()){
            realm.islandComplete=true;realm.previousRadius=realm.radius;realm.copiedChunks.clear();
            RealmData.get(source.getServer()).setDirty();return true;
        }
        return false;
    }
    private void copy(LevelChunk from,LevelChunk to){
        for(var pos:new ArrayList<>(to.getBlockEntities().keySet()))if(!preserved(pos.getX()-realm.dx,pos.getZ()-realm.dz))to.removeBlockEntity(pos);
        // The Other World is a visual shell. Fully enclosed voxels can never be seen
        // or touched here, so do not store millions of buried copies underneath it.
        for(int i=0;i<to.getSections().length;i++){
            int y=target.getSectionYFromSectionIndex(i)*16;
            if(y<source.getMinBuildHeight()||y>=source.getMaxBuildHeight()){to.getSections()[i]=new LevelChunkSection(target.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME));continue;}
            var src=from.getSection(source.getSectionIndex(y));var oldBiomes=to.getSection(i).getBiomes();
            var states=new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY,Blocks.AIR.defaultBlockState(),PalettedContainer.Strategy.SECTION_STATES);
            if(!src.hasOnlyAir()||realm.preservedRadius>0)for(int x=0;x<16;x++)for(int z=0;z<16;z++){
                int wx=from.getPos().getMinBlockX()+x,wz=from.getPos().getMinBlockZ()+z;
                if(preserved(wx,wz)){for(int ly=0;ly<16;ly++)states.set(x,ly,z,to.getSection(i).getBlockState(x,ly,z));continue;}
                if((long)(wx-door.pos.getX())*(wx-door.pos.getX())+(long)(wz-door.pos.getZ())*(wz-door.pos.getZ())>realm.radius*realm.radius)continue;
                // Heightmap tops can be bamboo, logs, or a canopy. Find the
                // actual earth below so the copied shell never omits its floor.
                int terrain=terrainSurface(from,x,z);
                // A shallow surface shell cuts open natural caves and ravines,
                // exposing the dimension's blue void through their floors.
                // Preserve their visible walls all the way to the source base.
                for(int ly=0;ly<16;ly++){
                    var state=src.getBlockState(x,ly,z);if(state.isAir())continue;
                    var p=new BlockPos(wx,y+ly,wz);
                    if((y+ly>=terrain-15&&y+ly<=terrain)||exposed(from,p,state))states.set(x,ly,z,state);
                }
            }
            to.getSections()[i]=new LevelChunkSection(states,oldBiomes);
        }
        // Only the copied dimension's visual biome palette changes. Keep every block
        // state and block-entity copy rule above/below intact, including source loot.
        var garden=target.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("coraline","perfected_garden")));
        for(var section:to.getSections()){
            section.recalcBlockCounts();
            section.fillBiomesFromNoise((x,y,z,sampler)->garden,null,0,0,0);
        }
        Heightmap.primeHeightmaps(to,EnumSet.allOf(Heightmap.Types.class));
        for(int x=0;x<16;x++)for(int z=0;z<16;z++){
            if(preserved(from.getPos().getMinBlockX()+x,from.getPos().getMinBlockZ()+z))continue;
            int y=to.getHeight(Heightmap.Types.WORLD_SURFACE,x,z)+1;var pos=new BlockPos(to.getPos().getMinBlockX()+x,y,to.getPos().getMinBlockZ()+z);
            long hash=net.minecraft.util.Mth.getSeed(pos);
            if(pos.distSqr(realm.doorPos)>64&&Math.floorMod(hash,10000)<com.coraline.config.CoralineConfig.OTHER_FLOWERS.get()*10000&&to.getBlockState(pos).isAir()&&to.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)){
                var flower=Math.floorMod(hash,3)==0?Blocks.ALLIUM:Math.floorMod(hash,3)==1?Blocks.OXEYE_DAISY:Blocks.PINK_TULIP;
                to.setBlockState(pos,flower.defaultBlockState(),false);
            }
        }
        for(var original:from.getBlockEntities().values()){
            if(preserved(original.getBlockPos().getX(),original.getBlockPos().getZ()))continue;
            var pos=original.getBlockPos().offset(realm.dx,0,realm.dz);var state=to.getBlockState(pos);if(!state.hasBlockEntity())continue;
            if(state.is(com.coraline.registry.ModBlocks.DOLL.get())){to.setBlockState(pos,Blocks.AIR.defaultBlockState(),false);continue;}
            if(state.is(com.coraline.registry.ModBlocks.TINY_DOOR.get())){state=state.setValue(com.coraline.block.TinyDoorBlock.PORTAL,false);to.setBlockState(pos,state,false);}
            var n=original.saveWithFullMetadata();n.putInt("x",pos.getX());n.putInt("y",pos.getY());n.putInt("z",pos.getZ());
            // A visual snapshot, not a resource duplication or executable command-block clone.
            n.remove("Items");n.remove("LootTable");n.remove("Command");n.remove("LastOutput");n.putBoolean("powered",false);n.putBoolean("auto",false);
            n.remove("ForgeCaps");
            var copy=BlockEntity.loadStatic(pos,state,n);if(copy!=null)to.setBlockEntity(copy);
        }
        to.setUnsaved(true);
        var targetBox=new net.minecraft.world.phys.AABB(to.getPos().getMinBlockX(),target.getMinBuildHeight(),to.getPos().getMinBlockZ(),to.getPos().getMaxBlockX()+1,target.getMaxBuildHeight(),to.getPos().getMaxBlockZ()+1);
        for(var old:target.getEntities((net.minecraft.world.entity.Entity)null,targetBox,e->e.getPersistentData().getBoolean("CoralineDecoration")&&e.chunkPosition().equals(to.getPos())&&!preserved(e.getBlockX()-realm.dx,e.getBlockZ()-realm.dz)))old.discard();
        var sourceBox=targetBox.move(-realm.dx,0,-realm.dz);
        for(var entity:source.getEntities((net.minecraft.world.entity.Entity)null,sourceBox,e->e.chunkPosition().equals(from.getPos())&&(e instanceof net.minecraft.world.entity.decoration.HangingEntity||e instanceof net.minecraft.world.entity.decoration.ArmorStand||e instanceof net.minecraft.world.entity.Display))){
            if(preserved(entity.getBlockX(),entity.getBlockZ()))continue;
            if(entity.blockPosition().distSqr(door.pos)>realm.radius*realm.radius)continue;
            var tag=new net.minecraft.nbt.CompoundTag();if(!entity.save(tag))continue;tag.remove("UUID");tag.remove("Passengers");
            var position=tag.getList("Pos",6);position.set(0,net.minecraft.nbt.DoubleTag.valueOf(position.getDouble(0)+realm.dx));position.set(2,net.minecraft.nbt.DoubleTag.valueOf(position.getDouble(2)+realm.dz));
            if(tag.contains("TileX"))tag.putInt("TileX",tag.getInt("TileX")+realm.dx);if(tag.contains("TileZ"))tag.putInt("TileZ",tag.getInt("TileZ")+realm.dz);
            var copy=net.minecraft.world.entity.EntityType.loadEntityRecursive(tag,target,e->{e.setInvulnerable(true);e.setNoGravity(true);e.getPersistentData().putBoolean("CoralineDecoration",true);return e;});if(copy!=null)target.addFreshEntity(copy);
        }
        for(int i=0;i<to.getSections().length;i++)target.getChunkSource().getLightEngine().updateSectionStatus(SectionPos.of(to.getPos(),target.getSectionYFromSectionIndex(i)),to.getSection(i).hasOnlyAir());
        target.getChunkSource().getLightEngine().initializeLight(to,false).thenCompose(chunk->target.getChunkSource().getLightEngine().lightChunk(chunk,false));
        // Refresh existing visitors too when another player takes a new snapshot.
        var packet=new net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket(to,target.getChunkSource().getLightEngine(),null,null);
        for(var player:target.players())if(player.chunkPosition().getChessboardDistance(to.getPos())<=target.getServer().getPlayerList().getViewDistance())player.connection.send(packet);
    }
    private boolean preserved(int x,int z){long dx=(long)x-door.pos.getX(),dz=(long)z-door.pos.getZ();return realm.preservedRadius>0&&dx*dx+dz*dz<=(long)realm.preservedRadius*realm.preservedRadius;}
    private boolean exposed(LevelChunk chunk,BlockPos pos,net.minecraft.world.level.block.state.BlockState state){
        for(var direction:Direction.values()){
            var near=pos.relative(direction);
            // Keep boundary blocks rather than synchronously generating a
            // neighboring chunk during an otherwise asynchronous copy.
            if((near.getX()>>4)!=chunk.getPos().x||(near.getZ()>>4)!=chunk.getPos().z)return true;
            var neighbor=chunk.getBlockState(near);
            if(neighbor.isAir()||!neighbor.getFluidState().isEmpty()||!neighbor.canOcclude()||neighbor.getCollisionShape(source,pos.relative(direction)).isEmpty())return true;
        }
        return !state.canOcclude()||!state.getFluidState().isEmpty();
    }
    private int terrainSurface(LevelChunk chunk,int x,int z){
        int top=chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
        int wx=chunk.getPos().getMinBlockX()+x,wz=chunk.getPos().getMinBlockZ()+z;
        for(int y=top;y>=Math.max(source.getMinBuildHeight()+4,top-90);y--){
            var state=chunk.getBlockState(new BlockPos(wx,y,wz));
            if(state.is(BlockTags.DIRT)||state.is(BlockTags.BASE_STONE_OVERWORLD)||
                state.is(Blocks.SAND)||state.is(Blocks.RED_SAND)||state.is(Blocks.GRAVEL)||
                state.is(Blocks.CLAY)||state.is(Blocks.SNOW_BLOCK))return y;
        }
        return top;
    }
}
