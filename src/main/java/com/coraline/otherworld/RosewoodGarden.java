package com.coraline.otherworld;

import com.google.gson.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Authored front garden with a larger planted border graded into copied terrain. */
final class RosewoodGarden {
    private record Cell(int x,int y,int z,BlockState state){}
    private record Blueprint(List<Cell> features,List<Cell> terrain){}
    private static Blueprint blueprint;
    private static synchronized Blueprint blueprint(){
        if(blueprint!=null)return blueprint;
        try(var raw=Objects.requireNonNull(RosewoodGarden.class.getResourceAsStream("/assets/coraline/structures/rosewood_garden.json.gz"));
            var reader=new InputStreamReader(new GZIPInputStream(raw),StandardCharsets.UTF_8)){
            var root=JsonParser.parseReader(reader).getAsJsonObject();var palette=new ArrayList<BlockState>();
            for(var entry:root.getAsJsonArray("palette")){
                var json=entry.getAsJsonObject();var id=new ResourceLocation(json.get("id").getAsString());
                if(!BuiltInRegistries.BLOCK.containsKey(id))throw new IllegalArgumentException("Unknown garden block "+id);
                var block=BuiltInRegistries.BLOCK.get(id);var state=block.defaultBlockState();
                for(var prop:json.getAsJsonObject("properties").entrySet()){
                    var property=block.getStateDefinition().getProperty(prop.getKey());
                    if(property==null)throw new IllegalArgumentException("Unknown garden property "+id+"/"+prop.getKey());
                    state=property(state,property,prop.getValue().getAsString());
                }
                palette.add(state);
            }
            return blueprint=new Blueprint(read(root.getAsJsonArray("blocks"),palette),read(root.getAsJsonArray("terrain"),palette));
        }catch(IOException e){throw new IllegalStateException("Cannot load Rosewood garden",e);}
    }
    private static List<Cell> read(JsonArray array,List<BlockState> palette){
        var result=new ArrayList<Cell>();for(var entry:array){var a=entry.getAsJsonArray();result.add(new Cell(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt(),palette.get(a.get(3).getAsInt())));}return List.copyOf(result);
    }
    private static <T extends Comparable<T>> BlockState property(BlockState state,Property<T> property,String value){return state.setValue(property,property.getValue(value).orElseThrow(()->new IllegalArgumentException("Invalid garden state "+value)));}
    static BlockPos at(BlockPos house,int x,int y,int z){return house.offset(x-67,y-3,z-129);}
    /** Only the authored garden's still-matching natural cells may wither. */
    static int decay(ServerLevel level,BlockPos house,int cursor,int budget,java.util.function.BiConsumer<BlockPos,BlockState> place){
        var plan=blueprint();int total=plan.features.size()+plan.terrain.size(),end=Math.min(total,cursor+budget);
        for(;cursor<end;cursor++){var cell=cursor<plan.features.size()?plan.features.get(cursor):plan.terrain.get(cursor-plan.features.size());var at=at(house,cell.x,cell.y,cell.z);level.getChunkAt(at);var state=level.getBlockState(at);if(state.getBlock()!=cell.state.getBlock()||level.getBlockEntity(at)!=null)continue;
            BlockState dead=null;if(state.is(BlockTags.FLOWERS)||state.is(Blocks.GRASS)||state.is(Blocks.FERN)||state.is(Blocks.TALL_GRASS)||state.is(Blocks.LARGE_FERN))dead=Blocks.DEAD_BUSH.defaultBlockState();
            else if(state.is(BlockTags.LEAVES))dead=Blocks.GRAY_STAINED_GLASS.defaultBlockState();else if(state.is(Blocks.GRASS_BLOCK)||state.is(Blocks.MOSS_BLOCK))dead=Blocks.COARSE_DIRT.defaultBlockState();
            else if(state.is(BlockTags.LOGS))dead=Blocks.BASALT.defaultBlockState();if(dead!=null)place.accept(at,dead);
        }return cursor>=total?-1:cursor;
    }
    static void build(ServerLevel source,ServerLevel level,BlockPos house,RealmData.Realm realm){
        var garden=blueprint();var cores=new HashMap<BlockPos,Cell>();
        for(var cell:garden.terrain){var p=at(house,cell.x,cell.y,cell.z);cores.put(new BlockPos(p.getX(),0,p.getZ()),cell);}
        var distance=new HashMap<BlockPos,Double>();var roots=new HashMap<BlockPos,BlockPos>();
        for(var core:cores.keySet())for(int dx=-14;dx<=14;dx++)for(int dz=-14;dz<=14;dz++){
            double d=Math.hypot(dx,dz);if(d>14)continue;var p=core.offset(dx,0,dz);
            if(p.getZ()>house.getZ()-44)continue;
            if(d<distance.getOrDefault(p,Double.MAX_VALUE)){distance.put(p,d);roots.put(p,core);}
        }
        // Snapshot natural heights first: native previews may build in the same level.
        var natural=new HashMap<BlockPos,Integer>();
        for(var p:distance.keySet())natural.put(p,OtherWorldEstate.naturalSurface(source,p.getX()-realm.dx,p.getZ()-realm.dz)-1);
        var grades=new HashMap<BlockPos,Integer>();
        for(var entry:distance.entrySet()){
            var p=entry.getKey();double d=entry.getValue();int original=natural.get(p);var core=cores.get(roots.get(p));
            int authored=house.getY()-3+core.y;
            double t=Math.max(0,Math.min(1,(d+.55*Math.sin(p.getX()*.29)+.55*Math.cos(p.getZ()*.31))/14));if(d==0)t=0;
            int desired=(int)Math.round(authored+(original-authored)*t*t*(3-2*t));grades.put(p,desired);
            int top=Math.min(level.getMaxBuildHeight()-1,Math.max(original,desired)+40);
            for(int y=desired+1;y<=top;y++){
                var pos=new BlockPos(p.getX(),y,p.getZ());var state=level.getBlockState(pos);
                if(d==0||y<=original+1||state.is(BlockTags.LOGS)||state.is(BlockTags.LEAVES))GenerationBlocks.set(level,pos,Blocks.AIR.defaultBlockState(),2);
            }
            for(int y=Math.max(level.getMinBuildHeight(),Math.min(original,desired)-3);y<=desired;y++){
                BlockState soil=y==desired?(d==0&&!core.state.is(Blocks.STONE)&&!core.state.is(Blocks.DIRT)?core.state:Blocks.GRASS_BLOCK.defaultBlockState()):Blocks.DIRT.defaultBlockState();
                GenerationBlocks.set(level,new BlockPos(p.getX(),y,p.getZ()),soil,2);
            }
            // Extend planting outwards without stretching doors or stair geometry.
            int hash=Math.floorMod(p.getX()*7349+p.getZ()*9151,101);
            if(d>1&&d<11&&hash<7){var flower=new Block[]{Blocks.ALLIUM,Blocks.AZURE_BLUET,Blocks.WHITE_TULIP,Blocks.BLUE_ORCHID}[hash%4];GenerationBlocks.set(level,new BlockPos(p.getX(),desired+1,p.getZ()),flower.defaultBlockState(),2);}
        }
        for(var cell:garden.features)GenerationBlocks.set(level,at(house,cell.x,cell.y,cell.z),cell.state,2);
        for(var cell:garden.features){
            var p=at(house,cell.x,cell.y,cell.z);var state=level.getBlockState(p);
            if(state.getBlock() instanceof FenceBlock||state.getBlock() instanceof IronBarsBlock||state.getBlock() instanceof StairBlock){
                for(var dir:Direction.Plane.HORIZONTAL)state=state.updateShape(dir,level.getBlockState(p.relative(dir)),level,p,p.relative(dir));
                GenerationBlocks.set(level,p,state,2);
            }
        }
        // Match the garden's approach marker to the front steps with a gentle path.
        for(int z=-49;z<=-38;z++){
            int y=house.getY()-2+(z>=-41?1:0);
            for(int x=-1;x<=3;x++){
                var p=house.offset(x,y-house.getY(),z);int old=OtherWorldEstate.naturalSurface(level,p.getX(),p.getZ())-1;
                for(int fill=Math.min(old,y)-2;fill<y;fill++)GenerationBlocks.set(level,new BlockPos(p.getX(),fill,p.getZ()),Blocks.DIRT.defaultBlockState(),2);
                for(int clear=y+1;clear<=Math.max(old+2,y+3);clear++)GenerationBlocks.set(level,new BlockPos(p.getX(),clear,p.getZ()),Blocks.AIR.defaultBlockState(),2);
                GenerationBlocks.set(level,p,(z==-41?Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.SOUTH):Blocks.MOSSY_STONE_BRICKS.defaultBlockState()),2);
            }
        }
    }
    private RosewoodGarden(){}
}
