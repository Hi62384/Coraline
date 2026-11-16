package com.coraline.postgame;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** One cut opens one generated cell permanently, with no block item/drop to
 * copy its receipt. A normal player passage needs two cuts, not an entire room. */
public final class SiteBindingBlockEntity extends BlockEntity {
    private CompoundTag receipt=new CompoundTag();
    private Tag opaque;
    private SiteBindingReceipt.View parsed;
    public SiteBindingBlockEntity(BlockPos at,BlockState state){super(com.coraline.registry.ModBlockEntities.INFINITE_THREAD_BINDING.get(),at,state);}
    void configure(InfiniteStructurePlan.Plan plan,int module){
        if(opaque!=null||!receipt.isEmpty())return;var created=SiteBindingReceipt.create(plan,module,worldPosition);if(created.isEmpty())return;
        receipt=created.orElseThrow();parsed=SiteBindingReceipt.read(receipt,worldPosition).orElse(null);setChanged();
    }
    boolean authentic(){
        if(parsed==null||isRemoved()||level==null||!level.hasChunkAt(worldPosition)||level.getBlockEntity(worldPosition)!=this||!getBlockState().is(com.coraline.registry.ModBlocks.INFINITE_THREAD_BINDING.get())
            ||!(level.getChunkSource() instanceof net.minecraft.server.level.ServerChunkCache chunks)||!(chunks.getGenerator() instanceof InfiniteChunkGenerator generator))return false;
        try{var bound=generator.structureGeneration();return parsed.generation().equals(bound.id())&&parsed.seed()==bound.seed();}
        catch(IllegalStateException unbound){return false;}
    }
    boolean alreadyCut(){return parsed!=null&&parsed.cut();}
    boolean cut(){
        if(alreadyCut()||!authentic()||getBlockState().getValue(SiteThreadBinding.OPEN))return false;
        if(!level.setBlock(worldPosition,getBlockState().setValue(SiteThreadBinding.OPEN,true),3))return false;
        receipt.putBoolean("Cut",true);parsed=new SiteBindingReceipt.View(parsed.site(),parsed.generation(),parsed.seed(),true);setChanged();return true;
    }
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.put("CoralineThreadBinding",opaque==null?receipt.copy():opaque.copy());}
    @Override public void load(CompoundTag n){super.load(n);var raw=n.get("CoralineThreadBinding");opaque=raw!=null&&!(raw instanceof CompoundTag)?raw.copy():null;receipt=n.getCompound("CoralineThreadBinding").copy();parsed=opaque==null?SiteBindingReceipt.read(receipt,worldPosition).orElse(null):null;}
}
