package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Finite, structure-owned defenders. Issuance survives piece serialization;
 * killing or unloading a defender never replenishes its assignment. Unknown
 * journals fail closed, without deleting their original metadata. */
final class InfiniteSiteGuards {
    record Assignment(UUID id,PostgameHostiles.Kind kind,BlockPos at) {}
    static List<Assignment> assignments(InfiniteStructurePlan.Plan plan,int moduleIndex){
        var room=plan.modules().get(moduleIndex);var kinds=new ArrayList<PostgameHostiles.Kind>();
        switch(plan.family()){
            case DOLL_WORKSHOP->{
                if(room.role()==InfiniteStructurePlan.Role.HALL||room.role()==InfiniteStructurePlan.Role.STORAGE)kinds.add(PostgameHostiles.Kind.BUTTON_EYED_WANDERER);
            }
            case CLOCKWORK_FACTORY->{
                if(room.role()==InfiniteStructurePlan.Role.HALL)kinds.add(PostgameHostiles.Kind.CLOCKWORK_WATCHER);
                if(room.role()==InfiniteStructurePlan.Role.WORKROOM||room.role()==InfiniteStructurePlan.Role.CONTROL)kinds.add(PostgameHostiles.Kind.RIFT_SOLDIER);
            }
            case SOUL_GLASS_MINE->{
                if(room.role()==InfiniteStructurePlan.Role.WORKROOM)kinds.add(PostgameHostiles.Kind.BUTTON_EYED_WANDERER);
                if(room.role()==InfiniteStructurePlan.Role.ORE_VAULT){kinds.add(PostgameHostiles.Kind.RIFT_SOLDIER);kinds.add(PostgameHostiles.Kind.RIFT_SOLDIER);}
            }
            case RIFT_DRILL_CENTER->{
                if(room.role()==InfiniteStructurePlan.Role.HALL){kinds.add(PostgameHostiles.Kind.CLOCKWORK_WATCHER);kinds.add(PostgameHostiles.Kind.RIFT_SOLDIER);}
                if(room.role()==InfiniteStructurePlan.Role.CONTROL)kinds.add(PostgameHostiles.Kind.RIFT_SOLDIER);
                if(room.role()==InfiniteStructurePlan.Role.DRILL_CHAMBER){kinds.add(PostgameHostiles.Kind.RIFT_SOLDIER);kinds.add(PostgameHostiles.Kind.RIFT_GUARDIAN);}
                if(room.role()==InfiniteStructurePlan.Role.TOWER)kinds.add(PostgameHostiles.Kind.CLOCKWORK_WATCHER);
            }
            case RIFT_SOLDIER_OUTPOST->{
                if(room.role()==InfiniteStructurePlan.Role.HALL||room.role()==InfiniteStructurePlan.Role.CONTROL||room.role()==InfiniteStructurePlan.Role.WORKROOM||room.role()==InfiniteStructurePlan.Role.STORAGE)kinds.add(PostgameHostiles.Kind.RIFT_SOLDIER);
                if(room.role()==InfiniteStructurePlan.Role.TOWER)kinds.add(PostgameHostiles.Kind.CLOCKWORK_WATCHER);
            }
            case MIRROR_TOWER->{
                if(room.role()==InfiniteStructurePlan.Role.HALL)kinds.add(PostgameHostiles.Kind.PORCELAIN_SENTINEL);
                if(room.role()==InfiniteStructurePlan.Role.MIRROR_SHAFT){kinds.add(PostgameHostiles.Kind.PAPER_WRAITH);kinds.add(PostgameHostiles.Kind.PAPER_WRAITH);}
            }
            case SUNKEN_OTHERWORLD_RUINS->{/* Diving and navigation, not land mobs placed underwater. */}
            case PORCELAIN_MANSION->{if(room.role()==InfiniteStructurePlan.Role.MANSION_HALL){kinds.add(PostgameHostiles.Kind.FALSE_VILLAGER);kinds.add(PostgameHostiles.Kind.FALSE_VILLAGER);kinds.add(PostgameHostiles.Kind.FALSE_VILLAGER);kinds.add(PostgameHostiles.Kind.PORCELAIN_SENTINEL);}}
            case DIMENSIONAL_SEAM->{if(room.role()==InfiniteStructurePlan.Role.SEAM_CHAMBER){kinds.add(PostgameHostiles.Kind.VOID_STALKER);kinds.add(PostgameHostiles.Kind.THREAD_SPINNER);}if(room.role()==InfiniteStructurePlan.Role.STORAGE)kinds.add(PostgameHostiles.Kind.RIFT_SOLDIER);}
            case OTHER_VILLAGE->{/* Peaceful inhabited settlement. */}
            case ABANDONED_VILLAGE->{if(room.role()==InfiniteStructurePlan.Role.RUINED_RESIDENCE)kinds.add(PostgameHostiles.Kind.THREAD_SPINNER);if(room.role()==InfiniteStructurePlan.Role.CONTROL)kinds.add(PostgameHostiles.Kind.VOID_STALKER);}
            case ROOTBOUND_TEMPLE->{if(room.role()==InfiniteStructurePlan.Role.HALL)kinds.add(PostgameHostiles.Kind.THREADCRAWLER);if(room.role()==InfiniteStructurePlan.Role.ROOT_SANCTUM){kinds.add(PostgameHostiles.Kind.ROOTBOUND_HORROR);kinds.add(PostgameHostiles.Kind.THREADCRAWLER);}}
            case ANCIENT_BOUNDARY_SHRINE->{/* Attackers require the real initiated defense, never chunk reload. */}
            case RESISTANCE_SANCTUARY->{/* Residents and a real allied lookout; attackers are encounter-owned. */}
            case FORGOTTEN_OBSERVATORY->{if(room.role()==InfiniteStructurePlan.Role.OBSERVATORY_HALL){kinds.add(PostgameHostiles.Kind.PAPER_WRAITH);kinds.add(PostgameHostiles.Kind.PAPER_WRAITH);}}
            case FALSE_PINK_PALACE->{if(room.role()==InfiniteStructurePlan.Role.FALSE_PALACE_HALL){kinds.add(PostgameHostiles.Kind.FALSE_VILLAGER);kinds.add(PostgameHostiles.Kind.FALSE_VILLAGER);kinds.add(PostgameHostiles.Kind.FALSE_VILLAGER);}}
            case ENTITY_HEART_CHAMBER->{/* Defenders are issued once per admitted containment attempt. */}
            case ABANDONED_ORPHANAGE->{if(room.role()==InfiniteStructurePlan.Role.ORPHAN_RECORD&&moduleIndex<3)kinds.add(PostgameHostiles.Kind.THREAD_SPINNER);}
            case ENDLESS_CARNIVAL->{/* Physical shooting gallery and troupe. */}
            case GRAND_THEATRE->{/* The real troupe and performance drive the venue, not a disposable combat wave. */}
        }
        var result=new ArrayList<Assignment>();
        for(int ordinal=0;ordinal<kinds.size();ordinal++){
            long slot=((long)moduleIndex<<32)|ordinal;
            var id=new UUID(plan.site().getMostSignificantBits()^0x6775617264730001L,plan.site().getLeastSignificantBits()^slot);
            var at=new BlockPos(plan.x()+room.x()+room.width()-(room.role()==InfiniteStructurePlan.Role.MIRROR_SHAFT?4:3),plan.y()+room.y()+(room.role()==InfiniteStructurePlan.Role.MIRROR_SHAFT?6+ordinal*10:room.role()==InfiniteStructurePlan.Role.TOWER?9:1),plan.z()+room.z()+2+ordinal*3);
            if(room.role()==InfiniteStructurePlan.Role.MANSION_HALL)at=new BlockPos(plan.x()+room.x()+(ordinal%2==0?3:19),plan.y()+room.y()+(ordinal==3?9:1),plan.z()+room.z()+(ordinal<2?4:18));
            if(room.role()==InfiniteStructurePlan.Role.ROOT_SANCTUM)at=new BlockPos(plan.x()+room.x()+(ordinal==0?4:8),plan.y()+room.y()+1,plan.z()+room.z()+(ordinal==0?4:9));
            if(room.role()==InfiniteStructurePlan.Role.OBSERVATORY_HALL)at=new BlockPos(plan.x()+room.x()+3+ordinal*6,plan.y()+room.y()+6,plan.z()+room.z()+6);
            if(room.role()==InfiniteStructurePlan.Role.FALSE_PALACE_HALL)at=new BlockPos(plan.x()+(ordinal==0?-7:7),plan.y()+(ordinal==2?9:1),plan.z()+(ordinal==1?6:-6));
            result.add(new Assignment(id,kinds.get(ordinal),at));
        }
        return List.copyOf(result);
    }
    static boolean validJournal(CompoundTag journal,List<Assignment> assignments){
        if(journal.isEmpty())return true;
        if(!journal.contains("Version",Tag.TAG_INT)||journal.getInt("Version")!=1||!journal.contains("Issued",Tag.TAG_LIST))return false;
        var raw=journal.getList("Issued",Tag.TAG_INT_ARRAY);if(raw.size()!=((ListTag)journal.get("Issued")).size()||raw.size()>assignments.size())return false;
        var allowed=new HashSet<UUID>();for(var assignment:assignments)allowed.add(assignment.id());
        var seen=new HashSet<UUID>();
        for(var entry:raw){
            if(!(entry instanceof IntArrayTag array)||array.getAsIntArray().length!=4)return false;
            var id=NbtUtils.loadUUID(array);if(!allowed.contains(id)||!seen.add(id))return false;
        }
        return true;
    }
    static void place(WorldGenLevel level,BoundingBox clip,InfiniteStructurePlan.Plan plan,int module,CompoundTag journal){
        // A caller-supplied piece/generator is not authority to seed original
        // dimensions. Actual level binding must match the saved generation.
        var server=level.getLevel();
        if(!(server.getChunkSource().getGenerator() instanceof InfiniteChunkGenerator generator))return;
        try{var bound=generator.structureGeneration();if(!bound.id().equals(plan.generation())||bound.seed()!=plan.seed())return;}
        catch(IllegalStateException unbound){return;}
        var assignments=assignments(plan,module);if(!validJournal(journal,assignments))return;
        if(journal.isEmpty()){journal.putInt("Version",1);journal.put("Issued",new ListTag());}
        var issued=journal.getList("Issued",Tag.TAG_INT_ARRAY);
        for(var assignment:assignments){
            if(!clip.isInside(assignment.at())||issued.stream().anyMatch(raw->NbtUtils.loadUUID(raw).equals(assignment.id())))continue;
            var existing=level instanceof net.minecraft.server.level.ServerLevel direct?direct.getEntity(assignment.id()):null;
            if(existing!=null){
                // Preserve foreign/ambiguous identity collisions. An already
                // issued own defender may be reconciled without another spawn.
                var n=existing.getPersistentData().getCompound("CoralineSiteGuard");
                if(n.hasUUID("Site")&&n.hasUUID("Generation")&&plan.site().equals(n.getUUID("Site"))&&plan.generation().equals(n.getUUID("Generation")))issued.add(NbtUtils.createUUID(assignment.id()));
                continue;
            }
            var mob=PostgameHostiles.TYPES.get(assignment.kind()).get().create(server);if(mob==null)continue;
            mob.setUUID(assignment.id());mob.moveTo(assignment.at().getX()+.5,assignment.at().getY(),assignment.at().getZ()+.5,0,0);
            mob.finalizeSpawn(level,level.getCurrentDifficultyAt(assignment.at()),MobSpawnType.STRUCTURE,null,null);mob.setPersistenceRequired();
            var receipt=new CompoundTag();receipt.putInt("Version",1);receipt.putUUID("Site",plan.site());receipt.putUUID("Generation",plan.generation());receipt.putInt("Module",module);
            mob.getPersistentData().put("CoralineSiteGuard",receipt);
            if(level.addFreshEntity(mob))issued.add(NbtUtils.createUUID(assignment.id()));else mob.discard();
        }
    }
    private InfiniteSiteGuards(){}
}
