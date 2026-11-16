package com.coraline.postgame;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class WellScarClueEntity extends BlockEntity {
    private Tag raw=new CompoundTag();
    public WellScarClueEntity(BlockPos at,BlockState state){super(com.coraline.registry.ModBlockEntities.WELL_SCAR_CLUE.get(),at,state);}
    void configure(InfiniteStructurePlan.Plan plan,int clue){if(!(raw instanceof CompoundTag n)||!n.isEmpty())return;var tag=SeamLayout.metadata(plan,0);tag.putInt("Clue",clue);raw=tag;setChanged();}
    boolean matches(InfiniteStructurePlan.Plan p,int clue){return raw instanceof CompoundTag n&&n.contains("Version",Tag.TAG_INT)&&n.getInt("Version")==1&&n.contains("Clue",Tag.TAG_INT)&&n.getInt("Clue")==clue&&n.hasUUID("Generation")&&p.generation().equals(n.getUUID("Generation"))&&n.hasUUID("Site")&&p.site().equals(n.getUUID("Site"))&&WellScarLayout.clue(p,clue).equals(worldPosition);}
    void read(ServerPlayer p){if(!(raw instanceof CompoundTag n)||!n.contains("Version",Tag.TAG_INT)||n.getInt("Version")!=1||!n.contains("Clue",Tag.TAG_INT)||n.getInt("Clue")<0||n.getInt("Clue")>3||p.level()!=level||!p.isAlive()||p.distanceToSqr(worldPosition.getCenter())>36||!level.mayInteract(p,worldPosition)||!(level instanceof ServerLevel server)||server.getBlockEntity(worldPosition)!=this||!getBlockState().is(com.coraline.registry.ModBlocks.WELL_SCAR_CLUE.get())||getBlockState().getValue(WellScarClueBlock.CLUE)!=n.getInt("Clue")||!(server.getChunkSource().getGenerator() instanceof InfiniteChunkGenerator generator))return;var plan=FalsePalaceLayout.read(n).orElse(null);if(plan==null||!WellScarLayout.clue(plan,n.getInt("Clue")).equals(worldPosition)||(!generator.structureGeneration().id().equals(plan.generation())||generator.structureGeneration().seed()!=plan.seed())||!InfiniteWorldData.get(p.server).matchesGeneration(plan.generation()))return;WybieRescue.clue(p,plan,n.getInt("Clue"));}
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.put("CoralineWellScarClue",raw.copy());}
    @Override public void load(CompoundTag n){super.load(n);raw=n.contains("CoralineWellScarClue")?n.get("CoralineWellScarClue").copy():new CompoundTag();}
}
