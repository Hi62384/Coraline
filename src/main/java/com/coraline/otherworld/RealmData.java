package com.coraline.otherworld;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class RealmData extends SavedData {
    public final Map<UUID,Realm> realms=new HashMap<>();
    private int nextIndex;
    public static final class Realm {
        public UUID door;
        public int dx,dz,radius,parentGeneration,previousRadius,estateVersion,preservedRadius;
        /** The real-world door is kept separately because the Other World door lives inside the estate. */
        public BlockPos sourceDoor,doorPos,bed,house,porch,piano,table;
        /** Ready means the house and garden are safe to enter; the island may still be weaving. */
        public boolean ready,islandComplete;
        public final Set<Long> copiedChunks=new HashSet<>();
        public BlockPos tunnel(){
            // The estate moves doorPos into its sitting-room wall after copying.
            // The tunnel must remain fixed while the player is inside it.
            BlockPos anchor=sourceDoor==null?doorPos:sourceDoor.offset(dx,0,dz);
            return new BlockPos((anchor.getX()>>4)*16,300,(anchor.getZ()>>4)*16+radius+64);
        }
    }
    public static RealmData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(RealmData::load,RealmData::new,"coraline_other_realms");}
    public Realm realm(DoorData.Door door){
        var r=realms.get(door.id);if(r!=null){
            if(r.sourceDoor==null)r.sourceDoor=r.doorPos.offset(-r.dx,0,-r.dz);
            if(!r.sourceDoor.equals(door.pos)){
                realms.remove(door.id);return realm(door);
            }
            r.previousRadius=Math.max(r.previousRadius,r.radius);
            expand(r);setDirty();return r;
        }
        int slot=++nextIndex;r=new Realm();r.door=door.id;r.radius=500;
        r.dx=(slot%512)*2048-(door.pos.getX()&~15);r.dz=(slot/512)*2048-(door.pos.getZ()&~15);r.sourceDoor=door.pos.immutable();r.doorPos=door.pos.offset(r.dx,0,r.dz);
        realms.put(door.id,r);setDirty();return r;
    }
    public static void expand(Realm r){
        if(r.radius>=500)return;
        if(r.islandComplete)r.preservedRadius=r.radius;
        r.previousRadius=r.radius;r.radius=500;r.islandComplete=false;
    }
    public static RealmData load(CompoundTag root){var data=new RealmData();data.nextIndex=root.getInt("NextIndex");for(Tag entry:root.getList("Realms",10)){
        // The previous completed disk survives expansion and reload without recopying the house.
        var n=(CompoundTag)entry;var r=new Realm();r.door=n.getUUID("Door");r.dx=n.getInt("DX");r.dz=n.getInt("DZ");r.radius=n.getInt("Radius");r.previousRadius=n.getInt("PreviousRadius");r.parentGeneration=n.getInt("ParentGeneration");r.estateVersion=n.getInt("EstateVersion");r.doorPos=BlockPos.of(n.getLong("Position"));r.sourceDoor=n.contains("SourceDoor")?BlockPos.of(n.getLong("SourceDoor")):r.doorPos.offset(-r.dx,0,-r.dz);if(n.contains("Bed"))r.bed=BlockPos.of(n.getLong("Bed"));if(n.contains("House"))r.house=BlockPos.of(n.getLong("House"));if(n.contains("Porch"))r.porch=BlockPos.of(n.getLong("Porch"));if(n.contains("Piano"))r.piano=BlockPos.of(n.getLong("Piano"));if(n.contains("Table"))r.table=BlockPos.of(n.getLong("Table"));r.ready=n.getBoolean("Ready");r.islandComplete=n.contains("IslandComplete")?n.getBoolean("IslandComplete"):r.ready;for(long chunk:n.getLongArray("CopiedChunks"))r.copiedChunks.add(chunk);data.realms.put(r.door,r);
        r.preservedRadius=n.getInt("PreservedRadius");
    }return data;}
    @Override public CompoundTag save(CompoundTag root){root.putInt("NextIndex",nextIndex);var list=new ListTag();for(var r:realms.values()){
        var n=new CompoundTag();n.putUUID("Door",r.door);n.putInt("DX",r.dx);n.putInt("DZ",r.dz);n.putInt("Radius",r.radius);n.putInt("PreviousRadius",r.previousRadius);n.putInt("ParentGeneration",r.parentGeneration);n.putInt("EstateVersion",r.estateVersion);n.putLong("Position",r.doorPos.asLong());if(r.sourceDoor!=null)n.putLong("SourceDoor",r.sourceDoor.asLong());if(r.bed!=null)n.putLong("Bed",r.bed.asLong());if(r.house!=null)n.putLong("House",r.house.asLong());if(r.porch!=null)n.putLong("Porch",r.porch.asLong());if(r.piano!=null)n.putLong("Piano",r.piano.asLong());if(r.table!=null)n.putLong("Table",r.table.asLong());n.putBoolean("Ready",r.ready);n.putBoolean("IslandComplete",r.islandComplete);n.putLongArray("CopiedChunks",r.copiedChunks.stream().mapToLong(Long::longValue).toArray());list.add(n);
        n.putInt("PreservedRadius",r.preservedRadius);
    }root.put("Realms",list);return root;}
}
