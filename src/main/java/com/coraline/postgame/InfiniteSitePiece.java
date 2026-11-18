package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** A saved module is clipped to Minecraft's current generation chunk. Existing
 * completed chunks are not retrofitted. Unknown/future piece metadata survives
 * loading but authorizes neither placement nor future quest operations. */
final class InfiniteSitePiece extends StructurePiece {
    private InfiniteStructurePlan.Plan plan;
    private int index;
    private CompoundTag opaque;
    private CompoundTag extra=new CompoundTag();
    private CompoundTag journal=new CompoundTag();
    private CompoundTag guardJournal=new CompoundTag();
    private CompoundTag residentJournal=new CompoundTag();
    Optional<InfiniteStructurePlan.Plan> sourcePlan(){return Optional.ofNullable(plan);}
    InfiniteSitePiece(InfiniteStructurePlan.Plan plan,int index){
        super(InfiniteSiteStructures.MODULE.get(),index,box(plan,plan.modules().get(index)));this.plan=plan;this.index=index;
        if(index==0){journal.putBoolean("Completed",false);journal.putBoolean("ThreatActive",false);journal.putBoolean("HooksActive",false);journal.put("ImportantNpcs",new ListTag());}
    }
    InfiniteSitePiece(CompoundTag saved){
        super(InfiniteSiteStructures.MODULE.get(),saved);opaque=saved.copy();
        try{
            if(saved.getInt("SiteVersion")!=InfiniteStructurePlan.VERSION||!saved.hasUUID("Site")||!saved.hasUUID("Generation")||!saved.contains("Seed",Tag.TAG_LONG))return;
            var family=InfiniteStructurePlan.Family.valueOf(saved.getString("Family"));
            var candidate=InfiniteStructurePlan.at(saved.getUUID("Generation"),saved.getLong("Seed"),family,saved.getInt("AnchorX"),saved.getInt("AnchorZ")).orElse(null);
            int module=saved.getInt("Module");
            if(candidate==null||!candidate.site().equals(saved.getUUID("Site"))||candidate.y()!=saved.getInt("AnchorY")||candidate.variation()!=saved.getInt("Variation")||module<0||module>=candidate.modules().size())return;
            if(!sameBox(getBoundingBox(),box(candidate,candidate.modules().get(module))))return;
            plan=candidate;index=module;extra=saved.copy();opaque=null;
            if(saved.contains("GuardJournal",Tag.TAG_COMPOUND))guardJournal=saved.getCompound("GuardJournal").copy();
            else if(saved.contains("GuardJournal")){plan=null;opaque=saved.copy();return;}
            if(saved.contains("ResidentJournal",Tag.TAG_COMPOUND))residentJournal=saved.getCompound("ResidentJournal").copy();
            else if(saved.contains("ResidentJournal")){plan=null;opaque=saved.copy();return;}
            if(index==0&&saved.contains("Journal",Tag.TAG_COMPOUND))journal=saved.getCompound("Journal").copy();
        }catch(IllegalArgumentException failure){/* Preserve unfamiliar piece data. */}
    }
    private static boolean sameBox(BoundingBox a,BoundingBox b){return a.minX()==b.minX()&&a.minY()==b.minY()&&a.minZ()==b.minZ()&&a.maxX()==b.maxX()&&a.maxY()==b.maxY()&&a.maxZ()==b.maxZ();}
    private static BoundingBox box(InfiniteStructurePlan.Plan plan,InfiniteStructurePlan.Module module){
        int floor=plan.y()+module.y();return new BoundingBox(plan.x()+module.x(),floor-(module.role()==InfiniteStructurePlan.Role.CHIMNEY?0:plan.family()==InfiniteStructurePlan.Family.RIFT_DRILL_CENTER?12:8),plan.z()+module.z()-(module.role()==InfiniteStructurePlan.Role.TOWER?1:0),plan.x()+module.maxX(),floor+module.height()-1,plan.z()+module.maxZ());
    }
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag out){
        if(opaque!=null){out.merge(opaque.copy());return;}
        out.merge(extra.copy());
        out.putInt("SiteVersion",InfiniteStructurePlan.VERSION);out.putUUID("Site",plan.site());out.putUUID("Generation",plan.generation());out.putLong("Seed",plan.seed());out.putString("Family",plan.family().name());
        out.putInt("AnchorX",plan.x());out.putInt("AnchorY",plan.y());out.putInt("AnchorZ",plan.z());out.putInt("Variation",plan.variation());out.putInt("Module",index);
        out.putString("Biome",plan.biome().name());out.putInt("Difficulty",plan.family().difficulty);var hooks=new ListTag();for(String hook:plan.family().hooks)hooks.add(StringTag.valueOf(hook));out.put("QuestHooks",hooks);
        out.putString("LootTable",lootTable().toString());if(index==0)out.put("Journal",journal.copy());
        out.put("GuardJournal",guardJournal.copy());
        out.put("ResidentJournal",residentJournal.copy());
    }
    private ResourceLocation lootTable(){
        var role=plan.modules().get(index).role();
        String name=role==InfiniteStructurePlan.Role.HALL?"common_supply":plan.family()==InfiniteStructurePlan.Family.DOLL_WORKSHOP&&role==InfiniteStructurePlan.Role.WORKROOM?"thread_workroom":plan.family().id();
        if(role==InfiniteStructurePlan.Role.CONTROL&&(plan.family()==InfiniteStructurePlan.Family.DOLL_WORKSHOP||plan.family()==InfiniteStructurePlan.Family.CLOCKWORK_FACTORY||plan.family()==InfiniteStructurePlan.Family.RIFT_DRILL_CENTER))name=plan.family().id()+"_control";
        if(role==InfiniteStructurePlan.Role.ORE_VAULT&&plan.family()==InfiniteStructurePlan.Family.SOUL_GLASS_MINE)name="soul_glass_mine_vault";
        if(role==InfiniteStructurePlan.Role.CONTROL&&plan.family()==InfiniteStructurePlan.Family.RIFT_SOLDIER_OUTPOST)name="rift_soldier_outpost_control";
        if(role==InfiniteStructurePlan.Role.CONTROL&&plan.family()==InfiniteStructurePlan.Family.ABANDONED_VILLAGE)name="abandoned_village_crypt";
        if(role==InfiniteStructurePlan.Role.ROOT_SANCTUM)name="rootbound_temple_sanctum";
        if(role==InfiniteStructurePlan.Role.RESIDENCE&&plan.family()==InfiniteStructurePlan.Family.RESISTANCE_SANCTUARY)name="resistance_sanctuary_supplies";
        if(role==InfiniteStructurePlan.Role.CARNIVAL_SUPPLIES)name="endless_carnival_supplies";
        return ResourceLocation.fromNamespaceAndPath("coraline","postgame/structures/"+name);
    }
    @Override public void postProcess(WorldGenLevel level,StructureManager structures,ChunkGenerator generator,RandomSource ignored,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
        if(plan==null||!(generator instanceof InfiniteChunkGenerator infinite))return;
        var bound=infinite.structureGeneration();if(!bound.id().equals(plan.generation())||bound.seed()!=plan.seed())return;
        var module=plan.modules().get(index);var role=module.role();int floor=plan.y()+module.y();
        if(role==InfiniteStructurePlan.Role.PERFORMER_DAIS){for(int x=0;x<module.width();x++)for(int z=0;z<module.depth();z++)place(level,clip,new BlockPos(plan.x()+module.x()+x,floor,plan.z()+module.z()+z),Blocks.SMOOTH_QUARTZ.defaultBlockState());InfiniteSiteResidents.place(level,clip,plan,index,residentJournal);return;}
        boolean submerged=plan.family()==InfiniteStructurePlan.Family.SUNKEN_OTHERWORLD_RUINS;
        boolean derelict=plan.family()==InfiniteStructurePlan.Family.ABANDONED_VILLAGE;
        BlockState wall=(plan.family()==InfiniteStructurePlan.Family.DOLL_WORKSHOP?InfiniteContent.block("buttonwood_planks"):plan.family()==InfiniteStructurePlan.Family.CLOCKWORK_FACTORY||plan.family()==InfiniteStructurePlan.Family.RIFT_DRILL_CENTER?Blocks.POLISHED_DEEPSLATE:plan.family()==InfiniteStructurePlan.Family.RIFT_SOLDIER_OUTPOST?Blocks.DEEPSLATE_BRICKS:plan.family()==InfiniteStructurePlan.Family.MIRROR_TOWER?InfiniteContent.block("mirrorstone"):InfiniteContent.block("threadbound_rock")).defaultBlockState();
        if(submerged)wall=Blocks.PRISMARINE_BRICKS.defaultBlockState();
        if(plan.family()==InfiniteStructurePlan.Family.PORCELAIN_MANSION)wall=InfiniteContent.block("porcelain_stone").defaultBlockState();
        if(plan.family()==InfiniteStructurePlan.Family.OTHER_VILLAGE)wall=InfiniteContent.block("paperwood_planks").defaultBlockState();
        if(derelict)wall=InfiniteContent.block("paperwood_planks").defaultBlockState();
        if(plan.family()==InfiniteStructurePlan.Family.ROOTBOUND_TEMPLE)wall=Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        if(plan.family()==InfiniteStructurePlan.Family.ANCIENT_BOUNDARY_SHRINE)wall=Blocks.SMOOTH_QUARTZ.defaultBlockState();
        if(plan.family()==InfiniteStructurePlan.Family.RESISTANCE_SANCTUARY)wall=InfiniteContent.block("lanternwood_planks").defaultBlockState();
        if(plan.family()==InfiniteStructurePlan.Family.FORGOTTEN_OBSERVATORY)wall=InfiniteContent.block("clockwork_stone").defaultBlockState();
        if(plan.family()==InfiniteStructurePlan.Family.FALSE_PINK_PALACE)wall=Blocks.PINK_TERRACOTTA.defaultBlockState();
        if(plan.family()==InfiniteStructurePlan.Family.ENTITY_HEART_CHAMBER)wall=Blocks.DEEPSLATE_TILES.defaultBlockState();
        if(plan.family()==InfiniteStructurePlan.Family.ABANDONED_ORPHANAGE)wall=Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        if(plan.family()==InfiniteStructurePlan.Family.ENDLESS_CARNIVAL)wall=Blocks.PINK_TERRACOTTA.defaultBlockState();
        if(plan.family()==InfiniteStructurePlan.Family.GRAND_THEATRE)wall=InfiniteContent.block("porcelain_stone").defaultBlockState();
        var foundation=(submerged?Blocks.MOSSY_STONE_BRICKS:Blocks.DEEPSLATE_BRICKS).defaultBlockState();var roof=plan.family()==InfiniteStructurePlan.Family.DOLL_WORKSHOP?Blocks.DARK_OAK_PLANKS.defaultBlockState():submerged?Blocks.PRISMARINE_BRICKS.defaultBlockState():Blocks.DEEPSLATE_TILES.defaultBlockState();
        var natural=role==InfiniteStructurePlan.Role.APPROACH||role==InfiniteStructurePlan.Role.TOWER?new InfiniteTerrain(plan.seed()):null;
        int approachRise=role==InfiniteStructurePlan.Role.APPROACH?Math.max(0,floor-natural.column(plan.x()+module.x()+module.width()/2,plan.z()+module.z()).surfaceY()):0;
        for(int x=0;x<module.width();x++)for(int z=0;z<module.depth();z++){
            int walk=role==InfiniteStructurePlan.Role.STAIRS?floor+18-Math.min(18,z):role==InfiniteStructurePlan.Role.APPROACH?floor-Math.min(approachRise,module.depth()-1-z):floor;
            for(int y=getBoundingBox().minY();y<=getBoundingBox().maxY();y++){
                var at=new BlockPos(plan.x()+module.x()+x,y,plan.z()+module.z()+z);if(!clip.isInside(at))continue;
                // Native chunk stages normally call each module once. Also
                // preserve any already-existing container if a generation
                // callback is repeated, instead of refilling its lazy loot.
                if(level.getBlockEntity(at)!=null)continue;
                boolean edge=x==0||z==0||x==module.width()-1||z==module.depth()-1;
                boolean doorway=InfiniteStructurePlan.doorway(module,x,z);
                BlockState state;
                if(y<walk)state=foundation;
                else if(y==walk)state=role==InfiniteStructurePlan.Role.APPROACH&&module.depth()-1-z<=approachRise?Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,net.minecraft.core.Direction.SOUTH):wall;
                else if(role==InfiniteStructurePlan.Role.APPROACH||role==InfiniteStructurePlan.Role.VILLAGE_SQUARE)state=Blocks.AIR.defaultBlockState();
                else if(y==getBoundingBox().maxY())state=roof;
                else if(edge&&!(doorway&&y<=walk+2)){
                    boolean window=role!=InfiniteStructurePlan.Role.ORE_VAULT&&role!=InfiniteStructurePlan.Role.STAIRS&&role!=InfiniteStructurePlan.Role.CHIMNEY&&y==walk+3&&(x+z)%3==0;
                    state=window?Blocks.PURPLE_STAINED_GLASS.defaultBlockState():wall;
                    if(role==InfiniteStructurePlan.Role.ORE_VAULT&&y<=walk+3&&(x*17+z*7+y)%13==0)state=com.coraline.registry.ModBlocks.SOULGLASS_ORE.get().defaultBlockState();
                }else state=Blocks.AIR.defaultBlockState();
                if(submerged){
                    boolean brokenArch=edge&&y>walk+2&&y<getBoundingBox().maxY()&&(x+z)%3!=0;
                    boolean brokenRoof=y==getBoundingBox().maxY()&&(x*7+z*11+plan.variation())%5==0;
                    if(state.isAir()||brokenArch||brokenRoof)state=Blocks.WATER.defaultBlockState();
                }
                if(derelict){
                    if(y==walk&&role==InfiniteStructurePlan.Role.VILLAGE_SQUARE)state=Blocks.MOSSY_COBBLESTONE.defaultBlockState();
                    if(role==InfiniteStructurePlan.Role.RUINED_RESIDENCE&&y>walk&&((y==getBoundingBox().maxY()&&(x*7+z*3+plan.variation())%4==0)||(edge&&y<=walk+3&&(x*3+z*7+plan.variation())%7==0)))state=Blocks.AIR.defaultBlockState();
                }
                level.setBlock(at,state,2);
            }
        }
        if(role==InfiniteStructurePlan.Role.VILLAGE_SQUARE){
            if(module.width()==9&&module.depth()==9){
                for(int x=2;x<=6;x++)for(int z=2;z<=6;z++)place(level,clip,new BlockPos(plan.x()+module.x()+x,floor+1,plan.z()+module.z()+z),x==2||x==6||z==2||z==6?(derelict?Blocks.MOSSY_STONE_BRICKS:Blocks.SMOOTH_QUARTZ).defaultBlockState():Blocks.WATER.defaultBlockState());
                place(level,clip,new BlockPos(plan.x()+module.x()+4,floor,plan.z()+module.z()+4),Blocks.SEA_LANTERN.defaultBlockState());
            }return;
        }
        if(role==InfiniteStructurePlan.Role.APPROACH)WellScarLayout.place(level,clip,plan,index);
        if(role==InfiniteStructurePlan.Role.CHIMNEY||role==InfiniteStructurePlan.Role.STAIRS||role==InfiniteStructurePlan.Role.CORRIDOR||role==InfiniteStructurePlan.Role.APPROACH)return;
        if(role==InfiniteStructurePlan.Role.TOWER){
            for(int y=1;y<=12;y++){
                place(level,clip,new BlockPos(plan.x()+module.x()+1,floor+y,plan.z()+module.z()),wall);
                place(level,clip,new BlockPos(plan.x()+module.x()+1,floor+y,plan.z()+module.z()+1),Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,net.minecraft.core.Direction.SOUTH));
            }
            for(int x=1;x<=3;x++)for(int z=1;z<=3;z++)if(x!=1||z!=1)place(level,clip,new BlockPos(plan.x()+module.x()+x,floor+8,plan.z()+module.z()+z),roof);
            for(int p=1;p<=3;p++)for(int y=10;y<=11;y++)for(var at:List.of(new BlockPos(plan.x()+module.x()+p,floor+y,plan.z()+module.z()),new BlockPos(plan.x()+module.x()+p,floor+y,plan.z()+module.maxZ()),new BlockPos(plan.x()+module.x(),floor+y,plan.z()+module.z()+p),new BlockPos(plan.x()+module.maxX(),floor+y,plan.z()+module.z()+p)))place(level,clip,at,Blocks.IRON_BARS.defaultBlockState());
            var outside=new BlockPos(plan.x()+module.x()+1,0,plan.z()+module.z()-1);int ground=natural.column(outside.getX(),outside.getZ()).surfaceY();
            for(int y=ground+1;y<=floor+1;y++)place(level,clip,new BlockPos(outside.getX(),y,outside.getZ()),Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,net.minecraft.core.Direction.NORTH));
            place(level,clip,new BlockPos(outside.getX()+1,floor,outside.getZ()),wall);
        }
        place(level,clip,new BlockPos(plan.x()+module.x()+1,floor+1,plan.z()+module.z()+module.depth()-2),(submerged?Blocks.SEA_LANTERN:Blocks.LANTERN).defaultBlockState());
        if(role==InfiniteStructurePlan.Role.WORKROOM){
            place(level,clip,new BlockPos(plan.x()+module.x()+module.width()-2,floor+1,plan.z()+module.z()+module.depth()-2),(plan.family()==InfiniteStructurePlan.Family.CLOCKWORK_FACTORY?Blocks.BLAST_FURNACE:Blocks.LOOM).defaultBlockState());
            place(level,clip,new BlockPos(plan.x()+module.x()+module.width()-2,floor+1,plan.z()+module.z()+1),Blocks.CRAFTING_TABLE.defaultBlockState());
            for(var at:SiteBindingReceipt.cells(plan,index))if(clip.isInside(at)&&level.getBlockEntity(at)==null){
                level.setBlock(at,com.coraline.registry.ModBlocks.INFINITE_THREAD_BINDING.get().defaultBlockState(),2);
                if(level.getBlockEntity(at) instanceof SiteBindingBlockEntity binding)binding.configure(plan,index);
            }
        }
        if(plan.family()==InfiniteStructurePlan.Family.RIFT_SOLDIER_OUTPOST){
            if(role==InfiniteStructurePlan.Role.WORKROOM){
                for(int bed=0;bed<3;bed++){
                    var at=new BlockPos(plan.x()+module.x()+2+bed*2,floor+1,plan.z()+module.z()+module.depth()-4);
                    var foot=Blocks.BLACK_BED.defaultBlockState().setValue(BedBlock.FACING,net.minecraft.core.Direction.SOUTH).setValue(BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.FOOT);
                    place(level,clip,at,foot);place(level,clip,at.south(),foot.setValue(BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.HEAD));
                }
            }
            if(role==InfiniteStructurePlan.Role.CONTROL){
                for(int x=3;x<module.width()-2;x++)place(level,clip,new BlockPos(plan.x()+module.x()+x,floor+1,plan.z()+module.z()+module.depth()-3),Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
            }
            if(role==InfiniteStructurePlan.Role.STORAGE)place(level,clip,new BlockPos(plan.x()+module.maxX()-1,floor+1,plan.z()+module.maxZ()-1),Blocks.ANVIL.defaultBlockState());
        }
        MirrorTowerLayout.place(level,clip,plan,index);
        PorcelainMansionLayout.place(level,clip,plan,index);
        ObservatoryLayout.place(level,clip,plan,index);
        PerformanceLayout.place(level,clip,plan,index);
        CarnivalLayout.place(level,clip,plan,index);
        OrphanageLayout.place(level,clip,plan,index);
        HeartChamberLayout.place(level,clip,plan,index);
        FalsePalaceLayout.place(level,clip,plan,index);
        if(role==InfiniteStructurePlan.Role.SEAM_CHAMBER)SeamLayout.place(level,clip,plan);
        if(role==InfiniteStructurePlan.Role.DEFENSE_HALL){
            var core=BoundaryDefenseLayout.core(plan);if(clip.isInside(core)&&level.getBlockEntity(core)==null){level.setBlock(core,com.coraline.registry.ModBlocks.BOUNDARY_BEACON.get().defaultBlockState(),2);if(level.getBlockEntity(core) instanceof BoundaryDefenseEntity defense)defense.configure(plan);}
        }
        if(role==InfiniteStructurePlan.Role.ROOT_SANCTUM){
            for(int x:new int[]{2,module.width()-3})for(int z:new int[]{2,module.depth()-3}){
                for(int y=1;y<=6;y++)place(level,clip,new BlockPos(plan.x()+module.x()+x,floor+y,plan.z()+module.z()+z),InfiniteContent.block("buttonwood_log").defaultBlockState());
                for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)place(level,clip,new BlockPos(plan.x()+module.x()+x+dx,floor+6,plan.z()+module.z()+z+dz),InfiniteContent.block("lanternwood_leaves").defaultBlockState().setValue(LeavesBlock.PERSISTENT,true));
            }
            // Roots arch above, rather than across, the player route.
            for(int x=2;x<module.width()-2;x++)place(level,clip,new BlockPos(plan.x()+module.x()+x,floor+5,plan.z()+module.z()+module.depth()/2),InfiniteContent.block("buttonwood_log").defaultBlockState().setValue(RotatedPillarBlock.AXIS,net.minecraft.core.Direction.Axis.X));
            for(int x=3;x<module.width()-3;x++)place(level,clip,new BlockPos(plan.x()+module.x()+x,floor,plan.z()+module.z()+module.depth()/2),Blocks.MOSS_BLOCK.defaultBlockState());
        }
        if(role==InfiniteStructurePlan.Role.RESIDENCE){
            var at=new BlockPos(plan.x()+module.x()+2,floor+1,plan.z()+module.z()+module.depth()-3);
            var bed=Blocks.PURPLE_BED.defaultBlockState().setValue(BedBlock.FACING,net.minecraft.core.Direction.SOUTH).setValue(BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.FOOT);
            place(level,clip,at,bed);place(level,clip,at.south(),bed.setValue(BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.HEAD));
            place(level,clip,new BlockPos(plan.x()+module.maxX()-1,floor+1,plan.z()+module.maxZ()-1),(InfiniteSiteResidents.role(plan,index)==1?Blocks.BLAST_FURNACE:InfiniteSiteResidents.role(plan,index)==2?Blocks.LOOM:Blocks.CRAFTING_TABLE).defaultBlockState());
        }
        if(role==InfiniteStructurePlan.Role.RUINED_RESIDENCE){
            place(level,clip,new BlockPos(plan.x()+module.x()+3,floor+1,plan.z()+module.z()+5),Blocks.COBWEB.defaultBlockState());
            if(index==1){
                var lectern=new BlockPos(plan.x()+module.x()+2,floor+1,plan.z()+module.z()+2);
                if(clip.isInside(lectern)&&level.getBlockEntity(lectern)==null){
                    level.setBlock(lectern,Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.HAS_BOOK,true),2);
                    if(level.getBlockEntity(lectern) instanceof net.minecraft.world.level.block.entity.LecternBlockEntity holder){
                        var book=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITTEN_BOOK);var tag=book.getOrCreateTag();tag.putString("title","Ledger of Empty Houses");tag.putString("author","The Last Clerk");var pages=new ListTag();pages.add(StringTag.valueOf(net.minecraft.network.chat.Component.Serializer.toJson(net.minecraft.network.chat.Component.literal("Every house was counted twice. Every name was written in the same hand.\n\nWhen the lamps went out, the square stayed quiet.\n\nThe old stair behind the square leads beneath the roots. The archive was the last room we locked."))));tag.put("pages",pages);holder.setBook(book);holder.setChanged();
                    }
                }
            }
        }
        if(role==InfiniteStructurePlan.Role.FALSE_PALACE_HALL||role==InfiniteStructurePlan.Role.ORPHAN_VAULT||role==InfiniteStructurePlan.Role.CARNIVAL_SUPPLIES||role==InfiniteStructurePlan.Role.CARNIVAL_VAULT||role==InfiniteStructurePlan.Role.HALL||role==InfiniteStructurePlan.Role.STORAGE||role==InfiniteStructurePlan.Role.CONTROL||role==InfiniteStructurePlan.Role.ORE_VAULT||role==InfiniteStructurePlan.Role.MIRROR_SHAFT||role==InfiniteStructurePlan.Role.RUIN_CHAMBER||role==InfiniteStructurePlan.Role.MANSION_HALL||role==InfiniteStructurePlan.Role.RESIDENCE||role==InfiniteStructurePlan.Role.RUINED_RESIDENCE||role==InfiniteStructurePlan.Role.ROOT_SANCTUM||role==InfiniteStructurePlan.Role.DEFENSE_VAULT||role==InfiniteStructurePlan.Role.RESEARCH_VAULT||role==InfiniteStructurePlan.Role.PERFORMANCE_VAULT||plan.family()==InfiniteStructurePlan.Family.DOLL_WORKSHOP&&role==InfiniteStructurePlan.Role.WORKROOM){
            var chest=SiteCacheReceipt.position(plan,index);
            if(clip.isInside(chest)){
                if(level.getBlockEntity(chest)==null){
                    boolean guarded=role!=InfiniteStructurePlan.Role.CARNIVAL_SUPPLIES&&role!=InfiniteStructurePlan.Role.HALL&&role!=InfiniteStructurePlan.Role.WORKROOM&&role!=InfiniteStructurePlan.Role.RESIDENCE&&role!=InfiniteStructurePlan.Role.RUINED_RESIDENCE;
                    var cacheState=(guarded?com.coraline.registry.ModBlocks.INFINITE_SITE_CACHE.get():Blocks.CHEST).defaultBlockState();
                    if(submerged&&cacheState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED))cacheState=cacheState.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,true);
                    level.setBlock(chest,cacheState,2);
                    if(level.getBlockEntity(chest) instanceof SiteCacheBlockEntity cache)cache.configure(plan,index);
                    if(level.getBlockEntity(chest) instanceof RandomizableContainerBlockEntity container)container.setLootTable(lootTable(),plan.site().getLeastSignificantBits()^index*0x632be59bd9b4e019L);
                }
            }
        }
        if(plan.family()==InfiniteStructurePlan.Family.RIFT_DRILL_CENTER){
            var machines=RiftMachineLayout.parts(plan);
            for(int i=0;i<machines.size();i++){
                var part=machines.get(i);var at=part.at();if(!getBoundingBox().isInside(at)||!clip.isInside(at)||level.getBlockEntity(at)!=null)continue;
                level.setBlock(at,RiftMachineBlock.block(part.kind()).defaultBlockState(),2);
                if(level.getBlockEntity(at) instanceof RiftMachineEntity machine)machine.configure(plan,i);
            }
            if(role==InfiniteStructurePlan.Role.DRILL_CHAMBER){
                var core=machines.get(0).at();for(int dx:new int[]{-2,2})for(int dz:new int[]{-2,2})for(int y=0;y<=5;y++)place(level,clip,core.offset(dx,y,dz),Blocks.IRON_BARS.defaultBlockState());
            }
        }
        FactoryLayout.place(level,clip,plan,index);
        InfiniteSiteGuards.place(level,clip,plan,index,guardJournal);
        InfiniteSiteResidents.place(level,clip,plan,index,residentJournal);
    }
    private static void place(WorldGenLevel level,BoundingBox clip,BlockPos at,BlockState state){if(clip.isInside(at)&&level.getBlockEntity(at)==null)level.setBlock(at,state,2);}
}
