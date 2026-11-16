package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

final class InfiniteSiteResidents {
    private record Key(UUID generation,long seed,int x,int z,InfiniteStructurePlan.Family family){}
    private static final Map<Key,Optional<InfiniteStructurePlan.Plan>> CACHE=new LinkedHashMap<>(128,.75F,true){@Override protected boolean removeEldestEntry(Map.Entry<Key,Optional<InfiniteStructurePlan.Plan>> entry){return size()>128;}};
    static UUID id(InfiniteStructurePlan.Plan plan,int module){return new UUID(plan.site().getMostSignificantBits()^0x7265736964656e74L,plan.site().getLeastSignificantBits()^module);}
    static BlockPos home(InfiniteStructurePlan.Plan plan,int module){var room=plan.modules().get(module);return new BlockPos(plan.x()+room.x()+room.width()/2,plan.y()+room.y()+1,plan.z()+room.z()+room.depth()/2);}
    static int role(InfiniteStructurePlan.Plan plan,int module){if(plan.family()==InfiniteStructurePlan.Family.ABANDONED_ORPHANAGE)return 5;return (plan.family()==InfiniteStructurePlan.Family.GRAND_THEATRE||plan.family()==InfiniteStructurePlan.Family.ENDLESS_CARNIVAL)?4:plan.family()==InfiniteStructurePlan.Family.RESISTANCE_SANCTUARY&&module==3?3:Math.floorMod(module-1,3);}
    static boolean family(InfiniteStructurePlan.Family family){return family==InfiniteStructurePlan.Family.OTHER_VILLAGE||family==InfiniteStructurePlan.Family.RESISTANCE_SANCTUARY||family==InfiniteStructurePlan.Family.GRAND_THEATRE||family==InfiniteStructurePlan.Family.ENDLESS_CARNIVAL||family==InfiniteStructurePlan.Family.ABANDONED_ORPHANAGE;}
    private static boolean residence(InfiniteStructurePlan.Plan plan,int module){return plan.family()==InfiniteStructurePlan.Family.ABANDONED_ORPHANAGE&&plan.modules().get(module).role()==InfiniteStructurePlan.Role.ORPHAN_DAIS|| plan.modules().get(module).role()==InfiniteStructurePlan.Role.RESIDENCE||(plan.family()==InfiniteStructurePlan.Family.GRAND_THEATRE||plan.family()==InfiniteStructurePlan.Family.ENDLESS_CARNIVAL)&&plan.modules().get(module).role()==InfiniteStructurePlan.Role.PERFORMER_DAIS;}
    static CompoundTag metadata(InfiniteStructurePlan.Plan plan,int module,UUID entity){var n=new CompoundTag();n.putInt("Version",1);n.putString("Family",plan.family().name());n.putUUID("Entity",entity);n.putUUID("Generation",plan.generation());n.putUUID("Site",plan.site());n.putLong("Seed",plan.seed());n.putInt("AnchorX",plan.x());n.putInt("AnchorZ",plan.z());n.putInt("Module",module);n.putLong("Home",home(plan,module).asLong());n.putInt("Role",role(plan,module));return n;}
    static boolean authentic(OtherworldResident actor,ServerLevel level){
        var n=actor.residence();for(String field:List.of("Version","AnchorX","AnchorZ","Module","Role"))if(!n.contains(field,Tag.TAG_INT))return false;
        if(n.getInt("Version")!=1||!n.hasUUID("Entity")||!n.getUUID("Entity").equals(actor.getUUID())||!n.hasUUID("Generation")||!n.hasUUID("Site")||!n.contains("Seed",Tag.TAG_LONG)||!n.contains("Home",Tag.TAG_LONG)||!(level.getChunkSource().getGenerator() instanceof InfiniteChunkGenerator generator))return false;
        try{var epoch=generator.structureGeneration();if(!epoch.id().equals(n.getUUID("Generation"))||epoch.seed()!=n.getLong("Seed")||!InfiniteWorldData.get(level.getServer()).matchesGeneration(epoch.id()))return false;}catch(IllegalStateException unavailable){return false;}
        InfiniteStructurePlan.Family family;try{if(n.contains("Family")&&!n.contains("Family",Tag.TAG_STRING))return false;family=n.contains("Family")?InfiniteStructurePlan.Family.valueOf(n.getString("Family")):InfiniteStructurePlan.Family.OTHER_VILLAGE;}catch(IllegalArgumentException bad){return false;}if(!family(family))return false;
        var key=new Key(n.getUUID("Generation"),n.getLong("Seed"),n.getInt("AnchorX"),n.getInt("AnchorZ"),family);Optional<InfiniteStructurePlan.Plan> candidate;
        synchronized(CACHE){candidate=CACHE.computeIfAbsent(key,k->InfiniteStructurePlan.at(k.generation,k.seed,k.family,k.x,k.z));}
        if(candidate.isEmpty())return false;var plan=candidate.orElseThrow();int module=n.getInt("Module");return module>=0&&module<plan.modules().size()&&residence(plan,module)&&plan.site().equals(n.getUUID("Site"))&&id(plan,module).equals(actor.getUUID())&&home(plan,module).asLong()==n.getLong("Home")&&n.getInt("Role")==role(plan,module)&&actor.role()==n.getInt("Role");
    }
    static void place(WorldGenLevel level,BoundingBox clip,InfiniteStructurePlan.Plan plan,int module,CompoundTag journal){
        if(!family(plan.family())||!residence(plan,module)||!(level.getLevel().getChunkSource().getGenerator() instanceof InfiniteChunkGenerator generator))return;
        try{var epoch=generator.structureGeneration();if(!epoch.id().equals(plan.generation())||epoch.seed()!=plan.seed())return;}catch(IllegalStateException unavailable){return;}
        if(!journal.isEmpty()&&(!journal.contains("Version",Tag.TAG_INT)||journal.getInt("Version")!=1||!journal.hasUUID("Entity")||!journal.getUUID("Entity").equals(id(plan,module))||!journal.contains("Issued",Tag.TAG_BYTE)||journal.getByte("Issued")<0||journal.getByte("Issued")>1))return;
        var at=home(plan,module);if(!clip.isInside(at)||journal.getBoolean("Issued"))return;var original=level instanceof ServerLevel direct?direct.getEntity(id(plan,module)):null;
        if(original!=null){if(original instanceof OtherworldResident resident&&authentic(resident,level.getLevel())){journal.putInt("Version",1);journal.putUUID("Entity",original.getUUID());journal.putBoolean("Issued",true);}return;}
        var resident=com.coraline.registry.ModEntities.OTHERWORLD_RESIDENT.get().create(level.getLevel());if(resident==null)return;resident.setUUID(id(plan,module));resident.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);resident.configure(plan,module);
        // Issuance precedes native insertion; reloads never replenish dead or
        // failed residents, reset stock, or create a second merchant identity.
        journal.putInt("Version",1);journal.putUUID("Entity",resident.getUUID());journal.putBoolean("Issued",true);if(!level.addFreshEntity(resident))resident.discard();
    }
    private InfiniteSiteResidents(){}
}
