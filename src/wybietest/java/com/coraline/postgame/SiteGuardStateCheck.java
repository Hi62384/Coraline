package com.coraline.postgame;

import java.util.*;
import java.nio.file.*;
import java.util.function.Consumer;
import net.minecraft.nbt.*;

/** Assignment and real NBT persistence only. Does not bootstrap Minecraft,
 * spawn entities, certify combat or bypass the native-game memory floor. */
public final class SiteGuardStateCheck {
    private static int checks;
    private static void require(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void reject(CompoundTag original,List<InfiniteSiteGuards.Assignment> assignments,Consumer<CompoundTag> corrupt){
        var copy=original.copy();corrupt.accept(copy);var before=copy.copy();
        require(!InfiniteSiteGuards.validJournal(copy,assignments),"Invalid/future guard issuance was accepted: "+copy);
        require(before.equals(copy),"Rejected journal was overwritten");
    }
    public static void main(String[] args)throws Exception{
        var directory=Files.createTempDirectory(Path.of("build-character-cast"),"site-guard-state-");
        var epoch=UUID.fromString("11111111-2222-3333-4444-555555555555");var all=new HashSet<UUID>();int files=0;
        for(var family:InfiniteStructurePlan.Family.values()){
            InfiniteStructurePlan.Plan plan=null;
            for(int x=-12288;x<=12288&&plan==null;x+=768)for(int z=-12288;z<=12288;z+=768){var p=InfiniteStructurePlan.at(epoch,124119,family,x,z);if(p.isPresent()){plan=p.orElseThrow();break;}}
            require(plan!=null,"No eligible actual family fixture");int total=0;
            for(int module=0;module<plan.modules().size();module++){
                var assignments=InfiniteSiteGuards.assignments(plan,module);var room=plan.modules().get(module);
                require(assignments.equals(InfiniteSiteGuards.assignments(plan,module)),"Reload/order changed assignment identity");
                require(assignments.size()<=(room.role()==InfiniteStructurePlan.Role.MANSION_HALL?4:3),"Unbounded per-module defender count");total+=assignments.size();
                var journal=new CompoundTag();require(InfiniteSiteGuards.validJournal(journal,assignments),"Fresh assignment journal rejected");
                journal.putInt("Version",1);var issued=new ListTag();journal.put("Issued",issued);
                for(var guard:assignments){
                    require(all.add(guard.id()),"Guard UUID collided across module/family");var at=guard.at();
                    int ordinal=issued.size();
                    int height=switch(room.role()){
                        case TOWER->9;
                        case MIRROR_SHAFT->6+10*ordinal;
                        case MANSION_HALL->ordinal==3?9:1;
                        case OBSERVATORY_HALL->6;
                        case FALSE_PALACE_HALL->ordinal==2?9:1;
                        default->1;
                    };
                    require(at.getX()>plan.x()+room.x()&&at.getX()<plan.x()+room.maxX()
                            &&at.getZ()>plan.z()+room.z()&&at.getZ()<plan.z()+room.maxZ()
                            &&at.getY()==plan.y()+room.y()+height,"Defender placed in wall/outside room/floor for "+family);
                    issued.add(NbtUtils.createUUID(guard.id()));require(InfiniteSiteGuards.validJournal(journal,assignments),"Partial issuance could not resume");
                }
                journal.putString("UnknownExtension","preserve");var before=journal.copy();
                require(InfiniteSiteGuards.validJournal(journal,assignments)&&journal.equals(before),"Valid journal mutated during read");
                var disk=directory.resolve("guards-"+files+++".dat");NbtIo.writeCompressed(journal,disk.toFile());var loaded=NbtIo.readCompressed(disk.toFile());
                require(loaded.equals(journal)&&InfiniteSiteGuards.validJournal(loaded,assignments),"Actual compressed NBT lost issued guard IDs");
                reject(journal,assignments,n->n.putInt("Version",2));reject(journal,assignments,n->n.putDouble("Version",1));
                reject(journal,assignments,n->n.remove("Issued"));reject(journal,assignments,n->n.putString("Issued","future"));
                reject(journal,assignments,n->{var wrong=new ListTag();wrong.add(StringTag.valueOf("not a UUID"));n.put("Issued",wrong);});
                reject(journal,assignments,n->{var wrong=new ListTag();wrong.add(NbtUtils.createUUID(UUID.randomUUID()));n.put("Issued",wrong);});
                if(!assignments.isEmpty()){
                    reject(journal,assignments,n->n.getList("Issued",Tag.TAG_INT_ARRAY).add(NbtUtils.createUUID(assignments.get(0).id())));
                    reject(journal,assignments,n->{var wrong=new ListTag();wrong.add(new IntArrayTag(new int[]{1,2,3}));n.put("Issued",wrong);});
                }
            }
            int expected=switch(family){
                case DOLL_WORKSHOP,FORGOTTEN_OBSERVATORY,ABANDONED_ORPHANAGE->2;
                case CLOCKWORK_FACTORY,SOUL_GLASS_MINE,MIRROR_TOWER,DIMENSIONAL_SEAM,ROOTBOUND_TEMPLE,FALSE_PINK_PALACE->3;
                case PORCELAIN_MANSION,ABANDONED_VILLAGE->4;
                case RIFT_DRILL_CENTER->10;
                case RIFT_SOLDIER_OUTPOST->8;
                case SUNKEN_OTHERWORLD_RUINS,OTHER_VILLAGE,ANCIENT_BOUNDARY_SHRINE,RESISTANCE_SANCTUARY,ENTITY_HEART_CHAMBER,ENDLESS_CARNIVAL,GRAND_THEATRE->0;
            };
            require(total==expected,"Family defender budget changed for "+family+": "+total);
        }
        System.out.println("GUARD_FILES="+files+" UNIQUE_GUARDS="+all.size()+" DIRECTORY="+directory);
        System.out.println("SITE_GUARD_STATE_NBT_OK checks="+checks);
    }
    private SiteGuardStateCheck(){}
}
