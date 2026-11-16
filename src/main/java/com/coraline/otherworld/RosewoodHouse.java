package com.coraline.otherworld;

import com.google.gson.*;
import com.coraline.block.TinyDoorBlock;
import com.coraline.registry.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Exact palette-based import; blueprint notes/labels are inert source data. */
public final class RosewoodHouse {
    private record Cell(int x,int y,int z,BlockState state){}
    private static List<Cell> cells;
    private static List<PalaceCell> palaceCells;
    /** The existing furnished Victorian master, without any realm/portal/story
     * allocation. The Overworld builder must survey/prepare safe terrain first.
     * Compact-coordinate collisions retain the SAME last-cell-wins order used
     * by the established house importer. Source artwork/blueprint is unchanged.
     */
    public record PalaceCell(BlockPos offset,BlockState state){}
    public static BlockPos palaceLittleDoor(BlockPos house){return at(house,14,6,38);}
    public static synchronized List<PalaceCell> palaceCells(){
        if(palaceCells!=null)return palaceCells;
        var positions=new HashSet<BlockPos>();for(var cell:cells())positions.add(at(BlockPos.ZERO,cell.x,cell.y,cell.z));
        var unique=new LinkedHashMap<BlockPos,BlockState>();
        for(var cell:cells()){
            var at=at(BlockPos.ZERO,cell.x,cell.y,cell.z);
            if(cell.y==3&&cell.state.is(Blocks.GRAVEL))at=new BlockPos(at.getX(),-1,at.getZ());
            else if(groundDecoration(cell,at,positions))at=new BlockPos(at.getX(),0,at.getZ());
            unique.put(at,cell.state);
        }
        var result=new ArrayList<PalaceCell>(unique.size());unique.forEach((at,state)->result.add(new PalaceCell(at,state)));
        return palaceCells=List.copyOf(result);
    }
    private RosewoodHouse(){}
    private static synchronized List<Cell> cells(){
        if(cells!=null)return cells;
        try(var raw=Objects.requireNonNull(RosewoodHouse.class.getResourceAsStream("/assets/coraline/structures/rosewood_house.json.gz"));
            var reader=new InputStreamReader(new GZIPInputStream(raw),StandardCharsets.UTF_8)){
            var root=JsonParser.parseReader(reader).getAsJsonObject();var palette=new ArrayList<BlockState>();
            for(var entry:root.getAsJsonArray("palette")){
                var state=entry.getAsJsonObject();var id=new ResourceLocation(state.get("id").getAsString());
                if(!BuiltInRegistries.BLOCK.containsKey(id))throw new IllegalArgumentException("Unknown blueprint block "+id);
                var block=BuiltInRegistries.BLOCK.get(id);var value=block.defaultBlockState();
                for(var prop:state.getAsJsonObject("properties").entrySet()){
                    var property=block.getStateDefinition().getProperty(prop.getKey());
                    if(property==null)throw new IllegalArgumentException("Unknown blueprint property "+id+"/"+prop.getKey());
                    value=property(value,property,prop.getValue().getAsString());
                }
                palette.add(value);
            }
            var result=new ArrayList<Cell>();for(var entry:root.getAsJsonArray("blocks")){
                var a=entry.getAsJsonArray();result.add(new Cell(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt(),palette.get(a.get(3).getAsInt())));
            }
            return cells=List.copyOf(result);
        }catch(IOException e){throw new IllegalStateException("Cannot load authored Rosewood house",e);}
    }
    private static <T extends Comparable<T>> BlockState property(BlockState state,Property<T> property,String value){
        return state.setValue(property,property.getValue(value).orElseThrow(()->new IllegalArgumentException("Invalid blueprint state "+value)));
    }
    private static int compact(int value,int... removed){int result=value;for(int strip:removed)if(strip<value)result--;return result;}
    static BlockPos at(BlockPos house,int x,int y,int z){return house.offset(compact(x,18,19)-compact(33,18,19),y-5,compact(z,46,47,61,62)-41);}
    /** Authored public rooms, transformed by the compact-house mapping used for blocks. */
    public static List<BlockPos> householdStops(BlockPos house){return List.of(at(house,33,6,26),at(house,43,6,36),at(house,43,6,55),at(house,24,6,34),at(house,34,6,12),at(house,32,6,49));}
    static void build(ServerLevel level,BlockPos house,DoorData.Door door,RealmData.Realm realm){
        build(level,level,house,door,realm,null);
    }
    static void build(ServerLevel source,ServerLevel level,BlockPos house,DoorData.Door door,RealmData.Realm realm,BlockPos previous){
        var authored=cells(); // Validate the entire palette before changing the world.
        var positions=new HashSet<BlockPos>();for(var cell:authored)positions.add(at(house,cell.x,cell.y,cell.z));
        var columns=new HashSet<BlockPos>();var footprint=new HashSet<BlockPos>();var bottoms=new HashMap<BlockPos,Integer>();
        for(var cell:authored){var p=at(house,cell.x,cell.y,cell.z);var column=new BlockPos(p.getX(),0,p.getZ());columns.add(column);
            if(!cell.state.is(Blocks.GRAVEL)&&!groundDecoration(cell,p,positions)){bottoms.merge(column,p.getY(),Math::min);if(cell.y<=5&&(cell.state.isSolid()||cell.state.getBlock() instanceof StairBlock))footprint.add(column);}
        }
        var grades=RosewoodTerrain.prepare(source,level,house,previous,realm,footprint,columns);
        // Excavate only actual building/porch columns. Preserve the landscape
        // outside them instead of clearing the full authored garden rectangle.
        for(var entry:bottoms.entrySet()){
            var column=entry.getKey();for(int y=entry.getValue();y<=house.getY()+40;y++){var p=new BlockPos(column.getX(),y,column.getZ());if(!level.isEmptyBlock(p))GenerationBlocks.set(level,p,Blocks.AIR.defaultBlockState(),2);}
            if(entry.getValue()<=house.getY()){
                var p=new BlockPos(column.getX(),entry.getValue()-1,column.getZ());
                for(int depth=0;depth<32&&!level.getBlockState(p).isFaceSturdy(level,p,Direction.UP);depth++,p=p.below())GenerationBlocks.set(level,p,Blocks.STONE_BRICKS.defaultBlockState(),2);
            }
        }
        // No neighbor updates during placement: paired beds/doors and attached decor
        // must not pop before their other halves/supports have been installed.
        for(var cell:authored){var p=at(house,cell.x,cell.y,cell.z);if(cell.y==3&&cell.state.is(Blocks.GRAVEL)||groundDecoration(cell,p,positions))p=new BlockPos(p.getX(),grades.get(new BlockPos(p.getX(),0,p.getZ()))+(cell.state.is(Blocks.GRAVEL)?0:1),p.getZ());GenerationBlocks.set(level,p,cell.state,2);}
        // Reconnect sparse export panes/rails and derive proper stair corners only
        // after every neighbour exists. Do not trigger gravity/support updates.
        for(var cell:authored){
            var p=at(house,cell.x,cell.y,cell.z);var state=level.getBlockState(p);
            if(state.getBlock() instanceof IronBarsBlock||state.getBlock() instanceof FenceBlock||state.getBlock() instanceof StairBlock){
                for(var direction:Direction.Plane.HORIZONTAL){var near=p.relative(direction);state=state.updateShape(direction,level.getBlockState(near),level,p,near);}
                GenerationBlocks.set(level,p,state,2);
            }
            if(isTable(state)){
                state=state.setValue(RosewoodFixtureBlock.NORTH,isTable(level.getBlockState(p.north()))).setValue(RosewoodFixtureBlock.EAST,isTable(level.getBlockState(p.east()))).setValue(RosewoodFixtureBlock.SOUTH,isTable(level.getBlockState(p.south()))).setValue(RosewoodFixtureBlock.WEST,isTable(level.getBlockState(p.west())));
                GenerationBlocks.set(level,p,state,2);
            }
        }
        realm.porch=at(house,34,6,10);
        realm.piano=at(house,20,6,60);
        realm.table=at(house,43,6,36);
        // A height/footprint migration must move saved scene anchors and their
        // owned props too, otherwise old furniture remains suspended above it.
        var stories=com.coraline.feast.VisitData.get(level.getServer());
        for(var scene:stories.scenes.values())if(Objects.equals(scene.door,door.id)){
            if(scene.table!=null){var delta=net.minecraft.world.phys.Vec3.atLowerCornerOf(realm.table.subtract(scene.table));
                for(var prop:level.getEntitiesOfClass(com.coraline.feast.ScenePropEntity.class,new net.minecraft.world.phys.AABB(scene.table).inflate(16)))if(Objects.equals(prop.owner,scene.owner)&&Objects.equals(prop.storyDoor,scene.door)&&prop.kind()!=com.coraline.feast.ScenePropEntity.PIANO)prop.setPos(prop.position().add(delta));}
            if(scene.piano!=null){var delta=net.minecraft.world.phys.Vec3.atLowerCornerOf(realm.piano.subtract(scene.piano));
                for(var prop:level.getEntitiesOfClass(com.coraline.feast.ScenePropEntity.class,new net.minecraft.world.phys.AABB(scene.piano).inflate(8)))if(Objects.equals(prop.owner,scene.owner)&&Objects.equals(prop.storyDoor,scene.door)&&prop.kind()==com.coraline.feast.ScenePropEntity.PIANO)prop.setPos(prop.position().add(delta));}
            scene.table=realm.table;scene.piano=realm.piano;stories.setDirty();
        }
        // Replace only static furniture in the two scripted prop footprints.
        clearProp(level,realm.piano,2,2);clearProp(level,realm.table,3,3);
        realm.bed=at(house,41,15,24);
        var foot=Blocks.BLUE_BED.defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH).setValue(BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.FOOT);
        GenerationBlocks.set(level,realm.bed,foot,2);GenerationBlocks.set(level,realm.bed.north(),foot.setValue(BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.HEAD),2);
        // Replace the decorative trapdoor in the marked blue-lined recess.
        realm.doorPos=at(house,14,6,38);
        var portal=ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(TinyDoorBlock.FACING,Direction.EAST).setValue(TinyDoorBlock.PORTAL,true).setValue(TinyDoorBlock.OPEN,true);
        GenerationBlocks.set(level,realm.doorPos,portal,3);
        for(var p:List.of(realm.doorPos.above(),realm.doorPos.east(),realm.doorPos.east().above()))GenerationBlocks.set(level,p,Blocks.AIR.defaultBlockState(),2);
        if(level.getBlockEntity(realm.doorPos) instanceof DoorBlockEntity block){block.doorId=door.id;block.setChanged();}
    }
    private static void clearProp(ServerLevel level,BlockPos center,int xRadius,int zRadius){
        for(int x=-xRadius;x<=xRadius;x++)for(int z=-zRadius;z<=zRadius;z++)for(int y=0;y<3;y++){var p=center.offset(x,y,z);if(!(level.getBlockState(p).getBlock() instanceof CarpetBlock))GenerationBlocks.set(level,p,Blocks.AIR.defaultBlockState(),2);}
    }
    private static boolean isTable(BlockState state){return state.is(ModBlocks.ROSEWOOD_TABLE.get())||state.is(ModBlocks.ROSEWOOD_TABLE_VASE.get())||state.is(ModBlocks.ROSEWOOD_TABLE_LAMP.get());}
    private static boolean groundDecoration(Cell cell,BlockPos at,Set<BlockPos> positions){return cell.y<=5&&!positions.contains(at.below())&&(cell.state.getBlock() instanceof FlowerPotBlock||cell.state.getBlock() instanceof FlowerBlock||cell.state.getBlock() instanceof DoublePlantBlock||cell.state.getBlock() instanceof LanternBlock);}
}
