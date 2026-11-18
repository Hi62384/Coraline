package com.coraline.palace;

import com.coraline.otherworld.RosewoodHouse;
import java.lang.management.ManagementFactory;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import static com.coraline.palace.PalaceGenerationRules.Stage.*;
import static com.coraline.palace.PalaceSitePlan.Kind;

/** One Overworld job. No global biome replacement, synchronous whole-region
 * preload, player-position random roll, unbounded build queue or weather lock.
 * Real chunks/blocks are surveyed, placed and read back incrementally. All
 * modification is confined to the virgin footprint claimed BEFORE chunk loads.
 * Player proximity pauses work. An edit abandons a survey or protects a partial build.
 */
@Mod.EventBusSubscriber(modid="coraline")
public final class PalaceWorldGeneration {
    private static final int R=PalaceTerrainPlan.EXTENT,SIDE=R*2+1,CHUNK_SIDE=R/8+1;
    private static final long START_FREE=640L*1024*1024,STOP_FREE=384L*1024*1024;
    private static final ResourceKey<net.minecraft.world.level.biome.Biome> BIOME=ResourceKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("coraline","pink_palace_valley"));
    private record HouseColumn(int x,int z,int bottom){}
    private static List<HouseColumn> columns;
    private static MinecraftServer currentServer;
    private static LevelChunk currentChunk;
    private static long currentChunkId=Long.MIN_VALUE;
    private static int loadedThisTick;
    private static long retryAt;
    private static final class RetryableGeneration extends RuntimeException {
        RetryableGeneration(String message){super(message);}
    }
    private static final UUID GENERATION_TICKET=UUID.randomUUID();
    private static ServerLevel pendingLevel;
    private static ChunkPos pendingPosition;
    private static java.util.concurrent.CompletableFuture<com.mojang.datafixers.util.Either<net.minecraft.world.level.chunk.ChunkAccess,ChunkHolder.ChunkLoadingFailure>> pendingChunk;
    private static void releasePending(){
        if(pendingLevel!=null&&pendingPosition!=null)pendingLevel.getChunkSource().removeRegionTicket(com.coraline.otherworld.RealmCopy.COPY_TICKET,pendingPosition,0,GENERATION_TICKET);
        pendingLevel=null;pendingPosition=null;pendingChunk=null;
    }
    private static final Map<Long,List<net.minecraft.world.level.levelgen.structure.BoundingBox>> STRUCTURES=new LinkedHashMap<>();
    private static void clear(net.minecraft.nbt.CompoundTag n){for(var key:Set.copyOf(n.getAllKeys()))n.remove(key);}
    private static long freeMemory(){var bean=ManagementFactory.getOperatingSystemMXBean();return bean instanceof com.sun.management.OperatingSystemMXBean os?os.getFreeMemorySize():0;}
    private static PalaceSitePlan.Point point(BlockPos at){return new PalaceSitePlan.Point(at.getX(),at.getZ());}
    private static PalaceSitePlan.Point candidate(net.minecraft.nbt.CompoundTag n){return new PalaceSitePlan.Point(n.getInt("CandidateX"),n.getInt("CandidateZ"));}
    private static void save(ServerLevel l,PalaceSiteData d){d.setDirty();l.getDataStorage().save();}
    private static void enter(PalaceSiteData d,PalaceGenerationRules.Stage stage){PalaceGenerationRules.enter(d.generation(),stage);d.setDirty();currentChunk=null;currentChunkId=Long.MIN_VALUE;}
    private static boolean virgin(ServerLevel l,int x,int z,int radius)throws java.io.IOException{
        // Include a generation/view buffer, not merely the structure center.
        return PalaceVirginTerrain.untouched(l.getServer().getWorldPath(LevelResource.ROOT).resolve("region"),Math.floorDiv(x-radius,16),Math.floorDiv(z-radius,16),Math.floorDiv(x+radius,16),Math.floorDiv(z+radius,16),l.getChunkSource()::hasChunk);
    }
    private static LevelChunk chunk(ServerLevel l,int x,int z){
        int cx=Math.floorDiv(x,16),cz=Math.floorDiv(z,16);long id=ChunkPos.asLong(cx,cz);
        var loaded=l.getChunkSource().getChunkNow(cx,cz);
        if(loaded!=null){if(pendingLevel==l&&pendingPosition!=null&&pendingPosition.equals(new ChunkPos(cx,cz)))releasePending();currentChunk=loaded;currentChunkId=id;return loaded;}
        if(loadedThisTick>=1)return null;
        // A wall-clock budget cannot interrupt synchronous terrain generation.
        // Request at most one chunk, then yield to entity/network ticks until ready.
        loadedThisTick++;
        var position=new ChunkPos(cx,cz);
        if(pendingChunk==null){pendingLevel=l;pendingPosition=position;pendingChunk=com.coraline.otherworld.RealmCopy.requestChunk(l,position,GENERATION_TICKET);return null;}
        if(!pendingChunk.isDone())return null;
        if(pendingLevel!=l||!pendingPosition.equals(position)){releasePending();return null;}
        var result=pendingChunk.getNow(null);
        if(result==null||result.left().isEmpty())throw new RetryableGeneration("Landmark chunk unavailable");
        currentChunk=(LevelChunk)result.left().get();currentChunkId=id;releasePending();return currentChunk;
    }
    private static boolean put(ServerLevel l,BlockPos at,BlockState expected){
        if(at.getY()<l.getMinBuildHeight()||at.getY()>=l.getMaxBuildHeight()||!l.getWorldBorder().isWithinBounds(at))throw new IllegalStateException("Landmark outside world/build bounds");
        var actual=l.getBlockState(at);if(actual.equals(expected))return false;
        if(actual.hasBlockEntity())throw new IllegalStateException("Existing block entity preserved at "+at.toShortString());
        l.setBlock(at,expected,2);if(!l.getBlockState(at).equals(expected))throw new RetryableGeneration("Block placement did not commit at "+at.toShortString());return true;
    }
    public static String travelStatus(MinecraftServer server){
        var data=PalaceSiteData.get(server);
        if(data.ready())return "Palace transportation is ready.";
        var world=server.overworld();
        if(!data.blocker().isEmpty())return data.blocker();
        var job=data.generation();
        String stage=PalaceGenerationRules.stage(job).name().toLowerCase(java.util.Locale.ROOT).replace('_',' ');
        if(com.coraline.otherworld.OtherWorld.weavingEntrance(server))return "Palace preparation is paused while a Little Door is weaving.";
        if(freeMemory()<STOP_FREE)return "Palace preparation is paused for available server memory. It will retry automatically.";
        if(occupied(world,data))return "Palace preparation is paused while players are near the construction area. It will resume when the area clears.";
        if(currentServer==server&&world.getGameTime()<retryAt)return "Palace preparation is retrying a temporary interruption at "+stage+".";
        return "The Moving Company is open. Palace preparation is at "+stage+" (section "+job.getInt("Cursor")+").";
    }
    private static boolean occupied(ServerLevel l,PalaceSiteData d){
        var n=d.generation();int buffer=l.getServer().getPlayerList().getViewDistance()*16+64;
        for(var p:l.players()){
            if(n.getBoolean("PalaceClaimed")&&Math.abs(p.getX()-n.getInt("CandidateX"))<=R+buffer&&Math.abs(p.getZ()-n.getInt("CandidateZ"))<=R+buffer)return true;
            if(n.getBoolean("CompanyClaimed")&&!d.generated(Kind.COMPANY)&&Math.abs(p.getX()-n.getInt("CompanyX"))<=48+buffer&&Math.abs(p.getZ()-n.getInt("CompanyZ"))<=48+buffer)return true;
        }
        return false;
    }
    /** Structure bounding boxes are checked against the actual mutation range,
     * so an underground mineshaft is not a spurious surface-settlement veto.
     */
    private static List<net.minecraft.world.level.levelgen.structure.BoundingBox> structures(ServerLevel l,LevelChunk c){
        long chunkId=c.getPos().toLong();var cached=STRUCTURES.get(chunkId);if(cached!=null)return cached;
        var result=new ArrayList<net.minecraft.world.level.levelgen.structure.BoundingBox>();
        for(var entry:c.getAllReferences().entrySet())for(long id:entry.getValue()){
            var origin=new ChunkPos(id);var original=chunk(l,origin.getMinBlockX(),origin.getMinBlockZ());if(original==null)return null;var start=original.getStartForStructure(entry.getKey());
            if(start!=null&&start.isValid())result.add(start.getBoundingBox());
        }
        for(var start:c.getAllStarts().values())if(start.isValid())result.add(start.getBoundingBox());
        if(STRUCTURES.size()>=128)STRUCTURES.remove(STRUCTURES.keySet().iterator().next());STRUCTURES.put(chunkId,List.copyOf(result));return result;
    }
    private static boolean intersects(List<net.minecraft.world.level.levelgen.structure.BoundingBox> boxes,int x,int low,int z,int high){
        for(var b:boxes)if(x>=b.minX()&&x<=b.maxX()&&z>=b.minZ()&&z<=b.maxZ()&&low<=b.maxY()&&high>=b.minY())return true;return false;
    }
    private static int natural(LevelChunk c,int x,int z){
        int y=c.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x&15,z&15);
        for(int scanned=0;scanned<64&&y>c.getMinBuildHeight();scanned++,y--){
            var state=c.getBlockState(new BlockPos(x,y,z));
            if(!state.is(BlockTags.LOGS)&&!state.is(BlockTags.LEAVES)&&!state.canBeReplaced())break;
        }
        return y;
    }
    private static void selectPalace(ServerLevel l,PalaceSiteData d)throws java.io.IOException{
        var n=d.generation();if(d.position(Kind.PALACE).isPresent())throw new IllegalStateException("Existing saved Palace preserved; mixed legacy generation needs review");
        var proposed=d.proposal(Kind.PALACE);if(proposed.isEmpty())return;var p=proposed.get();
        int buffer=Math.max(128,l.getServer().getPlayerList().getViewDistance()*16+64);
        if(Math.abs((long)p.x())>PalaceSitePlan.WORLD_LIMIT-R-buffer||Math.abs((long)p.z())>PalaceSitePlan.WORLD_LIMIT-R-buffer||!virgin(l,p.x(),p.z(),R+buffer)){d.reject(Kind.PALACE,p);return;}
        n.putInt("CandidateX",p.x());n.putInt("CandidateZ",p.z());n.putInt("ProfileBase",100);n.putBoolean("ProfileSurveyed",false);n.putBoolean("PalaceClaimed",true);
        enter(d,SURVEY_REGION);save(l,d); // Claim precedes ANY new chunk request.
    }
    private static void surveyRegion(ServerLevel l,PalaceSiteData d){
        var n=d.generation();var p=candidate(n);
        if(!n.getBoolean("ProfileSurveyed")){
            var c=chunk(l,p.x(),p.z());if(c==null)return;int h=natural(c,p.x(),p.z());
            int min=h,max=h;for(int dx:new int[]{-24,0,24})for(int dz:new int[]{-24,0,24}){
                // center-column survey first; complete region pass follows.
                if((p.x()+dx>>4)==c.getPos().x&&(p.z()+dz>>4)==c.getPos().z){int y=natural(c,p.x()+dx,p.z()+dz);min=Math.min(min,y);max=Math.max(max,y);}
            }
            var state=l.getBlockState(new BlockPos(p.x(),h,p.z()));
            if(h<64||h>142||max-min>10||!state.getFluidState().isEmpty()){
                d.reject(Kind.PALACE,p);clear(n);enter(d,SELECT_PALACE);return;
            }
            n.putInt("ProfileBase",h+1);n.putBoolean("ProfileSurveyed",true);d.setDirty();return;
        }
        int count=CHUNK_SIDE*CHUNK_SIDE,cursor=n.getInt("Cursor");if(cursor>count)throw new IllegalStateException("Invalid regional survey cursor");
        if(cursor==count){
            if(n.getInt("HouseSurveyMax")-n.getInt("HouseSurveyMin")>10){d.reject(Kind.PALACE,p);clear(n);enter(d,SELECT_PALACE);return;}
            if(!d.reserveGenerationPlan(Kind.PALACE,new BlockPos(p.x(),n.getInt("ProfileBase"),p.z())))throw new IllegalStateException("Original Palace reservation changed");
            enter(d,SELECT_WELL);save(l,d);return;
        }
        int cx=Math.floorDiv(p.x()-R,16)+cursor%CHUNK_SIDE,cz=Math.floorDiv(p.z()-R,16)+cursor/CHUNK_SIDE;
        var c=chunk(l,cx*16,cz*16);if(c==null)return;var boxes=structures(l,c);if(boxes==null)return;
        // The well is chosen later. Reject any above-ground structure anywhere
        // in this untouched valley, before the first terrain block is altered.
        for(var b:boxes)if(b.maxY()>=Math.min(70,n.getInt("ProfileBase")-20)){
            d.reject(Kind.PALACE,p);clear(n);enter(d,SELECT_PALACE);return;
        }
        for(var be:c.getBlockEntities().values())if(be.getBlockPos().getY()>=natural(c,be.getBlockPos().getX(),be.getBlockPos().getZ())-24){
            d.reject(Kind.PALACE,p);clear(n);enter(d,SELECT_PALACE);return;
        }
        for(int x=cx*16;x<cx*16+16;x++)for(int z=cz*16;z<cz*16+16;z++)if(Math.abs(x-p.x())<=33&&Math.abs(z-p.z())<=44){
            int h=natural(c,x,z);var at=new BlockPos(x,h,z);var state=l.getBlockState(at);
            if(!state.getFluidState().isEmpty()||!state.isFaceSturdy(l,at,Direction.UP)){d.reject(Kind.PALACE,p);clear(n);enter(d,SELECT_PALACE);return;}
            n.putInt("HouseSurveyMin",n.contains("HouseSurveyMin")?Math.min(n.getInt("HouseSurveyMin"),h):h);n.putInt("HouseSurveyMax",n.contains("HouseSurveyMax")?Math.max(n.getInt("HouseSurveyMax"),h):h);
        }
        n.putInt("Cursor",cursor+1);d.setDirty();if(cursor%16==0)save(l,d);
    }
    private static void selectWell(ServerLevel l,PalaceSiteData d){
        var proposed=d.proposal(Kind.WELL);if(proposed.isEmpty())return;var p=proposed.get();var c=chunk(l,p.x(),p.z());if(c==null)return;
        int y=d.generation().getInt("ProfileBase")+36;var boxes=structures(l,c);if(boxes==null)return;
        for(int dx=-5;dx<=5;dx++)for(int dz=-5;dz<=5;dz++)if(intersects(boxes,p.x()+dx,y-33,p.z()+dz,y+6)){d.reject(Kind.WELL,p);return;}
        if(!d.reserveGenerationPlan(Kind.WELL,new BlockPos(p.x(),y,p.z())))throw new IllegalStateException("Original well reservation changed");
        enter(d,SELECT_COMPANY);save(l,d);
    }
    private static void selectCompany(ServerLevel l,PalaceSiteData d)throws java.io.IOException{
        var proposed=d.proposal(Kind.COMPANY);if(proposed.isEmpty())return;var p=proposed.get();
        if(!virgin(l,p.x(),p.z(),80+l.getServer().getPlayerList().getViewDistance()*16+64)){d.reject(Kind.COMPANY,p);return;}
        var n=d.generation();n.putInt("CompanyX",p.x());n.putInt("CompanyZ",p.z());n.putBoolean("CompanyClaimed",true);enter(d,SURVEY_COMPANY);save(l,d);
    }
    private static void surveyCompany(ServerLevel l,PalaceSiteData d){
        var n=d.generation();int x=n.getInt("CompanyX"),z=n.getInt("CompanyZ");int cursor=n.getInt("Cursor");
        if(cursor>49)throw new IllegalStateException("Invalid company survey cursor");
        if(cursor==49){
            int low=n.getInt("SurveyMin"),high=n.getInt("SurveyMax");var p=new PalaceSitePlan.Point(x,z);
            if(low<64||high>180||high-low>10){d.reject(Kind.COMPANY,p);n.putBoolean("CompanyClaimed",false);enter(d,SELECT_COMPANY);return;}
            int y=(low+high)/2+1;n.putInt("CompanyY",y);
            if(!d.reserveGenerationPlan(Kind.COMPANY,new BlockPos(x,y,z)))throw new IllegalStateException("Company reservation changed");
            n.putBoolean("CompanyFirst",true);enter(d,COMPANY_PREPARE);save(l,d);return;
        }
        int cx=Math.floorDiv(x,16)-3+cursor%7,cz=Math.floorDiv(z,16)-3+cursor/7;var c=chunk(l,cx*16,cz*16);if(c==null)return;
        var boxes=structures(l,c);if(boxes==null)return;if(!boxes.isEmpty()||!c.getBlockEntities().isEmpty()){
            d.reject(Kind.COMPANY,new PalaceSitePlan.Point(x,z));n.putBoolean("CompanyClaimed",false);enter(d,SELECT_COMPANY);return;
        }
        int min=cursor==0?320:n.getInt("SurveyMin"),max=cursor==0?-64:n.getInt("SurveyMax");
        for(int dx=0;dx<16;dx++)for(int dz=0;dz<16;dz++)if(Math.abs(cx*16+dx-x)<=30&&Math.abs(cz*16+dz-z)<=28){
            int h=natural(c,cx*16+dx,cz*16+dz);var at=new BlockPos(cx*16+dx,h,cz*16+dz);var state=l.getBlockState(at);
            if(!state.getFluidState().isEmpty()||!state.isFaceSturdy(l,at,Direction.UP)){d.reject(Kind.COMPANY,new PalaceSitePlan.Point(x,z));n.putBoolean("CompanyClaimed",false);enter(d,SELECT_COMPANY);return;}
            min=Math.min(min,h);max=Math.max(max,h);
        }
        n.putInt("SurveyMin",min);n.putInt("SurveyMax",max);n.putInt("Cursor",cursor+1);d.setDirty();
    }
    private static void beginColumn(net.minecraft.nbt.CompoundTag n,int x,int z,int bottom,int top,int target){
        n.putBoolean("ColumnActive",true);n.putInt("ColumnX",x);n.putInt("ColumnZ",z);n.putInt("ColumnBottom",bottom);n.putInt("ColumnTop",top);n.putInt("ColumnTarget",target);n.putInt("Y",bottom);
    }
    private static boolean column(ServerLevel l,PalaceSiteData d,boolean house){
        var n=d.generation();int x=n.getInt("ColumnX"),z=n.getInt("ColumnZ"),y=n.getInt("Y"),top=n.getInt("ColumnTop"),target=n.getInt("ColumnTarget");
        if(chunk(l,x,z)==null)return false;
        if(y>top){n.remove("ColumnActive");n.putInt("Cursor",n.getInt("Cursor")+1);d.setDirty();return true;}
        BlockState value;
        if(y>target)value=Blocks.AIR.defaultBlockState();
        else if(house)value=Blocks.STONE_BRICKS.defaultBlockState();
        else if(y==target){
            var p=point(d.position(Kind.PALACE).orElseThrow());var w=point(d.position(Kind.WELL).orElseThrow());
            var s=PalaceTerrainPlan.sample(l.getSeed(),p,w,n.getInt("ProfileBase"),x,z,target);
            double material=PalaceTerrainPlan.unit(l.getSeed()+31,x,z);
            value=(s.trail()?material<.45?Blocks.GRAVEL:Blocks.COARSE_DIRT:target>n.getInt("ProfileBase")+25&&material<.46?material<.18?Blocks.ANDESITE:Blocks.STONE:material<.12?Blocks.COARSE_DIRT:Blocks.GRASS_BLOCK).defaultBlockState();
        }else value=(y>=target-3?Blocks.DIRT:Blocks.STONE).defaultBlockState();
        put(l,new BlockPos(x,y,z),value);n.putInt("Y",y+1);d.setDirty();return true;
    }
    private static void terrain(ServerLevel l,PalaceSiteData d){
        var n=d.generation();if(n.getBoolean("ColumnActive")){column(l,d,false);return;}
        int i=n.getInt("Cursor");if(i>SIDE*SIDE)throw new IllegalStateException("Invalid terrain cursor");
        if(i==SIDE*SIDE){enter(d,DETAILS);save(l,d);return;}
        var palace=d.position(Kind.PALACE).orElseThrow();
        int minX=palace.getX()-R,minZ=palace.getZ()-R;
        var cell=n.getBoolean("CompanyFirst")?PalaceTerrainPlan.column(minX,minZ,i):new PalaceSitePlan.Point(minX+i%SIDE,minZ+i/SIDE);
        int x=cell.x(),z=cell.z();
        var c=chunk(l,x,z);if(c==null)return;
        int old=natural(c,x,z),top=c.getHeight(Heightmap.Types.WORLD_SURFACE,x&15,z&15);
        var sample=PalaceTerrainPlan.sample(l.getSeed(),point(palace),point(d.position(Kind.WELL).orElseThrow()),n.getInt("ProfileBase"),x,z,old);
        if(n.getBoolean("CompanyFirst")?(x==minX||(x&15)==0)&&(z==minZ||(z&15)==0):(i%SIDE)%16==0&&(i/SIDE)%16==0){
            var biome=l.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(BIOME);
            c.fillBiomesFromNoise((qx,qy,qz,sampler)->PalaceSitePlan.regionWeight(l.getSeed(),point(palace),QuartPos.toBlock(qx)+2,QuartPos.toBlock(qz)+2)>0?biome:c.getNoiseBiome(qx,qy,qz),l.getChunkSource().randomState().sampler());c.setUnsaved(true);
        }
        if(sample.weight()==0){n.putInt("Cursor",i+1);d.setDirty();return;}
        // Protect any actual subterranean structure intersecting excavation.
        var boxes=structures(l,c);if(boxes==null)return;
        if(intersects(boxes,x,Math.min(old,sample.surface())-3,z,Math.max(top,sample.surface())+1))throw new IllegalStateException("Terrain alteration would touch an unrelated structure");
        beginColumn(n,x,z,Math.max(l.getMinBuildHeight(),Math.min(old,sample.surface())-3),Math.min(l.getMaxBuildHeight()-1,Math.max(top,sample.surface())+1),sample.surface());
        d.setDirty();
    }
    private static List<HouseColumn> houseColumns(){
        if(columns!=null)return columns;var bottoms=new TreeMap<Long,Integer>();
        for(var cell:RosewoodHouse.palaceCells()){var p=cell.offset();long id=BlockPos.asLong(p.getX(),0,p.getZ());bottoms.merge(id,p.getY(),Math::min);}
        var result=new ArrayList<HouseColumn>();bottoms.forEach((id,y)->{var p=BlockPos.of(id);result.add(new HouseColumn(p.getX(),p.getZ(),y));});return columns=List.copyOf(result);
    }
    private static void prepareHouse(ServerLevel l,PalaceSiteData d){
        var n=d.generation();if(n.getBoolean("ColumnActive")){column(l,d,true);return;}
        int i=n.getInt("Cursor");var columns=houseColumns();if(i>columns.size())throw new IllegalStateException("Invalid house foundation cursor");
        if(i==columns.size()){enter(d,HOUSE);save(l,d);return;}
        var column=columns.get(i);var p=d.position(Kind.PALACE).orElseThrow();int x=p.getX()+column.x,z=p.getZ()+column.z;var c=chunk(l,x,z);if(c==null)return;
        int floor=p.getY()+column.bottom;beginColumn(n,x,z,Math.max(l.getMinBuildHeight(),Math.min(floor-1,natural(c,x,z))-4),Math.min(l.getMaxBuildHeight()-1,Math.max(p.getY()+62,c.getHeight(Heightmap.Types.WORLD_SURFACE,x&15,z&15)+1)),floor-1);d.setDirty();
    }
    private static void prepareCompany(ServerLevel l,PalaceSiteData d){
        var n=d.generation();if(n.getBoolean("ColumnActive")){column(l,d,false);return;}
        int i=n.getInt("Cursor");if(i>71*71)throw new IllegalStateException("Invalid company foundation cursor");
        if(i==71*71){enter(d,COMPANY);save(l,d);return;}
        var p=d.position(Kind.COMPANY).orElseThrow();int dx=i%71-35,dz=i/71-35,x=p.getX()+dx,z=p.getZ()+dz;var c=chunk(l,x,z);if(c==null)return;
        int old=natural(c,x,z);double edge=Math.max(Math.max(0,Math.abs(dx)-24),Math.max(0,Math.abs(dz)-18))/11.0;edge=Math.min(1,edge);edge=edge*edge*(3-2*edge);
        int target=(int)Math.round(p.getY()-1+(old-p.getY()+1)*edge);
        var boxes=structures(l,c);if(boxes==null)return;
        int bottom=Math.max(l.getMinBuildHeight(),Math.min(old,target)-3),top=Math.min(l.getMaxBuildHeight()-1,Math.max(p.getY()+16,c.getHeight(Heightmap.Types.WORLD_SURFACE,x&15,z&15)+1));
        if(intersects(boxes,x,bottom,z,top))throw new IllegalStateException("Company grading would touch an unrelated structure");
        beginColumn(n,x,z,bottom,top,target);d.setDirty();
    }
    private static BlockState connected(ServerLevel l,BlockPos p,BlockState state){
        if(state.getBlock() instanceof IronBarsBlock||state.getBlock() instanceof FenceBlock||state.getBlock() instanceof StairBlock)
            for(var dir:Direction.Plane.HORIZONTAL)state=state.updateShape(dir,l.getBlockState(p.relative(dir)),l,p,p.relative(dir));
        return state;
    }
    private static void house(ServerLevel l,PalaceSiteData d,boolean verify){
        var n=d.generation();int i=n.getInt("Cursor");var cells=RosewoodHouse.palaceCells();if(i>cells.size())throw new IllegalStateException("Invalid original house cursor");
        if(i==cells.size()){
            if(verify&&!d.generationVerified(Kind.PALACE,d.position(Kind.PALACE).orElseThrow(),n.getInt("Verified"),cells.size()))throw new IllegalStateException("House readback incomplete");
            enter(d,verify?(d.generated(Kind.COMPANY)?WELL:COMPANY_PREPARE):HOUSE_VERIFY);save(l,d);return;
        }
        var cell=cells.get(i);var p=d.position(Kind.PALACE).orElseThrow().offset(cell.offset());if(chunk(l,p.getX(),p.getZ())==null)return;
        if(!verify)put(l,p,cell.state());
        else{
            var expected=connected(l,p,cell.state());var actual=l.getBlockState(p);
            if(actual.getBlock()!=cell.state().getBlock())throw new IllegalStateException("Victorian-master readback conflict at "+p.toShortString());
            // Only derived connection properties are adjusted after all cells.
            if(!actual.equals(expected))put(l,p,expected);
            n.putInt("Verified",n.getInt("Verified")+1);
        }
        n.putInt("Cursor",i+1);d.setDirty();
    }
    private static BlockState material(PalaceLandmarkPlan.Material material){return switch(material){
        case KEEP->null;case AIR->Blocks.AIR.defaultBlockState();case STONE->Blocks.MOSSY_COBBLESTONE.defaultBlockState();case BRICK->Blocks.BRICKS.defaultBlockState();case PINK->Blocks.PINK_TERRACOTTA.defaultBlockState();case CREAM->Blocks.SMOOTH_SANDSTONE.defaultBlockState();case DARK->Blocks.DARK_OAK_PLANKS.defaultBlockState();case PLANKS->Blocks.SPRUCE_PLANKS.defaultBlockState();case GLASS->Blocks.GLASS_PANE.defaultBlockState();case FENCE->Blocks.SPRUCE_FENCE.defaultBlockState();case CRATE->Blocks.BARREL.defaultBlockState();case LIGHT->Blocks.SEA_LANTERN.defaultBlockState();case STAIR_N->Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.NORTH);case STAIR_S->Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.SOUTH);
        case COUNTER->com.coraline.registry.ModBlocks.ROSEWOOD_COUNTER.get().defaultBlockState();case CHAIR->com.coraline.registry.ModBlocks.ROSEWOOD_CHAIR.get().defaultBlockState();case ROPE->Blocks.CHAIN.defaultBlockState();
    };}
    private static void landmark(ServerLevel l,PalaceSiteData d,Kind kind,boolean verify){
        var n=d.generation();int i=n.getInt("Cursor");boolean company=kind==Kind.COMPANY;var bounds=company?PalaceLandmarkPlan.COMPANY:PalaceLandmarkPlan.WELL;
        if(i>bounds.size())throw new IllegalStateException("Invalid secondary landmark cursor");
        if(i==bounds.size()){
            if(verify&&!d.generationVerified(kind,d.position(kind).orElseThrow(),n.getInt("Verified"),bounds.size()))throw new IllegalStateException("Landmark readback incomplete");
            enter(d,company?verify?(n.getBoolean("CompanyFirst")?TERRAIN:WELL):COMPANY_VERIFY:verify?SIGNS:WELL_VERIFY);save(l,d);return;
        }
        var cell=bounds.cell(i);var p=d.position(kind).orElseThrow().offset(cell[0],cell[1],cell[2]);if(chunk(l,p.getX(),p.getZ())==null)return;
        var expected=material(company?PalaceLandmarkPlan.company(cell[0],cell[1],cell[2]):PalaceLandmarkPlan.well(cell[0],cell[1],cell[2]));
        if(expected!=null){
            if(verify){if(l.getBlockState(p).getBlock()!=expected.getBlock())throw new IllegalStateException("Landmark readback conflict at "+p.toShortString());put(l,p,connected(l,p,expected));}
            else{
                boolean fresh=put(l,p,expected);
                if(fresh&&company&&cell[1]==1&&cell[2]==6&&(cell[0]==-5||cell[0]==8)&&l.getBlockEntity(p) instanceof net.minecraft.world.Container supplies){
                    supplies.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD,3));supplies.setItem(1,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER,8));
                    supplies.setItem(2,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STRING,4));supplies.setItem(3,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_NUGGET,6));supplies.setChanged();
                }
            }
        }
        n.putInt("Cursor",i+1);if(verify)n.putInt("Verified",n.getInt("Verified")+1);d.setDirty();
    }
    /** Decorations are streamed one block at a time, never hundreds of new mobs. */
    private static void details(ServerLevel l,PalaceSiteData d){
        var n=d.generation();int grid=SIDE/8+1,i=n.getInt("Cursor");if(i>grid*grid)throw new IllegalStateException("Invalid vegetation cursor");
        if(i==grid*grid){enter(d,HOUSE_PREPARE);save(l,d);return;}
        var p=d.position(Kind.PALACE).orElseThrow();var w=d.position(Kind.WELL).orElseThrow();int x=p.getX()-R+(i%grid)*8,z=p.getZ()-R+(i/grid)*8;
        if(!PalaceTerrainPlan.vegetation(l.getSeed(),point(p),point(w),x,z)){n.putInt("Cursor",i+1);n.putInt("Subcursor",0);return;}
        var c=chunk(l,x,z);if(c==null)return;int sub=n.getInt("Subcursor");int h=sub==0?natural(c,x,z)+1:n.getInt("DetailBaseY");if(sub==0)n.putInt("DetailBaseY",h);
        boolean rock=(PalaceTerrainPlan.hash(l.getSeed()+18,x,z)&3)==0;int height=9+(int)(PalaceTerrainPlan.unit(l.getSeed()+3,x,z)*9),total=rock?7*7*4:height+7*7*(height-3);
        if(h+height>=l.getMaxBuildHeight()){n.putInt("Cursor",i+1);n.putInt("Subcursor",0);return;}
        if(sub>=total){n.putInt("Cursor",i+1);n.putInt("Subcursor",0);return;}
        BlockPos at;BlockState state=null;
        if(rock){int dx=sub%7-3,dz=(sub/7)%7-3,y=sub/49;at=new BlockPos(x+dx,h+y,z+dz);if(dx*dx/9.0+dz*dz/7.0+y*y/7.0<=1.1)state=((sub+x+z)%4==0?Blocks.MOSSY_COBBLESTONE:Blocks.ANDESITE).defaultBlockState();}
        else if(sub<height){at=new BlockPos(x,h+sub,z);state=Blocks.SPRUCE_LOG.defaultBlockState();}
        else{int leaf=sub-height,y=leaf/49+3,dx=leaf%7-3,dz=(leaf/7)%7-3;at=new BlockPos(x+dx,h+y,z+dz);double radius=3.1*(1-y/(double)height);
            if(Math.hypot(dx,dz)<=radius+.6&&Math.abs(dx)+Math.abs(dz)>0)state=Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true);
        }
        if(state!=null){if(chunk(l,at.getX(),at.getZ())==null)return;if(l.isEmptyBlock(at))put(l,at,state);}
        n.putInt("Subcursor",sub+1);d.setDirty();
    }
    private static void signs(ServerLevel l,PalaceSiteData d){
        var n=d.generation();int index=n.getInt("Cursor");if(index>2)throw new IllegalStateException("Invalid shared sign cursor");
        if(index==2){enter(d,FINISH);save(l,d);return;}
        String key=index==0?"PalaceSign":"CompanySign";var base=d.position(index==0?Kind.PALACE:Kind.COMPANY).orElseThrow().offset(index==0?28:-15,0,index==0?-48:-14);
        if(l.players().stream().anyMatch(player->player.distanceToSqr(Vec3.atCenterOf(base))<16*16))return;
        var signChunk=new ChunkPos(base);
        l.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.PORTAL,signChunk,2,signChunk.getWorldPosition());
        if(chunk(l,base.getX(),base.getZ())==null||!l.areEntitiesLoaded(signChunk.toLong()))return;
        com.coraline.feast.CinematicPropEntity sign=null;
        if(n.hasUUID(key)){
            var e=l.getEntity(n.getUUID(key));if(!(e instanceof com.coraline.feast.CinematicPropEntity existing)||existing.kind()!=com.coraline.feast.CinematicPropEntity.WORLD_SIGN||existing.slot()!=index)throw new IllegalStateException("Original shared sign unavailable; no duplicate created");sign=existing;
        }else{
            var found=l.getEntitiesOfClass(com.coraline.feast.CinematicPropEntity.class,new AABB(base).inflate(1),e->e.kind()==com.coraline.feast.CinematicPropEntity.WORLD_SIGN&&e.slot()==index);
            if(found.size()>1)throw new IllegalStateException("Duplicate shared sign identities need review");
            if(found.size()==1)sign=found.get(0);
            else{sign=com.coraline.feast.CinematicPropEntity.TYPE.get().create(l);if(sign==null)throw new RetryableGeneration("Shared sign creation unavailable");sign.worldSign(index);sign.setPos(Vec3.atBottomCenterOf(base));if(!l.addFreshEntity(sign))throw new RetryableGeneration("Shared sign spawn refused");}
            n.putUUID(key,sign.getUUID());save(l,d);
        }
        n.putInt("Cursor",index+1);d.setDirty();
    }
    private static void finish(ServerLevel l,PalaceSiteData d){
        var house=d.position(Kind.PALACE).orElseThrow();var well=d.position(Kind.WELL).orElseThrow();
        var roof=RosewoodHouse.palaceCells().stream().filter(c->c.state().is(Blocks.DEEPSLATE_TILES)||c.state().is(Blocks.DEEPSLATE_TILE_SLAB)||c.state().is(Blocks.DARK_OAK_STAIRS)).max(Comparator.comparingInt(c->c.offset().getY())).orElseThrow();
        var from=Vec3.atCenterOf(well).add(0,2.1,0);var to=Vec3.atCenterOf(house.offset(roof.offset()));int total=(int)Math.ceil(from.distanceTo(to)*2),i=d.generation().getInt("Cursor");
        if(i>=total)throw new IllegalStateException("Sightline did not reach an actual original roof block");
        var a=from.lerp(to,i/(double)total);var b=from.lerp(to,(i+1)/(double)total);
        for(int cx=Math.floorDiv((int)Math.floor(Math.min(a.x,b.x)),16);cx<=Math.floorDiv((int)Math.floor(Math.max(a.x,b.x)),16);cx++)for(int cz=Math.floorDiv((int)Math.floor(Math.min(a.z,b.z)),16);cz<=Math.floorDiv((int)Math.floor(Math.max(a.z,b.z)),16);cz++)if(chunk(l,cx*16,cz*16)==null)return;
        var hit=l.clip(new net.minecraft.world.level.ClipContext(a,b,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,null));
        if(hit.getType()==HitResult.Type.BLOCK){
            var local=hit.getBlockPos().subtract(house);boolean actualHouse=RosewoodHouse.palaceCells().stream().anyMatch(c->c.offset().equals(local)&&c.state().getBlock()==l.getBlockState(hit.getBlockPos()).getBlock());
            if(!actualHouse)throw new IllegalStateException("Actual well-to-Palace sightline obstructed; no false visible receipt");
            enter(d,DONE);save(l,d);currentChunk=null;
        }else{d.generation().putInt("Cursor",i+1);d.setDirty();}
    }
    @SubscribeEvent public static void started(ServerStartedEvent event){var l=event.getServer().overworld();PalaceSiteData.get(event.getServer()).initialize(l.getSeed(),l.getSharedSpawnPos());}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||freeMemory()<START_FREE)return;
        var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return;
        // The player's requested portal takes priority over optional distant
        // landmark generation; do not run two generation pipelines together.
        if(com.coraline.otherworld.OtherWorld.weavingEntrance(server))return;
        if(server instanceof net.minecraft.gametest.framework.GameTestServer&&!Boolean.getBoolean("coraline.palaceWorldgenQa"))return;
        if(currentServer!=server){releasePending();currentServer=server;retryAt=0;currentChunk=null;currentChunkId=Long.MIN_VALUE;STRUCTURES.clear();}

        var l=server.overworld();var d=PalaceSiteData.get(server);
        if(l.getGameTime()<retryAt)return;
        d.retryLegacyPause();
        if(!d.initialized()||!d.blocker().isEmpty()){releasePending();return;}
        if(d.generation().isEmpty()&&Arrays.stream(Kind.values()).allMatch(d::generated))return;
        try{
            var stage=PalaceGenerationRules.stage(d.generation());if(stage==DONE)return;
            if(occupied(l,d)){releasePending();return;}
            loadedThisTick=0;long deadline=System.nanoTime()+2_000_000;
            for(int count=0;count<128&&System.nanoTime()<deadline&&freeMemory()>=STOP_FREE;count++){
                stage=d.generation().isEmpty()?SELECT_PALACE:PalaceGenerationRules.Stage.valueOf(d.generation().getString("Stage"));
                switch(stage){
                    case SELECT_PALACE->selectPalace(l,d);case SURVEY_REGION->surveyRegion(l,d);case SELECT_WELL->selectWell(l,d);case SELECT_COMPANY->selectCompany(l,d);case SURVEY_COMPANY->surveyCompany(l,d);
                    case TERRAIN->terrain(l,d);case DETAILS->details(l,d);case HOUSE_PREPARE->prepareHouse(l,d);case HOUSE->house(l,d,false);case HOUSE_VERIFY->house(l,d,true);case COMPANY_PREPARE->prepareCompany(l,d);
                    case COMPANY->landmark(l,d,Kind.COMPANY,false);case COMPANY_VERIFY->landmark(l,d,Kind.COMPANY,true);case WELL->landmark(l,d,Kind.WELL,false);case WELL_VERIFY->landmark(l,d,Kind.WELL,true);case SIGNS->signs(l,d);case FINISH->finish(l,d);case DONE->{return;}
                }
                if(!d.blocker().isEmpty()||loadedThisTick>0||stage==SIGNS)break;
            }
        }catch(RetryableGeneration|java.io.IOException|java.util.concurrent.CompletionException failure){
            releasePending();
            retryAt=l.getGameTime()+100;
            d.setDirty();
        }catch(RuntimeException failure){
            releasePending();
            d.stopGeneration("Palace generation needs review at "+d.generation().getString("Stage")
                    +" cursor "+d.generation().getInt("Cursor")+": "+failure.getMessage());
            save(l,d);
        }
    }
    private static void edited(BlockEvent event){
        if(!(event.getLevel() instanceof ServerLevel l)||l!=l.getServer().overworld())return;var d=PalaceSiteData.get(l.getServer());var n=d.generation();
        if(!d.blocker().isEmpty()||n.isEmpty()||PalaceGenerationRules.stage(n)==DONE)return;var p=event.getPos();
        if(n.getBoolean("PalaceClaimed")&&Math.abs(p.getX()-n.getInt("CandidateX"))<=R+128&&Math.abs(p.getZ()-n.getInt("CandidateZ"))<=R+128
                ||n.getBoolean("CompanyClaimed")&&!d.generated(Kind.COMPANY)&&Math.abs(p.getX()-n.getInt("CompanyX"))<=80&&Math.abs(p.getZ()-n.getInt("CompanyZ"))<=80){
            releasePending();
            if(!d.reselectUnbuiltSite())d.stopGeneration("Player edit at "+p.toShortString()+" intersects the partially built landmark. Inspect the original generation checkpoint before resuming.");
            save(l,d);
        }
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST) public static void placed(BlockEvent.EntityPlaceEvent event){edited(event);}
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST) public static void broken(BlockEvent.BreakEvent event){edited(event);}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){releasePending();currentServer=null;currentChunk=null;currentChunkId=Long.MIN_VALUE;STRUCTURES.clear();}
    private PalaceWorldGeneration(){}
}
