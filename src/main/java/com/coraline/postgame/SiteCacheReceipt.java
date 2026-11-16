package com.coraline.postgame;

import java.util.*;
import net.minecraft.nbt.*;

/** Strict saved metadata validation only. A decoded receipt is not permission
 * to unlock: the live server must also attest its current generator/epoch. */
final class SiteCacheReceipt {
    record View(UUID site,UUID generation,long seed,int tier,boolean locked){}
    static int tier(InfiniteStructurePlan.Family family,InfiniteStructurePlan.Role role){
        if(family==InfiniteStructurePlan.Family.FALSE_PINK_PALACE&&role==InfiniteStructurePlan.Role.FALSE_PALACE_HALL)return 4;
        if(family==InfiniteStructurePlan.Family.ABANDONED_ORPHANAGE&&role==InfiniteStructurePlan.Role.ORPHAN_VAULT)return 3;
        if(family==InfiniteStructurePlan.Family.ENDLESS_CARNIVAL&&role==InfiniteStructurePlan.Role.CARNIVAL_VAULT)return 3;
        if(family==InfiniteStructurePlan.Family.GRAND_THEATRE&&role==InfiniteStructurePlan.Role.PERFORMANCE_VAULT)return 3;
        if(family==InfiniteStructurePlan.Family.FORGOTTEN_OBSERVATORY&&role==InfiniteStructurePlan.Role.RESEARCH_VAULT)return 3;
        if((family==InfiniteStructurePlan.Family.ANCIENT_BOUNDARY_SHRINE||family==InfiniteStructurePlan.Family.RESISTANCE_SANCTUARY||family==InfiniteStructurePlan.Family.ENTITY_HEART_CHAMBER)&&role==InfiniteStructurePlan.Role.DEFENSE_VAULT)return 4;
        if(family==InfiniteStructurePlan.Family.ROOTBOUND_TEMPLE)return role==InfiniteStructurePlan.Role.ROOT_SANCTUM?4:role==InfiniteStructurePlan.Role.STORAGE?2:0;
        if(family==InfiniteStructurePlan.Family.ABANDONED_VILLAGE&&role==InfiniteStructurePlan.Role.CONTROL)return 3;
        if(family==InfiniteStructurePlan.Family.DIMENSIONAL_SEAM&&role==InfiniteStructurePlan.Role.STORAGE)return 3;
        if(family==InfiniteStructurePlan.Family.PORCELAIN_MANSION&&role==InfiniteStructurePlan.Role.MANSION_HALL)return 4;
        if(family==InfiniteStructurePlan.Family.SUNKEN_OTHERWORLD_RUINS&&role==InfiniteStructurePlan.Role.RUIN_CHAMBER)return 4;
        if(family==InfiniteStructurePlan.Family.MIRROR_TOWER&&role==InfiniteStructurePlan.Role.MIRROR_SHAFT)return 3;
        if(family==InfiniteStructurePlan.Family.RIFT_SOLDIER_OUTPOST)return role==InfiniteStructurePlan.Role.CONTROL?3:role==InfiniteStructurePlan.Role.STORAGE?2:0;
        if(role==InfiniteStructurePlan.Role.ORE_VAULT)return family==InfiniteStructurePlan.Family.SOUL_GLASS_MINE?4:0;
        if(role==InfiniteStructurePlan.Role.CONTROL)return family==InfiniteStructurePlan.Family.CLOCKWORK_FACTORY||family==InfiniteStructurePlan.Family.RIFT_DRILL_CENTER?3:family==InfiniteStructurePlan.Family.DOLL_WORKSHOP?2:0;
        if(role==InfiniteStructurePlan.Role.STORAGE)return family==InfiniteStructurePlan.Family.RIFT_DRILL_CENTER?3:family==InfiniteStructurePlan.Family.CLOCKWORK_FACTORY?2:family==InfiniteStructurePlan.Family.DOLL_WORKSHOP?1:0;
        return 0;
    }
    static net.minecraft.core.BlockPos position(InfiniteStructurePlan.Plan plan,int index){var room=plan.modules().get(index);boolean mansion=room.role()==InfiniteStructurePlan.Role.MANSION_HALL||room.role()==InfiniteStructurePlan.Role.FALSE_PALACE_HALL;return new net.minecraft.core.BlockPos(plan.x()+room.x()+(mansion?room.width()-3:1),plan.y()+room.y()+(mansion?9:room.role()==InfiniteStructurePlan.Role.MIRROR_SHAFT?25:1),plan.z()+room.z()+(mansion?room.depth()-3:1));}
    static Optional<CompoundTag> create(InfiniteStructurePlan.Plan plan,int index){
        if(index<0||index>=plan.modules().size())return Optional.empty();int tier=tier(plan.family(),plan.modules().get(index).role());if(tier==0)return Optional.empty();
        var n=new CompoundTag();n.putInt("Version",1);n.putUUID("Site",plan.site());n.putUUID("Generation",plan.generation());n.putLong("Seed",plan.seed());n.putString("Family",plan.family().name());n.putInt("AnchorX",plan.x());n.putInt("AnchorZ",plan.z());n.putInt("Module",index);n.putInt("Tier",tier);n.putBoolean("Locked",true);if(plan.family()==InfiniteStructurePlan.Family.CLOCKWORK_FACTORY)n.putInt("FactoryProductionVersion",1);return Optional.of(n);
    }
    static Optional<View> read(CompoundTag n,int x,int y,int z){
        for(String key:List.of("Version","AnchorX","AnchorZ","Module","Tier"))if(!n.contains(key,Tag.TAG_INT))return Optional.empty();
        if(n.getInt("Version")!=1||!n.hasUUID("Site")||!n.hasUUID("Generation")||!n.contains("Seed",Tag.TAG_LONG)||!n.contains("Family",Tag.TAG_STRING)||!n.contains("Locked",Tag.TAG_BYTE)||n.getByte("Locked")<0||n.getByte("Locked")>1)return Optional.empty();
        if(n.contains("FactoryProductionVersion")&&(!n.contains("FactoryProductionVersion",Tag.TAG_INT)||n.getInt("FactoryProductionVersion")!=1||!n.getString("Family").equals(InfiniteStructurePlan.Family.CLOCKWORK_FACTORY.name())))return Optional.empty();
        if(n.contains("FactoryInvestigator")||n.contains("FactoryInvestigatedAt"))if(!n.hasUUID("FactoryInvestigator")||!n.contains("FactoryInvestigatedAt",Tag.TAG_LONG)||n.getLong("FactoryInvestigatedAt")<0||!n.getString("Family").equals(InfiniteStructurePlan.Family.CLOCKWORK_FACTORY.name()))return Optional.empty();
        try{
            var family=InfiniteStructurePlan.Family.valueOf(n.getString("Family"));
            var plan=InfiniteStructurePlan.at(n.getUUID("Generation"),n.getLong("Seed"),family,n.getInt("AnchorX"),n.getInt("AnchorZ")).orElse(null);
            int module=n.getInt("Module");if(plan==null||!plan.site().equals(n.getUUID("Site"))||module<0||module>=plan.modules().size())return Optional.empty();
            var room=plan.modules().get(module);int expected=tier(family,room.role());
            if(expected==0||n.getInt("Tier")!=expected||!position(plan,module).equals(new net.minecraft.core.BlockPos(x,y,z)))return Optional.empty();
            return Optional.of(new View(plan.site(),plan.generation(),plan.seed(),expected,n.getBoolean("Locked")));
        }catch(IllegalArgumentException unsupported){return Optional.empty();}
    }
    private SiteCacheReceipt(){}
}
