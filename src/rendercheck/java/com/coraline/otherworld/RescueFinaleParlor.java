package com.coraline.otherworld;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** QA-only exact authored parlor/hall slice; not full-estate visual/performance acceptance. */
final class RescueFinaleParlor {
    static void build(ServerLevel level,BlockPos house,DoorData.Door door,RealmData.Realm realm){
        try(var raw=RescueFinaleParlor.class.getResourceAsStream("/assets/coraline/structures/rosewood_house.json.gz");var zip=new java.util.zip.GZIPInputStream(java.util.Objects.requireNonNull(raw));var reader=new java.io.InputStreamReader(zip,java.nio.charset.StandardCharsets.UTF_8)){
            var root=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();var palette=new java.util.ArrayList<BlockState>();
            for(var entry:root.getAsJsonArray("palette")){var cell=entry.getAsJsonObject();var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.tryParse(cell.get("id").getAsString()));var state=java.util.Objects.requireNonNull(block).defaultBlockState();for(var property:cell.getAsJsonObject("properties").entrySet())state=property(state,property.getKey(),property.getValue().getAsString());palette.add(state);}
            int count=0;for(var entry:root.getAsJsonArray("blocks")){var cell=entry.getAsJsonArray();int x=cell.get(0).getAsInt(),y=cell.get(1).getAsInt(),z=cell.get(2).getAsInt();if(x<12||x>36||y<5||y>14||z<30||z>55)continue;var at=RosewoodHouse.at(house,x,y,z);level.getChunkAt(at);level.setBlock(at,palette.get(cell.get(3).getAsInt()),2);count++;}
            realm.porch=RosewoodHouse.at(house,34,6,10);realm.piano=RosewoodHouse.at(house,20,6,60);realm.table=RosewoodHouse.at(house,43,6,36);realm.bed=RosewoodHouse.at(house,41,15,24);realm.doorPos=RosewoodHouse.at(house,14,6,38);
            var portal=com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(com.coraline.block.TinyDoorBlock.FACING,net.minecraft.core.Direction.EAST).setValue(com.coraline.block.TinyDoorBlock.OPEN,true).setValue(com.coraline.block.TinyDoorBlock.PORTAL,true);level.setBlock(realm.doorPos,portal,3);for(var at:java.util.List.of(realm.doorPos.above(),realm.doorPos.east(),realm.doorPos.east().above()))level.setBlock(at,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);if(level.getBlockEntity(realm.doorPos) instanceof DoorBlockEntity block){block.doorId=door.id;block.setChanged();}System.out.println("RESCUE_FINALE_BOUNDED_AUTHORED_PARLOR cells="+count+"; original palette/mapping, no surrounding estate/terrain generation; NOT full-estate acceptance");
        }catch(java.io.IOException failure){throw new IllegalStateException("Could not prepare the original authored parlor slice",failure);}
    }
    private static <T extends Comparable<T>> BlockState property(BlockState state,String key,String value){@SuppressWarnings("unchecked") var property=(net.minecraft.world.level.block.state.properties.Property<T>)state.getBlock().getStateDefinition().getProperty(key);if(property==null)throw new IllegalArgumentException("Missing authored property "+key);return state.setValue(property,property.getValue(value).orElseThrow());}
    private RescueFinaleParlor(){}
}
